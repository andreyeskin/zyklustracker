package at.fhj.andrey.zyklustracker.statistik;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * StatistikData - Sammlung aller Datenmodelle für Statistiken
 *
 * Diese Klasse enthält alle Datenstrukturen die von StatistikManager
 * und ChartManager verwendet werden:
 * - CycleStatistics: Zyklusstatistiken (Durchschnitt, Min, Max)
 * - MoodStatistics: Stimmungsstatistiken
 * - PainStatistics: Schmerzstatistiken
 * - SymptomStatistics: Symptomstatistiken
 * - FilteredData: Gefilterte Daten für Zeitraum-Filter
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class StatistikData {

    /**
     * Zyklusstatistiken - Durchschnitt, Minimum und Maximum der Zykluslängen
     */
    public static class CycleStatistics {
        public final long average;
        public final long min;
        public final long max;
        public final boolean hasEnoughData;

        public CycleStatistics(long average, long min, long max, boolean hasEnoughData) {
            this.average = average;
            this.min = min;
            this.max = max;
            this.hasEnoughData = hasEnoughData;
        }

        /**
         * Erstellt leere Statistiken wenn nicht genug Daten
         */
        public static CycleStatistics empty() {
            return new CycleStatistics(0, 0, 0, false);
        }

        @Override
        public String toString() {
            return "CycleStatistics{average=" + average + ", min=" + min + ", max=" + max + "}";
        }
    }

    /**
     * Stimmungsstatistiken - häufigste Stimmung und Verteilung
     */
    public static class MoodStatistics {
        public final String mostFrequentMood;
        public final int count;
        public final int totalEntries;
        public final float percentage;
        public final boolean hasData;

        public MoodStatistics(String mostFrequentMood, int count, int totalEntries) {
            this.mostFrequentMood = mostFrequentMood;
            this.count = count;
            this.totalEntries = totalEntries;
            this.percentage = totalEntries > 0 ? (count * 100f / totalEntries) : 0;
            this.hasData = mostFrequentMood != null && !mostFrequentMood.isEmpty();
        }

        /**
         * Erstellt leere Statistiken wenn keine Daten
         */
        public static MoodStatistics empty() {
            return new MoodStatistics(null, 0, 0);
        }

        @Override
        public String toString() {
            return "MoodStatistics{mood=" + mostFrequentMood + ", count=" + count + ", percentage=" + percentage + "%}";
        }
    }

    /**
     * Schmerzstatistiken - häufigster Schmerzlevel und Verteilung
     */
    public static class PainStatistics {
        public final String mostFrequentPain;
        public final int count;
        public final int totalEntries;
        public final float percentage;
        public final boolean hasData;

        public PainStatistics(String mostFrequentPain, int count, int totalEntries) {
            this.mostFrequentPain = mostFrequentPain;
            this.count = count;
            this.totalEntries = totalEntries;
            this.percentage = totalEntries > 0 ? (count * 100f / totalEntries) : 0;
            this.hasData = mostFrequentPain != null && !mostFrequentPain.isEmpty();
        }

        /**
         * Erstellt leere Statistiken wenn keine Daten
         */
        public static PainStatistics empty() {
            return new PainStatistics(null, 0, 0);
        }

        @Override
        public String toString() {
            return "PainStatistics{pain=" + mostFrequentPain + ", count=" + count + ", percentage=" + percentage + "%}";
        }
    }

    /**
     * Periodendauer-Statistiken
     */
    public static class PeriodStatistics {
        public final int averageDuration;
        public final boolean hasData;

        public PeriodStatistics(int averageDuration, boolean hasData) {
            this.averageDuration = averageDuration;
            this.hasData = hasData;
        }

        /**
         * Erstellt leere Statistiken wenn keine Daten
         */
        public static PeriodStatistics empty() {
            return new PeriodStatistics(0, false);
        }

        @Override
        public String toString() {
            return "PeriodStatistics{averageDuration=" + averageDuration + ", hasData=" + hasData + "}";
        }
    }

    /**
     * Symptomstatistiken - Top-Symptome und deren Häufigkeiten
     */
    public static class SymptomStatistics {
        public final java.util.Map<String, Integer> symptomFrequencies;
        public final boolean hasData;

        public SymptomStatistics(java.util.Map<String, Integer> symptomFrequencies) {
            this.symptomFrequencies = symptomFrequencies != null ? symptomFrequencies : new java.util.HashMap<>();
            this.hasData = !this.symptomFrequencies.isEmpty();

        }

        /**
         * Erstellt leere Statistiken wenn keine Daten
         */
        public static SymptomStatistics empty() {
            return new SymptomStatistics(new java.util.HashMap<>());
        }

        public Map<String, Integer> getSymptomFrequencies() {
            return symptomFrequencies;
        }


        /**
         * Gibt die Top N Symptome zurück, sortiert nach Häufigkeit
         */
        public java.util.List<java.util.Map.Entry<String, Integer>> getTopSymptoms(int topCount) {
            java.util.List<java.util.Map.Entry<String, Integer>> sortedSymptoms =
                    new java.util.ArrayList<>(symptomFrequencies.entrySet());
            sortedSymptoms.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            return sortedSymptoms.subList(0, Math.min(topCount, sortedSymptoms.size()));
        }

        @Override
        public String toString() {
            return "SymptomStatistics{symptoms=" + symptomFrequencies.size() + ", hasData=" + hasData + "}";
        }
    }

    /**
     * Gefilterte Daten basierend auf Zeitraum-Filter
     */
    public static class FilteredData {
        public final List<LocalDate> periodDates;
        public final List<at.fhj.andrey.zyklustracker.datenbank.WohlbefindenEintrag> wellbeingEntries;
        public final int timeframeMonths;

        public FilteredData(List<LocalDate> periodDates,
                            List<at.fhj.andrey.zyklustracker.datenbank.WohlbefindenEintrag> wellbeingEntries,
                            int timeframeMonths) {
            this.periodDates = periodDates != null ? periodDates : new java.util.ArrayList<>();
            this.wellbeingEntries = wellbeingEntries != null ? wellbeingEntries : new java.util.ArrayList<>();
            this.timeframeMonths = timeframeMonths;
        }

        /**
         * Prüft ob genug Daten für Berechnungen vorhanden sind
         */
        public boolean hasEnoughPeriodData() {
            return periodDates.size() >= 2;
        }

        /**
         * Prüft ob Wohlbefindensdaten vorhanden sind
         */
        public boolean hasWellbeingData() {
            return !wellbeingEntries.isEmpty();
        }

        @Override
        public String toString() {
            return "FilteredData{periods=" + periodDates.size() +
                    ", wellbeing=" + wellbeingEntries.size() +
                    ", timeframe=" + timeframeMonths + "months}";
        }
    }

    /**
     * Sammlung aller berechneten Statistiken für eine einfache Übertragung
     */
    public static class AllStatistics {
        public final CycleStatistics cycle;
        public final MoodStatistics mood;
        public final PainStatistics pain;
        public final PeriodStatistics period;
        public final SymptomStatistics symptoms;

        public AllStatistics(CycleStatistics cycle,
                             MoodStatistics mood,
                             PainStatistics pain,
                             PeriodStatistics period,
                             SymptomStatistics symptoms) {
            this.cycle = cycle;
            this.mood = mood;
            this.pain = pain;
            this.period = period;
            this.symptoms = symptoms;
        }

        /**
         * Erstellt leere Statistiken wenn keine Daten
         */
        public static AllStatistics empty() {
            return new AllStatistics(
                    CycleStatistics.empty(),
                    MoodStatistics.empty(),
                    PainStatistics.empty(),
                    PeriodStatistics.empty(),
                    SymptomStatistics.empty()
            );
        }

        @Override
        public String toString() {
            return "AllStatistics{" +
                    "cycle=" + cycle +
                    ", mood=" + mood +
                    ", pain=" + pain +
                    ", period=" + period +
                    ", symptoms=" + symptoms +
                    '}';
        }
    }

    /**
     * Callback-Interface für asynchrone Statistik-Berechnungen
     */
    public interface StatistikCallback {
        /**
         * Wird aufgerufen wenn alle Statistiken berechnet wurden
         */
        void onStatistikenBerechnet(AllStatistics statistics);

        /**
         * Wird aufgerufen bei Fehlern in der Berechnung
         */
        void onFehler(String fehlermeldung);
    }

    /**
     * Callback-Interface für Chart-Updates
     */
    public interface ChartCallback {
        /**
         * Wird aufgerufen wenn Charts aktualisiert werden sollen
         */
        void onChartsAktualisieren(FilteredData data);

        /**
         * Wird aufgerufen bei Chart-Fehlern
         */
        void onChartFehler(String fehlermeldung);
    }
}