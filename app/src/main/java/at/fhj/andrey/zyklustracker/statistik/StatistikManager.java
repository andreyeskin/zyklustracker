package at.fhj.andrey.zyklustracker.statistik;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

import at.fhj.andrey.zyklustracker.datenbank.*;

import at.fhj.andrey.zyklustracker.zyklusanalyse.ZyklusPhaseBerechnung;
import at.fhj.andrey.zyklustracker.zyklusanalyse.AnalyseErgebnis;

/**
 * StatistikManager - Zentrale Logik für alle Statistik-Berechnungen
 *
 * Diese Klasse ist verantwortlich für:
 * - Laden und Filtern von Daten aus der Datenbank
 * - Berechnung aller Statistiken (Zyklus, Stimmung, Schmerz, Symptome)
 * - Asynchrone Verarbeitung im Background Thread
 * - Callback-basierte Kommunikation mit der UI
 *
 * Alle Datenbankzugriffe laufen in Background Threads!
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class StatistikManager {

    private static final String TAG = "StatistikManager";

    // Datenbankzugriff
    private final ZyklusDatenbank database;
    private final WohlbefindenDao wellbeingDao;
    private final ZyklusDao cycleDao;

    // Aktuelle Filtereinstellungen
    private int currentTimeframeMonths = 3; // Standard: 3 Monate

    // ===== NEUE ZYKLUSPHASEN-INTEGRATION =====
    private ZyklusPhaseBerechnung phasenBerechnung;

    /**
     * Callback-Interface für Zyklusphasen-Analyse
     */
    public interface ZyklusPhasenCallback {
        void onZyklusPhasenAnalyseBerechnet(AnalyseErgebnis analyseErgebnis);
        void onZyklusFehler(String fehlermeldung);
    }

    /**
     * Konstruktor - initialisiert Datenbankzugriff
     */
    public StatistikManager(Context context) {
        this.database = ZyklusDatenbank.getInstanz(context);
        this.wellbeingDao = database.wohlbefindenDao();
        this.cycleDao = database.zyklusDao();
        // Zyklusphasen-Berechnung initialisieren
        this.phasenBerechnung = new ZyklusPhaseBerechnung();
        Log.d(TAG, "ZyklusPhaseBerechnung initialisiert");
        Log.d(TAG, "StatistikManager initialisiert");
    }

    /**
     * Hauptmethode: Berechnet alle Statistiken asynchron
     */
    public void berechneAlleStatistiken(int timeframeMonths, StatistikData.StatistikCallback callback) {
        this.currentTimeframeMonths = timeframeMonths;
        Log.d(TAG, "Starte Berechnung aller Statistiken für " + timeframeMonths + " Monate");

        // Background Thread für alle Datenbankoperationen
        new Thread(() -> {
            try {
                // 1. Daten aus Datenbank laden und filtern
                StatistikData.FilteredData filteredData = ladeDatenUndFilter();
                Log.d(TAG, "Daten geladen: " + filteredData);

                // 2. Alle Statistiken berechnen
                StatistikData.CycleStatistics cycleStats = berechneZyklusStatistiken(filteredData.periodDates);
                StatistikData.MoodStatistics moodStats = berechneStimmungsStatistiken(filteredData.wellbeingEntries);
                StatistikData.PainStatistics painStats = berechneSchmerzStatistiken(filteredData.wellbeingEntries);
                StatistikData.PeriodStatistics periodStats = berechnePeriodendauerStatistiken(filteredData.periodDates);
                StatistikData.SymptomStatistics symptomStats = berechneSymptomStatistiken(filteredData.wellbeingEntries);

                // 3. Alle Statistiken sammeln
                StatistikData.AllStatistics allStats = new StatistikData.AllStatistics(
                        cycleStats, moodStats, painStats, periodStats, symptomStats
                );

                Log.d(TAG, "Alle Statistiken berechnet: " + allStats);

                // 4. Callback auf Main Thread
                if (callback != null) {
                    callback.onStatistikenBerechnet(allStats);
                }

            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Berechnen der Statistiken: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onFehler("Fehler beim Berechnen der Statistiken: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Lädt Daten aus der Datenbank und filtert nach Zeitraum
     */
    private StatistikData.FilteredData ladeDatenUndFilter() {
        Log.d(TAG, "Lade Daten aus Datenbank...");

        // Alle Daten aus DB laden
        List<LocalDate> allPeriodData = cycleDao.getAllePeriodeStartDaten();
        List<WohlbefindenEintrag> allWellbeingEntries = wellbeingDao.getAlleEintraege();

        // Nach Zeitraum filtern
        List<LocalDate> filteredPeriods = filterDatesByTimeframe(allPeriodData);
        List<WohlbefindenEintrag> filteredWellbeing = filterWellbeingByTimeframe(allWellbeingEntries);

        Log.d(TAG, "Daten gefiltert: " + filteredPeriods.size() + " Perioden, " +
                filteredWellbeing.size() + " Wohlbefinden-Einträge");

        return new StatistikData.FilteredData(filteredPeriods, filteredWellbeing, currentTimeframeMonths);
    }

    /**
     * Filtert Daten basierend auf dem ausgewählten Zeitraum
     */
    private List<LocalDate> filterDatesByTimeframe(List<LocalDate> allDates) {
        if (allDates == null || allDates.isEmpty()) {
            return new ArrayList<>();
        }

        LocalDate cutoffDate = LocalDate.now().minusMonths(currentTimeframeMonths);
        List<LocalDate> filteredDates = new ArrayList<>();

        for (LocalDate date : allDates) {
            if (date.isAfter(cutoffDate) || date.isEqual(cutoffDate)) {
                filteredDates.add(date);
            }
        }

        return filteredDates;
    }

    /**
     * Filtert Wohlbefinden-Einträge basierend auf dem Zeitraum
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
     * Berechnet Zyklusstatistiken (Durchschnitt, Min, Max)
     */
    private StatistikData.CycleStatistics berechneZyklusStatistiken(List<LocalDate> periodData) {
        Log.d(TAG, "Berechne Zyklusstatistiken...");

        if (periodData.size() < 2) {
            Log.d(TAG, "Zu wenig Periodendaten für Zyklusberechnung");
            return StatistikData.CycleStatistics.empty();
        }

        // Aufeinanderfolgende Daten zu Perioden gruppieren
        List<List<LocalDate>> periods = groupConsecutiveDates(periodData);

        // Zykluslängen zwischen Periodenbeginnen berechnen
        List<Long> cycleLengths = calculateCycleLengths(periods);

        if (cycleLengths.isEmpty()) {
            Log.d(TAG, "Keine gültigen Zykluslängen gefunden");
            return StatistikData.CycleStatistics.empty();
        }

        // Statistiken berechnen
        long sum = 0;
        long min = cycleLengths.get(0);
        long max = cycleLengths.get(0);

        for (Long length : cycleLengths) {
            sum += length;
            if (length < min) min = length;
            if (length > max) max = length;
        }

        long average = sum / cycleLengths.size();

        Log.d(TAG, "Zyklusstatistiken berechnet: Durchschnitt=" + average + ", Min=" + min + ", Max=" + max);
        return new StatistikData.CycleStatistics(average, min, max, true);
    }

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
     * Berechnet Stimmungsstatistiken
     */
    private StatistikData.MoodStatistics berechneStimmungsStatistiken(List<WohlbefindenEintrag> entries) {
        Log.d(TAG, "Berechne Stimmungsstatistiken...");

        if (entries.isEmpty()) {
            return StatistikData.MoodStatistics.empty();
        }

        Map<String, Integer> moodCounts = new HashMap<>();

        // Stimmungen zählen
        for (WohlbefindenEintrag entry : entries) {
            String mood = entry.getStimmung();
            if (mood != null && !mood.isEmpty()) {
                moodCounts.put(mood, moodCounts.getOrDefault(mood, 0) + 1);
            }
        }

        if (moodCounts.isEmpty()) {
            return StatistikData.MoodStatistics.empty();
        }

        // Häufigste Stimmung finden
        String mostFrequent = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequent = entry.getKey();
            }
        }

        Log.d(TAG, "Stimmungsstatistiken berechnet: " + mostFrequent + " (" + maxCount + "/" + entries.size() + ")");
        return new StatistikData.MoodStatistics(mostFrequent, maxCount, entries.size());
    }

    /**
     * Berechnet Schmerzstatistiken
     */
    private StatistikData.PainStatistics berechneSchmerzStatistiken(List<WohlbefindenEintrag> entries) {
        Log.d(TAG, "Berechne Schmerzstatistiken...");

        if (entries.isEmpty()) {
            return StatistikData.PainStatistics.empty();
        }

        Map<String, Integer> painCounts = new HashMap<>();

        // Schmerzlevel zählen
        for (WohlbefindenEintrag entry : entries) {
            String pain = entry.getSchmerzLevel();
            if (pain != null && !pain.isEmpty()) {
                painCounts.put(pain, painCounts.getOrDefault(pain, 0) + 1);
            }
        }

        if (painCounts.isEmpty()) {
            return StatistikData.PainStatistics.empty();
        }

        // Häufigsten Schmerzlevel finden
        String mostFrequent = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : painCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostFrequent = entry.getKey();
            }
        }

        Log.d(TAG, "Schmerzstatistiken berechnet: " + mostFrequent + " (" + maxCount + "/" + entries.size() + ")");
        return new StatistikData.PainStatistics(mostFrequent, maxCount, entries.size());
    }

    /**
     * Berechnet Periodendauer-Statistiken
     */
    private StatistikData.PeriodStatistics berechnePeriodendauerStatistiken(List<LocalDate> periodData) {
        Log.d(TAG, "Berechne Periodendauer-Statistiken...");

        if (periodData.isEmpty()) {
            return StatistikData.PeriodStatistics.empty();
        }

        // Perioden gruppieren und Durchschnittsdauer berechnen
        List<List<LocalDate>> periods = groupConsecutiveDates(periodData);

        if (periods.isEmpty()) {
            return StatistikData.PeriodStatistics.empty();
        }

        // Durchschnittliche Periodendauer berechnen
        int totalDays = 0;
        for (List<LocalDate> period : periods) {
            totalDays += period.size();
        }

        int averageDuration = totalDays / periods.size();

        Log.d(TAG, "Periodendauer-Statistiken berechnet: " + averageDuration + " Tage durchschnittlich");
        return new StatistikData.PeriodStatistics(averageDuration, true);
    }

    /**
     * Berechnet Symptomstatistiken
     */
    private StatistikData.SymptomStatistics berechneSymptomStatistiken(List<WohlbefindenEintrag> entries) {
        Log.d(TAG, "Berechne Symptomstatistiken...");

        if (entries.isEmpty()) {
            return StatistikData.SymptomStatistics.empty();
        }

        // Symptom-Listen extrahieren
        List<String> symptomLists = new ArrayList<>();
        Gson gson = new Gson();

        for (WohlbefindenEintrag entry : entries) {
            if (entry.getSymptome() != null && !entry.getSymptome().isEmpty()) {
                String jsonString = gson.toJson(entry.getSymptome());
                symptomLists.add(jsonString);
            }
        }

        if (symptomLists.isEmpty()) {
            return StatistikData.SymptomStatistics.empty();
        }

        // Symptomhäufigkeiten analysieren
        Map<String, Integer> symptomFrequencies = analyzeSymptomFrequencies(symptomLists);

        Log.d(TAG, "Symptomstatistiken berechnet: " + symptomFrequencies.size() + " verschiedene Symptome");
        return new StatistikData.SymptomStatistics(symptomFrequencies);
    }

    /**
     * Analysiert die Häufigkeit aller Symptome aus den JSON-Listen
     */
    private Map<String, Integer> analyzeSymptomFrequencies(List<String> symptomLists) {
        Map<String, Integer> symptomCounter = new HashMap<>();
        Gson gson = new Gson();

        for (String listJson : symptomLists) {
            if (listJson != null && !listJson.isEmpty()) {
                try {
                    List<String> symptoms = gson.fromJson(listJson,
                            new TypeToken<List<String>>(){}.getType());

                    if (symptoms != null) {
                        for (String symptom : symptoms) {
                            symptomCounter.put(symptom,
                                    symptomCounter.getOrDefault(symptom, 0) + 1);
                        }
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Fehler beim Parsen der Symptom-Liste: " + e.getMessage());
                }
            }
        }

        return symptomCounter;
    }

    /**
     * Getter für gefilterte Daten (für ChartManager)
     */
    public void ladeGefilterteDaten(int timeframeMonths, StatistikData.ChartCallback callback) {
        this.currentTimeframeMonths = timeframeMonths;

        new Thread(() -> {
            try {
                StatistikData.FilteredData filteredData = ladeDatenUndFilter();
                if (callback != null) {
                    callback.onChartsAktualisieren(filteredData);
                }
            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Laden der gefilterten Daten: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onChartFehler("Fehler beim Laden der Daten: " + e.getMessage());
                }
            }
        }).start();
    }


    /**
     * Analysiert aktuelle Zyklusphase mit Sensor-Daten
     * NEUE FUNKTION: Verknüpft Sensor-Werte mit Zyklusphasen
     *
     * @param temperatur Aktuelle Körpertemperatur
     * @param puls Aktueller Ruhepuls
     * @param spo2 Aktuelle Sauerstoffsättigung
     * @param callback Callback für Ergebnis-Rückgabe
     */
    public void analysiereAktuelleZyklusphaseUndSensoren(float temperatur, int puls, int spo2,
                                                         ZyklusPhasenCallback callback) {
        Log.d(TAG, "Starte Zyklusphasen-Analyse mit Sensor-Daten");

        // Background Thread für Datenbankoperationen
        new Thread(() -> {
            try {
                // Periodendaten aus Datenbank laden
                List<LocalDate> allPeriodData = cycleDao.getAlleEchtenPerioden()
                        .stream()
                        .map(PeriodeEintrag::getDatum)
                        .collect(java.util.stream.Collectors.toList());

                // Heute als Analysedatum
                LocalDate heute = LocalDate.now();

                // Zyklusphasen-Analyse durchführen
                AnalyseErgebnis ergebnis = phasenBerechnung.analysiereSensorWerte(
                        heute, temperatur, puls, spo2, allPeriodData);

                Log.d(TAG, "Zyklusphasen-Analyse abgeschlossen: " + ergebnis);

                // Callback auf Main Thread
                if (callback != null) {
                    callback.onZyklusPhasenAnalyseBerechnet(ergebnis);
                }

            } catch (Exception e) {
                Log.e(TAG, "Fehler bei Zyklusphasen-Analyse: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onZyklusFehler("Fehler bei der Analyse: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Analysiert Zyklusphase für spezifisches Datum (ohne aktuelle Sensor-Daten)
     *
     * @param datum Zu analysierendes Datum
     * @param callback Callback für Ergebnis-Rückgabe
     */
    public void analysiereZyklusphaseFürDatum(LocalDate datum, ZyklusPhasenCallback callback) {
        Log.d(TAG, "Analysiere Zyklusphase für Datum: " + datum);

        new Thread(() -> {
            try {
                // Periodendaten laden
                List<LocalDate> periodData = cycleDao.getAlleEchtenPerioden()
                        .stream()
                        .map(PeriodeEintrag::getDatum)
                        .collect(java.util.stream.Collectors.toList());

                // Nur Phasenbestimmung ohne Sensor-Bewertung
                ZyklusPhaseBerechnung.ZyklusPhase phase = phasenBerechnung.berechneAktuellePhase(datum, periodData);

                // Vereinfachtes Ergebnis erstellen
                AnalyseErgebnis ergebnis = new AnalyseErgebnis(phase,
                        berechneSingleZyklusTag(datum, periodData),
                        phase.getBeschreibung());

                if (callback != null) {
                    callback.onZyklusPhasenAnalyseBerechnet(ergebnis);
                }

            } catch (Exception e) {
                Log.e(TAG, "Fehler bei Phasenanalyse: " + e.getMessage(), e);
                if (callback != null) {
                    callback.onZyklusFehler("Fehler: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Hilfsmethode: Berechnet Zyklustag für einzelnes Datum
     */
    private int berechneSingleZyklusTag(LocalDate datum, List<LocalDate> periodData) {
        if (periodData.isEmpty()) return 1;

        // Finde den ersten Tag der letzten Periode vor dem Datum
        LocalDate zyklusStart = findeErstenTagDerLetztenPeriode(datum, periodData);

        if (zyklusStart == null) return 1;

        return (int) ChronoUnit.DAYS.between(zyklusStart, datum) + 1;
    }

    /**
     * Findet den ersten Tag der letzten zusammenhängenden Periode vor dem gegebenen Datum
     */
    private LocalDate findeErstenTagDerLetztenPeriode(LocalDate datum, List<LocalDate> periodData) {
        // Alle Periodentage vor/am gegebenen Datum
        List<LocalDate> relevantePeriodenTage = periodData.stream()
                .filter(d -> !d.isAfter(datum))
                .sorted()
                .collect(java.util.stream.Collectors.toList());

        if (relevantePeriodenTage.isEmpty()) return null;

        // Finde alle zusammenhängenden Periodengruppen
        List<List<LocalDate>> periodenGruppen = new ArrayList<>();
        List<LocalDate> aktuelleGruppe = new ArrayList<>();

        for (LocalDate tag : relevantePeriodenTage) {
            if (aktuelleGruppe.isEmpty() ||
                    ChronoUnit.DAYS.between(aktuelleGruppe.get(aktuelleGruppe.size() - 1), tag) <= 1) {
                aktuelleGruppe.add(tag);
            } else {
                // Lücke > 1 Tag = neue Periode
                if (!aktuelleGruppe.isEmpty()) {
                    periodenGruppen.add(new ArrayList<>(aktuelleGruppe));
                }
                aktuelleGruppe.clear();
                aktuelleGruppe.add(tag);
            }
        }

        // Letzte Gruppe hinzufügen
        if (!aktuelleGruppe.isEmpty()) {
            periodenGruppen.add(aktuelleGruppe);
        }

        if (periodenGruppen.isEmpty()) return null;

        // Letzte Periodengruppe nehmen und ersten Tag zurückgeben
        List<LocalDate> letzteGruppe = periodenGruppen.get(periodenGruppen.size() - 1);
        return letzteGruppe.get(0);
    }

    /**
     * Cleanup-Methode
     */
    public void cleanup() {
        Log.d(TAG, "StatistikManager cleanup");
        // Hier könnten laufende Threads abgebrochen werden falls nötig
    }
}