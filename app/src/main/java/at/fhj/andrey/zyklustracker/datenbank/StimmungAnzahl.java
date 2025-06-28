package at.fhj.andrey.zyklustracker.datenbank;

/**
 * Hilfsklasse zur Speicherung der Ergebnisse der Stimmungsstatistik.
 * Room verwendet diese Klasse, um die Ergebnisse einer SQL-Abfrage zurückzugeben.
 */
public class StimmungAnzahl {
    public String stimmung;
    public int anzahl;


    public StimmungAnzahl() {}


    public String getStimmung() {
        return stimmung;
    }

    public int getAnzahl() {
        return anzahl;
    }
}