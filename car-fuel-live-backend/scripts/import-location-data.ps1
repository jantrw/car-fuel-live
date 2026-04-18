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
    param([AllowNull()][string]$Value)

    if ([string]::IsNullOrWhiteSpace($Value)) {
        return ''
    }

    $normalized = $Value.Trim().ToLowerInvariant()
    $normalized = $normalized.Replace('ä', 'ae')
    $normalized = $normalized.Replace('ö', 'oe')
    $normalized = $normalized.Replace('ü', 'ue')
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

    $containerId = (& docker compose --env-file $EnvironmentFile -f $ComposeFile ps -q $ServiceName).Trim()
    if ($LASTEXITCODE -ne 0) {
        throw "Resolving the docker compose service '$ServiceName' failed."
    }
    if ([string]::IsNullOrWhiteSpace($containerId)) {
        throw "No running container found for docker compose service '$ServiceName'. Start the database first with 'docker compose -f $ComposeFile up -d'."
    }

    return $containerId
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

$migrationFile = Join-Path $BackendDirectory 'src\main\resources\db\migration\V1__create_location_seed_schema.sql'
$seedStageSqlFile = Join-Path $BackendDirectory 'src\main\resources\db\seed\location_seed_stage_tables.sql'
$seedTransformSqlFile = Join-Path $BackendDirectory 'src\main\resources\db\seed\location_seed_transform.sql'
if (-not (Test-Path $migrationFile)) {
    throw "Missing migration file at $migrationFile"
}
if (-not (Test-Path $seedStageSqlFile)) {
    throw "Missing seed stage SQL file at $seedStageSqlFile"
}
if (-not (Test-Path $seedTransformSqlFile)) {
    throw "Missing seed transform SQL file at $seedTransformSqlFile"
}

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

            if ([string]::IsNullOrWhiteSpace($alternateNames)) {
                continue
            }

            foreach ($alias in $alternateNames.Split(',')) {
                if ([string]::IsNullOrWhiteSpace($alias)) {
                    continue
                }

                $trimmedAlias = $alias.Trim()
                $normalizedAlias = Normalize-SearchText $trimmedAlias
                if ([string]::IsNullOrWhiteSpace($normalizedAlias)) {
                    continue
                }

                $dedupeKey = "$geonameId|$trimmedAlias"
                if (-not $aliasDeduplication.Add($dedupeKey)) {
                    continue
                }

                $aliasesWriter.WriteLine((
                    @(
                        $geonameId,
                        $trimmedAlias,
                        $normalizedAlias
                    ) -join "`t"
                ))
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

Write-Host 'Copying schema and generated files into PostgreSQL container...'
docker cp $migrationFile "${containerId}:/tmp/V1__create_location_seed_schema.sql" | Out-Null
docker cp $countriesOutput "${containerId}:/tmp/countries.tsv" | Out-Null
docker cp $placesOutput "${containerId}:/tmp/places.tsv" | Out-Null
docker cp $aliasesOutput "${containerId}:/tmp/place_aliases.tsv" | Out-Null
docker cp $postalCodesOutput "${containerId}:/tmp/de_postal_codes.tsv" | Out-Null
docker cp $loadSqlFile "${containerId}:/tmp/load-location-data.sql" | Out-Null

Write-Host 'Applying schema...'
docker exec -e PGPASSWORD=$dbPassword $containerId sh -lc "psql -v ON_ERROR_STOP=1 -U '$dbUser' -d '$dbName' -f /tmp/V1__create_location_seed_schema.sql" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Applying the location schema failed.'
}

Write-Host 'Loading location dataset into PostgreSQL...'
docker exec -e PGPASSWORD=$dbPassword $containerId sh -lc "psql -v ON_ERROR_STOP=1 -U '$dbUser' -d '$dbName' -f /tmp/load-location-data.sql" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Loading the location dataset failed.'
}

Write-Host 'Location dataset import finished.'
Write-Host "Countries imported: $(Get-Content $countriesOutput | Measure-Object | Select-Object -ExpandProperty Count)"
Write-Host "Places imported: $(Get-Content $placesOutput | Measure-Object | Select-Object -ExpandProperty Count)"
Write-Host "Aliases imported: $(Get-Content $aliasesOutput | Measure-Object | Select-Object -ExpandProperty Count)"
Write-Host "German postal codes imported: $(Get-Content $postalCodesOutput | Measure-Object | Select-Object -ExpandProperty Count)"

Remove-Item -Path $temporaryRoot -Recurse -Force
