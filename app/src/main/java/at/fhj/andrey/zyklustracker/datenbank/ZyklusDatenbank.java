package at.fhj.andrey.zyklustracker.datenbank;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * ZyklusDatenbank - Hauptklasse der Room-Datenbank für die ZyklusTracker-App
 *
 * Diese Klasse fungiert als zentraler Zugangspunkt für alle Datenbankoperationen.
 * Sie implementiert das Singleton-Pattern, um sicherzustellen, dass nur eine
 * Datenbankinstanz existiert.
 *
 * Enthaltene Entitäten:
 * - PeriodeEintrag: Speichert Menstruationstage und Prognosen
 * - WohlbefindenEintrag: Speichert täglich erfasste Gesundheitsdaten
 *
 * Verfügbare DAOs:
 * - PeriodeDao: CRUD-Operationen für Periodeneinträge
 * - WohlbefindenDao: CRUD-Operationen für Wohlbefindensdaten
 * - ZyklusDao: Statistische Auswertungen von Zyklusdaten
 *
 * Besonderheiten:
 * - TypeConverters für LocalDate und List<String> Konvertierung
 * - Fallback zu destruktiver Migration bei Schema-Änderungen
 * - Main-Thread-Queries für vereinfachte Entwicklung (nicht produktionsreif!)
 *
 * Schema-Version: 2
 * - Version 1: Nur PeriodeEintrag
 * - Version 2: Hinzufügung von WohlbefindenEintrag
 *
 * @author Andrey Eskin
 * @version 2.0
 * @since Mai 2025
 */
@Database(
        entities = {PeriodeEintrag.class, WohlbefindenEintrag.class},
        version = 2,
        exportSchema = false
)
@TypeConverters({DatumKonverter.class})
public abstract class ZyklusDatenbank extends RoomDatabase {

    // Singleton-Instanz der Datenbank
    private static ZyklusDatenbank instanz;

    /**
     * Abstrakte Methode zur Bereitstellung des PeriodeDao.
     * Room generiert automatisch die Implementierung.
     *
     * @return PeriodeDao für Zugriff auf Periodeneinträge
     */
    public abstract PeriodeDao periodeDao();

    /**
     * Abstrakte Methode zur Bereitstellung des WohlbefindenDao.
     * Room generiert automatisch die Implementierung.
     *
     * @return WohlbefindenDao für Zugriff auf Wohlbefindensdaten
     */
    public abstract WohlbefindenDao wohlbefindenDao();

    /**
     * Abstrakte Methode zur Bereitstellung des ZyklusDao.
     * Room generiert automatisch die Implementierung.
     *
     * @return ZyklusDao für statistische Zyklusauswertungen
     */
    public abstract ZyklusDao zyklusDao();

    /**
     * Singleton-Methode zur Bereitstellung der Datenbankinstanz.
     *
     * Diese Methode implementiert das Singleton-Pattern mit Thread-Sicherheit.
     * Die Datenbankinstanz wird nur einmal erstellt und bei nachfolgenden
     * Aufrufen wiederverwendet.
     *
     * Konfiguration:
     * - Fallback zu destruktiver Migration bei Schema-Änderungen
     * - Main-Thread-Queries erlaubt (nur für Entwicklung!)
     * - Datenbankdatei: "zyklus_datenbank"
     *
     * @param context Anwendungskontext für die Datenbankinitialisierung
     * @return Singleton-Instanz der ZyklusDatenbank
     */
    public static synchronized ZyklusDatenbank getInstanz(Context context) {
        if (instanz == null) {
            instanz = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ZyklusDatenbank.class,
                            "zyklus_datenbank" // Name der Datenbankdatei
                    )
                    .fallbackToDestructiveMigration() // Bei Schema-Änderungen: DB neu erstellen
                    .allowMainThreadQueries()         // ACHTUNG: Nur für Entwicklung!
                    .build();
        }
        return instanz;
    }
}