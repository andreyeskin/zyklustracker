package at.fhj.andrey.zyklustracker;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import at.fhj.andrey.zyklustracker.datenbank.*;
        import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import android.widget.FrameLayout;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.formatter.ValueFormatter;

/**
 * StatistikActivity - Aktivität für die Anzeige von Zyklusstatistiken und Trends
 *
 * Diese Aktivität analysiert und präsentiert verschiedene Aspekte der erfassten Zyklusdaten:
 *
 * Hauptfunktionalitäten:
 * - Zyklusstatistiken (Durchschnittslänge, Variationsbereich)
 * - Stimmungsanalyse (häufigste Stimmung, Prozentverteilung)
 * - Symptomauswertung (häufigste Begleitsymptome)
 * - Schmerzlevel-Statistiken
 * - Grafische Darstellungen und Trends
 *
 * Datenquellen:
 * - PeriodeEintrag: Für Zykluslängen-Berechnungen
 * - WohlbefindenEintrag: Für Stimmung, Symptome und Schmerzlevel
 *
 * Berechnungslogik:
 * - Zykluslänge: Zeitspanne zwischen aufeinanderfolgenden Periodenbeginnen
 * - Nur realistische Zykluslängen (20-40 Tage) werden berücksichtigt
 * - Symptomhäufigkeit basiert auf JSON-Parsing der Symptom-Listen
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class StatistikActivity extends AppCompatActivity {

    // Datenbankzugriff
    private ZyklusDatenbank database;
    private WohlbefindenDao wellbeingDao;
    private ZyklusDao cycleDao;

    // Zeitraum-Filter
    private Spinner timeframeSpinner;
    private int currentTimeframeMonths = 3; // Standard: 3 Monate
    // Chart-Komponenten für grafische Darstellung
    private LineChart cycleChart;      // Liniendiagramm für Zykluslängen
    private PieChart moodChart;        // Kreisdiagramm für StimmungsverteilunginitializeCharts
    private PieChart painChart;        // Kreisdiagramm für Schmerzverteilung

    // UI-Komponenten für Karten-Färbung
    private LinearLayout cycleCardLayout;
    private LinearLayout periodCardLayout;
    private LinearLayout painCardLayout;
    private LinearLayout moodCardLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Datenbank initialisieren
        initializeDatabase();

        // Navigation konfigurieren
        setupBottomNavigation();

        // Statistiken laden und anzeigen
        loadAndDisplayStatistics();
    }

    /**
     * Initialisiert die Datenbankverbindung und die entsprechenden DAOs.
     */
    private void initializeDatabase() {
        database = ZyklusDatenbank.getInstanz(this);
        wellbeingDao = database.wohlbefindenDao();
        cycleDao = database.zyklusDao();
        // Spinner initialisieren
        timeframeSpinner = findViewById(R.id.spinner_timeframe);
        setupTimeframeSpinner();
    }
    /**
     * Konfiguriert den Zeitraum-Filter Spinner.
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

                // Statistiken neu laden mit neuem Zeitraum
                loadAndDisplayStatistics();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nichts tun
            }
        });
    }
    /**
     * Filtert Daten basierend auf dem ausgewählten Zeitraum.
     * @param allDates Alle verfügbaren Daten
     * @return Gefilterte Daten innerhalb des Zeitraums
     */
    private List<LocalDate> filterDatesByTimeframe(List<LocalDate> allDates) {
        if (allDates == null || allDates.isEmpty()) {
            return new ArrayList<>();
        }

        // Cutoff-Datum berechnen
        LocalDate cutoffDate = LocalDate.now().minusMonths(currentTimeframeMonths);

        // Nur Daten nach dem Cutoff-Datum behalten
        List<LocalDate> filteredDates = new ArrayList<>();
        for (LocalDate date : allDates) {
            if (date.isAfter(cutoffDate) || date.isEqual(cutoffDate)) {
                filteredDates.add(date);
            }
        }

        return filteredDates;
    }

    /**
     * Filtert Wohlbefinden-Einträge basierend auf dem Zeitraum.
     * @param allEntries Alle Wohlbefinden-Einträge
     * @return Gefilterte Einträge innerhalb des Zeitraums
     */
    private List<WohlbefindenEintrag> filterWellbeingByTimeframe(List<WohlbefindenEintrag> allEntries) {
        if (allEntries == null || allEntries.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDate cutoffDate = LocalDate.now().minusMonths(currentTimeframeMonths);

        List<WohlbefindenEintrag> filteredEntries = new ArrayList<>();
        for (WohlbefindenEintrag entry : allEntries) {
            if (entry.getDatum() != null &&
                    (entry.getDatum().isAfter(cutoffDate) || entry.getDatum().isEqual(cutoffDate))) {
                filteredEntries.add(entry);
            }
        }

        return filteredEntries;
    }

    /**
     * Initialisiert die Karton-Layouts für dynamische Färbung.
     */
    private void initializeCardLayouts() {
        cycleCardLayout = findViewById(R.id.layout_cycle_length);
        periodCardLayout = findViewById(R.id.layout_period_duration);
        painCardLayout = findViewById(R.id.layout_pain_stats);
        moodCardLayout = findViewById(R.id.layout_mood_stats);
    }

    /**
     * Konfiguriert die Bottom Navigation mit entsprechenden Intent-Handlern.
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
     * Lädt alle Statistiken und aktualisiert die entsprechenden UI-Komponenten.
     */
    private void loadAndDisplayStatistics() {
        // 1. Charts initialisieren
        initializeCharts();

        // 2. Card-Layouts initialisieren
        initializeCardLayouts();

        // 2. Zyklusstatistiken berechnen und anzeigen
        calculateAndDisplayCycleStatistics();

        // 3. Stimmungsstatistiken berechnen und anzeigen
        calculateAndDisplayMoodStatistics();

        // 4. Periodendauer-Statistiken berechnen und anzeigen
        calculateAndDisplayPeriodStatistics();

        // 5. Schmerz-Statistiken berechnen und anzeigen - DIESE ZEILE HINZUFÜGEN
        calculateAndDisplayPainStatistics();

        // 6. Symptomstatistiken berechnen und anzeigen
        calculateAndDisplaySymptomStatistics();

        // 7. Charts mit Daten füllen
        populateCharts();
    }

    /**
     * Berechnet Zyklusstatistiken basierend auf den Periodenstart-Daten.
     *
     * Algorithmus:
     * 1. Alle Periodenstart-Daten aus der Datenbank abrufen
     * 2. Aufeinanderfolgende Daten zu Perioden gruppieren
     * 3. Zykluslängen zwischen Periodenbeginnen berechnen
     * 4. Unrealistische Werte herausfiltern (< 20 oder > 40 Tage)
     * 5. Durchschnitt, Minimum und Maximum berechnen
     */
    private void calculateAndDisplayCycleStatistics() {
        // UI-Komponenten mit sicherer Referenzierung
        TextView cycleText = findViewById(R.id.text_cycle_length);
        TextView rangeText = findViewById(R.id.text_cycle_range);

        if (cycleText == null) {
            return; // Fallback wenn Layout-IDs nicht gefunden werden
        }

        // Alle Periodenstart-Daten aus der Datenbank abrufen
        List<LocalDate> allPeriodData = cycleDao.getAllePeriodeStartDaten();
        List<LocalDate> periodData = filterDatesByTimeframe(allPeriodData);

        if (periodData.size() < 2) {
            cycleText.setText("--");
            if (rangeText != null) {
                rangeText.setText("Zu wenig Daten");
            }
            return;
        }

        // Aufeinanderfolgende Daten zu Perioden gruppieren
        List<List<LocalDate>> periods = groupConsecutiveDates(periodData);

        // Zykluslängen zwischen Periodenbeginnen berechnen
        List<Long> cycleLengths = calculateCycleLengths(periods);

        if (cycleLengths.isEmpty()) {
            cycleText.setText("--");
            if (rangeText != null) {
                rangeText.setText("Keine Zyklusdaten");
            }
            // Grau für keine Daten
            if (cycleCardLayout != null) {
                cycleCardLayout.setBackgroundColor(android.graphics.Color.parseColor("#9E9E9E"));
            }
            return;
        }

        // Statistiken berechnen und anzeigen
        CycleStatistics stats = calculateStatistics(cycleLengths);
        cycleText.setText(String.valueOf(stats.average));
        if (rangeText != null) {
            rangeText.setText(stats.min + "-" + stats.max + " Tage");
        }

// Kartenfarbe basierend auf Normalwerten setzen
        updateCycleCardColor(stats.average);
    }

    /**
     * Gruppiert aufeinanderfolgende Datumsangaben zu zusammenhängenden Perioden.
     *
     * @param periodData Sortierte Liste aller Periodenstart-Daten
     * @return Liste von Perioden (jeweils Liste aufeinanderfolgender Tage)
     */
    private List<List<LocalDate>> groupConsecutiveDates(List<LocalDate> periodData) {
        List<List<LocalDate>> periods = new ArrayList<>();
        List<LocalDate> currentPeriod = new ArrayList<>();

        for (LocalDate date : periodData) {
            if (currentPeriod.isEmpty() ||
                    ChronoUnit.DAYS.between(currentPeriod.get(currentPeriod.size() - 1), date) == 1) {
                // Tag gehört zur aktuellen Periode
                currentPeriod.add(date);
            } else {
                // Neue Periode beginnt
                if (!currentPeriod.isEmpty()) {
                    periods.add(new ArrayList<>(currentPeriod));
                }
                currentPeriod = new ArrayList<>();
                currentPeriod.add(date);
            }
        }

        // Letzte Periode hinzufügen
        if (!currentPeriod.isEmpty()) {
            periods.add(currentPeriod);
        }

        return periods;
    }

    /**
     * Berechnet die Zykluslängen zwischen den Periodenbeginnen.
     *
     * @param periods Liste der gruppierten Perioden
     * @return Liste der Zykluslängen (gefiltert nach realistischen Werten)
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
     * Berechnet Durchschnitt, Minimum und Maximum einer Liste von Zykluslängen.
     *
     * @param cycleLengths Liste der Zykluslängen
     * @return CycleStatistics-Objekt mit berechneten Werten
     */
    private CycleStatistics calculateStatistics(List<Long> cycleLengths) {
        long sum = 0;
        long min = cycleLengths.get(0);
        long max = cycleLengths.get(0);

        for (Long length : cycleLengths) {
            sum += length;
            if (length < min) min = length;
            if (length > max) max = length;
        }

        long average = sum / cycleLengths.size();
        return new CycleStatistics(average, min, max);
    }

    /**
     * Berechnet und zeigt Stimmungsstatistiken basierend auf Wohlbefindensdaten an.
     * Ermittelt die häufigste Stimmung und deren prozentuale Verteilung.
     */
    private void calculateAndDisplayMoodStatistics() {
        // UI-Komponenten mit sicherer Referenzierung
        TextView moodText = findViewById(R.id.text_most_frequent_mood);
        TextView frequencyText = findViewById(R.id.text_mood_frequency);

        if (moodText == null) return;

        // Häufigste Stimmung aus der Datenbank abrufen
        // Gefilterte Daten verwenden statt direkte DB-Abfrage
        List<WohlbefindenEintrag> allEntries = wellbeingDao.getAlleEintraege();
        List<WohlbefindenEintrag> filteredEntries = filterWellbeingByTimeframe(allEntries);
        StimmungAnzahl mostFrequentMood = getMostFrequentMoodFromEntries(filteredEntries);

        if (mostFrequentMood == null || mostFrequentMood.stimmung == null) {
            moodText.setText("Keine Daten");
            if (frequencyText != null) {
                frequencyText.setText("0% der Tage");
            }

            if (moodCardLayout != null) {
                moodCardLayout.setBackgroundColor(android.graphics.Color.parseColor("#9E9E9E"));
            }
            return;
        }

        // Emojis aus der Stimmungsbezeichnung entfernen
        String moodWithoutEmoji = removeMoodEmojis(mostFrequentMood.stimmung);
        moodText.setText(moodWithoutEmoji);

        if (frequencyText != null) {
            // Prozentsatz berechnen
            int totalEntries = filteredEntries.size();
            int percentage = totalEntries > 0 ?
                    (mostFrequentMood.anzahl * 100 / totalEntries) : 0;
            frequencyText.setText(percentage + "% der Tage");
        }

// Kartenfarbe basierend auf Stimmung setzen
        updateMoodCardColor(mostFrequentMood.stimmung);
    }
    /**
     * Ermittelt die häufigste Stimmung aus gefilterten Einträgen.
     */
    private StimmungAnzahl getMostFrequentMoodFromEntries(List<WohlbefindenEintrag> entries) {
        if (entries.isEmpty()) return null;

        Map<String, Integer> moodCounts = new HashMap<>();

        // Stimmungen zählen
        for (WohlbefindenEintrag entry : entries) {
            String mood = entry.getStimmung();
            if (mood != null && !mood.isEmpty()) {
                moodCounts.put(mood, moodCounts.getOrDefault(mood, 0) + 1);
            }
        }

        if (moodCounts.isEmpty()) return null;

        // Häufigste Stimmung finden
        String mostFrequent = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequent = entry.getKey();
            }
        }

        if (mostFrequent == null) return null;

        StimmungAnzahl result = new StimmungAnzahl();
        result.stimmung = mostFrequent;
        result.anzahl = maxCount;
        return result;
    }
    /**
     * Ermittelt den häufigsten Schmerzlevel aus gefilterten Einträgen.
     */
    private StimmungAnzahl getMostFrequentPainFromEntries(List<WohlbefindenEintrag> entries) {
        if (entries.isEmpty()) return null;

        Map<String, Integer> painCounts = new HashMap<>();

        // Schmerzlevel zählen
        for (WohlbefindenEintrag entry : entries) {
            String pain = entry.getSchmerzLevel();
            if (pain != null && !pain.isEmpty()) {
                painCounts.put(pain, painCounts.getOrDefault(pain, 0) + 1);
            }
        }

        if (painCounts.isEmpty()) return null;

        // Häufigsten Schmerzlevel finden
        String mostFrequent = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : painCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequent = entry.getKey();
            }
        }

        if (mostFrequent == null) return null;

        StimmungAnzahl result = new StimmungAnzahl();
        result.stimmung = mostFrequent;
        result.anzahl = maxCount;
        return result;
    }
    /**
     * Extrahiert Symptom-Listen aus gefilterten Einträgen.
     */
    private List<String> getSymptomListsFromEntries(List<WohlbefindenEintrag> entries) {
        List<String> symptomLists = new ArrayList<>();
        Gson gson = new Gson();

        for (WohlbefindenEintrag entry : entries) {
            if (entry.getSymptome() != null && !entry.getSymptome().isEmpty()) {
                // Liste zu JSON String konvertieren
                String jsonString = gson.toJson(entry.getSymptome());
                symptomLists.add(jsonString);
            }
        }

        return symptomLists;
    }

    /**
     * Berechnet und zeigt Periodendauer-Statistiken an.
     */
    private void calculateAndDisplayPeriodStatistics() {
        TextView periodText = findViewById(R.id.text_period_duration);
        TextView unitText = findViewById(R.id.text_period_unit);

        if (periodText == null) return;

        // Alle echten Periodeneinträge abrufen
        List<LocalDate> allPeriodData = cycleDao.getAllePeriodeStartDaten();
        List<LocalDate> periodData = filterDatesByTimeframe(allPeriodData);


        if (periodData.isEmpty()) {
            periodText.setText("--");
            if (unitText != null) {
                unitText.setText("Keine Daten");
            }

            if (periodCardLayout != null) {
                periodCardLayout.setBackgroundColor(android.graphics.Color.parseColor("#9E9E9E"));
            }
            return;
        }

        // Perioden gruppieren und Durchschnittsdauer berechnen
        List<List<LocalDate>> periods = groupConsecutiveDates(periodData);

        if (periods.isEmpty()) {
            periodText.setText("--");
            if (unitText != null) {
                unitText.setText("Keine Daten");
            }

            if (periodCardLayout != null) {
                periodCardLayout.setBackgroundColor(android.graphics.Color.parseColor("#9E9E9E"));
            }
            return;
        }

        // Durchschnittliche Periodendauer berechnen
        int totalDays = 0;
        for (List<LocalDate> period : periods) {
            totalDays += period.size();
        }

        int averageDuration = totalDays / periods.size();
        periodText.setText(String.valueOf(averageDuration));
        if (unitText != null) {
            unitText.setText("Tage Ø");
        }

// Kartenfarbe basierend auf Normalwerten setzen
        updatePeriodCardColor(averageDuration);
    }

    /**
     * Berechnet und zeigt Schmerz-Statistiken an.
     */
    private void calculateAndDisplayPainStatistics() {
        TextView painText = findViewById(R.id.text_most_frequent_pain);
        TextView frequencyText = findViewById(R.id.text_pain_frequency);

        if (painText == null) return;

        // Häufigsten Schmerzlevel aus der Datenbank abrufen
        // Gefilterte Daten verwenden
        List<WohlbefindenEintrag> allEntries = wellbeingDao.getAlleEintraege();
        List<WohlbefindenEintrag> filteredEntries = filterWellbeingByTimeframe(allEntries);
        StimmungAnzahl mostFrequentPain = getMostFrequentPainFromEntries(filteredEntries);

        if (mostFrequentPain == null || mostFrequentPain.stimmung == null) {
            painText.setText("Keine Daten");
            if (frequencyText != null) {
                frequencyText.setText("0% der Tage");
            }

            if (painCardLayout != null) {
                painCardLayout.setBackgroundColor(android.graphics.Color.parseColor("#9E9E9E"));
            }
            return;
        }

        // Schmerzlevel anzeigen
        painText.setText(mostFrequentPain.stimmung);

        if (frequencyText != null) {
            // Prozentsatz berechnen
            int totalEntries = filteredEntries.size();
            int percentage = totalEntries > 0 ?
                    (mostFrequentPain.anzahl * 100 / totalEntries) : 0;
            frequencyText.setText(percentage + "% der Tage");
        }

// Kartenfarbe basierend auf Schmerzlevel setzen
        updatePainCardColor(mostFrequentPain.stimmung);
    }







    /**
     * Entfernt Emoji-Zeichen aus Stimmungsbezeichnungen für eine saubere Anzeige.
     *
     * @param mood Die Stimmungsbezeichnung mit möglichen Emojis
     * @return Bereinigte Stimmungsbezeichnung ohne Emojis
     */
    private String removeMoodEmojis(String mood) {
        return mood.replace("😀 ", "")
                .replace("🙂 ", "")
                .replace("😐 ", "")
                .replace("🙁 ", "");
    }

    /**
     * Berechnet und zeigt Symptomstatistiken an.
     * Analysiert die JSON-Listen der Begleitsymptome und ermittelt die häufigsten.
     *
     * Hinweis: Diese Methode erstellt dynamisch Symptom-Balken im container_symptom_bars
     */
    private void calculateAndDisplaySymptomStatistics() {
        // Container für dynamische Symptom-Balken
        LinearLayout symptomsContainer = findViewById(R.id.container_symptom_bars);

        if (symptomsContainer == null) return;

        // Bestehende Views löschen
        symptomsContainer.removeAllViews();

        // Alle Symptom-Listen aus der Datenbank abrufen
        // Gefilterte Symptom-Listen verwenden
        List<WohlbefindenEintrag> allEntries = wellbeingDao.getAlleEintraege();
        List<WohlbefindenEintrag> filteredEntries = filterWellbeingByTimeframe(allEntries);
        List<String> symptomLists = getSymptomListsFromEntries(filteredEntries);

        if (symptomLists.isEmpty()) {
            // Placeholder TextView für "keine Daten"
            TextView noDataText = new TextView(this);
            noDataText.setText("Keine Symptomdaten");
            noDataText.setTextSize(14);
            noDataText.setTextColor(0xFF666666);
            symptomsContainer.addView(noDataText);
            return;
        }

        // Symptomhäufigkeiten analysieren
        Map<String, Integer> symptomFrequencies = analyzeSymptomFrequencies(symptomLists);

        if (symptomFrequencies.isEmpty()) {
            TextView noSymptomsText = new TextView(this);
            noSymptomsText.setText("Keine Symptome erfasst");
            noSymptomsText.setTextSize(14);
            noSymptomsText.setTextColor(0xFF666666);
            symptomsContainer.addView(noSymptomsText);
            return;
        }

        // Top-5-Symptome anzeigen
        createSymptomBars(symptomsContainer, symptomFrequencies, 5);
    }

    /**
     * Erstellt einen einzelnen Symptom-Balken mit schöner Visualisierung wie im Design.
     *
     * @param container Der Parent-Container für den Balken
     * @param symptomName Name des Symptoms (z.B. "Rückenschmerzen")
     * @param count Häufigkeit des Symptoms
     * @param maxCount Maximale Häufigkeit für Prozentberechnung
     */
    private void createSymptomBar(LinearLayout container, String symptomName, int count, int maxCount) {
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
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) progressBar.getLayoutParams();

                // Breite dynamisch setzen nach Container-Messung
                progressBar.post(() -> {
                    View parentContainer = (View) progressBar.getParent();
                    int parentWidth = parentContainer.getWidth();
                    if (parentWidth > 0) {
                        params.width = (int) (parentWidth * percentage);
                        progressBar.setLayoutParams(params);
                    }
                });
            }

            container.addView(barView);

        } catch (Exception e) {
            // Fallback: Erstelle Balken manuell wenn Layout fehlt
            createManualSymptomBar(container, symptomName, count, maxCount);
        }
    }
    /**
     * Erstellt visuelle Balken für die häufigsten Symptome.
     *
     * @param container Der Container für die Symptom-Balken
     * @param symptomFrequencies Map der Symptomhäufigkeiten
     * @param topCount Anzahl der anzuzeigenden Top-Symptome
     */
    private void createSymptomBars(LinearLayout container, Map<String, Integer> symptomFrequencies, int topCount) {
        // Nach Häufigkeit sortieren
        List<Map.Entry<String, Integer>> sortedSymptoms = new ArrayList<>(symptomFrequencies.entrySet());
        sortedSymptoms.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Maximale Häufigkeit für Balken-Skalierung
        int maxCount = sortedSymptoms.isEmpty() ? 1 : sortedSymptoms.get(0).getValue();

        // Top-Symptome als Balken erstellen
        for (int i = 0; i < Math.min(topCount, sortedSymptoms.size()); i++) {
            Map.Entry<String, Integer> entry = sortedSymptoms.get(i);
            createSymptomBar(container, entry.getKey(), entry.getValue(), maxCount);
        }
    }

    /**
     * Fallback-Methode: Erstellt Symptom-Balken programmatisch falls XML-Layout fehlt.
     */
    private void createManualSymptomBar(LinearLayout container, String symptomName, int count, int maxCount) {
        // Äußerer Container für einen Symptom-Eintrag
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.VERTICAL);
        itemLayout.setPadding(0, 24, 0, 24); // Vertikale Abstände

        // Text-Zeile (Name links, Anzahl rechts)
        LinearLayout textRow = new LinearLayout(this);
        textRow.setOrientation(LinearLayout.HORIZONTAL);

        // Symptom-Name (linksbündig)
        TextView nameText = new TextView(this);
        nameText.setText(symptomName);
        nameText.setTextSize(16);
        nameText.setTextColor(0xFF333333);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        nameParams.weight = 1; // Nimmt verfügbaren Platz
        nameText.setLayoutParams(nameParams);

        // Anzahl (rechtsbündig)
        TextView countText = new TextView(this);
        countText.setText(count + "x");
        countText.setTextSize(16);
        countText.setTextColor(0xFF666666);

        textRow.addView(nameText);
        textRow.addView(countText);

        // Progress-Balken Container (grauer Hintergrund)
        FrameLayout progressContainer = new FrameLayout(this);
        progressContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (6 * getResources().getDisplayMetrics().density))); // 6dp Höhe
        progressContainer.setBackgroundColor(0xFFE8E8E8); // Hellgrau

        // Aktiver Balken (blauer Fortschritt)
        View progressBar = new View(this);
        float percentage = maxCount > 0 ? (float) count / maxCount : 0;
        FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
                (int) (300 * percentage * getResources().getDisplayMetrics().density),
                FrameLayout.LayoutParams.MATCH_PARENT);
        progressBar.setLayoutParams(progressParams);
        progressBar.setBackgroundColor(0xFF81D4FA); // Hellblau

        progressContainer.addView(progressBar);

        // Alles zusammenfügen
        itemLayout.addView(textRow);

        // Abstand zwischen Text und Balken
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (8 * getResources().getDisplayMetrics().density))); // 8dp Abstand
        itemLayout.addView(spacer);

        itemLayout.addView(progressContainer);
        container.addView(itemLayout);
    }


    /**
     * Analysiert die Häufigkeit aller Symptome aus den JSON-Listen.
     *
     * @param symptomLists Liste der JSON-Strings mit Symptom-Arrays
     * @return Map mit Symptomen als Schlüssel und Häufigkeiten als Werte
     */
    private Map<String, Integer> analyzeSymptomFrequencies(List<String> symptomLists) {
        Map<String, Integer> symptomCounter = new HashMap<>();
        Gson gson = new Gson();

        for (String listJson : symptomLists) {
            if (listJson != null && !listJson.isEmpty()) {
                try {
                    // JSON-String zu Liste parsen
                    List<String> symptoms = gson.fromJson(listJson,
                            new TypeToken<List<String>>(){}.getType());

                    if (symptoms != null) {
                        // Jedes Symptom zählen
                        for (String symptom : symptoms) {
                            symptomCounter.put(symptom,
                                    symptomCounter.getOrDefault(symptom, 0) + 1);
                        }
                    }
                } catch (Exception e) {
                    // Parsing-Fehler ignorieren und mit nächstem Eintrag fortfahren
                }
            }
        }

        return symptomCounter;
    }

    /**
     * Formatiert die häufigsten Symptome für die Anzeige.
     *
     * @param symptomFrequencies Map der Symptomhäufigkeiten
     * @param topCount Anzahl der anzuzeigenden Top-Symptome
     * @return Formatierter String mit den häufigsten Symptomen
     */
    private String formatTopSymptoms(Map<String, Integer> symptomFrequencies, int topCount) {
        // Nach Häufigkeit sortieren
        List<Map.Entry<String, Integer>> sortedSymptoms =
                new ArrayList<>(symptomFrequencies.entrySet());
        sortedSymptoms.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        // Text für Top-Symptome erstellen
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(topCount, sortedSymptoms.size()); i++) {
            Map.Entry<String, Integer> entry = sortedSymptoms.get(i);
            sb.append(i + 1).append(". ").append(entry.getKey())
                    .append(" (").append(entry.getValue()).append("x)\n");
        }

        return sb.toString().trim();
    }

    /**
     * Hilfsklasse für die Speicherung von Zyklusstatistiken.
     */
    private static class CycleStatistics {
        final long average;
        final long min;
        final long max;

        CycleStatistics(long average, long min, long max) {
            this.average = average;
            this.min = min;
            this.max = max;
        }
    }

    /**
     * Initialisiert die Chart-Komponenten und konfiguriert deren Aussehen.
     */
    private void initializeCharts() {
        // LineChart für Zykluslängen-Trends finden
        cycleChart = findViewById(R.id.chart_cycle_trends);

        // PieChart für Stimmungsverteilung finden
        moodChart = findViewById(R.id.chart_mood_distribution);

        // PieChart für Schmerzverteilung finden
        painChart = findViewById(R.id.chart_pain_distribution);

        // LineChart konfigurieren (falls vorhanden)
        if (cycleChart != null) {
            setupLineChart(cycleChart);
        }

        // PieChart konfigurieren (falls vorhanden)
        if (moodChart != null) {
            setupPieChart(moodChart);
        }
        // PieChart für Schmerz konfigurieren
        if (painChart != null) {
            setupPainPieChart(painChart);
        }
    }

    /**
     * Konfiguriert das LineChart für Zykluslängen-Darstellung.
     *
     * @param chart Das zu konfigurierende LineChart
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
     * Konfiguriert das PieChart für Stimmungsverteilung mit modernem Design.
     *
     * @param chart Das zu konfigurierende PieChart
     */
    private void setupPieChart(PieChart chart) {

        chart.getDescription().setEnabled(false);

        // Prozentuale Werte anzeigen
        chart.setUsePercentValues(true);

        // Modernes Loch-Design in der Mitte - größeres Loch für dickere Segmente
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(60f);    // Mittel-dick
        chart.setTransparentCircleRadius(63f);

        // Schöner Text in der Mitte
        chart.setDrawCenterText(true);
        chart.setCenterText("Stimmung");
        chart.setCenterTextSize(18f);
        chart.setCenterTextColor(android.graphics.Color.parseColor("#333333"));

        // Legende konfigurieren

        chart.getLegend().setEnabled(true);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);
        chart.getLegend().setTextSize(13f);
        chart.getLegend().setTextColor(android.graphics.Color.parseColor("#666666"));
        chart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE);
        chart.getLegend().setFormSize(10f);
        chart.getLegend().setXEntrySpace(15f);
        chart.getLegend().setYEntrySpace(5f);

        // Interaktionen aktivieren
        chart.setRotationAngle(0);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        // Animation hinzufügen
        chart.animateY(1000); // 1 Sekunde Animation beim Laden

        // Click-Listener für detaillierte Informationen hinzufügen
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
                // Nichts tun wenn nichts ausgewählt ist
            }
        });
        // Text auf Segmenten komplett deaktivieren
        chart.setDrawSliceText(false);
        chart.setDrawEntryLabels(false);
    }

    /**
     * Konfiguriert das PieChart für Schmerzverteilung mit modernem Design.
     *
     * @param chart Das zu konfigurierende PieChart
     */
    private void setupPainPieChart(PieChart chart) {
        chart.getDescription().setEnabled(false);

        // Prozentuale Werte anzeigen
        chart.setUsePercentValues(true);

        // Modernes Loch-Design in der Mitte - größeres Loch für dickere Segmente
        chart.setDrawHoleEnabled(true);
        chart.setHoleRadius(60f);    // Dick
        chart.setTransparentCircleRadius(63f);

        // Schöner Text in der Mitte
        chart.setDrawCenterText(true);
        chart.setCenterText("Schmerz");
        chart.setCenterTextSize(18f);
        chart.setCenterTextColor(android.graphics.Color.parseColor("#333333"));

        // Legende konfigurieren
        chart.getLegend().setEnabled(true);
        chart.getLegend().setVerticalAlignment(com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM);
        chart.getLegend().setHorizontalAlignment(com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);
        chart.getLegend().setTextSize(13f);
        chart.getLegend().setTextColor(android.graphics.Color.parseColor("#666666"));
        chart.getLegend().setForm(com.github.mikephil.charting.components.Legend.LegendForm.CIRCLE);
        chart.getLegend().setFormSize(10f);
        chart.getLegend().setXEntrySpace(15f);
        chart.getLegend().setYEntrySpace(5f);

        // Interaktionen aktivieren
        chart.setRotationAngle(0);
        chart.setRotationEnabled(true);
        chart.setHighlightPerTapEnabled(true);

        // Animation hinzufügen
        chart.animateY(1000); // 1 Sekunde Animation beim Laden

        // Click-Listener für detaillierte Informationen hinzufügen
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
                // Nichts tun wenn nichts ausgewählt ist
            }
        });

        // Text auf Segmenten komplett deaktivieren
        chart.setDrawSliceText(false);
        chart.setDrawEntryLabels(false);
    }

    /**
     * Füllt alle Charts mit Daten aus der Datenbank.
     */
    private void populateCharts() {
        // Zykluslängen-Chart mit Daten füllen
        populateCycleChart();

        // Stimmungs-Chart mit Daten füllen
        populateMoodChart();
        // Schmerz-Chart mit Daten füllen
        populatePainChart();
    }
    /**
     * Füllt das LineChart mit Zykluslängen-Daten aus der Datenbank.
     * Zeigt die Entwicklung der Zykluslängen über Zeit als Linie an.
     */
    private void populateCycleChart() {
        // Prüfen ob Chart verfügbar ist
        if (cycleChart == null) return;

        // Zykluslängen-Daten aus der Datenbank abrufen
        List<LocalDate> allPeriodData = cycleDao.getAllePeriodeStartDaten();
        List<LocalDate> periodData = filterDatesByTimeframe(allPeriodData);
        List<List<LocalDate>> periods = groupConsecutiveDates(periodData);
        List<Long> cycleLengths = calculateCycleLengths(periods);

        // Prüfen ob genug Daten vorhanden sind
        if (cycleLengths.isEmpty()) {
            cycleChart.setNoDataText("Noch nicht genug Daten für Zyklustrends");
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
        // Farben passend zu Ihrer App
        dataSet.setColor(android.graphics.Color.parseColor("#D81B60"));
        dataSet.setCircleColor(android.graphics.Color.parseColor("#D81B60"));
        dataSet.setCircleHoleColor(android.graphics.Color.WHITE);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(android.graphics.Color.parseColor("#D81B60"));
        dataSet.setFillAlpha(30); // Leichte Transparenz wie in Ihrer App
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(9f);

        // LineData erstellen und dem Chart zuweisen
        com.github.mikephil.charting.data.LineData lineData =
                new com.github.mikephil.charting.data.LineData(dataSet);
        cycleChart.setData(lineData);
        cycleChart.invalidate(); // Chart neu zeichnen
    }

    /**
     * Füllt das PieChart mit Stimmungsverteilungs-Daten aus der Datenbank.
     * Zeigt prozentuale Verteilung der verschiedenen Stimmungen als Kreisdiagramm.
     */
    private void populateMoodChart() {
        // Prüfen ob Chart verfügbar ist
        if (moodChart == null) return;

        // Stimmungsverteilung aus der Datenbank sammeln
        Map<String, Integer> moodCounts = getMoodDistribution();

        // Prüfen ob Daten vorhanden sind
        if (moodCounts.isEmpty()) {
            moodChart.setNoDataText("Noch keine Stimmungsdaten");
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
                colors[i] = android.graphics.Color.parseColor("#2E7D32"); // Dunkelgrün für "Sehr gut"
            } else if (moodName.contains("gut")) {
                colors[i] = android.graphics.Color.parseColor("#66BB6A"); // Hellgrün für "Gut"
            } else if (moodName.contains("mittel")) {
                colors[i] = android.graphics.Color.parseColor("#FF9800"); // Orange für "Mittel"
            } else if (moodName.contains("schlecht")) {
                colors[i] = android.graphics.Color.parseColor("#F44336"); // Hellrot für "Schlecht"
            } else {
                colors[i] = android.graphics.Color.parseColor("#BDBDBD"); // Hellgrau für unbekannte
            }
        }
        dataSet.setColors(colors);

        // Prozente auf Segmenten anzeigen
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(12f); //
        dataSet.setValueTextColor(android.graphics.Color.WHITE); //
        dataSet.setValueFormatter(createPercentFormatter()); //

// Schöne Abstände zwischen Segmenten
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(8f);




        // PieData erstellen und dem Chart zuweisen
        com.github.mikephil.charting.data.PieData pieData =
                new com.github.mikephil.charting.data.PieData(dataSet);
        moodChart.setData(pieData);
        moodChart.invalidate(); // Chart neu zeichnen
    }

    /**
     * Füllt das PieChart mit Schmerz-Verteilungs-Daten aus der Datenbank.
     */
    private void populatePainChart() {
        // Prüfen ob Chart verfügbar ist
        if (painChart == null) return;

        // Schmerzverteilung aus der Datenbank sammeln
        Map<String, Integer> painCounts = getPainDistribution();

        // Prüfen ob Daten vorhanden sind
        if (painCounts.isEmpty()) {
            painChart.setNoDataText("Noch keine Schmerzdaten");
            painChart.invalidate();
            return;
        }

        // Daten für das PieChart vorbereiten
        List<com.github.mikephil.charting.data.PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : painCounts.entrySet()) {
            entries.add(new com.github.mikephil.charting.data.PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        // PieDataSet erstellen und stylen
        com.github.mikephil.charting.data.PieDataSet dataSet =
                new com.github.mikephil.charting.data.PieDataSet(entries, "");

        // Schöne Farben für die Segmente - gleiche Farben wie Stimmung
        int[] colors = new int[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            String painName = entries.get(i).getLabel().toLowerCase();
            if (painName.contains("keine")) {
                colors[i] = android.graphics.Color.parseColor("#2E7D32"); // Dunkelgrün für "Keine"
            } else if (painName.contains("leicht")) {
                colors[i] = android.graphics.Color.parseColor("#66BB6A"); // Hellgrün für "Leicht"
            } else if (painName.contains("mittel")) {
                colors[i] = android.graphics.Color.parseColor("#FF9800"); // Orange für "Mittel"
            } else if (painName.contains("stark")) {
                colors[i] = android.graphics.Color.parseColor("#F44336"); // Rot für "Stark"
            } else if (painName.contains("krampfartig")) {
                colors[i] = android.graphics.Color.parseColor("#B71C1C"); // Dunkelrot für "Krampfartig"
            } else {
                colors[i] = android.graphics.Color.parseColor("#BDBDBD"); // Grau für unbekannte
            }
        }
        dataSet.setColors(colors);

        // Prozente auf Segmenten anzeigen
        dataSet.setDrawValues(true);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(android.graphics.Color.WHITE);
        dataSet.setValueFormatter(createPercentFormatter());

        // Schöne Abstände zwischen Segmenten
        dataSet.setSliceSpace(2f);
        dataSet.setSelectionShift(8f);

        // PieData erstellen und dem Chart zuweisen
        com.github.mikephil.charting.data.PieData pieData =
                new com.github.mikephil.charting.data.PieData(dataSet);
        painChart.setData(pieData);
        painChart.invalidate(); // Chart neu zeichnen
    }

    /**
     * Erstellt eine Verteilung aller Stimmungen aus der Datenbank.
     * Sammelt alle Stimmungseinträge und zählt deren Häufigkeit.
     *
     * @return Map mit Stimmungen als Schlüssel und Häufigkeiten als Werte
     */
    private Map<String, Integer> getMoodDistribution() {
        Map<String, Integer> moodCounts = new HashMap<>();

        // Alle Wohlbefinden-Einträge aus der Datenbank abrufen
        List<WohlbefindenEintrag> allEntries = wellbeingDao.getAlleEintraege();
        List<WohlbefindenEintrag> entries = filterWellbeingByTimeframe(allEntries);

        // Jede Stimmung zählen
        for (WohlbefindenEintrag entry : entries) {
            String mood = entry.getStimmung();
            if (mood != null && !mood.isEmpty()) {
                moodCounts.put(mood, moodCounts.getOrDefault(mood, 0) + 1);
            }
        }

        return moodCounts;
    }
    /**
     * Erstellt eine Verteilung aller Schmerzlevel aus der Datenbank.
     */
    private Map<String, Integer> getPainDistribution() {
        Map<String, Integer> painCounts = new HashMap<>();

        // Alle Wohlbefinden-Einträge aus der Datenbank abrufen
        List<WohlbefindenEintrag> allEntries = wellbeingDao.getAlleEintraege();
        List<WohlbefindenEintrag> entries = filterWellbeingByTimeframe(allEntries);

        // Jeden Schmerzlevel zählen
        for (WohlbefindenEintrag entry : entries) {
            String pain = entry.getSchmerzLevel();
            if (pain != null && !pain.isEmpty()) {
                painCounts.put(pain, painCounts.getOrDefault(pain, 0) + 1);
            }
        }

        return painCounts;
    }
    /**
     * Zeigt detaillierte Informationen zu einer Stimmung in einem modernen Dialog an.
     * Dialog entspricht dem Design der App mit Rosa-Akzenten und modernen Elementen.
     *
     * @param moodName Name der Stimmung (z.B. "Gut", "Schlecht")
     * @param count Anzahl der Tage mit dieser Stimmung
     */
    private void showMoodDetails(String moodName, int count) {
        // Gesamtanzahl der Einträge für Prozentberechnung
        List<WohlbefindenEintrag> allEntries = wellbeingDao.getAlleEintraege();
        List<WohlbefindenEintrag> filteredEntries = filterWellbeingByTimeframe(allEntries);
        int totalEntries = filteredEntries.size();
        float percentage = totalEntries > 0 ? (count * 100f / totalEntries) : 0;

        // Passende Farbe und Emoji je nach Stimmung
        String emoji = getMoodEmoji(moodName);
        String description = getMoodDescription(moodName);
        int accentColor = getMoodColor(moodName);

        // Custom Dialog Layout erstellen
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(32, 32, 32, 32); // Увеличенные отступы
        dialogLayout.setBackgroundResource(R.drawable.dialog_background_rounded);



        // Header mit Emoji und Stimmung
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(0, 0, 0, 24);

        TextView emojiText = new TextView(this);
        emojiText.setText(emoji);
        emojiText.setTextSize(32f);
        emojiText.setPadding(0, 0, 16, 0);

        TextView titleText = new TextView(this);
        titleText.setText(moodName);
        titleText.setTextSize(24f);
        titleText.setTextColor(accentColor);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);

        headerLayout.addView(emojiText);
        headerLayout.addView(titleText);

        // Statistik-Karten Container
        LinearLayout statsContainer = new LinearLayout(this);
        statsContainer.setOrientation(LinearLayout.VERTICAL);
        statsContainer.setPadding(0, 0, 0, 24);

        // Statistik-Karte: Anzahl Tage
        LinearLayout daysCard = createStatCard("📊", "Anzahl Tage", String.valueOf(count), "#42A5F5");

        // Statistik-Karte: Prozentsatz
        LinearLayout percentCard = createStatCard("📈", "Prozentsatz", String.format("%.1f%%", percentage), "#D81B60");

        // Statistik-Karte: Von insgesamt
        LinearLayout totalCard = createStatCard("📅", "Von insgesamt", totalEntries + " Einträgen", "#FBC02D");

        statsContainer.addView(daysCard);
        statsContainer.addView(percentCard);
        statsContainer.addView(totalCard);

        // Beschreibungstext
        TextView descriptionText = new TextView(this);
        descriptionText.setText(description);
        descriptionText.setTextSize(16f);
        descriptionText.setTextColor(android.graphics.Color.parseColor("#616161"));
        descriptionText.setPadding(0, 0, 0, 32);
        descriptionText.setLineSpacing(1.2f, 1.2f);

        // OK Button im App-Stil с rounded corners
        android.widget.Button okButton = new android.widget.Button(this);
        okButton.setText("OK");
        okButton.setTextColor(android.graphics.Color.WHITE);
        okButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D81B60")));
        okButton.setTextSize(16f);
        okButton.setTypeface(null, android.graphics.Typeface.BOLD);
        okButton.setPadding(48, 20, 48, 20);

        // Rounded corners button
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            okButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_rounded));
        }

        // Button Layout
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setGravity(android.view.Gravity.CENTER);
        buttonLayout.addView(okButton);

        // Alles zusammenfügen
        dialogLayout.addView(headerLayout);
        dialogLayout.addView(statsContainer);
        dialogLayout.addView(descriptionText);
        dialogLayout.addView(buttonLayout);

        // Dialog erstellen und anzeigen
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogLayout)
                .setCancelable(true)
                .create();

        // OK Button Click Handler
        okButton.setOnClickListener(v -> dialog.dismiss());

        // Dialog anzeigen
        dialog.show();

        // Dialog-Fenster anpassen background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            // dim effect
            dialog.getWindow().setDimAmount(0.5f);
        }
    }

    /**
     * Zeigt detaillierte Informationen zu einem Schmerzlevel in einem Dialog an.
     */
    private void showPainDetails(String painName, int count) {
        // Gesamtanzahl der Einträge für Prozentberechnung
        List<WohlbefindenEintrag> allEntries = wellbeingDao.getAlleEintraege();
        List<WohlbefindenEintrag> filteredEntries = filterWellbeingByTimeframe(allEntries);
        int totalEntries = filteredEntries.size();
        float percentage = totalEntries > 0 ? (count * 100f / totalEntries) : 0;

        // Passende Farbe und Emoji je nach Schmerzlevel
        String emoji = getPainEmoji(painName);
        String description = getPainDescription(painName);
        int accentColor = getPainColor(painName);

        // Custom Dialog Layout erstellen
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(32, 32, 32, 32); // Увеличенные отступы
        dialogLayout.setBackgroundResource(R.drawable.dialog_background_rounded);


        // Header mit Emoji und Schmerzlevel
        LinearLayout headerLayout = new LinearLayout(this);
        headerLayout.setOrientation(LinearLayout.HORIZONTAL);
        headerLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        headerLayout.setPadding(0, 0, 0, 24);

        TextView emojiText = new TextView(this);
        emojiText.setText(emoji);
        emojiText.setTextSize(32f);
        emojiText.setPadding(0, 0, 16, 0);

        TextView titleText = new TextView(this);
        titleText.setText(painName);
        titleText.setTextSize(24f);
        titleText.setTextColor(accentColor);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);

        headerLayout.addView(emojiText);
        headerLayout.addView(titleText);

        // Statistik-Karten Container
        LinearLayout statsContainer = new LinearLayout(this);
        statsContainer.setOrientation(LinearLayout.VERTICAL);
        statsContainer.setPadding(0, 0, 0, 24);

        // Statistik-Karten
        LinearLayout daysCard = createStatCard("📊", "Anzahl Tage", String.valueOf(count), "#42A5F5");
        LinearLayout percentCard = createStatCard("📈", "Prozentsatz", String.format("%.1f%%", percentage), "#D81B60");
        LinearLayout totalCard = createStatCard("📅", "Von insgesamt", totalEntries + " Einträgen", "#FBC02D");

        statsContainer.addView(daysCard);
        statsContainer.addView(percentCard);
        statsContainer.addView(totalCard);

        // Beschreibungstext
        TextView descriptionText = new TextView(this);
        descriptionText.setText(description);
        descriptionText.setTextSize(16f);
        descriptionText.setTextColor(android.graphics.Color.parseColor("#616161"));
        descriptionText.setPadding(0, 0, 0, 32);
        descriptionText.setLineSpacing(1.2f, 1.2f);

        // OK Button im App-Stil с rounded corners
        android.widget.Button okButton = new android.widget.Button(this);
        okButton.setText("OK");
        okButton.setTextColor(android.graphics.Color.WHITE);
        okButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#D81B60")));
        okButton.setTextSize(16f);
        okButton.setTypeface(null, android.graphics.Typeface.BOLD);
        okButton.setPadding(48, 20, 48, 20);

        // Rounded corners button
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            okButton.setBackground(ContextCompat.getDrawable(this, R.drawable.button_rounded));
        }

        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setGravity(android.view.Gravity.CENTER);
        buttonLayout.addView(okButton);

        // Alles zusammenfügen
        dialogLayout.addView(headerLayout);
        dialogLayout.addView(statsContainer);
        dialogLayout.addView(descriptionText);
        dialogLayout.addView(buttonLayout);

        // Dialog erstellen und anzeigen
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogLayout)
                .setCancelable(true)
                .create();

        // OK Button Click Handler
        okButton.setOnClickListener(v -> dialog.dismiss());

        // Dialog anzeigen
        dialog.show();

        // Dialog-Fenster anpassen background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            //  dim effect
            dialog.getWindow().setDimAmount(0.5f);
        }
    }

    /**
     * Erstellt eine moderne Statistik-Karte für den Dialog.
     *
     * @param icon Emoji-Icon für die Karte
     * @param label Beschriftung der Statistik
     * @param value Wert der Statistik
     * @param colorHex Hex-Farbe für den Akzent
     * @return LinearLayout der Statistik-Karte
     */
    private LinearLayout createStatCard(String icon, String label, String value, String colorHex) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(24, 20, 24, 20); // Увеличенные отступы

        //XML drawable
        card.setBackgroundResource(R.drawable.stat_card_background);



        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.bottomMargin = 16; // Увеличенный отступ
        card.setLayoutParams(cardParams);

        // Icon
        TextView iconText = new TextView(this);
        iconText.setText(icon);
        iconText.setTextSize(24f); //
        iconText.setPadding(0, 0, 20, 0); // Больше отступ

        // Text Container
        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.weight = 1;
        textContainer.setLayoutParams(textParams);

        // Label
        TextView labelText = new TextView(this);
        labelText.setText(label);
        labelText.setTextSize(14f);
        labelText.setTextColor(android.graphics.Color.parseColor("#666666"));
        labelText.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Value
        TextView valueText = new TextView(this);
        valueText.setText(value);
        valueText.setTextSize(20f); //
        valueText.setTextColor(android.graphics.Color.parseColor(colorHex));
        valueText.setTypeface(null, android.graphics.Typeface.BOLD);
        valueText.setPadding(0, 4, 0, 0); //

        textContainer.addView(labelText);
        textContainer.addView(valueText);

        card.addView(iconText);
        card.addView(textContainer);

        return card;
    }

    /**
     * Gibt die passende Akzentfarbe für eine Stimmung zurück.
     *
     * @param moodName Name der Stimmung
     * @return Farbe als int
     */
    private int getMoodColor(String moodName) {
        String mood = moodName.toLowerCase();
        if (mood.contains("sehr gut")) {
            return android.graphics.Color.parseColor("#4CAF50"); // Grün
        } else if (mood.contains("gut")) {
            return android.graphics.Color.parseColor("#42A5F5"); // App-Blau
        } else if (mood.contains("mittel")) {
            return android.graphics.Color.parseColor("#D81B60"); // App-Rosa
        } else if (mood.contains("schlecht")) {
            return android.graphics.Color.parseColor("#FF5722"); // Orange-Rot
        } else {
            return android.graphics.Color.parseColor("#616161"); // Grau
        }
    }
    /**
     * Gibt eine passende Beschreibung für eine Stimmung zurück.
     *
     * @param moodName Name der Stimmung
     * @return Beschreibende Text für die Stimmung
     */
    private String getMoodDescription(String moodName) {
        String mood = moodName.toLowerCase();
        if (mood.contains("sehr gut")) {
            return "Fantastische Tage! Du warst in ausgezeichneter Stimmung. 🌟";
        } else if (mood.contains("gut")) {
            return "Gute Tage! Du hast dich wohl und positiv gefühlt. ✨";
        } else if (mood.contains("mittel")) {
            return "Durchschnittliche Tage. Weder besonders gut noch schlecht. 📊";
        } else if (mood.contains("schlecht")) {
            return "Herausfordernde Tage. Vielleicht war dein Zyklus belastend. 💝";
        } else {
            return "Deine persönliche Stimmungsaufzeichnung.";
        }
    }

    /**
     * Gibt ein passendes Emoji für eine Stimmung zurück.
     *
     * @param moodName Name der Stimmung
     * @return Emoji als String
     */
    private String getMoodEmoji(String moodName) {
        String mood = moodName.toLowerCase();
        if (mood.contains("sehr gut")) {
            return "😀";
        } else if (mood.contains("gut")) {
            return "🙂";
        } else if (mood.contains("mittel")) {
            return "😐";
        } else if (mood.contains("schlecht")) {
            return "🙁";
        } else {
            return "📊";
        }
    }

    /**
     * Gibt eine passende Farbe für einen Schmerzlevel zurück.
     */
    private int getPainColor(String painName) {
        String pain = painName.toLowerCase();
        if (pain.contains("keine")) {
            return android.graphics.Color.parseColor("#4CAF50"); // Grün
        } else if (pain.contains("leicht")) {
            return android.graphics.Color.parseColor("#81C784"); // Hellgrün
        } else if (pain.contains("mittel")) {
            return android.graphics.Color.parseColor("#FF9800"); // Orange
        } else if (pain.contains("stark")) {
            return android.graphics.Color.parseColor("#F44336"); // Rot
        } else if (pain.contains("krampfartig")) {
            return android.graphics.Color.parseColor("#D32F2F"); // Dunkelrot
        } else {
            return android.graphics.Color.parseColor("#616161"); // Grau
        }
    }

    /**
     * Gibt eine passende Beschreibung für einen Schmerzlevel zurück.
     */
    private String getPainDescription(String painName) {
        String pain = painName.toLowerCase();
        if (pain.contains("keine")) {
            return "Schmerzfreie Tage! Du hattest keine spürbaren Beschwerden. 🌟";
        } else if (pain.contains("leicht")) {
            return "Leichte Beschwerden. Erträglich und gut zu bewältigen. ✨";
        } else if (pain.contains("mittel")) {
            return "Mittlere Schmerzen. Spürbar, aber noch gut aushaltbar. 📊";
        } else if (pain.contains("stark")) {
            return "Starke Schmerzen. Diese Tage waren belastend für dich.";
        } else if (pain.contains("krampfartig")) {
            return "Intensive Krämpfe. Sehr herausfordernde Tage.";
        } else {
            return "Deine persönliche Schmerzaufzeichnung.";
        }
    }

    /**
     * Gibt ein passendes Emoji für einen Schmerzlevel zurück.
     */
    private String getPainEmoji(String painName) {
        String pain = painName.toLowerCase();
        if (pain.contains("keine")) {
            return "😊";
        } else if (pain.contains("leicht")) {
            return "😐";
        } else if (pain.contains("mittel")) {
            return "😕";
        } else if (pain.contains("stark")) {
            return "😣";
        } else if (pain.contains("krampfartig")) {
            return "😖";
        } else {
            return "📊";
        }
    }
    /**
     * Bestimmt ob Zykluslänge normal ist und setzt entsprechende Farbe.
     */
    private void updateCycleCardColor(long averageCycle) {
        if (cycleCardLayout == null) return;

        String color;
        if (averageCycle >= 21 && averageCycle <= 35) {
            // Normal - Grün
            color = "#4CAF50";
        } else if (averageCycle >= 18 && averageCycle <= 40) {
            // Grenzwertig - Orange
            color = "#FF9800";
        } else {
            // Abnormal - Rot
            color = "#F44336";
        }

        cycleCardLayout.setBackgroundColor(android.graphics.Color.parseColor(color));
    }

    /**
     * Bestimmt ob Periodendauer normal ist und setzt entsprechende Farbe.
     */
    private void updatePeriodCardColor(int averageDuration) {
        if (periodCardLayout == null) return;

        String color;
        if (averageDuration >= 3 && averageDuration <= 7) {
            // Normal - Grün
            color = "#4CAF50";
        } else if (averageDuration >= 2 && averageDuration <= 8) {
            // Grenzwertig - Orange
            color = "#FF9800";
        } else {
            // Abnormal - Rot
            color = "#F44336";
        }

        periodCardLayout.setBackgroundColor(android.graphics.Color.parseColor(color));
    }

    /**
     * Bestimmt Schmerzlevel-Farbe basierend auf häufigstem Schmerz.
     */
    private void updatePainCardColor(String mostFrequentPain) {
        if (painCardLayout == null) return;

        String color;
        if (mostFrequentPain == null || mostFrequentPain.toLowerCase().contains("keine")) {
            // Keine Schmerzen - Grün
            color = "#4CAF50";
        } else if (mostFrequentPain.toLowerCase().contains("leicht")) {
            // Leichte Schmerzen - Hellgrün
            color = "#8BC34A";
        } else if (mostFrequentPain.toLowerCase().contains("mittel")) {
            // Mittlere Schmerzen - Orange
            color = "#FF9800";
        } else if (mostFrequentPain.toLowerCase().contains("stark")) {
            // Starke Schmerzen - Rot
            color = "#F44336";
        } else if (mostFrequentPain.toLowerCase().contains("krampfartig")) {
            // Krampfartige Schmerzen - Dunkelrot
            color = "#D32F2F";
        } else {
            // Unbekannt - Grau
            color = "#9E9E9E";
        }

        painCardLayout.setBackgroundColor(android.graphics.Color.parseColor(color));
    }

    /**
     * Bestimmt Stimmungs-Farbe basierend auf häufigster Stimmung.
     */
    private void updateMoodCardColor(String mostFrequentMood) {
        if (moodCardLayout == null) return;

        String cleanMood = mostFrequentMood != null ? removeMoodEmojis(mostFrequentMood).toLowerCase() : "";
        String color;

        if (cleanMood.contains("sehr gut")) {
            // Sehr gute Stimmung - Dunkelgrün
            color = "#4CAF50";
        } else if (cleanMood.contains("gut")) {
            // Gute Stimmung - Grün
            color = "#4CAF50";
        } else if (cleanMood.contains("mittel")) {
            // Mittlere Stimmung - Orange
            color = "#FF9800";
        } else if (cleanMood.contains("schlecht")) {
            // Schlechte Stimmung - Rot
            color = "#F44336";
        } else {
            // Keine Daten - Grau
            color = "#9E9E9E";
        }

        moodCardLayout.setBackgroundColor(android.graphics.Color.parseColor(color));
    }
    /**
     * Custom ValueFormatter für gerundete Prozentangaben mit %-Zeichen
     */
    private com.github.mikephil.charting.formatter.ValueFormatter createPercentFormatter() {
        return new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return Math.round(value) + "%";
            }
        };
    }
}