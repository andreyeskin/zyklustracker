package at.fhj.andrey.zyklustracker.datenbank;

import androidx.room.Dao;
import androidx.room.Query;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO для расчета статистики циклов
 */
@Dao
public interface ZyklusDao {

    /**
     * Получить все даты начала периодов для расчета циклов
     * Только реальные периоды, не прогнозы
     */
    @Query("SELECT datum FROM periode_eintraege WHERE istPrognose = 0 ORDER BY datum ASC")
    List<LocalDate> getAllePeriodeStartDaten();

    /**
     * Получить количество записанных периодов
     */
    @Query("SELECT COUNT(DISTINCT datum) FROM periode_eintraege WHERE istPrognose = 0")
    int getAnzahlPerioden();
}