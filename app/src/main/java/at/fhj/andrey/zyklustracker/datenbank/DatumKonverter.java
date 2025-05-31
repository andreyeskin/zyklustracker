package at.fhj.andrey.zyklustracker.datenbank;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;

/**
 * Этот класс помогает Room сохранять сложные типы данных в базу.
 * Room не умеет сохранять LocalDate и List<String> напрямую,
 * поэтому мы преобразуем их в String для сохранения.
 */
public class DatumKonverter {

    // Gson помогает преобразовывать списки в JSON строки
    private static Gson gson = new Gson();

    /**
     * Преобразует строку из базы данных в LocalDate
     * Например: "2025-01-15" → LocalDate объект
     */
    @TypeConverter
    public static LocalDate vonString(String wert) {
        return wert == null ? null : LocalDate.parse(wert);
    }

    /**
     * Преобразует LocalDate в строку для сохранения в базе
     * Например: LocalDate объект → "2025-01-15"
     */
    @TypeConverter
    public static String zuString(LocalDate datum) {
        return datum == null ? null : datum.toString();
    }

    /**
     * Преобразует список строк в JSON строку
     * Например: ["Kopfschmerzen", "Übelkeit"] → "[\"Kopfschmerzen\",\"Übelkeit\"]"
     */
    @TypeConverter
    public static String vonStringListe(List<String> liste) {
        if (liste == null) {
            return null;
        }
        return gson.toJson(liste);
    }

    /**
     * Преобразует JSON строку обратно в список
     * Например: "[\"Kopfschmerzen\",\"Übelkeit\"]" → ["Kopfschmerzen", "Übelkeit"]
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