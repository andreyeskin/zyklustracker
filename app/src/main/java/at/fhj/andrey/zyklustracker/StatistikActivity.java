package at.fhj.andrey.zyklustracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import at.fhj.andrey.zyklustracker.statistik.*;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * StatistikActivity - Neue, vereinfachte Version mit Manager-Pattern
 *
 * Diese refaktorierte Aktivität koordiniert nur noch die UI und delegiert
 * alle Berechnungen und Chart-Operationen an spezialisierte Manager:
 *
 * - StatistikManager: Alle Berechnungen und Datenbankzugriffe
 * - ChartManager: Chart-Konfiguration und -Updates
 * - CardColorManager: Kartenfarben-Management
 *
 * Die Activity fungiert nur noch als Koordinator und UI-Controller.
 * Alle schweren Operationen laufen in Background Threads.
 *
 * @author Andrey Eskin
 * @version 2.0 (Refactored)
 * @since Mai 2025
 */
public class StatistikActivity extends AppCompatActivity
        implements StatistikData.StatistikCallback, StatistikData.ChartCallback {

    private static final String TAG = "StatistikActivity";

    // ===== MANAGER-KOMPONENTEN =====
    private StatistikManager statistikManager;
    private ChartManager chartManager;
    private CardColorManager cardColorManager;

    // ===== UI-KOMPONENTEN =====
    private Spinner timeframeSpinner;
    private int currentTimeframeMonths = 3; // Standard: 3 Monate

    // Charts
    private LineChart cycleChart;
    private PieChart moodChart;
    private PieChart painChart;
    private PieChart bleedingChart;

    // Karten-Layouts für Farbmanagement
    private LinearLayout cycleCardLayout;
    private LinearLayout periodCardLayout;
    private LinearLayout painCardLayout;
    private LinearLayout moodCardLayout;

    // Text-Views für Statistik-Anzeige
    private TextView cycleText, rangeText;
    private TextView periodText, unitText;
    private TextView painText, painFrequencyText;
    private TextView moodText, moodFrequencyText;
    private LinearLayout symptomsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        Log.d(TAG, "StatistikActivity (refactored) gestartet");

        // 1. Manager initialisieren
        initializeManagers();

        // 2. UI-Komponenten initialisieren
        initializeUIComponents();

        // 3. Navigation konfigurieren
        setupBottomNavigation();

        // 4. Zeitraum-Filter konfigurieren
        setupTimeframeSpinner();

        // 5. Statistiken laden
        loadStatistics();
    }

    /**
     * Initialisiert alle Manager-Komponenten
     */
    private void initializeManagers() {
        Log.d(TAG, "Initialisiere Manager...");

        // StatistikManager für Berechnungen
        statistikManager = new StatistikManager(this);

        // ChartManager für Diagramme
        chartManager = new ChartManager(this);

        // CardColorManager für Kartenfarben
        cardColorManager = new CardColorManager();

        Log.d(TAG, "Alle Manager initialisiert");
    }

    /**
     * Initialisiert alle UI-Komponenten und verbindet sie mit den Managern
     */
    private void initializeUIComponents() {
        Log.d(TAG, "Initialisiere UI-Komponenten...");

        // Zeitraum-Spinner
        timeframeSpinner = findViewById(R.id.spinner_timeframe);

        // Charts finden und an ChartManager übergeben
        cycleChart = findViewById(R.id.chart_cycle_trends);
        moodChart = findViewById(R.id.chart_mood_distribution);
        painChart = findViewById(R.id.chart_pain_distribution);
        bleedingChart = findViewById(R.id.chart_bleeding_distribution);

        chartManager.initializeCharts(cycleChart, moodChart, painChart, bleedingChart);

        // Karten-Layouts für Farbmanagement
        cycleCardLayout = findViewById(R.id.layout_cycle_length);
        periodCardLayout = findViewById(R.id.layout_period_duration);
        painCardLayout = findViewById(R.id.layout_pain_stats);
        moodCardLayout = findViewById(R.id.layout_mood_stats);

        cardColorManager.initializeCardLayouts(cycleCardLayout, periodCardLayout,
                painCardLayout, moodCardLayout);

        // Text-Views für Statistiken
        cycleText = findViewById(R.id.text_cycle_length);
        rangeText = findViewById(R.id.text_cycle_range);
        periodText = findViewById(R.id.text_period_duration);
        unitText = findViewById(R.id.text_period_unit);
        painText = findViewById(R.id.text_most_frequent_pain);
        painFrequencyText = findViewById(R.id.text_pain_frequency);
        moodText = findViewById(R.id.text_most_frequent_mood);
        moodFrequencyText = findViewById(R.id.text_mood_frequency);
        symptomsContainer = findViewById(R.id.container_symptom_bars);

        Log.d(TAG, "UI-Komponenten initialisiert und mit Managern verbunden");
    }

    /**
     * Konfiguriert die Bottom Navigation
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.nav_statistics);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_cycle)  {
                startActivity(new Intent(this, ZyklusActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_wellbeing) {
                startActivity(new Intent(this, WohlbefindenActivity.class));
                finish();
                return true;
            }
            return true;
        });
    }

    /**
     * Konfiguriert den Zeitraum-Filter Spinner
     */
    private void setupTimeframeSpinner() {
        if (timeframeSpinner == null) return;

        timeframeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Zeitraum basierend auf Position setzen
                switch (position) {
                    case 0: // Letzte 3 Monate
                        currentTimeframeMonths = 3;
                        break;
                    case 1: // Letzte 6 Monate
                        currentTimeframeMonths = 6;
                        break;
                    case 2: // Letztes Jahr
                        currentTimeframeMonths = 12;
                        break;
                    default:
                        currentTimeframeMonths = 3;
                        break;
                }

                Log.d(TAG, "Zeitraum geändert auf " + currentTimeframeMonths + " Monate");

                // Statistiken neu laden mit neuem Zeitraum
                loadStatistics();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nichts tun
            }
        });
    }

    /**
     * Hauptmethode: Lädt alle Statistiken über die Manager
     */
    private void loadStatistics() {
        Log.d(TAG, "Lade Statistiken für " + currentTimeframeMonths + " Monate...");

        // UI auf "Lade-Zustand" setzen
        setUIToLoadingState();

        // StatistikManager beauftragen, alle Statistiken zu berechnen
        // Callback erfolgt über onStatistikenBerechnet()
        statistikManager.berechneAlleStatistiken(currentTimeframeMonths, this);

        // Charts separat aktualisieren
        // Callback erfolgt über onChartsAktualisieren()
        statistikManager.ladeGefilterteDaten(currentTimeframeMonths, this);
    }

    /**
     * Setzt die UI auf "Lade-Zustand" (Grau, Platzhalter-Texte)
     */
    private void setUIToLoadingState() {
        // Alle Karten auf Grau setzen
        cardColorManager.setAllCardsToLoadingState();

        // Platzhalter-Texte setzen
        if (cycleText != null) cycleText.setText("--");
        if (rangeText != null) rangeText.setText("Lädt...");
        if (periodText != null) periodText.setText("--");
        if (unitText != null) unitText.setText("Lädt...");
        if (painText != null) painText.setText("Lädt...");
        if (painFrequencyText != null) painFrequencyText.setText("--");
        if (moodText != null) moodText.setText("Lädt...");
        if (moodFrequencyText != null) moodFrequencyText.setText("--");

        // Symptom-Container leeren
        if (symptomsContainer != null) {
            symptomsContainer.removeAllViews();
            TextView loadingText = new TextView(this);
            loadingText.setText("Lade Symptom-Statistiken...");
            loadingText.setTextColor(0xFF666666);
            symptomsContainer.addView(loadingText);
        }
    }

    // ===== CALLBACK-IMPLEMENTIERUNGEN =====

    /**
     * Callback: Wird aufgerufen wenn StatistikManager alle Berechnungen abgeschlossen hat
     */
    @Override
    public void onStatistikenBerechnet(StatistikData.AllStatistics statistics) {
        // WICHTIG: Auf Main Thread wechseln für UI-Updates
        runOnUiThread(() -> {
            Log.d(TAG, "Statistiken empfangen, aktualisiere UI...");

            try {
                // 1. Kartenfarben aktualisieren
                cardColorManager.updateAllCardColors(statistics);

                // 2. Text-Werte in den Karten aktualisieren
                updateStatisticTexts(statistics);

                // 3. Symptom-Statistiken aktualisieren
                updateSymptomDisplay(statistics.symptoms);

                Log.d(TAG, "UI erfolgreich mit neuen Statistiken aktualisiert");

            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Aktualisieren der UI: " + e.getMessage(), e);
                Toast.makeText(this, "Fehler beim Anzeigen der Statistiken",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Callback: Wird aufgerufen bei Fehlern in der Statistik-Berechnung
     */
    @Override
    public void onFehler(String fehlermeldung) {
        runOnUiThread(() -> {
            Log.e(TAG, "Statistik-Fehler: " + fehlermeldung);
            Toast.makeText(this, "Fehler: " + fehlermeldung, Toast.LENGTH_LONG).show();

            // UI auf Fehler-Zustand setzen
            setUIToErrorState(fehlermeldung);
        });
    }

    /**
     * Callback: Wird aufgerufen wenn Charts aktualisiert werden sollen
     */
    @Override
    public void onChartsAktualisieren(StatistikData.FilteredData data) {
        runOnUiThread(() -> {
            Log.d(TAG, "Chart-Daten empfangen, aktualisiere Charts...");

            try {
                // Charts über ChartManager aktualisieren
                chartManager.updateCycleChart(data);
                chartManager.updateMoodChart(data);
                chartManager.updatePainChart(data);
                chartManager.updateBleedingChart(data);


                Log.d(TAG, "Alle Charts erfolgreich aktualisiert");

            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Aktualisieren der Charts: " + e.getMessage(), e);
                Toast.makeText(this, "Fehler beim Laden der Diagramme",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Callback: Wird aufgerufen bei Chart-Fehlern
     */
    @Override
    public void onChartFehler(String fehlermeldung) {
        runOnUiThread(() -> {
            Log.e(TAG, "Chart-Fehler: " + fehlermeldung);
            Toast.makeText(this, "Chart-Fehler: " + fehlermeldung, Toast.LENGTH_SHORT).show();
        });
    }

    // ===== UI-UPDATE METHODEN =====

    /**
     * Aktualisiert alle Text-Werte in den Statistik-Karten
     */
    private void updateStatisticTexts(StatistikData.AllStatistics statistics) {
        // Zykluslänge
        if (statistics.cycle.hasEnoughData) {
            if (cycleText != null) cycleText.setText(String.valueOf(statistics.cycle.average));
            if (rangeText != null) rangeText.setText(statistics.cycle.min + "-" + statistics.cycle.max + " Tage");
        } else {
            if (cycleText != null) cycleText.setText("--");
            if (rangeText != null) rangeText.setText("Zu wenig Daten");
        }

        // Periodendauer
        if (statistics.period.hasData) {
            if (periodText != null) periodText.setText(String.valueOf(statistics.period.averageDuration));
            if (unitText != null) unitText.setText("Tage Ø");
        } else {
            if (periodText != null) periodText.setText("--");
            if (unitText != null) unitText.setText("Keine Daten");
        }

        // Schmerz
        if (statistics.pain.hasData) {
            if (painText != null) painText.setText(statistics.pain.mostFrequentPain);
            if (painFrequencyText != null) {
                painFrequencyText.setText(Math.round(statistics.pain.percentage) + "% der Tage");
            }
        } else {
            if (painText != null) painText.setText("Keine Daten");
            if (painFrequencyText != null) painFrequencyText.setText("0% der Tage");
        }

        // Stimmung
        if (statistics.mood.hasData) {
            String cleanMood = removeMoodEmojis(statistics.mood.mostFrequentMood);
            if (moodText != null) moodText.setText(cleanMood);
            if (moodFrequencyText != null) {
                moodFrequencyText.setText(Math.round(statistics.mood.percentage) + "% der Tage");
            }
        } else {
            if (moodText != null) moodText.setText("Keine Daten");
            if (moodFrequencyText != null) moodFrequencyText.setText("0% der Tage");
        }
    }

    /**
     * Aktualisiert die Symptom-Anzeige
     */
    /**
     * Aktualisiert die Symptom-Anzeige mit schönen Balken
     */
    private void updateSymptomDisplay(StatistikData.SymptomStatistics symptoms) {
        if (symptomsContainer == null) return;

        // Container leeren
        symptomsContainer.removeAllViews();

        if (!symptoms.hasData) {
            TextView noDataText = new TextView(this);
            noDataText.setText("Keine Symptomdaten verfügbar");
            noDataText.setTextColor(0xFF666666);
            noDataText.setTextSize(14);
            symptomsContainer.addView(noDataText);
            return;
        }

        // Schöne Symptom-Balken erstellen
        erstelleSymptomBalken(symptomsContainer, symptoms.getSymptomFrequencies());
    }

    /**
     * Erstellt schöne Symptom-Balken im Container
     */
    private void erstelleSymptomBalken(LinearLayout container, Map<String, Integer> symptomFrequencies) {
        if (container == null) return;

        // Container leeren
        container.removeAllViews();

        if (symptomFrequencies == null || symptomFrequencies.isEmpty()) {
            // Fallback: Keine Symptome
            TextView noDataText = new TextView(this);
            noDataText.setText("Keine Symptomdaten");
            noDataText.setTextSize(14);
            noDataText.setTextColor(android.graphics.Color.parseColor("#666666"));
            container.addView(noDataText);
            return;
        }

        // Nach Häufigkeit sortieren
        List<Map.Entry<String, Integer>> sortedSymptoms = new ArrayList<>(symptomFrequencies.entrySet());
        sortedSymptoms.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Maximale Häufigkeit für Balken-Skalierung
        int maxCount = sortedSymptoms.isEmpty() ? 1 : sortedSymptoms.get(0).getValue();

        // Top-5-Symptome als schöne Balken erstellen
        for (int i = 0; i < Math.min(5, sortedSymptoms.size()); i++) {
            Map.Entry<String, Integer> entry = sortedSymptoms.get(i);
            erstelleEinzelnenSymptomBalken(container, entry.getKey(), entry.getValue(), maxCount);
        }
    }

    /**
     * Erstellt einen einzelnen schönen Symptom-Balken
     */
    private void erstelleEinzelnenSymptomBalken(LinearLayout container, String symptomName, int count, int maxCount) {
        try {
            // Versuche das item_symptom_bar Layout zu laden
            View barView = getLayoutInflater().inflate(R.layout.item_symptom_bar, container, false);

            // UI-Komponenten vom Layout holen
            TextView nameText = barView.findViewById(R.id.text_symptom_name);
            TextView countText = barView.findViewById(R.id.text_symptom_count);
            View progressBar = barView.findViewById(R.id.view_symptom_progress_bar);

            // Texte setzen
            if (nameText != null) nameText.setText(symptomName);
            if (countText != null) countText.setText(count + "x");

            // Balken-Breite basierend auf Prozentsatz berechnen
            if (progressBar != null && maxCount > 0) {
                float percentage = (float) count / maxCount;

                // LayoutParams für korrekte Breiteneinstellung
                android.widget.FrameLayout.LayoutParams params =
                        (android.widget.FrameLayout.LayoutParams) progressBar.getLayoutParams();

                // Breite dynamisch setzen nach Container-Messung
                progressBar.post(() -> {
                    View parentContainer = (View) progressBar.getParent();
                    int parentWidth = parentContainer.getWidth();
                    if (parentWidth > 0) {
                        params.width = (int) (parentWidth * percentage); // 100% max width
                        progressBar.setLayoutParams(params);
                    }
                });
            }

            container.addView(barView);

        }
        catch (Exception e) {
            Log.e(TAG, "Fehler beim Erstellen eines Symptom-Balken: " + e.getMessage(), e);
        }
    }
    /**
     * Setzt die UI auf Fehler-Zustand
     */
    private void setUIToErrorState(String errorMessage) {
        if (cycleText != null) cycleText.setText("Fehler");
        if (rangeText != null) rangeText.setText("Daten nicht verfügbar");
        if (periodText != null) periodText.setText("Fehler");
        if (unitText != null) unitText.setText("Daten nicht verfügbar");
        if (painText != null) painText.setText("Fehler beim Laden");
        if (painFrequencyText != null) painFrequencyText.setText("--");
        if (moodText != null) moodText.setText("Fehler beim Laden");
        if (moodFrequencyText != null) moodFrequencyText.setText("--");
    }

    /**
     * Entfernt Emoji-Zeichen aus Stimmungsbezeichnungen
     */
    private String removeMoodEmojis(String mood) {
        if (mood == null) return "";
        return mood.replace("😀 ", "")
                .replace("🙂 ", "")
                .replace("😐 ", "")
                .replace("🙁 ", "");
    }

    // ===== LIFECYCLE-METHODEN =====

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cleanup aller Manager
        if (statistikManager != null) {
            statistikManager.cleanup();
        }
        if (chartManager != null) {
            chartManager.cleanup();
        }
        if (cardColorManager != null) {
            cardColorManager.cleanup();
        }

        Log.d(TAG, "StatistikActivity und alle Manager cleanup abgeschlossen");
    }
}