package at.fhj.andrey.zyklustracker.datenbank;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO = Data Access Object (Объект доступа к данным)
 * Этот интерфейс определяет все операции с таблицей periode_eintraege
 * Room автоматически создаст реализацию этих методов
 */
@Dao
public interface PeriodeDao {

    /**
     * Получить все реальные периоды (не прогнозы)
     * Сортировка по дате - новые сверху
     */
    @Query("SELECT * FROM periode_eintraege WHERE istPrognose = 0 ORDER BY datum DESC")
    List<PeriodeEintrag> getAlleEchtenPerioden();

    /**
     * Получить все прогнозируемые периоды
     * Сортировка по дате - старые сверху
     */
    @Query("SELECT * FROM periode_eintraege WHERE istPrognose = 1 ORDER BY datum ASC")
    List<PeriodeEintrag> getAllePrognostizierten();

    /**
     * Найти период по конкретной дате
     */
    @Query("SELECT * FROM periode_eintraege WHERE datum = :datum")
    PeriodeEintrag getPeriodeNachDatum(LocalDate datum);

    /**
     * Получить все периоды между двумя датами
     */
    @Query("SELECT * FROM periode_eintraege WHERE datum BETWEEN :startDatum AND :endDatum")
    List<PeriodeEintrag> getPeriodenZwischen(LocalDate startDatum, LocalDate endDatum);

    /**
     * Добавить одну новую запись о периоде
     */
    @Insert
    void einfuegenPeriode(PeriodeEintrag periode);

    /**
     * Добавить несколько записей о периодах сразу
     */
    @Insert
    void einfuegenMehrerePerioden(List<PeriodeEintrag> perioden);

    /**
     * Обновить существующую запись
     */
    @Update
    void aktualisierenPeriode(PeriodeEintrag periode);

    /**
     * Удалить запись о периоде
     */
    @Delete
    void loeschenPeriode(PeriodeEintrag periode);

    /**
     * Удалить период по дате
     */
    @Query("DELETE FROM periode_eintraege WHERE datum = :datum")
    void loeschenPeriodeNachDatum(LocalDate datum);

    /**
     * Удалить все прогнозы (при пересчете)
     */
    @Query("DELETE FROM periode_eintraege WHERE istPrognose = 1")
    void loeschenAllePrognosen();
}