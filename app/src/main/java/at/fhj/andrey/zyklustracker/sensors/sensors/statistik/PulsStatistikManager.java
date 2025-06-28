package at.fhj.andrey.zyklustracker.sensors.sensors.statistik;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import at.fhj.andrey.zyklustracker.datenbank.WohlbefindenDao;
import at.fhj.andrey.zyklustracker.datenbank.WohlbefindenEintrag;
import at.fhj.andrey.zyklustracker.datenbank.ZyklusDatenbank;

/**
 * PulsStatistikManager - Verwaltung der Puls-Statistiken und -Diagramme
 *
 * Diese Klasse ist spezialisiert auf:
 * - Laden von Pulsdaten aus der Datenbank
 * - Berechnung statistischer Kennwerte für Ruhepuls
 * - Erstellung von Liniendiagrammen für Pulsverläufe
 * - Analyse zyklusbezogener Pulsmuster und Herzfrequenzvariabilität
 *
 * Medizinischer Hintergrund:
 * - Normaler Ruhepuls: 60-100 bpm (altersabhängig)
 * - Zyklusbedingte Schwankungen: ~3-5% Anstieg in der Lutealphase
 * - Pulsanstieg durch erhöhte sympathische Aktivität nach Eisprung
 * - Trainierte Personen: oft niedrigerer Grundpuls (40-60 bpm)
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class PulsStatistikManager {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String TAG = "PulsStatistikManager";

    // Medizinische Referenzwerte für Ruhepuls
    private static final int PULS_NORMAL_MIN = 60;
    private static final int PULS_NORMAL_MAX = 100;
    private static final int PULS_TRAINIERT_MIN = 40;
    private static final int PULS_BRADYKARDIE_SCHWELLE = 50;
    private static final int PULS_TACHYKARDIE_SCHWELLE = 100;

    // Datenbankzugriff
    private final Context context;
    private final ZyklusDatenbank database;
    private final WohlbefindenDao wellbeingDao;

    /**
     * Datenmodell für Pulsstatistiken
     */
    public static class PulsStatistiken {
        public final float durchschnitt;
        public final int minimum;
        public final int maximum;
        public final int anzahlMessungen;
        public final float standardAbweichung;
        public final boolean hatGenugDaten;
        public final String bewertung;
        public final String empfehlung;
        public final String fitnessBewertung;

        public PulsStatistiken(float durchschnitt, int minimum, int maximum,
                               int anzahlMessungen, float standardAbweichung,
                               boolean hatGenugDaten, String bewertung,
                               String empfehlung, String fitnessBewertung) {
            this.durchschnitt = durchschnitt;
            this.minimum = minimum;
            this.maximum = maximum;
            this.anzahlMessungen = anzahlMessungen;
            this.standardAbweichung = standardAbweichung;
            this.hatGenugDaten = hatGenugDaten;
            this.bewertung = bewertung;
            this.empfehlung = empfehlung;
            this.fitnessBewertung = fitnessBewertung;
        }
    }

    /**
     * Callback-Interface für asynchrone Statistik-Berechnungen
     */
    public interface PulsStatistikCallback {
        void onStatistikenBerechnet(PulsStatistiken statistiken);
        void onFehler(String fehlermeldung);
    }

    /**
     * Konstruktor
     */
    public PulsStatistikManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = ZyklusDatenbank.getInstanz(this.context);
        this.wellbeingDao = database.wohlbefindenDao();
        Log.d(TAG, "PulsStatistikManager initialisiert");
    }

    /**
     * Berechnet umfassende Pulsstatistiken für einen bestimmten Zeitraum
     *
     * @param zeitraumTage Anzahl der Tage rückwirkend (z.B. 30 für letzten Monat)
     * @param callback Callback für das Ergebnis
     */
    public void berechneStatistiken(int zeitraumTage, PulsStatistikCallback callback) {
        Log.d(TAG, "Berechne Pulsstatistiken für " + zeitraumTage + " Tage");

        // Background Thread für Datenbankoperationen
        new Thread(() -> {
            try {
                // Zeitraum definieren
                LocalDate endDatum = LocalDate.now();
                LocalDate startDatum = endDatum.minusDays(zeitraumTage);

                // Daten aus Datenbank laden
                List<WohlbefindenEintrag> eintraege =
                        wellbeingDao.getEintraegeZwischen(startDatum, endDatum);

                // Pulsdaten extrahieren
                List<Integer> pulswerte = new ArrayList<>();
                for (WohlbefindenEintrag eintrag : eintraege) {
                    if (eintrag.getPuls() != null && eintrag.getPuls() > 0) {
                        pulswerte.add(eintrag.getPuls());
                    }
                }
                Log.d(TAG, "=== PULS DEBUG ===");
                Log.d(TAG, "Zeitraum: " + startDatum + " bis " + endDatum);
                Log.d(TAG, "Geladene Einträge gesamt: " + eintraege.size());
                Log.d(TAG, "Extrahierte Pulswerte: " + pulswerte.size() + " Werte");
                if (!pulswerte.isEmpty()) {
                    Log.d(TAG, "Erster Puls: " + pulswerte.get(0) + "bpm");
                    Log.d(TAG, "Letzter Puls: " + pulswerte.get(pulswerte.size()-1) + "bpm");
                }
                Log.d(TAG, "=== PULS DEBUG ENDE ===");

                // Statistiken berechnen
                PulsStatistiken statistiken = berechnePulsStatistiken(pulswerte);

                // Callback auf Main Thread
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onStatistikenBerechnet(statistiken);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Berechnen der Pulsstatistiken: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onFehler("Fehler beim Laden der Pulsdaten: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    /**
     * Berechnet die eigentlichen Pulsstatistiken
     */
    private PulsStatistiken berechnePulsStatistiken(List<Integer> pulswerte) {
        Log.d(TAG, "Berechne Statistiken für " + pulswerte.size() + " Pulswerte");

        // Keine Daten vorhanden
        if (pulswerte.isEmpty()) {
            return new PulsStatistiken(
                    0f, 0, 0, 0, 0f, false,
                    "Keine Pulsdaten vorhanden",
                    "Beginnen Sie mit der Aufzeichnung Ihres Ruhepulses",
                    "Fitness-Level kann nicht bestimmt werden"
            );
        }

        // Grundstatistiken berechnen
        float summe = 0f;
        int min = pulswerte.get(0);
        int max = pulswerte.get(0);

        for (Integer puls : pulswerte) {
            summe += puls;
            if (puls < min) min = puls;
            if (puls > max) max = puls;
        }

        float durchschnitt = summe / pulswerte.size();

        // Standardabweichung berechnen
        float varianzSumme = 0f;
        for (Integer puls : pulswerte) {
            varianzSumme += Math.pow(puls - durchschnitt, 2);
        }
        float standardAbweichung = (float) Math.sqrt(varianzSumme / pulswerte.size());

        // Bewertungen generieren
        String bewertung = bewertePuls(durchschnitt, min, max);
        String empfehlung = generiereEmpfehlung(durchschnitt, min, max, pulswerte.size());
        String fitnessBewertung = bewerteFitnessLevel(durchschnitt);

        boolean hatGenugDaten = pulswerte.size() >= 14; // Mindestens eine Woche Daten

        return new PulsStatistiken((int)durchschnitt, min, max, pulswerte.size(),
                standardAbweichung, hatGenugDaten, bewertung, empfehlung, fitnessBewertung);
    }

    /**
     * Bewertet die Pulswerte medizinisch
     */
    private String bewertePuls(float durchschnitt, int min, int max) {
        if (durchschnitt >= PULS_NORMAL_MIN && durchschnitt <= PULS_NORMAL_MAX) {
            return "Ihr Ruhepuls liegt im normalen Bereich.";
        } else if (durchschnitt < PULS_NORMAL_MIN) {
            if (durchschnitt >= PULS_TRAINIERT_MIN) {
                return "Niedriger Ruhepuls - typisch für gut trainierte Personen.";
            } else {
                return "Sehr niedriger Ruhepuls. Konsultieren Sie einen Arzt bei Beschwerden.";
            }
        } else {
            return "Erhöhter Ruhepuls. Dies kann verschiedene Ursachen haben.";
        }
    }

    /**
     * Bewertet das Fitness-Level basierend auf dem Ruhepuls
     */
    private String bewerteFitnessLevel(float durchschnitt) {
        if (durchschnitt < 60) {
            return "🏃‍♀️ Exzellente kardiovaskuläre Fitness";
        } else if (durchschnitt <= 70) {
            return "💪 Gute kardiovaskuläre Fitness";
        } else if (durchschnitt <= 80) {
            return "✅ Durchschnittliche Fitness";
        } else if (durchschnitt <= 90) {
            return "⚠️ Fitness kann verbessert werden";
        } else {
            return "🏃‍♂️ Ausdauertraining empfohlen";
        }
    }

    /**
     * Generiert personalisierte Empfehlungen
     */
    private String generiereEmpfehlung(float durchschnitt, int min, int max, int anzahlMessungen) {
        StringBuilder empfehlung = new StringBuilder();

        if (anzahlMessungen < 14) {
            empfehlung.append("📊 Sammeln Sie weitere Daten für präzisere Analysen. ");
        }

        if (max - min > 30) {
            empfehlung.append("💓 Große Pulsschwankungen erkannt. Dies kann normal sein oder auf Stress hinweisen. ");
        }

        if (durchschnitt > PULS_NORMAL_MAX) {
            empfehlung.append("🏃‍♀️ Regelmäßiges Ausdauertraining kann Ihren Ruhepuls senken. ");
        }

        empfehlung.append("⏰ Messen Sie Ihren Puls am besten morgens nach dem Aufwachen.");

        return empfehlung.toString();
    }

    /**
     * Erstellt Liniendiagramm für Pulsverlauf
     */
    public void erstellePulsDiagramm(LineChart chart, int zeitraumTage) {
        Log.d(TAG, "Erstelle Pulsdiagramm für " + zeitraumTage + " Tage");

        new Thread(() -> {
            try {
                // Zeitraum definieren
                LocalDate endDatum = LocalDate.now();
                LocalDate startDatum = endDatum.minusDays(zeitraumTage);

                // Daten aus Datenbank laden
                List<WohlbefindenEintrag> eintraege =
                        wellbeingDao.getEintraegeZwischen(startDatum, endDatum);

                // Chart-Daten vorbereiten
                List<Entry> entries = new ArrayList<>();
                List<String> dates = new ArrayList<>();

                int index = 0;
                for (WohlbefindenEintrag eintrag : eintraege) {
                    if (eintrag.getPuls() != null && eintrag.getPuls() > 0) {
                        entries.add(new Entry(index, eintrag.getPuls()));
                        dates.add(eintrag.getDatum().format(DateTimeFormatter.ofPattern("dd.MM")));
                        index++;
                    }
                }

                // Chart auf Main Thread aktualisieren
                mainHandler.post(() -> {
                    aktualisiereChart(chart, entries, dates, "Ruhepuls (bpm)");
                });

            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Erstellen des Pulsdiagramms: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Aktualisiert das LineChart mit den bereitgestellten Daten
     */
    private void aktualisiereChart(LineChart chart, List<Entry> entries, List<String> dates, String label) {
        if (entries.isEmpty()) {
            chart.setNoDataText("Keine Pulsdaten verfügbar");
            chart.invalidate();
            return;
        }

        // Dataset erstellen
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(0xFFE91E63); // Material Pink
        dataSet.setCircleColor(0xFFE91E63);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(30);
        dataSet.setFillColor(0xFFE91E63);

        // LineData erstellen
        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        // Chart-Konfiguration
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);

        // X-Achse konfigurieren
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dates.size()) {
                    return dates.get(index);
                }
                return "";
            }
        });

        // Y-Achse konfigurieren
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(40f);
        leftAxis.setAxisMaximum(120f);
        leftAxis.setGranularity(5f);

        chart.getAxisRight().setEnabled(false);

        // Chart aktualisieren
        chart.invalidate();
        Log.d(TAG, "Pulsdiagramm erfolgreich aktualisiert mit " + entries.size() + " Datenpunkten");
    }

    /**
     * Cleanup-Methode
     */
    public void cleanup() {
        Log.d(TAG, "PulsStatistikManager cleanup");
    }
}