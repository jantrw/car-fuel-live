<#
.SYNOPSIS
Seeds the local PostgreSQL database with the initial location dataset.

.DESCRIPTION
Use this script after creating a new or empty database, or when the location dataset
must be refreshed deliberately. It is not part of the normal application runtime.
#>

param(
    [string]$BackendDirectory = (Split-Path -Parent $PSScriptRoot),
    [string]$PostgresServiceName = 'postgres'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Normalize-SearchText {
    param(
        [AllowNull()][string]$Value,
        [switch]$FoldGermanicDigraphs
    )

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ''
    }

    $normalized = $Value.Trim().ToLowerInvariant()
    if ($FoldGermanicDigraphs) {
        $normalized = $normalized.Replace('ä', 'a')
        $normalized = $normalized.Replace('ö', 'o')
        $normalized = $normalized.Replace('ü', 'u')
    } else {
        $normalized = $normalized.Replace('ä', 'ae')
        $normalized = $normalized.Replace('ö', 'oe')
        $normalized = $normalized.Replace('ü', 'ue')
    }
    $normalized = $normalized.Replace('ß', 'ss')
    $normalized = $normalized.Replace('æ', 'ae')
    $normalized = $normalized.Replace('œ', 'oe')
    $normalized = $normalized.Replace('ø', 'o')
    $normalized = $normalized.Normalize([Text.NormalizationForm]::FormD)

    $builder = New-Object System.Text.StringBuilder
    foreach ($character in $normalized.ToCharArray()) {
        if ([Globalization.CharUnicodeInfo]::GetUnicodeCategory($character) -ne [Globalization.UnicodeCategory]::NonSpacingMark) {
            [void]$builder.Append($character)
        }
    }

    $collapsed = $builder.ToString().Normalize([Text.NormalizationForm]::FormC)
    $collapsed = [Regex]::Replace($collapsed, '[^a-z0-9]+', ' ')
    return [Regex]::Replace($collapsed, '\s+', ' ').Trim()
}

function Get-SearchTextVariants {
    param([AllowNull()][string]$Value)

    $variants = New-Object 'System.Collections.Generic.List[string]'
    $seen = New-Object 'System.Collections.Generic.HashSet[string]'

    foreach ($candidate in @(
        $(Normalize-SearchText $Value),
        $(Normalize-SearchText $Value -FoldGermanicDigraphs)
    )) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and $seen.Add($candidate)) {
            $variants.Add($candidate)
        }
    }

    return $variants
}

function Write-AliasStageRow {
    param(
        [System.IO.StreamWriter]$Writer,
        [System.Collections.Generic.HashSet[string]]$AliasDeduplication,
        [string]$PlaceGeonameId,
        [string]$AliasName,
        [string]$NormalizedAliasName
    )

    if ([string]::IsNullOrWhiteSpace($AliasName) -or [string]::IsNullOrWhiteSpace($NormalizedAliasName)) {
        return
    }

    $dedupeKey = "$PlaceGeonameId|$AliasName"
    if (-not $AliasDeduplication.Add($dedupeKey)) {
        return
    }

    $Writer.WriteLine((
        @(
            $PlaceGeonameId,
            $AliasName,
            $NormalizedAliasName
        ) -join "`t"
    ))
}

function Download-File {
    param(
        [string]$Url,
        [string]$Destination
    )

    Invoke-WebRequest -Uri $Url -OutFile $Destination
}

function Get-TabField {
    param(
        [string[]]$Fields,
        [int]$Index
    )

    if ($Fields.Length -le $Index) {
        return ''
    }

    return $Fields[$Index]
}

function Resolve-PostgresContainerId {
    param(
        [string]$ComposeFile,
        [string]$EnvironmentFile,
        [string]$ServiceName
    )

    $containerIdOutput = & docker compose --env-file $EnvironmentFile -f $ComposeFile ps -q $ServiceName
    if ($LASTEXITCODE -ne 0) {
        throw "Resolving the docker compose service '$ServiceName' failed."
    }
    $containerId = if ($null -eq $containerIdOutput) { '' } else { ($containerIdOutput -join "`n").Trim() }
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw "No running container found for docker compose service '$ServiceName'. Start the database first with 'docker compose -f $ComposeFile up -d'."
    }

    return $containerId
}

function Invoke-PostgresScalar {
    param(
        [string]$ContainerId,
        [string]$DatabaseUser,
        [string]$DatabasePassword,
        [string]$DatabaseName,
        [string]$Query,
        [string]$FailureMessage
    )

    $commandOutput = & docker exec -e PGPASSWORD=$DatabasePassword $ContainerId psql -t -A -v ON_ERROR_STOP=1 -U $DatabaseUser -d $DatabaseName -c $Query 2>&1
    if ($LASTEXITCODE -ne 0) {
        $details = if ($null -eq $commandOutput) { '' } else { "`n$($commandOutput -join "`n")" }
        throw "$FailureMessage$details"
    }
    $result = if ($null -eq $commandOutput) { '' } else { ($commandOutput -join "`n").Trim() }

    return $result
}

function Assert-LocationSchemaMigrated {
    param(
        [string]$ContainerId,
        [string]$DatabaseUser,
        [string]$DatabasePassword,
        [string]$DatabaseName
    )

    $historyTableExists = Invoke-PostgresScalar `
        -ContainerId $ContainerId `
        -DatabaseUser $DatabaseUser `
        -DatabasePassword $DatabasePassword `
        -DatabaseName $DatabaseName `
        -Query "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'flyway_schema_history');" `
        -FailureMessage 'Checking the Flyway schema history failed.'

    if ($historyTableExists -ne 't') {
        throw "Flyway has not created the location schema in this database. Start the backend first with '.\car-fuel-live-backend\gradlew.bat -p .\car-fuel-live-backend bootRun', then run this seed script."
    }

    $locationSchemaMigrated = Invoke-PostgresScalar `
        -ContainerId $ContainerId `
        -DatabaseUser $DatabaseUser `
        -DatabasePassword $DatabasePassword `
        -DatabaseName $DatabaseName `
        -Query "SELECT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '1' AND success = true);" `
        -FailureMessage 'Checking the location schema migration failed.'

    if ($locationSchemaMigrated -ne 't') {
        throw "Flyway migration V1 has not completed successfully in this database. Start the backend first with '.\car-fuel-live-backend\gradlew.bat -p .\car-fuel-live-backend bootRun', then run this seed script."
    }
}

$environmentFile = Join-Path $BackendDirectory '.env'
if (-not (Test-Path $environmentFile)) {
    throw "Missing backend .env file at $environmentFile. Copy .env.example to .env first."
}

$environmentValues = @{}
Get-Content $environmentFile | ForEach-Object {
    if ($_ -match '^\s*#' -or [string]::IsNullOrWhiteSpace($_)) {
        return
    }

    $parts = $_.Split('=', 2)
    if ($parts.Length -eq 2) {
        $environmentValues[$parts[0]] = $parts[1]
    }
}

$dbUser = $environmentValues['DB_USER']
$dbPassword = $environmentValues['DB_PASSWORD']
$dbName = $environmentValues['DB_NAME']

if ([string]::IsNullOrWhiteSpace($dbUser) -or [string]::IsNullOrWhiteSpace($dbPassword) -or [string]::IsNullOrWhiteSpace($dbName)) {
    throw 'DB_USER, DB_PASSWORD, and DB_NAME must be present in the backend .env file.'
}

$composeFile = Join-Path $BackendDirectory 'docker-compose.yml'
if (-not (Test-Path $composeFile)) {
    throw "Missing docker compose file at $composeFile"
}
$containerId = Resolve-PostgresContainerId -ComposeFile $composeFile -EnvironmentFile $environmentFile -ServiceName $PostgresServiceName

$seedStageSqlFile = Join-Path $BackendDirectory 'src\main\resources\db\seed\location_seed_stage_tables.sql'
$seedTransformSqlFile = Join-Path $BackendDirectory 'src\main\resources\db\seed\location_seed_transform.sql'
if (-not (Test-Path $seedStageSqlFile)) {
    throw "Missing seed stage SQL file at $seedStageSqlFile"
}
if (-not (Test-Path $seedTransformSqlFile)) {
    throw "Missing seed transform SQL file at $seedTransformSqlFile"
}

Assert-LocationSchemaMigrated -ContainerId $containerId -DatabaseUser $dbUser -DatabasePassword $dbPassword -DatabaseName $dbName

$temporaryRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("car-fuel-live-location-seed-" + [guid]::NewGuid())
New-Item -ItemType Directory -Path $temporaryRoot | Out-Null

$allCountriesZip = Join-Path $temporaryRoot 'allCountries.zip'
$dePostalZip = Join-Path $temporaryRoot 'DE.zip'
$countryInfoFile = Join-Path $temporaryRoot 'countryInfo.txt'
$allCountriesDirectory = Join-Path $temporaryRoot 'allCountries'
$dePostalDirectory = Join-Path $temporaryRoot 'de-postal'
$generatedDirectory = Join-Path $temporaryRoot 'generated'

New-Item -ItemType Directory -Path $allCountriesDirectory | Out-Null
New-Item -ItemType Directory -Path $dePostalDirectory | Out-Null
New-Item -ItemType Directory -Path $generatedDirectory | Out-Null

$countriesOutput = Join-Path $generatedDirectory 'countries.tsv'
$placesOutput = Join-Path $generatedDirectory 'places.tsv'
$aliasesOutput = Join-Path $generatedDirectory 'place_aliases.tsv'
$postalCodesOutput = Join-Path $generatedDirectory 'de_postal_codes.tsv'
$loadSqlFile = Join-Path $generatedDirectory 'load-location-data.sql'
$utf8WithoutBom = [System.Text.UTF8Encoding]::new($false)

Write-Host 'Downloading GeoNames datasets...'
Download-File -Url 'https://download.geonames.org/export/dump/allCountries.zip' -Destination $allCountriesZip
Download-File -Url 'https://download.geonames.org/export/dump/countryInfo.txt' -Destination $countryInfoFile
Download-File -Url 'https://download.geonames.org/export/zip/DE.zip' -Destination $dePostalZip

Write-Host 'Extracting GeoNames datasets...'
Expand-Archive -LiteralPath $allCountriesZip -DestinationPath $allCountriesDirectory
Expand-Archive -LiteralPath $dePostalZip -DestinationPath $dePostalDirectory

$allCountriesFile = Join-Path $allCountriesDirectory 'allCountries.txt'
$dePostalFile = Join-Path $dePostalDirectory 'DE.txt'

if (-not (Test-Path $allCountriesFile)) {
    throw "Missing allCountries.txt after extraction at $allCountriesFile"
}

if (-not (Test-Path $dePostalFile)) {
    throw "Missing DE.txt after extraction at $dePostalFile"
}

$europeanCountryCodes = New-Object System.Collections.Generic.HashSet[string]
$countryWriter = [System.IO.StreamWriter]::new($countriesOutput, $false, $utf8WithoutBom)

try {
    foreach ($line in Get-Content $countryInfoFile) {
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
            continue
        }

        $fields = $line -split "`t"
        if ($fields.Length -lt 17) {
            continue
        }

        $countryCode = Get-TabField -Fields $fields -Index 0
        $continentCode = Get-TabField -Fields $fields -Index 8
        if ($continentCode -ne 'EU') {
            continue
        }

        [void]$europeanCountryCodes.Add($countryCode)
        $countryWriter.WriteLine((
            @(
                $countryCode,
                $(Get-TabField -Fields $fields -Index 1),
                $(Get-TabField -Fields $fields -Index 2),
                $(Get-TabField -Fields $fields -Index 4),
                $(Normalize-SearchText (Get-TabField -Fields $fields -Index 4)),
                $(Get-TabField -Fields $fields -Index 5),
                $(Get-TabField -Fields $fields -Index 7),
                $continentCode,
                $(Get-TabField -Fields $fields -Index 16)
            ) -join "`t"
        ))
    }
}
finally {
    $countryWriter.Dispose()
}

if ($europeanCountryCodes.Count -eq 0) {
    throw 'No European country codes were loaded from countryInfo.txt'
}

$placesWriter = [System.IO.StreamWriter]::new($placesOutput, $false, $utf8WithoutBom)
$aliasesWriter = [System.IO.StreamWriter]::new($aliasesOutput, $false, $utf8WithoutBom)
$aliasDeduplication = New-Object 'System.Collections.Generic.HashSet[string]'

try {
    $reader = [System.IO.StreamReader]::new($allCountriesFile, [System.Text.Encoding]::UTF8)
    try {
        while (-not $reader.EndOfStream) {
            $line = $reader.ReadLine()
            if ([string]::IsNullOrWhiteSpace($line)) {
                continue
            }

            $fields = $line -split "`t"
            if ($fields.Length -lt 19) {
                continue
            }

            $countryCode = Get-TabField -Fields $fields -Index 8
            if (-not $europeanCountryCodes.Contains($countryCode)) {
                continue
            }

            $featureClass = Get-TabField -Fields $fields -Index 6
            if ($featureClass -notin @('A', 'P')) {
                continue
            }

            $geonameId = Get-TabField -Fields $fields -Index 0
            $name = Get-TabField -Fields $fields -Index 1
            $asciiName = Get-TabField -Fields $fields -Index 2
            $normalizedName = Normalize-SearchText $name
            $normalizedAsciiName = Normalize-SearchText $asciiName
            $alternateNames = Get-TabField -Fields $fields -Index 3

            $placesWriter.WriteLine((
                @(
                    $geonameId,
                    $countryCode,
                    $name,
                    $asciiName,
                    $normalizedName,
                    $normalizedAsciiName,
                    $(Get-TabField -Fields $fields -Index 4),
                    $(Get-TabField -Fields $fields -Index 5),
                    $featureClass,
                    $(Get-TabField -Fields $fields -Index 7),
                    $(Get-TabField -Fields $fields -Index 10),
                    $(Get-TabField -Fields $fields -Index 11),
                    $(Get-TabField -Fields $fields -Index 12),
                    $(Get-TabField -Fields $fields -Index 13),
                    $(Get-TabField -Fields $fields -Index 14),
                    $(Get-TabField -Fields $fields -Index 17),
                    $(Get-TabField -Fields $fields -Index 18),
                    $alternateNames
                ) -join "`t"
            ))            

            foreach ($variant in Get-SearchTextVariants $name) {
                if ($variant -eq $normalizedName) {
                    continue
                }

                Write-AliasStageRow `
                    -Writer $aliasesWriter `
                    -AliasDeduplication $aliasDeduplication `
                    -PlaceGeonameId $geonameId `
                    -AliasName $variant `
                    -NormalizedAliasName $variant
            }

            if ([string]::IsNullOrWhiteSpace($alternateNames)) {
                continue
            }

            foreach ($alias in $alternateNames.Split(',')) {
                if ([string]::IsNullOrWhiteSpace($alias)) {
                    continue
                }

                $trimmedAlias = $alias.Trim()
                $normalizedAlias = Normalize-SearchText $trimmedAlias
                Write-AliasStageRow `
                    -Writer $aliasesWriter `
                    -AliasDeduplication $aliasDeduplication `
                    -PlaceGeonameId $geonameId `
                    -AliasName $trimmedAlias `
                    -NormalizedAliasName $normalizedAlias

                foreach ($variant in Get-SearchTextVariants $trimmedAlias) {
                    if ($variant -eq $normalizedAlias) {
                        continue
                    }

                    Write-AliasStageRow `
                        -Writer $aliasesWriter `
                        -AliasDeduplication $aliasDeduplication `
                        -PlaceGeonameId $geonameId `
                        -AliasName $variant `
                        -NormalizedAliasName $variant
                }
            }
        }
    }
    finally {
        $reader.Dispose()
    }
}
finally {
    $placesWriter.Dispose()
    $aliasesWriter.Dispose()
}

$postalWriter = [System.IO.StreamWriter]::new($postalCodesOutput, $false, $utf8WithoutBom)
try {
    $reader = [System.IO.StreamReader]::new($dePostalFile, [System.Text.Encoding]::UTF8)
    try {
        while (-not $reader.EndOfStream) {
            $line = $reader.ReadLine()
            if ([string]::IsNullOrWhiteSpace($line)) {
                continue
            }

            $fields = $line -split "`t"
            if ($fields.Length -lt 12) {
                continue
            }

            $placeName = Get-TabField -Fields $fields -Index 2
            $postalWriter.WriteLine((
                @(
                    $(Get-TabField -Fields $fields -Index 0),
                    $(Get-TabField -Fields $fields -Index 1),
                    $placeName,
                    $(Normalize-SearchText $placeName),
                    $(Get-TabField -Fields $fields -Index 3),
                    $(Get-TabField -Fields $fields -Index 5),
                    $(Get-TabField -Fields $fields -Index 7),
                    $(Get-TabField -Fields $fields -Index 9),
                    $(Get-TabField -Fields $fields -Index 10),
                    $(Get-TabField -Fields $fields -Index 11)
                ) -join "`t"
            ))
        }
    }
    finally {
        $reader.Dispose()
    }
}
finally {
    $postalWriter.Dispose()
}

$stageSql = Get-Content -Raw $seedStageSqlFile
$transformSql = Get-Content -Raw $seedTransformSqlFile

$loadSql = @"
$stageSql
COPY location_country_stage FROM '/tmp/countries.tsv' WITH (FORMAT text, DELIMITER E'\t');
COPY location_place_stage FROM '/tmp/places.tsv' WITH (FORMAT text, DELIMITER E'\t');
COPY location_place_alias_stage FROM '/tmp/place_aliases.tsv' WITH (FORMAT text, DELIMITER E'\t');
COPY german_postal_code_stage FROM '/tmp/de_postal_codes.tsv' WITH (FORMAT text, DELIMITER E'\t');

$transformSql
"@

[System.IO.File]::WriteAllText($loadSqlFile, $loadSql, $utf8WithoutBom)

Write-Host 'Copying generated files into PostgreSQL container...'
docker cp $countriesOutput "${containerId}:/tmp/countries.tsv" | Out-Null
docker cp $placesOutput "${containerId}:/tmp/places.tsv" | Out-Null
docker cp $aliasesOutput "${containerId}:/tmp/place_aliases.tsv" | Out-Null
docker cp $postalCodesOutput "${containerId}:/tmp/de_postal_codes.tsv" | Out-Null
docker cp $loadSqlFile "${containerId}:/tmp/load-location-data.sql" | Out-Null

Write-Host 'Loading location dataset into PostgreSQL...'
$loadOutput = & docker exec -e PGPASSWORD=$dbPassword $containerId psql -v ON_ERROR_STOP=1 -U $dbUser -d $dbName -f /tmp/load-location-data.sql 2>&1
if ($LASTEXITCODE -ne 0) {
    $details = if ($null -eq $loadOutput) { '' } else { "`n$($loadOutput -join "`n")" }
    throw "Loading the location dataset failed.$details"
}

Write-Host 'Location dataset import finished.'
Write-Host "Countries imported: $(Get-Content $countriesOutput | Measure-Object | Select-Object -ExpandProperty Count)"
Write-Host "Places imported: $(Get-Content $placesOutput | Measure-Object | Select-Object -ExpandProperty Count)"
Write-Host "Aliases imported: $(Get-Content $aliasesOutput | Measure-Object | Select-Object -ExpandProperty Count)"
Write-Host "German postal codes imported: $(Get-Content $postalCodesOutput | Measure-Object | Select-Object -ExpandProperty Count)"

Remove-Item -Path $temporaryRoot -Recurse -Force
