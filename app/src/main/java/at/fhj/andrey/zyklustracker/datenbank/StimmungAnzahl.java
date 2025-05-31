package at.fhj.andrey.zyklustracker.datenbank;

/**
 * Вспомогательный класс для хранения результатов статистики настроения
 * Room использует этот класс для возврата результатов SQL запроса
 */
public class StimmungAnzahl {
    public String stimmung;  // Настроение (например: "Gut")
    public int anzahl;       // Количество раз

    // Конструктор по умолчанию
    public StimmungAnzahl() {}

    // Геттеры для удобства использования
    public String getStimmung() {
        return stimmung;
    }

    public int getAnzahl() {
        return anzahl;
    }
}