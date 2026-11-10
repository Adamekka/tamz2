# Počasí

Jednoduchá Android aplikace s českými popisky a spodními záložkami Aktuálně a Předpověď.

## Ovládání

Zadejte název města a stiskněte Aktualizovat nebo hledání na klávesnici. Tři karty zobrazí teplotu ve °C, český popis a odpovídající ikonu počasí. Spodní záložka Předpověď otevře seznam na následujících pět dní ve tříhodinových intervalech. Každá položka obsahuje datum, místní čas hledaného města, teplotu, popis a ikonu. Zpět vrací na aktuální počasí.

První spuštění má prázdné pole města. Po úspěšném načtení se poslední hledané město uloží a při dalším spuštění znovu načte. Načítání a data zůstanou zachovány při otočení zařízení. Neúspěšný požadavek zobrazí chybu a lze jej opakovat tlačítkem Aktualizovat. Aplikace nepoužívá ukázková data ani polohu telefonu.

## API a sestavení

Do ignorovaného `local.properties` přidejte `OPENWEATHER_API_KEY=<váš klíč>`. Chybějící nebo prázdný klíč zastaví sestavení s popisem chyby. Klíč se sestaví do aplikace, proto APK obsahuje přístup ke stejnému účtu OpenWeatherMap.

Sestavení: `./gradlew :app:assembleDebug`. Kontroly: `./gradlew :app:lintDebug :app:assembleDebugAndroidTest`; testy na připojeném Android zařízení: `./gradlew :app:connectedDebugAndroidTest`.

Aplikace používá HTTPS, systémové `HttpURLConnection`, `org.json` a ViewModel. [Aktuální počasí](https://openweathermap.org/api/current) se vyhledá podle názvu města. [Předpověď](https://openweathermap.org/api/forecast5) používá souřadnice vrácené prvním požadavkem, aby obě obrazovky patřily stejnému místu. Oba požadavky mají `units=metric` a `lang=cz`. Datum předpovědi používá časový posun vrácený API. [Ikony OpenWeatherMap](https://openweathermap.org/api/weather-conditions) jsou přibalené k aplikaci, aby jejich zobrazení nevyžadovalo další síťové požadavky.

Chyby zahrnují nenalezené město, neplatný nebo neaktivní klíč, překročený limit, nedostupné připojení a neplatnou odpověď. Debug sestavení přidá podrobnosti chyby, release zobrazí pouze zprávu pro uživatele.
