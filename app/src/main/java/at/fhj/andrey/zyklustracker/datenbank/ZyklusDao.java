package at.fhj.andrey.zyklustracker.datenbank;

import androidx.room.Dao;
import androidx.room.Query;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO für die Zyklusberechnung und Periodenstatistik
 *
 * Diese Klasse stellt Datenbankzugriffe für:
 * - Periodendaten für Zyklusberechnungen
 * - Statistiken über Zykluslängen
 * - Filterung zwischen echten und prognostizierten Daten
 */
@Dao
public interface ZyklusDao {

    /**
     * Holt alle Datumsangaben der Periodenbeginne (nur echte Daten, keine Prognosen)
     * Wird für Zyklusstatistik-Berechnungen verwendet
     *
     * @return Liste aller Periodenstartdaten, chronologisch sortiert
     */
    @Query("SELECT datum FROM periode_eintraege WHERE istPrognose = 0 ORDER BY datum ASC")
    List<LocalDate> getAllePeriodeStartDaten();

    /**
     * Holt alle echten Periodeneinträge (keine Prognosen)
     * Wird für erweiterte Zyklusanalyse mit ZyklusPhaseBerechnung verwendet
     *
     * @return Liste aller echten PeriodeEintrag-Objekte
     */
    @Query("SELECT * FROM periode_eintraege WHERE istPrognose = 0 ORDER BY datum ASC")
    List<PeriodeEintrag> getAlleEchtenPerioden();

    /**
     * Zählt die Anzahl der erfassten Perioden (nur echte Daten)
     *
     * @return Anzahl der verschiedenen Periodentage
     */


}