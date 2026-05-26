# Location-to-Price MVP Findings

Diese Datei listet die wichtigsten Findings aus Review, Hardening und Live-E2E fuer den aktuellen Location-to-Price-MVP sowie den jeweils umgesetzten Fix.

## Scope

Betroffen ist der aktuelle MVP-Flow:

1. Manuelle Ortssuche gegen den lokalen PostgreSQL-Datensatz
2. Explizite Auswahl eines `place`- oder `postalCode`-Treffers
3. Live-Preisabruf ueber den Backend-Proxy zu Tankerkönig
4. Rendering von Tankstellenpreisen im Frontend

## Gefixte Findings

### Backend Request- und Error-Handling

1. `High`: Deduplizierte parallele Preis-Requests konnten bei Upstream-Fehlern als `500` statt `502` enden.
   - Fix: In-Flight-Fehler werden korrekt entpackt, sodass `FuelPriceLookupException` weiter sauber auf `UPSTREAM_ERROR / 502` gemappt wird.

2. `High`: Unmapped Requests konnten Stacktraces und interne Klassen im Response-Body exponieren.
   - Fix: Unmapped Routen liefern jetzt ein strukturiertes `NOT_FOUND`, und die Default-Error-Pipeline ist so gehärtet, dass keine `trace`-, `exception`- oder internen Paketdetails im Response landen.

### Security

3. `High`: Upstream-Fehler konnten den Tankerkönig-API-Key indirekt in Logs leaken.
   - Fix: Die urspruengliche HTTP-Client-Exception-Chain wird nicht mehr weitergereicht oder mit Stacktrace geloggt, wenn sie die Request-URI enthalten koennte.

4. `High`: `GET /api/v1/gas-stations` war oeffentlich und unthrottled.
   - Fix: Es gibt jetzt einen konservativen In-Memory-Rate-Limit-Guard auf dem Preis-Endpoint.

5. `Medium`: Rate Limiting war hinter Proxy/LB nicht belastbar, wenn nur `remoteAddr` verwendet wurde.
   - Fix: Client-IP-Aufloesung ist jetzt proxy-aware, aber nur fuer explizit konfigurierte `app.security.trusted-proxies`. Ohne diese Konfiguration bleibt der Guard bewusst bei `remoteAddr`.

### Frontend UX und Korrektheit

6. `Medium`: Long-Query-Validierung war widerspruechlich zwischen Formular und Ergebnisbox.
   - Fix: Beide Stellen zeigen jetzt denselben Fehlertext fuer `QUERY_TOO_LONG`.

7. `Medium`: Alte Preisresultate blieben sichtbar, waehrend der Nutzer schon nach etwas anderem suchte.
   - Fix: Preiszustand und laufende Preis-Requests werden beim Editieren der Query sofort zurueckgesetzt.

8. `Medium`: Der Privacy-Hinweis war fachlich falsch und behauptete, die UI nutze nur lokale Seed-Daten.
   - Fix: Die UI kommuniziert jetzt korrekt, dass die Ortssuche lokal ist, aber nach Auswahl Live-Kraftstoffpreise geladen werden.

9. `Low`: Ein toter State (`hasVisibleResults`) blieb unbenutzt im Frontend bestehen.
   - Fix: Entfernt.

### Live-E2E Parsing

10. `Bug`: Tankerkönig `postCode` kam live nicht immer als String, teilweise numerisch.
    - Fix: Der Parser akzeptiert `postCode` jetzt robust sowohl als Text als auch als Zahl.

## Aktueller Sicherheits- und Betriebsstand

- Tankerkönig-API-Key bleibt backend-only
- keine Stacktraces in oeffentlichen Error-Bodies
- strukturierte Error-Codes fuer Validation, Not Found, Upstream und Rate Limit
- In-Flight-Deduplizierung fuer identische Preis-Requests
- konservatives Rate Limiting auf `GET /api/v1/gas-stations`
- kein Forwarded-Header-Trust ohne explizite Proxy-Konfiguration

## Noch offen ausserhalb dieses Findings-Sets

Diese Punkte sind bewusst noch nicht Teil des aktuellen Fix-Sets:

- Filter-UI fuer Radius, Fuel-Typ und Sortierung
- Station-Detailansicht
- Country-default-Preisfluss
- `Use my city`-Geolocation
- verteilter statt node-lokaler Rate Limiter
- echte Produktionswerte fuer `app.security.trusted-proxies`

## Wichtige Notiz zur Proxy-Konfiguration

Solange die Ziel-Umgebung noch nicht festgelegt ist:

- `app.security.trusted-proxies` leer lassen
- keinen Header-Trust aktivieren
- keine Platzhalter-CIDRs ins Repo eintragen

Das ist aktuell der sichere Minimalstand.
