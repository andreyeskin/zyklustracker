package at.fhj.andrey.zyklustracker.datenbank;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.time.LocalDate;
import java.util.List;

/**
 * DAO для работы с данными о самочувствии
 * Содержит все методы для чтения и записи в таблицу wohlbefinden_eintraege
 */
@Dao
public interface WohlbefindenDao {

    /**
     * Получить все записи о самочувствии
     * Сортировка: новые записи сверху
     */
    @Query("SELECT * FROM wohlbefinden_eintraege ORDER BY datum DESC")
    List<WohlbefindenEintrag> getAlleEintraege();

    /**
     * Получить запись за конкретную дату
     * Возвращает null, если записи нет
     */
    @Query("SELECT * FROM wohlbefinden_eintraege WHERE datum = :datum LIMIT 1")
    WohlbefindenEintrag getEintragNachDatum(LocalDate datum);

    /**
     * Получить записи за период времени
     * Например, для статистики за месяц
     */
    @Query("SELECT * FROM wohlbefinden_eintraege WHERE datum BETWEEN :startDatum AND :endDatum")
    List<WohlbefindenEintrag> getEintraegeZwischen(LocalDate startDatum, LocalDate endDatum);

    /**
     * Добавить новую запись
     * Если запись с такой датой уже есть - будет ошибка!
     */
    @Insert
    void einfuegenEintrag(WohlbefindenEintrag eintrag);

    /**
     * Обновить существующую запись
     * Используется когда пользователь редактирует данные за день
     */
    @Update
    void aktualisierenEintrag(WohlbefindenEintrag eintrag);

    /**
     * Удалить запись за конкретную дату
     */
    @Query("DELETE FROM wohlbefinden_eintraege WHERE datum = :datum")
    void loeschenEintragNachDatum(LocalDate datum);

    /**
     * Получить последние N записей
     * Для отображения истории в приложении
     */
    @Query("SELECT * FROM wohlbefinden_eintraege ORDER BY datum DESC LIMIT :anzahl")
    List<WohlbefindenEintrag> getLetzteEintraege(int anzahl);

    /**
     * Проверить, есть ли запись за конкретную дату
     * Возвращает количество записей (0 или 1)
     */
    @Query("SELECT COUNT(*) FROM wohlbefinden_eintraege WHERE datum = :datum")
    int existiertEintrag(LocalDate datum);

    // ===== МЕТОДЫ ДЛЯ СТАТИСТИКИ =====

    /**
     * Получить самое частое настроение
     * Возвращает настроение и количество раз
     */
    @Query("SELECT stimmung, COUNT(*) as anzahl FROM wohlbefinden_eintraege " +
            "WHERE stimmung IS NOT NULL AND stimmung != '' " +
            "GROUP BY stimmung ORDER BY anzahl DESC LIMIT 1")
    StimmungAnzahl getHaeufigsteStimmung();

    /**
     * Получить все записанные симптомы
     * Возвращает JSON строки, которые нужно будет распарсить
     */
    @Query("SELECT symptome FROM wohlbefinden_eintraege WHERE symptome IS NOT NULL")
    List<String> getAlleSymptomeListen();

    /**
     * Получить количество записей всего
     */
    @Query("SELECT COUNT(*) FROM wohlbefinden_eintraege")
    int getAnzahlEintraege();
}