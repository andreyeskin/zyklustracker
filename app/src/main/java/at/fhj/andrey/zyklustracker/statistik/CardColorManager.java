package at.fhj.andrey.zyklustracker.statistik;

import android.graphics.Color;
import android.util.Log;
import android.widget.LinearLayout;

/**
 * CardColorManager - Verwaltung der Kartenfarben basierend auf Gesundheitswerten
 *
 * Diese Klasse ist verantwortlich für:
 * - Bestimmung der passenden Farben für Statistik-Karten
 * - Farblogik basierend auf medizinischen Normalwerten
 * - Visuelle Feedback-Systeme (Grün=Gut, Orange=Grenzwertig, Rot=Abnormal)
 * - Aktualisierung der UI-Kartenfarben
 *
 * Farbschema:
 * - Grün (#4CAF50): Normale/gesunde Werte
 * - Orange (#FF9800): Grenzwertige Werte
 * - Rot (#F44336): Abnormale/bedenkliche Werte
 * - Grau (#9E9E9E): Keine Daten verfügbar
 *
 * WICHTIG: Alle Methoden müssen auf dem Main Thread aufgerufen werden!
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class CardColorManager {

    private static final String TAG = "CardColorManager";

    // Standard-Farben (Hex-Codes)
    private static final String COLOR_NORMAL_GREEN = "#4CAF50";     // Grün für normale Werte
    private static final String COLOR_WARNING_ORANGE = "#FF9800";   // Orange für Grenzwerte
    private static final String COLOR_ABNORMAL_RED = "#F44336";     // Rot für abnormale Werte
    private static final String COLOR_NO_DATA_GRAY = "#9E9E9E";     // Grau für keine Daten

    // Spezielle Farben für verschiedene Kategorien
    private static final String COLOR_LIGHT_GREEN = "#8BC34A";      // Hellgrün für leichte Werte
    private static final String COLOR_DARK_RED = "#D32F2F";         // Dunkelrot für schwere Werte

    /**
     * UI-Komponenten für Karten-Layouts
     */
    private LinearLayout cycleCardLayout;
    private LinearLayout periodCardLayout;
    private LinearLayout painCardLayout;
    private LinearLayout moodCardLayout;

    /**
     * Konstruktor
     */
    public CardColorManager() {
        Log.d(TAG, "CardColorManager initialisiert");
    }

    /**
     * Initialisiert die Karten-Layouts
     * WICHTIG: Muss auf Main Thread aufgerufen werden!
     */
    public void initializeCardLayouts(LinearLayout cycleCard, LinearLayout periodCard,
                                      LinearLayout painCard, LinearLayout moodCard) {
        Log.d(TAG, "Initialisiere Karten-Layouts...");

        this.cycleCardLayout = cycleCard;
        this.periodCardLayout = periodCard;
        this.painCardLayout = painCard;
        this.moodCardLayout = moodCard;
    }

    /**
     * Aktualisiert alle Kartenfarben basierend auf Statistiken
     * WICHTIG: Muss auf Main Thread aufgerufen werden!
     */
    public void updateAllCardColors(StatistikData.AllStatistics statistics) {
        Log.d(TAG, "Aktualisiere alle Kartenfarben...");

        try {
            // Zykluslänge-Karte
            if (statistics.cycle.hasEnoughData) {
                updateCycleCardColor(statistics.cycle.average);
            } else {
                setCardColor(cycleCardLayout, COLOR_NO_DATA_GRAY);
            }

            // Periodendauer-Karte
            if (statistics.period.hasData) {
                updatePeriodCardColor(statistics.period.averageDuration);
            } else {
                setCardColor(periodCardLayout, COLOR_NO_DATA_GRAY);
            }

            // Schmerz-Karte
            if (statistics.pain.hasData) {
                updatePainCardColor(statistics.pain.mostFrequentPain);
            } else {
                setCardColor(painCardLayout, COLOR_NO_DATA_GRAY);
            }

            // Stimmungs-Karte
            if (statistics.mood.hasData) {
                updateMoodCardColor(statistics.mood.mostFrequentMood);
            } else {
                setCardColor(moodCardLayout, COLOR_NO_DATA_GRAY);
            }

            Log.d(TAG, "Alle Kartenfarben erfolgreich aktualisiert");

        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Aktualisieren der Kartenfarben: " + e.getMessage(), e);
        }
    }

    /**
     * Bestimmt Zykluslänge-Kartenfarbe basierend auf medizinischen Normalwerten
     *
     * Normale Zykluslänge: 21-35 Tage (Grün)
     * Grenzwertig: 18-40 Tage (Orange)
     * Abnormal: außerhalb 18-40 Tage (Rot)
     */
    private void updateCycleCardColor(long averageCycle) {
        String color;

        if (averageCycle >= 21 && averageCycle <= 35) {
            // Normal - Grün
            color = COLOR_NORMAL_GREEN;
            Log.d(TAG, "Zykluslänge normal: " + averageCycle + " Tage");
        } else if (averageCycle >= 18 && averageCycle <= 40) {
            // Grenzwertig - Orange
            color = COLOR_WARNING_ORANGE;
            Log.d(TAG, "Zykluslänge grenzwertig: " + averageCycle + " Tage");
        } else {
            // Abnormal - Rot
            color = COLOR_ABNORMAL_RED;
            Log.d(TAG, "Zykluslänge abnormal: " + averageCycle + " Tage");
        }

        setCardColor(cycleCardLayout, color);
    }

    /**
     * Bestimmt Periodendauer-Kartenfarbe basierend auf medizinischen Normalwerten
     *
     * Normale Periodendauer: 3-7 Tage (Grün)
     * Grenzwertig: 2-8 Tage (Orange)
     * Abnormal: außerhalb 2-8 Tage (Rot)
     */
    private void updatePeriodCardColor(int averageDuration) {
        String color;

        if (averageDuration >= 3 && averageDuration <= 7) {
            // Normal - Grün
            color = COLOR_NORMAL_GREEN;
            Log.d(TAG, "Periodendauer normal: " + averageDuration + " Tage");
        } else if (averageDuration >= 2 && averageDuration <= 8) {
            // Grenzwertig - Orange
            color = COLOR_WARNING_ORANGE;
            Log.d(TAG, "Periodendauer grenzwertig: " + averageDuration + " Tage");
        } else {
            // Abnormal - Rot
            color = COLOR_ABNORMAL_RED;
            Log.d(TAG, "Periodendauer abnormal: " + averageDuration + " Tage");
        }

        setCardColor(periodCardLayout, color);
    }

    /**
     * Bestimmt Schmerz-Kartenfarbe basierend auf Schmerzlevel
     *
     * Keine Schmerzen: Grün
     * Leichte Schmerzen: Hellgrün
     * Mittlere Schmerzen: Orange
     * Starke/Krampfartige Schmerzen: Rot/Dunkelrot
     */
    private void updatePainCardColor(String mostFrequentPain) {
        if (mostFrequentPain == null) {
            setCardColor(painCardLayout, COLOR_NO_DATA_GRAY);
            return;
        }

        String color;
        String pain = mostFrequentPain.toLowerCase();

        if (pain.contains("keine")) {
            // Keine Schmerzen - Grün
            color = COLOR_NORMAL_GREEN;
            Log.d(TAG, "Schmerzlevel optimal: keine Schmerzen");
        } else if (pain.contains("leicht")) {
            // Leichte Schmerzen - Hellgrün
            color = COLOR_LIGHT_GREEN;
            Log.d(TAG, "Schmerzlevel akzeptabel: leichte Schmerzen");
        } else if (pain.contains("mittel")) {
            // Mittlere Schmerzen - Orange
            color = COLOR_WARNING_ORANGE;
            Log.d(TAG, "Schmerzlevel bedenklich: mittlere Schmerzen");
        } else if (pain.contains("stark")) {
            // Starke Schmerzen - Rot
            color = COLOR_ABNORMAL_RED;
            Log.d(TAG, "Schmerzlevel problematisch: starke Schmerzen");
        } else if (pain.contains("krampfartig")) {
            // Krampfartige Schmerzen - Dunkelrot
            color = COLOR_DARK_RED;
            Log.d(TAG, "Schmerzlevel kritisch: krampfartige Schmerzen");
        } else {
            // Unbekannt - Grau
            color = COLOR_NO_DATA_GRAY;
            Log.d(TAG, "Schmerzlevel unbekannt: " + mostFrequentPain);
        }

        setCardColor(painCardLayout, color);
    }

    /**
     * Bestimmt Stimmungs-Kartenfarbe basierend auf häufigster Stimmung
     *
     * Sehr gut/Gut: Grün
     * Mittel: Orange
     * Schlecht: Rot
     */
    private void updateMoodCardColor(String mostFrequentMood) {
        if (mostFrequentMood == null) {
            setCardColor(moodCardLayout, COLOR_NO_DATA_GRAY);
            return;
        }

        // Emojis entfernen für saubere Analyse
        String cleanMood = removeMoodEmojis(mostFrequentMood).toLowerCase();
        String color;

        if (cleanMood.contains("sehr gut") || cleanMood.contains("gut")) {
            // Gute Stimmung - Grün
            color = COLOR_NORMAL_GREEN;
            Log.d(TAG, "Stimmung positiv: " + cleanMood);
        } else if (cleanMood.contains("mittel")) {
            // Mittlere Stimmung - Orange
            color = COLOR_WARNING_ORANGE;
            Log.d(TAG, "Stimmung neutral: " + cleanMood);
        } else if (cleanMood.contains("schlecht")) {
            // Schlechte Stimmung - Rot
            color = COLOR_ABNORMAL_RED;
            Log.d(TAG, "Stimmung problematisch: " + cleanMood);
        } else {
            // Unbekannt - Grau
            color = COLOR_NO_DATA_GRAY;
            Log.d(TAG, "Stimmung unbekannt: " + mostFrequentMood);
        }

        setCardColor(moodCardLayout, color);
    }

    /**
     * Hilfsmethode: Setzt die Hintergrundfarbe einer Karte
     * WICHTIG: Muss auf Main Thread aufgerufen werden!
     */
    private void setCardColor(LinearLayout cardLayout, String colorHex) {
        if (cardLayout != null) {
            try {
                int color = Color.parseColor(colorHex);
                cardLayout.setBackgroundColor(color);
                Log.d(TAG, "Kartenfarbe gesetzt: " + colorHex);
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Setzen der Kartenfarbe: " + e.getMessage(), e);
                // Fallback zu Standard-Grau
                cardLayout.setBackgroundColor(Color.parseColor(COLOR_NO_DATA_GRAY));
            }
        } else {
            Log.w(TAG, "Karten-Layout ist null - kann Farbe nicht setzen");
        }
    }

    /**
     * Entfernt Emoji-Zeichen aus Stimmungsbezeichnungen
     */
    private String removeMoodEmojis(String mood) {
        if (mood == null) return "";

        return mood.replace("😀 ", "")
                .replace("🙂 ", "")
                .replace("😐 ", "")
                .replace("🙁 ", "")
                .trim();
    }

    /**
     * Öffentliche Methoden für manuelle Farbsetzung
     */

    /**
     * Setzt alle Karten auf "Lade-Modus" (Grau)
     * WICHTIG: Muss auf Main Thread aufgerufen werden!
     */
    public void setAllCardsToLoadingState() {
        Log.d(TAG, "Setze alle Karten auf Lade-Zustand...");
        setCardColor(cycleCardLayout, COLOR_NO_DATA_GRAY);
        setCardColor(periodCardLayout, COLOR_NO_DATA_GRAY);
        setCardColor(painCardLayout, COLOR_NO_DATA_GRAY);
        setCardColor(moodCardLayout, COLOR_NO_DATA_GRAY);
    }

    /**
     * Setzt eine spezifische Karte auf eine bestimmte Farbe
     * WICHTIG: Muss auf Main Thread aufgerufen werden!
     */
    public void setSpecificCardColor(String cardType, String colorHex) {
        switch (cardType.toLowerCase()) {
            case "cycle":
                setCardColor(cycleCardLayout, colorHex);
                break;
            case "period":
                setCardColor(periodCardLayout, colorHex);
                break;
            case "pain":
                setCardColor(painCardLayout, colorHex);
                break;
            case "mood":
                setCardColor(moodCardLayout, colorHex);
                break;
            default:
                Log.w(TAG, "Unbekannter Karten-Typ: " + cardType);
                break;
        }
    }

    /**
     * Gibt die empfohlene Farbe für einen Wert zurück (ohne UI zu ändern)
     */
    public String getRecommendedCycleColor(long averageCycle) {
        if (averageCycle >= 21 && averageCycle <= 35) {
            return COLOR_NORMAL_GREEN;
        } else if (averageCycle >= 18 && averageCycle <= 40) {
            return COLOR_WARNING_ORANGE;
        } else {
            return COLOR_ABNORMAL_RED;
        }
    }

    /**
     * Gibt die empfohlene Farbe für Periodendauer zurück
     */
    public String getRecommendedPeriodColor(int averageDuration) {
        if (averageDuration >= 3 && averageDuration <= 7) {
            return COLOR_NORMAL_GREEN;
        } else if (averageDuration >= 2 && averageDuration <= 8) {
            return COLOR_WARNING_ORANGE;
        } else {
            return COLOR_ABNORMAL_RED;
        }
    }

    /**
     * Cleanup-Methode
     */
    public void cleanup() {
        Log.d(TAG, "CardColorManager cleanup");
        cycleCardLayout = null;
        periodCardLayout = null;
        painCardLayout = null;
        moodCardLayout = null;
    }
}