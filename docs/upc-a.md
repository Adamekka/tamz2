# Čárové kódy UPC-A

Aplikace nahrazuje spořicí kalkulačku řešením zadání z referenčního obrázku. Používá jednoduché ovládání v češtině a dvě spodní záložky, Vytvořit a Načíst.

## Zadání a chování

- Uživatel zadá přesně 12 číslic včetně poslední kontrolní číslice. Tlačítko Vykreslit ověří vstup a zobrazí vlastní Composable s čárovým kódem. Po spuštění je vstup prázdný.
- Chybný počet číslic, jiné znaky a nesprávná kontrolní číslice se zobrazí jako chyba u pole. Aplikace kód automaticky neopravuje. Při úpravě vstupu se předchozí obrázek skryje.
- Kontrolní číslice doplňuje vážený součet prvních 11 číslic na násobek deseti. Liché pozice počítané od jedné mají váhu 3, sudé váhu 1. Při chybě aplikace uvede očekávanou poslední číslici.
- Vlastní vykreslení používá bitový posun a vzory L a R. Kód obsahuje 95 modulů: začátek 101, šest levých číslic po sedmi bitech, střed 01010, šest pravých číslic po sedmi bitech a konec 101. Po stranách zůstává nejméně devět bílých modulů.
- Záložka Načíst otevře na tlačítko Google Code Scanner nastavený pouze na UPC-A. Načtený kód prochází stejnou kontrolou jako ruční vstup a zobrazí se v záložce Vytvořit. Zrušení skeneru zachová dosavadní vstup. Selhání nabídne opakování.
- Tlačítko Uložit do galerie ukládá PNG do Pictures/UPC-A. Obrázek používá stejný renderer jako náhled. Neúplný soubor zůstává skrytý a při chybě se aplikace pokusí odstranit rozpracovaný záznam. Úspěch oznámí až po zveřejnění v MediaStore.

Skenování vyžaduje Služby Google Play a stažení jejich modulu skeneru. Aplikace sama nežádá o oprávnění fotoaparátu ani úložiště. Chyby skenování a ukládání mají stručnou českou zprávu; debug sestavení připojuje původní výjimku.

## Ověření

Lokální testy kontrolují platné kódy, počáteční nuly, neplatný vstup, změnu každé číslice a všech deset kódovacích vzorů. Testy na Androidu ověřují formulář, zachování výsledku při obnovení aktivity, spodní záložky a publikování PNG s bílými okraji. Exportovaný obrázek navíc nezávisle přečte ZXing a porovná načtené číslice se vstupem. ZXing je závislost pouze pro testy.

## Zdroje

- [Google Code Scanner](https://developers.google.com/ml-kit/vision/barcode-scanning/code-scanner)
- [Výpočet kontrolní číslice GS1](https://www.gs1.org/services/how-calculate-check-digit-manually)
- [Ukládání obrázků přes MediaStore](https://developer.android.com/training/data-storage/shared/media)
