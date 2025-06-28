package at.fhj.andrey.zyklustracker.datenbank;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO = Data Access Object (Datenzugriffsobjekt)
 * Dieses Interface definiert alle Operationen mit der Tabelle periode_eintraege.
 * Room erstellt automatisch die Implementierung dieser Methoden.
 */
@Dao
public interface PeriodeDao {

    /**
     * Alle echten Perioden (keine Prognosen) abrufen.
     * Sortierung nach Datum - neueste zuerst.
     */
    @Query("SELECT * FROM periode_eintraege WHERE istPrognose = 0 ORDER BY datum DESC")
    List<PeriodeEintrag> getAlleEchtenPerioden();

    /**
     * Alle prognostizierten Perioden abrufen.
     * Sortierung nach Datum - älteste zuerst.
     */
    @Query("SELECT * FROM periode_eintraege WHERE istPrognose = 1 ORDER BY datum ASC")
    List<PeriodeEintrag> getAllePrognostizierten();

    /**
     * Periode anhand eines bestimmten Datums finden.
     */
    @Query("SELECT * FROM periode_eintraege WHERE datum = :datum")
    PeriodeEintrag getPeriodeNachDatum(LocalDate datum);

    /**
     * Alle Perioden zwischen zwei Daten abrufen.
     */
    @Query("SELECT * FROM periode_eintraege WHERE datum BETWEEN :startDatum AND :endDatum")
    List<PeriodeEintrag> getPeriodenZwischen(LocalDate startDatum, LocalDate endDatum);

    /**
     * Einen neuen Periodeneintrag hinzufügen.
     */
    @Insert
    void einfuegenPeriode(PeriodeEintrag periode);

    /**
     * Mehrere Periodeneinträge gleichzeitig hinzufügen.
     */
    @Insert
    void einfuegenMehrerePerioden(List<PeriodeEintrag> perioden);

    /**
     * Einen bestehenden Eintrag aktualisieren.
     */
    @Update
    void aktualisierenPeriode(PeriodeEintrag periode);

    /**
     * Periode anhand des Datums löschen.
     */
    @Delete
    void loeschenPeriode(PeriodeEintrag periode);

    /**
     * Periode anhand des Datums löschen.
     */
    @Query("DELETE FROM periode_eintraege WHERE datum = :datum")
    void loeschenPeriodeNachDatum(LocalDate datum);


    /**
     * Alle Prognosen löschen (bei Neuberechnung).
     */
    @Query("DELETE FROM periode_eintraege WHERE istPrognose = 1")
    void loeschenAllePrognosen();
}