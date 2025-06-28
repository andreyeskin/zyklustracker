package at.fhj.andrey.zyklustracker.statistik;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import com.google.android.material.button.MaterialButton;

import at.fhj.andrey.zyklustracker.R;
import at.fhj.andrey.zyklustracker.datenbank.WohlbefindenEintrag;

/**
 * ChartManager - Verwaltung aller Diagramme und Charts
 *
 * Diese Klasse ist verantwortlich für:
 * - Initialisierung und Konfiguration aller Charts
 * - Füllen der Charts mit Daten
 * - Click-Handler und Interaktionen
 * - Chart-spezifische UI-Operationen
 *
 * WICHTIG: Alle Methoden müssen auf dem Main Thread aufgerufen werden!
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class ChartManager {

    private static final String TAG = "ChartManager";

    // Chart-Komponenten
    private LineChart cycleChart;
    private PieChart moodChart;
    private PieChart painChart;
    private PieChart bleedingChart;

    // Context für UI-Operationen
    private final Context context;

    // Referenz zu aktuellen Daten für Dialog-Berechnungen
    private StatistikData.FilteredData currentData;

    /**
     * Konstruktor
     */
    public ChartManager(Context context) {
        this.context = context;
        Log.d(TAG, "ChartManager initialisiert");
    }

    /**
     * Initialisiert alle Charts mit den UI-Komponenten
     * WICHTIG: Muss auf Main Thread aufgerufen werden!
     */
    /**
     * Initialisiert alle Charts mit den UI-Komponenten
     * WICHTIG: Muss auf Main Thread aufgerufen werden!
     */
    public void initializeCharts(LineChart cycleChart, PieChart moodChart, PieChart painChart, PieChart bleedingChart) {
        Log.d(TAG, "Initialisiere Charts...");

        this.cycleChart = cycleChart;
        this.moodChart = moodChart;
        this.painChart = painChart;
        this.bleedingChart = bleedingChart;

        // Charts konfigurieren
        if (cycleChart != null) {
            setupLineChart(cycleChart);
            Log.d(TAG, "LineChart konfiguriert");
        }

        if (moodChart != null) {
            setupPieChart(moodChart);
            Log.d(TAG, "Mood PieChart konfiguriert");
        }

        if (painChart != null) {
            setupPainPieChart(painChart);
            Log.d(TAG, "Pain PieChart konfiguriert");
        }

        if (bleedingChart != null) {
            setupBleedingPieChart(bleedingChart);
            Log.d(TAG, "Bleeding PieChart konfiguriert");
        }
    }

    /**
     * Konfiguriert das LineChart für Zykluslängen-Darstellung
     */
    private void setupLineChart(LineChart chart) {
        // Chart-Beschreibung setzen
        chart.getDescription().setText("Zykluslänge über Zeit");
        chart.getDescription().setTextSize(12f);

        // Interaktionen aktivieren
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        // Y-Achse konfigurieren (realistische Zykluslängen)
        chart.getAxisLeft().setAxisMinimum(20f);
        chart.getAxisLeft().setAxisMaximum(40f);
        chart.getAxisRight().setEnabled(false);

        // X-Achse konfigurieren
        chart.getXAxis().setPosition(com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setGranularity(1f);
    }

    /**
     * Konfiguriert das PieChart für Stimmungsverteilung
     */
    private void setupPieChart(PieChart chart) {
        chart.getDescription().setEnabled(false);

        // Prozentuale Werte anzeigen
        chart.setUsePercentValues(true);

        // Modernes Loch-Design in der Mitte
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(60f);
        chart.setTransparentCircleRadius(63f);

        // Schöner Text in der Mitte
        chart.setDrawCenterText(true);
        chart.setCenterText("Stimmung");
        chart.setCenterTextSize(18f);
        chart.setCenterTextColor(Color.parseColor("#333333"));

        // Legende konfigurieren
        chart.getLegend().setEnabled(true);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);
        chart.getLegend().setTextSize(13f);
        chart.getLegend().setTextColor(Color.parseColor("#666666"));
        chart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE);
        chart.getLegend().setFormSize(10f);
        chart.getLegend().setXEntrySpace(15f);
        chart.getLegend().setYEntrySpace(5f);

        // Interaktionen aktivieren
        chart.setRotationAngle(0);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        // Animation hinzufügen
        chart.animateY(1000);

        // Click-Listener für detaillierte Informationen
        chart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                if (e instanceof com.github.mikephil.charting.data.PieEntry) {
                    com.github.mikephil.charting.data.PieEntry pieEntry = (com.github.mikephil.charting.data.PieEntry) e;
                    showMoodDetails(pieEntry.getLabel(), (int) pieEntry.getValue());
                }
            }

            @Override
            public void onNothingSelected() {
                // Nichts tun
            }
        });

        // Text auf Segmenten deaktivieren
        chart.setDrawSliceText(false);
        chart.setDrawEntryLabels(false);
    }

    /**
     * Konfiguriert das PieChart für Schmerzverteilung
     */
    private void setupPainPieChart(PieChart chart) {
        chart.getDescription().setEnabled(false);

        // Prozentuale Werte anzeigen
        chart.setUsePercentValues(true);

        // Modernes Loch-Design in der Mitte
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(60f);
        chart.setTransparentCircleRadius(63f);

        // Schöner Text in der Mitte
        chart.setDrawCenterText(true);
        chart.setCenterText("Schmerz");
        chart.setCenterTextSize(18f);
        chart.setCenterTextColor(Color.parseColor("#333333"));

        // Legende konfigurieren (gleich wie Mood Chart)
        chart.getLegend().setEnabled(true);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);
        chart.getLegend().setTextSize(13f);
        chart.getLegend().setTextColor(Color.parseColor("#666666"));
        chart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE);
        chart.getLegend().setFormSize(10f);
        chart.getLegend().setXEntrySpace(15f);
        chart.getLegend().setYEntrySpace(5f);

        // Interaktionen aktivieren
        chart.setRotationAngle(0);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        // Animation hinzufügen
        chart.animateY(1000);

        // Click-Listener für detaillierte Informationen
        chart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                if (e instanceof com.github.mikephil.charting.data.PieEntry) {
                    com.github.mikephil.charting.data.PieEntry pieEntry = (com.github.mikephil.charting.data.PieEntry) e;
                    showPainDetails(pieEntry.getLabel(), (int) pieEntry.getValue());
                }
            }

            @Override
            public void onNothingSelected() {
                // Nichts tun
            }
        });

        // Text auf Segmenten deaktivieren
        chart.setDrawSliceText(false);
        chart.setDrawEntryLabels(false);
    }
    /**
     * Konfiguriert das PieChart für Blutungsverteilung
     */
    private void setupBleedingPieChart(PieChart chart) {
        chart.getDescription().setEnabled(false);

        // Prozentuale Werte anzeigen
        chart.setUsePercentValues(true);

        // Modernes Loch-Design in der Mitte
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(60f);
        chart.setTransparentCircleRadius(63f);

        // Schöner Text in der Mitte
        chart.setDrawCenterText(true);
        chart.setCenterText("Blutung");
        chart.setCenterTextSize(18f);
        chart.setCenterTextColor(Color.parseColor("#333333"));

        // Legende konfigurieren (gleich wie andere Charts)
        chart.getLegend().setEnabled(true);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);
        chart.getLegend().setTextSize(13f);
        chart.getLegend().setTextColor(Color.parseColor("#666666"));
        chart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE);
        chart.getLegend().setFormSize(10f);
        chart.getLegend().setXEntrySpace(15f);
        chart.getLegend().setYEntrySpace(5f);

        // Interaktionen aktivieren
        chart.setRotationAngle(0);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        // Animation hinzufügen
        chart.animateY(1000);

        // Click-Listener für detaillierte Informationen
        chart.setOnChartValueSelectedListener(new com.github.mikephil.charting.listener.OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                if (e instanceof com.github.mikephil.charting.data.PieEntry) {
                    com.github.mikephil.charting.data.PieEntry pieEntry = (com.github.mikephil.charting.data.PieEntry) e;
                    showBleedingDetails(pieEntry.getLabel(), (int) pieEntry.getValue());
                }
            }

            @Override
            public void onNothingSelected() {
                // Nichts tun
            }
        });

        // Text auf Segmenten deaktivieren
        chart.setDrawSliceText(false);
        chart.setDrawEntryLabels(false);
    }

    /**
     * Füllt das PieChart mit Blutungs-Verteilungs-Daten
     * WICHTIG: Muss auf Main Thread aufgerufen werden!
     */
    public void updateBleedingChart(StatistikData.FilteredData data) {
        if (bleedingChart == null) {
            Log.w(TAG, "BleedingChart ist null - kann nicht aktualisiert werden");
            return;
        }

        try {
            Log.d(TAG, "Aktualisiere Blutungs-Chart...");

            // Daten für Dialog-Berechnungen speichern
            this.currentData = data;

            // Blutungsverteilung aus den Daten sammeln
            Map<String, Integer> bleedingCounts = new HashMap<>();
            for (WohlbefindenEintrag entry : data.wellbeingEntries) {
                String bleeding = entry.getBlutungsstaerke();
                if (bleeding != null && !bleeding.isEmpty()) {
                    bleedingCounts.put(bleeding, bleedingCounts.getOrDefault(bleeding, 0) + 1);
                }
            }

            if (bleedingCounts.isEmpty()) {
                bleedingChart.setNoDataText("Keine Blutungsdaten verfügbar");
                bleedingChart.invalidate();
                return;
            }

            // Daten für das PieChart vorbereiten - SORTIERT nach Intensität
            List<com.github.mikephil.charting.data.PieEntry> entries = new ArrayList<>();

// Definiere die gewünschte Reihenfolge für Blutungsstärke
            String[] blutungsReihenfolge = {"Sehr leicht", "Leicht", "Mittel", "Stark"};

// Füge Einträge in der gewünschten Reihenfolge hinzu
            for (String blutungsart : blutungsReihenfolge) {
                if (bleedingCounts.containsKey(blutungsart)) {
                    int count = bleedingCounts.get(blutungsart);
                    entries.add(new com.github.mikephil.charting.data.PieEntry(count, blutungsart));
                }
            }

// Falls es unbekannte Kategorien gibt, am Ende hinzufügen
            for (Map.Entry<String, Integer> entry : bleedingCounts.entrySet()) {
                boolean istBereitsHinzugefuegt = false;
                for (String bekannteArt : blutungsReihenfolge) {
                    if (bekannteArt.equals(entry.getKey())) {
                        istBereitsHinzugefuegt = true;
                        break;
                    }
                }
                if (!istBereitsHinzugefuegt) {
                    entries.add(new com.github.mikephil.charting.data.PieEntry(entry.getValue().floatValue(), entry.getKey()));
                }
            }

            // PieDataSet erstellen und stylen
            com.github.mikephil.charting.data.PieDataSet dataSet =
                    new com.github.mikephil.charting.data.PieDataSet(entries, "");

            // Schöne Farben für die Segmente
            int[] colors = new int[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                String bleedingName = entries.get(i).getLabel().toLowerCase();
                if (bleedingName.contains("sehr leicht")) {
                    colors[i] = Color.parseColor("#F8BBD9"); // Sehr helles Rosa
                } else if (bleedingName.contains("leicht")) {
                    colors[i] = Color.parseColor("#E91E63"); // Helles Rosa
                } else if (bleedingName.contains("mittel")) {
                    colors[i] = Color.parseColor("#D81B60"); // Mittleres Rosa
                } else if (bleedingName.contains("stark")) {
                    colors[i] = Color.parseColor("#9C27B0"); // Dunkles Rosa
                } else {
                    colors[i] = Color.parseColor("#BDBDBD"); // Grau
                }
            }
            dataSet.setColors(colors);

            // Prozente auf Segmenten anzeigen
            dataSet.setDrawValues(true);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueFormatter(createPercentFormatter());

            // Schöne Abstände zwischen Segmenten
            dataSet.setSliceSpace(2f);
            dataSet.setSelectionShift(8f);

            // PieData erstellen und dem Chart zuweisen
            com.github.mikephil.charting.data.PieData pieData =
                    new com.github.mikephil.charting.data.PieData(dataSet);
            bleedingChart.setData(pieData);
            bleedingChart.invalidate();

            Log.d(TAG, "Blutungs-Chart erfolgreich aktualisiert mit " + entries.size() + " Kategorien");

        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Aktualisieren des Blutungs-Charts: " + e.getMessage(), e);
            bleedingChart.setNoDataText("Fehler beim Laden der Blutungsdaten");
            bleedingChart.invalidate();
        }
    }

    /**
     * Zeigt schöne Detail-Informationen für Blutung
     * Verwendet custom Dialog-Layout mit Statistik-Karten
     */
    private void showBleedingDetails(String bleedingName, int count) {
        showBeautifulChartDialog(bleedingName, count, "bleeding");
    }

    /**
     * Füllt das LineChart mit Zykluslängen-Daten
     * WICHTIG: Muss auf Main Thread aufgerufen werden!
     */
    public void updateCycleChart(StatistikData.FilteredData data) {
        if (cycleChart == null) {
            Log.w(TAG, "CycleChart ist null - kann nicht aktualisiert werden");
            return;
        }

        try {
            Log.d(TAG, "Aktualisiere Zyklus-Chart...");

            if (data.periodDates.size() < 2) {
                cycleChart.setNoDataText("Zu wenig Daten für Zyklustrends");
                cycleChart.invalidate();
                return;
            }

            // Berechnungen (gleich wie im StatistikManager)
            List<List<LocalDate>> periods = groupConsecutiveDates(data.periodDates);
            List<Long> cycleLengths = calculateCycleLengths(periods);

            if (cycleLengths.isEmpty()) {
                cycleChart.setNoDataText("Keine gültigen Zyklusdaten");
                cycleChart.invalidate();
                return;
            }

            // Daten für das LineChart vorbereiten
            List<com.github.mikephil.charting.data.Entry> entries = new ArrayList<>();
            for (int i = 0; i < cycleLengths.size(); i++) {
                entries.add(new com.github.mikephil.charting.data.Entry(i + 1, cycleLengths.get(i).floatValue()));
            }

            // LineDataSet erstellen und stylen
            com.github.mikephil.charting.data.LineDataSet dataSet =
                    new com.github.mikephil.charting.data.LineDataSet(entries, "Zykluslänge (Tage)");

            // Farben passend zur App
            dataSet.setColor(Color.parseColor("#D81B60"));
            dataSet.setCircleColor(Color.parseColor("#D81B60"));
            dataSet.setCircleHoleColor(Color.WHITE);
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(Color.parseColor("#D81B60"));
            dataSet.setFillAlpha(30);
            dataSet.setLineWidth(2f);
            dataSet.setCircleRadius(4f);
            dataSet.setValueTextSize(9f);

            // LineData erstellen und dem Chart zuweisen
            com.github.mikephil.charting.data.LineData lineData =
                    new com.github.mikephil.charting.data.LineData(dataSet);
            cycleChart.setData(lineData);
            cycleChart.invalidate();

            Log.d(TAG, "Zyklus-Chart erfolgreich aktualisiert mit " + cycleLengths.size() + " Datenpunkten");

        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Aktualisieren des Zyklus-Charts: " + e.getMessage(), e);
            cycleChart.setNoDataText("Fehler beim Laden der Zyklusdaten");
            cycleChart.invalidate();
        }
    }

    /**
     * Füllt das PieChart mit Stimmungsverteilungs-Daten
     * WICHTIG: Muss auf Main Thread aufgerufen werden!
     */
    public void updateMoodChart(StatistikData.FilteredData data) {
        if (moodChart == null) {
            Log.w(TAG, "MoodChart ist null - kann nicht aktualisiert werden");
            return;
        }

        try {
            Log.d(TAG, "Aktualisiere Stimmungs-Chart...");

            // Daten für Dialog-Berechnungen speichern
            this.currentData = data;

            // Stimmungsverteilung aus den Daten sammeln
            Map<String, Integer> moodCounts = new HashMap<>();
            for (WohlbefindenEintrag entry : data.wellbeingEntries) {
                String mood = entry.getStimmung();
                if (mood != null && !mood.isEmpty()) {
                    moodCounts.put(mood, moodCounts.getOrDefault(mood, 0) + 1);
                }
            }

            if (moodCounts.isEmpty()) {
                moodChart.setNoDataText("Keine Stimmungsdaten verfügbar");
                moodChart.invalidate();
                return;
            }

            // Daten für das PieChart vorbereiten
            List<com.github.mikephil.charting.data.PieEntry> entries = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
                String cleanMood = removeMoodEmojis(entry.getKey());
                entries.add(new com.github.mikephil.charting.data.PieEntry(entry.getValue().floatValue(), cleanMood));
            }

            // PieDataSet erstellen und stylen
            com.github.mikephil.charting.data.PieDataSet dataSet =
                    new com.github.mikephil.charting.data.PieDataSet(entries, "");

            // Schöne Farben für die Segmente
            int[] colors = new int[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                String moodName = entries.get(i).getLabel().toLowerCase();
                if (moodName.contains("sehr gut")) {
                    colors[i] = Color.parseColor("#2E7D32"); // Dunkelgrün
                } else if (moodName.contains("gut")) {
                    colors[i] = Color.parseColor("#66BB6A"); // Hellgrün
                } else if (moodName.contains("mittel")) {
                    colors[i] = Color.parseColor("#FF9800"); // Orange
                } else if (moodName.contains("schlecht")) {
                    colors[i] = Color.parseColor("#F44336"); // Rot
                } else {
                    colors[i] = Color.parseColor("#BDBDBD"); // Grau
                }
            }
            dataSet.setColors(colors);

            // Prozente auf Segmenten anzeigen
            dataSet.setDrawValues(true);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueFormatter(createPercentFormatter());

            // Schöne Abstände zwischen Segmenten
            dataSet.setSliceSpace(2f);
            dataSet.setSelectionShift(8f);

            // PieData erstellen und dem Chart zuweisen
            com.github.mikephil.charting.data.PieData pieData =
                    new com.github.mikephil.charting.data.PieData(dataSet);
            moodChart.setData(pieData);
            moodChart.invalidate();

            Log.d(TAG, "Stimmungs-Chart erfolgreich aktualisiert mit " + entries.size() + " Kategorien");

        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Aktualisieren des Stimmungs-Charts: " + e.getMessage(), e);
            moodChart.setNoDataText("Fehler beim Laden der Stimmungsdaten");
            moodChart.invalidate();
        }
    }

    /**
     * Füllt das PieChart mit Schmerz-Verteilungs-Daten
     * WICHTIG: Muss auf Main Thread aufgerufen werden!
     */
    public void updatePainChart(StatistikData.FilteredData data) {
        if (painChart == null) {
            Log.w(TAG, "PainChart ist null - kann nicht aktualisiert werden");
            return;
        }

        try {
            Log.d(TAG, "Aktualisiere Schmerz-Chart...");

            // Daten für Dialog-Berechnungen speichern
            this.currentData = data;

            // Schmerzverteilung aus den Daten sammeln
            Map<String, Integer> painCounts = new HashMap<>();
            for (WohlbefindenEintrag entry : data.wellbeingEntries) {
                String pain = entry.getSchmerzLevel();
                if (pain != null && !pain.isEmpty()) {
                    painCounts.put(pain, painCounts.getOrDefault(pain, 0) + 1);
                }
            }

            if (painCounts.isEmpty()) {
                painChart.setNoDataText("Keine Schmerzdaten verfügbar");
                painChart.invalidate();
                return;
            }

            // Daten für das PieChart vorbereiten - SORTIERT nach Intensität
            List<com.github.mikephil.charting.data.PieEntry> entries = new ArrayList<>();

// Definiere die gewünschte Reihenfolge für Schmerzen
            String[] schmerzReihenfolge = {"Keine", "Leicht", "Mittel", "Stark", "Krampfartig"};

// Füge Einträge in der gewünschten Reihenfolge hinzu
            for (String schmerzart : schmerzReihenfolge) {
                if (painCounts.containsKey(schmerzart)) {
                    int count = painCounts.get(schmerzart);
                    entries.add(new com.github.mikephil.charting.data.PieEntry(count, schmerzart));
                }
            }

// Falls es unbekannte Kategorien gibt, am Ende hinzufügen
            for (Map.Entry<String, Integer> entry : painCounts.entrySet()) {
                boolean istBereitsHinzugefuegt = false;
                for (String bekannteArt : schmerzReihenfolge) {
                    if (bekannteArt.equals(entry.getKey())) {
                        istBereitsHinzugefuegt = true;
                        break;
                    }
                }
                if (!istBereitsHinzugefuegt) {
                    entries.add(new com.github.mikephil.charting.data.PieEntry(entry.getValue().floatValue(), entry.getKey()));
                }
            }

            // PieDataSet erstellen und stylen
            com.github.mikephil.charting.data.PieDataSet dataSet =
                    new com.github.mikephil.charting.data.PieDataSet(entries, "");

            // Schöne Farben für die Segmente
            int[] colors = new int[entries.size()];
            for (int i = 0; i < entries.size(); i++) {
                String painName = entries.get(i).getLabel().toLowerCase();
                if (painName.contains("keine")) {
                    colors[i] = Color.parseColor("#2E7D32"); // Dunkelgrün
                } else if (painName.contains("leicht")) {
                    colors[i] = Color.parseColor("#66BB6A"); // Hellgrün
                } else if (painName.contains("mittel")) {
                    colors[i] = Color.parseColor("#FF9800"); // Orange
                } else if (painName.contains("stark")) {
                    colors[i] = Color.parseColor("#F44336"); // Rot
                } else if (painName.contains("krampfartig")) {
                    colors[i] = Color.parseColor("#B71C1C"); // Dunkelrot
                } else {
                    colors[i] = Color.parseColor("#BDBDBD"); // Grau
                }
            }
            dataSet.setColors(colors);

            // Prozente auf Segmenten anzeigen
            dataSet.setDrawValues(true);
            dataSet.setValueTextSize(12f);
            dataSet.setValueTextColor(Color.WHITE);
            dataSet.setValueFormatter(createPercentFormatter());

            // Schöne Abstände zwischen Segmenten
            dataSet.setSliceSpace(2f);
            dataSet.setSelectionShift(8f);

            // PieData erstellen und dem Chart zuweisen
            com.github.mikephil.charting.data.PieData pieData =
                    new com.github.mikephil.charting.data.PieData(dataSet);
            painChart.setData(pieData);
            painChart.invalidate();

            Log.d(TAG, "Schmerz-Chart erfolgreich aktualisiert mit " + entries.size() + " Kategorien");

        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Aktualisieren des Schmerz-Charts: " + e.getMessage(), e);
            painChart.setNoDataText("Fehler beim Laden der Schmerzdaten");
            painChart.invalidate();
        }
    }

    // ===== HILFSMETHODEN (aus StatistikManager kopiert) =====

    /**
     * Gruppiert aufeinanderfolgende Datumsangaben zu zusammenhängenden Perioden
     */
    private List<List<LocalDate>> groupConsecutiveDates(List<LocalDate> periodData) {
        List<List<LocalDate>> periods = new ArrayList<>();
        List<LocalDate> currentPeriod = new ArrayList<>();

        for (LocalDate date : periodData) {
            if (currentPeriod.isEmpty() ||
                    ChronoUnit.DAYS.between(currentPeriod.get(currentPeriod.size() - 1), date) == 1) {
                currentPeriod.add(date);
            } else {
                if (!currentPeriod.isEmpty()) {
                    periods.add(new ArrayList<>(currentPeriod));
                }
                currentPeriod = new ArrayList<>();
                currentPeriod.add(date);
            }
        }

        if (!currentPeriod.isEmpty()) {
            periods.add(currentPeriod);
        }

        return periods;
    }

    /**
     * Berechnet die Zykluslängen zwischen den Periodenbeginnen
     */
    private List<Long> calculateCycleLengths(List<List<LocalDate>> periods) {
        List<Long> cycleLengths = new ArrayList<>();

        for (int i = 1; i < periods.size(); i++) {
            LocalDate previousStart = periods.get(i-1).get(0);
            LocalDate currentStart = periods.get(i).get(0);
            long length = ChronoUnit.DAYS.between(previousStart, currentStart);

            // Nur realistische Zykluslängen berücksichtigen
            if (length >= 20 && length <= 40) {
                cycleLengths.add(length);
            }
        }

        return cycleLengths;
    }

    /**
     * Entfernt Emoji-Zeichen aus Stimmungsbezeichnungen
     */
    private String removeMoodEmojis(String mood) {
        return mood.replace("😀 ", "")
                .replace("🙂 ", "")
                .replace("😐 ", "")
                .replace("🙁 ", "");
    }

    /**
     * Custom ValueFormatter für gerundete Prozentangaben mit %-Zeichen
     */
    private ValueFormatter createPercentFormatter() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return Math.round(value) + "%";
            }
        };
    }

    // ===== DIALOG-METHODEN (vereinfacht) =====

    /**
     * Zeigt schöne Detail-Informationen für Stimmung
     * Verwendet custom Dialog-Layout mit Statistik-Karten
     */
    private void showMoodDetails(String moodName, int count) {
        showBeautifulChartDialog(moodName, count, "mood");
    }

    /**
     * Zeigt schöne Detail-Informationen für Schmerz
     * Verwendet custom Dialog-Layout mit Statistik-Karten
     */
    private void showPainDetails(String painName, int count) {
        showBeautifulChartDialog(painName, count, "pain");
    }

    /**
     * Cleanup-Methode
     */
    public void cleanup() {
        Log.d(TAG, "ChartManager cleanup");
        cycleChart = null;
        moodChart = null;
        painChart = null;
    }

    /**
     * Erstellt und zeigt einen schönen Chart-Detail-Dialog
     *
     * @param categoryName Name der Kategorie (z.B. "Keine", "Gut")
     * @param count Anzahl der Tage für diese Kategorie
     * @param type Typ der Daten ("mood" oder "pain")
     */
    private void showBeautifulChartDialog(String categoryName, int count, String type) {
        // Gesamtanzahl der Einträge berechnen
        int totalEntries = calculateTotalEntries(type);

        // Prozentsatz berechnen
        float percentage = totalEntries > 0 ? (count * 100f / totalEntries) : 0;

        // Custom Dialog-Layout aufblähen
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_chart_details, null);

        // UI-Komponenten finden
        TextView categoryEmoji = dialogView.findViewById(R.id.text_category_emoji);
        TextView categoryNameText = dialogView.findViewById(R.id.text_category_name);
        TextView dayCountText = dialogView.findViewById(R.id.text_day_count);
        TextView percentageText = dialogView.findViewById(R.id.text_percentage);
        TextView totalEntriesText = dialogView.findViewById(R.id.text_total_entries);
        TextView motivationMessage = dialogView.findViewById(R.id.text_motivation_message);
        ImageView closeButton = dialogView.findViewById(R.id.btn_close_dialog);
        MaterialButton okButton = dialogView.findViewById(R.id.btn_dialog_ok);

        // Dialog erstellen
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Dialog-Fenster transparent machen
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Daten einfüllen
        setupDialogContent(categoryEmoji, categoryNameText, dayCountText,
                percentageText, totalEntriesText, motivationMessage,
                categoryName, count, percentage, totalEntries, type);

        // Event-Handler einrichten
        closeButton.setOnClickListener(v -> dialog.dismiss());
        okButton.setOnClickListener(v -> dialog.dismiss());

        // Dialog anzeigen
        dialog.show();
    }

    /**
     * Füllt den Dialog mit den entsprechenden Daten
     */
    private void setupDialogContent(TextView categoryEmoji,
                                    TextView categoryNameText,
                                    TextView dayCountText,
                                    TextView percentageText,
                                    TextView totalEntriesText,
                                    TextView motivationMessage,
                                    String categoryName, int count, float percentage,
                                    int totalEntries, String type) {

        // Kategorie-Name setzen (ohne Emojis)
        String cleanName = removeMoodEmojis(categoryName);
        categoryNameText.setText(cleanName);

        // Emoji basierend auf Kategorie und Typ setzen
        String emoji = getEmojiForCategory(cleanName, type);
        categoryEmoji.setText(emoji);

        // Statistische Daten setzen
        dayCountText.setText(String.valueOf(count));
        percentageText.setText(String.format("%.1f%%", percentage));
        totalEntriesText.setText(totalEntries + " Einträgen");

        // Motivations-Nachricht generieren
        String motivation = generateMotivationMessage(cleanName, count, type);
        motivationMessage.setText(motivation);
    }

    /**
     * Berechnet die Gesamtanzahl der Einträge für einen bestimmten Typ
     */
    private int calculateTotalEntries(String type) {
        if (currentData == null) {
            Log.w(TAG, "Keine aktuellen Daten verfügbar für Berechnung");
            return 1; // Fallback um Division durch 0 zu vermeiden
        }

        if ("mood".equals(type)) {
            // Zähle alle Einträge mit Stimmungsdaten
            int count = 0;
            for (WohlbefindenEintrag entry : currentData.wellbeingEntries) {
                if (entry.getStimmung() != null && !entry.getStimmung().isEmpty()) {
                    count++;
                }
            }
            return Math.max(count, 1); // Mindestens 1 um Division durch 0 zu vermeiden
        } else if ("pain".equals(type)) {
            // Zähle alle Einträge mit Schmerzdaten
            int count = 0;
            for (WohlbefindenEintrag entry : currentData.wellbeingEntries) {
                if (entry.getSchmerzLevel() != null && !entry.getSchmerzLevel().isEmpty()) {
                    count++;
                }
            }
            return Math.max(count, 1); // Mindestens 1 um Division durch 0 zu vermeiden
        } else if ("bleeding".equals(type)) {
            // Zähle alle Einträge mit Blutungsdaten
            int count = 0;
            for (WohlbefindenEintrag entry : currentData.wellbeingEntries) {
                if (entry.getBlutungsstaerke() != null && !entry.getBlutungsstaerke().isEmpty()) {
                    count++;
                }
            }
            return Math.max(count, 1); // Mindestens 1 um Division durch 0 zu vermeiden
        }


        return 1; // Fallback
    }

    /**
     * Gibt das passende Emoji für eine Kategorie zurück
     */
    private String getEmojiForCategory(String categoryName, String type) {
        if ("mood".equals(type)) {
            // Stimmungs-Emojis
            switch (categoryName.toLowerCase()) {
                case "sehr gut": return "😀";
                case "gut": return "🙂";
                case "mittel": return "😐";
                case "schlecht": return "🙁";
                default: return "😊";
            }
        } else if ("pain".equals(type)) {
            // Schmerz-Emojis
            switch (categoryName.toLowerCase()) {
                case "keine": return "😊";
                case "leicht": return "😐";
                case "mittel": return "😕";
                case "stark": return "😖";
                case "krampfartig": return "😣";
                default: return "😊";
            }
        }  else if ("bleeding".equals(type)) {
        // Blutungs-Emojis
        switch (categoryName.toLowerCase()) {
            case "sehr leicht": return "💧";
            case "leicht": return "🩸";
            case "mittel": return "🔴";
            case "stark": return "🆘";
            default: return "💧";
        }
    }
        return "📊"; // Standard-Emoji
    }

    /**
     * Generiert eine motivierende Nachricht basierend auf der Kategorie
     */
    private String generateMotivationMessage(String categoryName, int count, String type) {
        if ("mood".equals(type)) {
            // Stimmungs-Nachrichten
            switch (categoryName.toLowerCase()) {
                case "sehr gut":
                case "gut":
                    return "Großartig! Du hattest viele positive Tage. Weiter so! 🌟";
                case "mittel":
                    return "Durchschnittliche Tage sind völlig normal. Achte auf deine Bedürfnisse.";
                case "schlecht":
                    return "Schwierige Zeiten gehören dazu. Sei geduldig mit dir selbst. 💚";
                default:
                    return "Jeder Tag ist ein neuer Anfang. Du schaffst das!";
            }
        } else if ("pain".equals(type)) {
            // Schmerz-Nachrichten
            switch (categoryName.toLowerCase()) {
                case "keine":
                    return "Schmerzfreie Tage! Du hattest keine spürbaren Beschwerden. 🌟";
                case "leicht":
                    return "Leichte Beschwerden sind manageable. Du handelst proaktiv!";
                case "mittel":
                    return "Mittlere Schmerzen können herausfordernd sein. Achte auf Entspannung.";
                case "stark":
                case "krampfartig":
                    return "Starke Schmerzen sind belastend. Sprich mit deinem Arzt darüber. 💚";
                default:
                    return "Jeder Körper ist anders. Höre auf deine Bedürfnisse.";
            }
        } else if ("bleeding".equals(type)) {
            // Blutungs-Nachrichten
            switch (categoryName.toLowerCase()) {
                case "sehr leicht":
                    return "Sehr leichte Blutung kann normal sein. Beobachte deinen Körper aufmerksam. 💧";
                case "leicht":
                    return "Leichte Blutung ist oft ein Zeichen für einen gesunden Zyklus. 💧";
                case "mittel":
                    return "Mittlere Blutungsstärke ist völlig normal für viele Frauen. 🩸";
                case "stark":
                    return "Starke Blutungen können anstrengend sein. Bitte suche einen Arzt auf, um die Ursache abklären zu lassen und mögliche Behandlung zu besprechen.";
                default:
                    return "Jeder Zyklus ist einzigartig. Du kennst deinen Körper am besten.";
            }

        }
        return "Danke, dass du deine Daten regelmäßig erfasst! 📈";
    }
}