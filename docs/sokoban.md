# Sokoban

Jednoduché školní řešení základního Sokobanu a všech osmi rozšíření. Aplikace používá české ovládání a spodní záložky Hra, Úrovně a Editor.

## Funkce

1. Osm úrovní. Po umístění všech beden na cíle se automaticky otevře další úroveň. Poslední zůstane zobrazená jako splněná.
2. Načítání map z `app/src/main/assets/levels.txt`.
3. Parser více různě velkých map v jednom textovém souboru.
4. Nejlepší výsledek pro každou úroveň podle nejnižšího počtu tahů.
5. Seznam úrovní s výběrem hry.
6. Uložení rozehraných her a naposledy vybrané úrovně.
7. Náhledy generované přímo z map stejným rendererem jako hra.
8. Grafický editor vlastních úrovní s výběrem nástroje a klepáním do mapy. Nové mapy mají pevnou velikost 8 × 8 polí. Vlastní úrovně lze uložit, upravit a smazat.

## Ovládání

Hráč se pohybuje šipkovými tlačítky nebo přejetím prstem. Jeden platný krok znamená jeden tah. Lze tlačit pouze jednu bednu, nelze ji táhnout ani posouvat přes zeď nebo jinou bednu. Začít znovu obnoví mapu bez smazání nejlepšího výsledku. Opětovný výběr splněné úrovně spustí nový pokus.

Editor má nástroje Zeď, Podlaha, Cíl, Bedna a Hráč. Podlaha odstraní obsah pole. Přemístění hráče zachová původní cíl; hráč i bedna mohou stát na cíli. Uložit a hrát kontroluje mapu a otevře ji ve hře. Přejmenování zachová skóre, změna mapy resetuje její pokus i výsledek. Neuložený obsah editoru se při obnovení aktivity zahodí.

## Textové mapy

Mapy odděluje prázdný řádek. Název před mapou začíná středníkem. Bez názvu se mapy číslují od jedné.

| Znak | Význam |
| --- | --- |
| `#` | Zeď |
| mezera, `-`, `_` | Podlaha |
| `.` | Cíl |
| `$` | Bedna |
| `@` | Hráč |
| `*` | Bedna na cíli |
| `+` | Hráč na cíli |

Parser zvládá nepravidelné řádky. Herní plochu určí průchodem od hráče; okolní mezery nejsou součástí místnosti. Mapa musí být uzavřená zdmi, mít jednoho hráče a stejný nenulový počet beden a cílů ve stejné místnosti. Alespoň jedna bedna musí být mimo cíl. Editor neověřuje řešitelnost hlavolamu.

## Ukládání a ověření

Hry, skóre a vlastní úrovně se ukládají přímo do interního souboru `sokoban.json`. Zápis proběhne po platném tahu a změně hry či úrovně. Chyba uložení nabídne opakování. Chybný soubor se automaticky nepřepisuje. Dříve uložené hry a úrovně zůstávají čitelné.

Lokální testy ověřují pohyb, parser, editor a řešitelnost vestavěných map. Testy na Androidu ověřují skóre, obnovení her, výběr úrovní, swipy a vytvoření, úpravu i smazání vlastní mapy.
