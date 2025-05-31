package at.fhj.andrey.zyklustracker.datenbank;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.time.LocalDate;

/**
 * PeriodeEintrag - Entitätsklasse für die Speicherung von Menstruationsdaten
 *
 * Diese Klasse repräsentiert eine einzelne Menstruationstag-Aufzeichnung in der Datenbank.
 * Sie unterscheidet zwischen tatsächlich erfassten Menstruationstagen und
 * algorithmisch berechneten Prognosen.
 *
 * Datenbank-Mapping:
 * - Tabelle: "periode_eintraege"
 * - Primärschlüssel: Auto-generierte ID
 * - Eindeutigkeit: Ein Datum kann sowohl als echter Eintrag als auch als Prognose existieren
 *
 * Verwendung:
 * - Echte Menstruationstage: istPrognose = false (von Nutzerin eingegeben)
 * - Prognostizierte Tage: istPrognose = true (von Algorithmus berechnet)
 *
 * Zyklusberechnung:
 * - Algorithmus verwendet nur echte Daten (istPrognose = false) zur Berechnung
 * - Prognosen werden bei jeder Neuberechnung gelöscht und neu erstellt
 * - Zykluslänge wird aus Abständen zwischen Periodenbeginnen ermittelt
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
@Entity(tableName = "periode_eintraege")
public class PeriodeEintrag {

    /**
     * Primärschlüssel der Entität.
     * Wird automatisch von Room generiert und sollte nicht manuell gesetzt werden.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Datum des Menstruationstages.
     *
     * Wichtige Hinweise:
     * - Verwendet LocalDate für typsichere Datumsoperationen
     * - Wird durch DatumKonverter in String für Datenbank-Speicherung konvertiert
     * - Sollte für Prognosen mindestens in der Zukunft liegen
     */
    private LocalDate datum;

    /**
     * Flag zur Unterscheidung zwischen echten Daten und Prognosen.
     *
     * Werte:
     * - false: Echte Menstruation (von Nutzerin eingegeben)
     * - true: Prognostizierte Menstruation (algorithmisch berechnet)
     *
     * Verwendung in Abfragen:
     * - Zyklusberechnung: Nur echte Daten verwenden
     * - Kalenderanzeige: Beide Typen mit unterschiedlicher Darstellung
     * - Statistiken: Nur echte Daten für Auswertungen
     */
    private boolean istPrognose;

    /**
     * Konstruktor für die Erstellung neuer Periodeneinträge.
     *
     * @param datum Das Datum des Menstruationstages
     * @param istPrognose true für Prognosen, false für echte Daten
     */
    public PeriodeEintrag(LocalDate datum, boolean istPrognose) {
        this.datum = datum;
        this.istPrognose = istPrognose;
    }

    // ===== GETTER UND SETTER METHODEN =====
    // Diese werden von Room für die Datenbankoperationen benötigt

    /**
     * Gibt die eindeutige ID des Eintrags zurück.
     *
     * @return Die auto-generierte Datenbank-ID
     */
    public int getId() {
        return id;
    }

    /**
     * Setzt die ID des Eintrags.
     * Normalerweise nur von Room verwendet.
     *
     * @param id Die zu setzende ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gibt das Datum des Menstruationstages zurück.
     *
     * @return Das LocalDate-Objekt des Eintrags
     */
    public LocalDate getDatum() {
        return datum;
    }

    /**
     * Setzt das Datum des Menstruationstages.
     *
     * @param datum Das zu setzende Datum
     */
    public void setDatum(LocalDate datum) {
        this.datum = datum;
    }

    /**
     * Prüft, ob dieser Eintrag eine Prognose oder echte Daten darstellt.
     *
     * @return true wenn es sich um eine Prognose handelt, false für echte Daten
     */
    public boolean isIstPrognose() {
        return istPrognose;
    }

    /**
     * Setzt den Prognose-Status des Eintrags.
     *
     * @param istPrognose true für Prognosen, false für echte Daten
     */
    public void setIstPrognose(boolean istPrognose) {
        this.istPrognose = istPrognose;
    }

    /**
     * Überschreibt toString() für bessere Debugging-Ausgabe.
     *
     * @return String-Repräsentation des Objekts
     */
    @Override
    public String toString() {
        return "PeriodeEintrag{" +
                "id=" + id +
                ", datum=" + datum +
                ", istPrognose=" + istPrognose +
                '}';
    }

    /**
     * Überschreibt equals() für korrekte Objektvergleiche.
     * Zwei Einträge sind gleich, wenn sie das gleiche Datum und den gleichen Prognose-Status haben.
     *
     * @param obj Das zu vergleichende Objekt
     * @return true wenn die Objekte gleich sind
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        PeriodeEintrag that = (PeriodeEintrag) obj;
        return istPrognose == that.istPrognose &&
                datum != null ? datum.equals(that.datum) : that.datum == null;
    }

    /**
     * Überschreibt hashCode() für korrekte Verwendung in Collections.
     *
     * @return Hash-Code basierend auf Datum und Prognose-Status
     */
    @Override
    public int hashCode() {
        int result = datum != null ? datum.hashCode() : 0;
        result = 31 * result + (istPrognose ? 1 : 0);
        return result;
    }
}