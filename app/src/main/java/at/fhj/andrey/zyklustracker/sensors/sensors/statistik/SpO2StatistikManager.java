package at.fhj.andrey.zyklustracker.sensors.sensors.statistik;

import android.content.Context;
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
import android.os.Handler;
import android.os.Looper;

/**
 * SpO2StatistikManager - Verwaltung der Sauerstoffsättigungs-Statistiken und -Diagramme
 *
 * Diese Klasse ist spezialisiert auf:
 * - Laden von SpO2-Daten aus der Datenbank
 * - Berechnung statistischer Kennwerte für Sauerstoffsättigung
 * - Erstellung von Liniendiagrammen für SpO2-Verläufe
 * - Analyse der allgemeinen Atemfunktion und Schlafqualität
 *
 * Medizinischer Hintergrund:
 * - Normale SpO2: 95-100% bei gesunden Personen
 * - Kritische Werte: <90% (Hypoxämie)
 * - SpO2 zeigt keine signifikanten zyklusbedingten Schwankungen
 * - Wichtiger Indikator für allgemeine Gesundheit und Schlafqualität
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class SpO2StatistikManager {
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String TAG = "SpO2StatistikManager";

    // Medizinische Referenzwerte für SpO2
    private static final int SPO2_NORMAL_MIN = 95;
    private static final int SPO2_OPTIMAL = 98;
    private static final int SPO2_KRITISCH = 90;
    private static final int SPO2_HYPOXAEMIE = 85;

    // Datenbankzugriff
    private final Context context;
    private final ZyklusDatenbank database;
    private final WohlbefindenDao wellbeingDao;

    /**
     * Datenmodell für SpO2-Statistiken
     */
    public static class SpO2Statistiken {
        public final float durchschnitt;
        public final int minimum;
        public final int maximum;
        public final int anzahlMessungen;
        public final float standardAbweichung;
        public final boolean hatGenugDaten;
        public final String bewertung;
        public final String empfehlung;
        public final String gesundheitsStatus;
        public final int anzahlKritischeWerte;

        public SpO2Statistiken(float durchschnitt, int minimum, int maximum,
                               int anzahlMessungen, float standardAbweichung,
                               boolean hatGenugDaten, String bewertung,
                               String empfehlung, String gesundheitsStatus,
                               int anzahlKritischeWerte) {
            this.durchschnitt = durchschnitt;
            this.minimum = minimum;
            this.maximum = maximum;
            this.anzahlMessungen = anzahlMessungen;
            this.standardAbweichung = standardAbweichung;
            this.hatGenugDaten = hatGenugDaten;
            this.bewertung = bewertung;
            this.empfehlung = empfehlung;
            this.gesundheitsStatus = gesundheitsStatus;
            this.anzahlKritischeWerte = anzahlKritischeWerte;
        }
    }

    /**
     * Callback-Interface für asynchrone Statistik-Berechnungen
     */
    public interface SpO2StatistikCallback {
        void onStatistikenBerechnet(SpO2Statistiken statistiken);
        void onFehler(String fehlermeldung);
    }

    /**
     * Konstruktor
     */
    public SpO2StatistikManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = ZyklusDatenbank.getInstanz(this.context);
        this.wellbeingDao = database.wohlbefindenDao();
        Log.d(TAG, "SpO2StatistikManager initialisiert");
    }

    /**
     * Berechnet umfassende SpO2-Statistiken für einen bestimmten Zeitraum
     *
     * @param zeitraumTage Anzahl der Tage rückwirkend (z.B. 30 für letzten Monat)
     * @param callback Callback für das Ergebnis
     */
    public void berechneStatistiken(int zeitraumTage, SpO2StatistikCallback callback) {
        Log.d(TAG, "Berechne SpO2-Statistiken für " + zeitraumTage + " Tage");

        // Background Thread für Datenbankoperationen
        new Thread(() -> {
            try {
                // Zeitraum definieren
                LocalDate endDatum = LocalDate.now();
                LocalDate startDatum = endDatum.minusDays(zeitraumTage);

                // Daten aus Datenbank laden
                List<WohlbefindenEintrag> eintraege =
                        wellbeingDao.getEintraegeZwischen(startDatum, endDatum);

                // SpO2-Daten extrahieren
                List<Integer> spo2Werte = new ArrayList<>();
                for (WohlbefindenEintrag eintrag : eintraege) {
                    if (eintrag.getSpo2() != null && eintrag.getSpo2() > 0) {
                        spo2Werte.add(eintrag.getSpo2());
                    }
                }


                Log.d(TAG, "=== SPO2 DEBUG ===");
                Log.d(TAG, "Zeitraum: " + startDatum + " bis " + endDatum);
                Log.d(TAG, "Geladene Einträge gesamt: " + eintraege.size());
                Log.d(TAG, "Extrahierte SpO2-Werte: " + spo2Werte.size() + " Werte");
                if (!spo2Werte.isEmpty()) {
                    Log.d(TAG, "Erster SpO2: " + spo2Werte.get(0) + "%");
                    Log.d(TAG, "Letzter SpO2: " + spo2Werte.get(spo2Werte.size()-1) + "%");
                }
                Log.d(TAG, "=== SPO2 DEBUG ENDE ===");

                // Statistiken berechnen
                SpO2Statistiken statistiken = berechneSpO2Statistiken(spo2Werte);

                // Callback auf Main Thread
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onStatistikenBerechnet(statistiken);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Berechnen der SpO2-Statistiken: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onFehler("Fehler beim Laden der SpO2-Daten: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    /**
     * Berechnet die eigentlichen SpO2-Statistiken
     */
    private SpO2Statistiken berechneSpO2Statistiken(List<Integer> spo2Werte) {
        Log.d(TAG, "Berechne Statistiken für " + spo2Werte.size() + " SpO2-Werte");

        // Keine Daten vorhanden
        if (spo2Werte.isEmpty()) {
            return new SpO2Statistiken(
                    0f, 0, 0, 0, 0f, false,
                    "Keine SpO2-Daten vorhanden",
                    "Beginnen Sie mit der Aufzeichnung Ihrer Sauerstoffsättigung",
                    "Gesundheitsstatus kann nicht bestimmt werden",
                    0
            );
        }

        // Grundstatistiken berechnen
        float summe = 0f;
        int min = spo2Werte.get(0);
        int max = spo2Werte.get(0);
        int anzahlKritischeWerte = 0;

        for (Integer spo2 : spo2Werte) {
            summe += spo2;
            if (spo2 < min) min = spo2;
            if (spo2 > max) max = spo2;
            if (spo2 < SPO2_KRITISCH) anzahlKritischeWerte++;
        }

        float durchschnitt = summe / spo2Werte.size();

        // Standardabweichung berechnen
        float varianzSumme = 0f;
        for (Integer spo2 : spo2Werte) {
            varianzSumme += Math.pow(spo2 - durchschnitt, 2);
        }
        float standardAbweichung = (float) Math.sqrt(varianzSumme / spo2Werte.size());

        // Bewertungen generieren
        String bewertung = bewerteSpO2(durchschnitt, min, max, anzahlKritischeWerte);
        String empfehlung = generiereEmpfehlung(durchschnitt, min, max, anzahlKritischeWerte, spo2Werte.size());
        String gesundheitsStatus = bewerteGesundheitsStatus(durchschnitt, anzahlKritischeWerte);

        boolean hatGenugDaten = spo2Werte.size() >= 4; // Mindestens eine Woche Daten

        return new SpO2Statistiken((int)durchschnitt, min, max, spo2Werte.size(),
                standardAbweichung, hatGenugDaten, bewertung, empfehlung,
                gesundheitsStatus, anzahlKritischeWerte);
    }

    /**
     * Bewertet die SpO2-Werte medizinisch
     */
    private String bewerteSpO2(float durchschnitt, int min, int max, int anzahlKritischeWerte) {
        if (durchschnitt >= SPO2_NORMAL_MIN) {
            if (durchschnitt >= SPO2_OPTIMAL) {
                return "Ihre Sauerstoffsättigung ist optimal.";
            } else {
                return "Ihre Sauerstoffsättigung liegt im normalen Bereich.";
            }
        } else if (durchschnitt >= SPO2_KRITISCH) {
            return "Leicht niedrige Sauerstoffsättigung. Beobachten Sie die Werte.";
        } else {
            return "⚠️ Niedrige Sauerstoffsättigung erkannt. Konsultieren Sie einen Arzt.";
        }
    }

    /**
     * Bewertet den allgemeinen Gesundheitsstatus
     */
    private String bewerteGesundheitsStatus(float durchschnitt, int anzahlKritischeWerte) {
        if (anzahlKritischeWerte > 0) {
            return "🏥 Ärztliche Konsultation empfohlen";
        } else if (durchschnitt >= SPO2_OPTIMAL) {
            return "💚 Ausgezeichnete Atemfunktion";
        } else if (durchschnitt >= SPO2_NORMAL_MIN) {
            return "✅ Normale Atemfunktion";
        } else {
            return "⚠️ Atemfunktion überwachen";
        }
    }

    /**
     * Generiert personalisierte Empfehlungen
     */
    private String generiereEmpfehlung(float durchschnitt, int min, int max, int anzahlKritischeWerte, int anzahlMessungen) {
        StringBuilder empfehlung = new StringBuilder();

        if (anzahlMessungen < 14) {
            empfehlung.append("📊 Sammeln Sie weitere Daten für präzisere Analysen. ");
        }

        if (anzahlKritischeWerte > 0) {
            empfehlung.append("🏥 Bei wiederholten Werten unter 90% sollten Sie einen Arzt konsultieren. ");
        }

        if (durchschnitt < SPO2_NORMAL_MIN) {
            empfehlung.append("🫁 Atemübungen und regelmäßige Bewegung können helfen. ");
        }

        if (max - min > 10) {
            empfehlung.append("📈 Große Schwankungen erkannt. Dies kann verschiedene Ursachen haben. ");
        }

        empfehlung.append("💡 Messen Sie in Ruhe für die genauesten Ergebnisse.");

        return empfehlung.toString();
    }

    /**
     * Erstellt Liniendiagramm für SpO2-Verlauf
     */
    public void erstelleSpO2Diagramm(LineChart chart, int zeitraumTage) {
        Log.d(TAG, "Erstelle SpO2-Diagramm für " + zeitraumTage + " Tage");

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
                    if (eintrag.getSpo2() != null && eintrag.getSpo2() > 0) {
                        entries.add(new Entry(index, eintrag.getSpo2()));
                        dates.add(eintrag.getDatum().format(DateTimeFormatter.ofPattern("dd.MM")));
                        index++;
                    }
                }

                // Chart auf Main Thread aktualisieren
                mainHandler.post(() -> {
                    aktualisiereChart(chart, entries, dates, "Sauerstoffsättigung (%)");
                });

            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Erstellen des SpO2-Diagramms: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Aktualisiert das LineChart mit den bereitgestellten Daten
     */
    private void aktualisiereChart(LineChart chart, List<Entry> entries, List<String> dates, String label) {
        if (entries.isEmpty()) {
            chart.setNoDataText("Keine SpO2-Daten verfügbar");
            chart.invalidate();
            return;
        }

        // Dataset erstellen
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(0xFF4CAF50); // Material Green
        dataSet.setCircleColor(0xFF4CAF50);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(30);
        dataSet.setFillColor(0xFF4CAF50);

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
        leftAxis.setAxisMinimum(85f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setGranularity(1f);

        chart.getAxisRight().setEnabled(false);

        // Chart aktualisieren
        chart.invalidate();
        Log.d(TAG, "SpO2-Diagramm erfolgreich aktualisiert mit " + entries.size() + " Datenpunkten");
    }

    /**
     * Cleanup-Methode
     */
    public void cleanup() {
        Log.d(TAG, "SpO2StatistikManager cleanup");
    }
}