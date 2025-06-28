package at.fhj.andrey.zyklustracker.datenbank;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;

/**
 * Diese Klasse hilft Room dabei, komplexe Datentypen in der Datenbank zu speichern.
 * Room kann LocalDate und List<String> nicht direkt speichern,
 * daher konvertieren wir sie zum Speichern in einen String.
 */
public class DatumKonverter {

    // Gson hilft bei der Umwandlung von Listen in JSON-Strings
    private static Gson gson = new Gson();

    /**
     * Konvertiert einen String aus der Datenbank in ein LocalDate.
     * Beispiel: "2025-01-15" → LocalDate-Objekt
     */
    @TypeConverter
    public static LocalDate vonString(String wert) {
        return wert == null ? null : LocalDate.parse(wert);
    }

    /**
     * Konvertiert ein LocalDate in einen String zum Speichern in der Datenbank.
     * Beispiel: LocalDate-Objekt → "2025-01-15"
     */
    @TypeConverter
    public static String zuString(LocalDate datum) {
        return datum == null ? null : datum.toString();
    }

    /**
     * Konvertiert eine Liste von Strings in einen JSON-String.
     * Beispiel: ["Kopfschmerzen", "Übelkeit"] → "[\"Kopfschmerzen\",\"Übelkeit\"]"
     */
    @TypeConverter
    public static String vonStringListe(List<String> liste) {
        if (liste == null) {
            return null;
        }
        return gson.toJson(liste);
    }

    /**
     * Konvertiert einen JSON-String zurück in eine Liste.
     * Beispiel: "[\"Kopfschmerzen\",\"Übelkeit\"]" → ["Kopfschmerzen", "Übelkeit"]
     */
    @TypeConverter
    public static List<String> zuStringListe(String wert) {
        if (wert == null) {
            return null;
        }
        Type listenTyp = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(wert, listenTyp);
    }
}