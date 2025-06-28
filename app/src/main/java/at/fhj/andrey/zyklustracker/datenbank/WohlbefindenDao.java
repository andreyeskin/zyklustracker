/**
 * SCHRITT 1: Korrigieren Sie WohlbefindenDao.java
 * ===============================================
 *
 * Das DAO-Interface darf NUR abstrakte Methoden enthalten!
 */

package at.fhj.andrey.zyklustracker.datenbank;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO für die Arbeit mit Wohlbefinden-Daten
 * Enthält alle Methoden für das Lesen und Schreiben in die Tabelle wohlbefinden_eintraege
 */
@Dao
public interface WohlbefindenDao {

    /**
     * Alle Wohlbefinden-Einträge abrufen
     * Sortierung: Neueste Einträge zuerst
     */
    @Query("SELECT * FROM wohlbefinden_eintraege ORDER BY datum DESC")
    List<WohlbefindenEintrag> getAlleEintraege();

    /**
     * Eintrag für ein bestimmtes Datum abrufen
     * Gibt null zurück, wenn kein Eintrag vorhanden ist
     */
    @Query("SELECT * FROM wohlbefinden_eintraege WHERE datum = :datum LIMIT 1")
    WohlbefindenEintrag getEintragNachDatum(LocalDate datum);

    /**
     * Einträge für einen Zeitraum abrufen
     * Zum Beispiel für Monatsstatistiken
     */
    @Query("SELECT * FROM wohlbefinden_eintraege WHERE datum BETWEEN :startDatum AND :endDatum")
    List<WohlbefindenEintrag> getEintraegeZwischen(LocalDate startDatum, LocalDate endDatum);

    /**
     * Neuen Eintrag hinzufügen
     * Wenn bereits ein Eintrag für dieses Datum existiert - Fehler!
     */
    @Insert
    void einfuegenEintrag(WohlbefindenEintrag eintrag);

    /**
     * Bestehenden Eintrag aktualisieren
     * Wird verwendet wenn Benutzer Daten für einen Tag bearbeitet
     */
    @Update
    void aktualisierenEintrag(WohlbefindenEintrag eintrag);



    /**
     * Eintrag für ein bestimmtes Datum löschen
     */
    @Query("DELETE FROM wohlbefinden_eintraege WHERE datum = :datum")
    void loeschenEintragNachDatum(LocalDate datum);

    /**
     * Die letzten N Einträge abrufen
     * Für die Verlaufsanzeige in der App
     */
    @Query("SELECT * FROM wohlbefinden_eintraege ORDER BY datum DESC LIMIT :anzahl")
    List<WohlbefindenEintrag> getLetzteEintraege(int anzahl);

    /**
     * Prüfen ob ein Eintrag für ein bestimmtes Datum existiert
     * Gibt die Anzahl der Einträge zurück (0 oder 1)
     */
    @Query("SELECT COUNT(*) FROM wohlbefinden_eintraege WHERE datum = :datum")
    int existiertEintrag(LocalDate datum);

    // ===== METHODEN FÜR STATISTIKEN =====

    /**
     * Häufigste Stimmung ermitteln
     * Gibt Stimmung und Häufigkeit zurück
     */
    @Query("SELECT stimmung, COUNT(*) as anzahl FROM wohlbefinden_eintraege " +
            "WHERE stimmung IS NOT NULL AND stimmung != '' " +
            "GROUP BY stimmung ORDER BY anzahl DESC LIMIT 1")
    StimmungAnzahl getHaeufigsteStimmung();

    /**
     * Häufigsten Schmerzlevel ermitteln
     * Gibt Schmerzlevel und Häufigkeit zurück
     */
    @Query("SELECT schmerzLevel as stimmung, COUNT(*) as anzahl FROM wohlbefinden_eintraege " +
            "WHERE schmerzLevel IS NOT NULL AND schmerzLevel != '' " +
            "GROUP BY schmerzLevel ORDER BY anzahl DESC LIMIT 1")
    StimmungAnzahl getHaeufigstesSchmerzLevel();

    /**
     * Alle erfassten Symptome abrufen
     * Gibt JSON-Strings zurück, die geparst werden müssen
     */
    @Query("SELECT symptome FROM wohlbefinden_eintraege WHERE symptome IS NOT NULL")
    List<String> getAlleSymptomeListen();

    /**
     * Gesamtanzahl der Einträge abrufen
     */
    @Query("SELECT COUNT(*) FROM wohlbefinden_eintraege")
    int getAnzahlEintraege();

    // ===== METHODEN FÜR SENSORDATEN =====

    /**
     * Alle Einträge mit Sensordaten abrufen (nicht null Werte)
     */
    @Query("SELECT * FROM wohlbefinden_eintraege " +
            "WHERE temperatur IS NOT NULL OR puls IS NOT NULL OR spo2 IS NOT NULL " +
            "ORDER BY datum DESC")
    List<WohlbefindenEintrag> getAlleEintraegeMetSensordaten();

    /**
     * Sensordaten für einen bestimmten Zeitraum abrufen
     */
    @Query("SELECT * FROM wohlbefinden_eintraege " +
            "WHERE datum BETWEEN :startDatum AND :endDatum " +
            "AND (temperatur IS NOT NULL OR puls IS NOT NULL OR spo2 IS NOT NULL) " +
            "ORDER BY datum ASC")
    List<WohlbefindenEintrag> getSensordatenZwischen(LocalDate startDatum, LocalDate endDatum);

    /**
     * Neueste Sensordaten abrufen
     */
    @Query("SELECT * FROM wohlbefinden_eintraege " +
            "WHERE temperatur IS NOT NULL OR puls IS NOT NULL OR spo2 IS NOT NULL " +
            "ORDER BY datum DESC LIMIT 1")
    WohlbefindenEintrag getLetztenSensordaten();
}