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
 * TemperaturStatistikManager - Verwaltung der Temperatur-Statistiken und -Diagramme
 *
 * Diese Klasse ist spezialisiert auf:
 * - Laden von Temperaturdaten aus der Datenbank
 * - Berechnung statistischer Kennwerte (Durchschnitt, Min, Max, Trends)
 * - Erstellung von Liniendiagrammen für Temperaturverläufe
 * - Analyse zyklusbezogener Temperaturmuster
 *
 * Medizinischer Hintergrund:
 * - Normale Körpertemperatur: 36.0-37.5°C
 * - Zyklusbedingte Schwankungen: ~0.3-0.7°C zwischen Follikel- und Lutealphase
 * - Temperaturanstieg nach Eisprung durch Progesteronwirkung
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class TemperaturStatistikManager {

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String TAG = "TemperaturStatistikManager";

    // Medizinische Referenzwerte für Temperatur
    private static final float TEMP_NORMAL_MIN = 36.0f;
    private static final float TEMP_NORMAL_MAX = 37.5f;
    private static final float TEMP_ERHOEHT_SCHWELLE = 37.6f;
    private static final float TEMP_FIEBER_SCHWELLE = 38.0f;

    // Datenbankzugriff
    private final Context context;
    private final ZyklusDatenbank database;
    private final WohlbefindenDao wellbeingDao;

    /**
     * Datenmodell für Temperaturstatistiken
     */
    public static class TemperaturStatistiken {
        public final float durchschnitt;
        public final float minimum;
        public final float maximum;
        public final int anzahlMessungen;
        public final float standardAbweichung;
        public final boolean hatGenugDaten;
        public final String bewertung;
        public final String empfehlung;

        public TemperaturStatistiken(float durchschnitt, float minimum, float maximum,
                                     int anzahlMessungen, float standardAbweichung,
                                     boolean hatGenugDaten, String bewertung, String empfehlung) {
            this.durchschnitt = durchschnitt;
            this.minimum = minimum;
            this.maximum = maximum;
            this.anzahlMessungen = anzahlMessungen;
            this.standardAbweichung = standardAbweichung;
            this.hatGenugDaten = hatGenugDaten;
            this.bewertung = bewertung;
            this.empfehlung = empfehlung;
        }
    }

    /**
     * Callback-Interface für asynchrone Statistik-Berechnungen
     */
    public interface TemperaturStatistikCallback {
        void onStatistikenBerechnet(TemperaturStatistiken statistiken);
        void onFehler(String fehlermeldung);
    }

    /**
     * Konstruktor
     */
    public TemperaturStatistikManager(Context context) {
        this.context = context.getApplicationContext();
        this.database = ZyklusDatenbank.getInstanz(this.context);
        this.wellbeingDao = database.wohlbefindenDao();
        Log.d(TAG, "TemperaturStatistikManager initialisiert");
    }

    /**
     * Berechnet umfassende Temperaturstatistiken für einen bestimmten Zeitraum
     *
     * @param zeitraumTage Anzahl der Tage rückwirkend (z.B. 30 für letzten Monat)
     * @param callback Callback für das Ergebnis
     */
    public void berechneStatistiken(int zeitraumTage, TemperaturStatistikCallback callback) {
        Log.d(TAG, "Berechne Temperaturstatistiken für " + zeitraumTage + " Tage");

        // Background Thread für Datenbankoperationen
        new Thread(() -> {
            try {
                // Zeitraum definieren
                LocalDate endDatum = LocalDate.now();
                LocalDate startDatum = endDatum.minusDays(zeitraumTage);

                // Daten aus Datenbank laden
                List<WohlbefindenEintrag> eintraege =
                        wellbeingDao.getEintraegeZwischen(startDatum, endDatum);

                // Temperaturdaten extrahieren
                List<Float> temperaturen = new ArrayList<>();
                for (WohlbefindenEintrag eintrag : eintraege) {
                    if (eintrag.getTemperatur() != null && eintrag.getTemperatur() > 0) {
                        temperaturen.add(eintrag.getTemperatur());
                    }
                }
                // NACH dem Extrahieren der Temperaturdaten:

                Log.d(TAG, "=== TEMPERATUR DEBUG ===");
                Log.d(TAG, "Zeitraum: " + startDatum + " bis " + endDatum);
                Log.d(TAG, "Geladene Einträge gesamt: " + eintraege.size());

// Debug: Alle Einträge durchgehen
                for (WohlbefindenEintrag eintrag : eintraege) {
                    Log.d(TAG, "Eintrag " + eintrag.getDatum() + ": Temperatur=" + eintrag.getTemperatur() +
                            ", Puls=" + eintrag.getPuls() + ", SpO2=" + eintrag.getSpo2());
                }

                Log.d(TAG, "Extrahierte Temperaturen: " + temperaturen.size() + " Werte");
                if (!temperaturen.isEmpty()) {
                    Log.d(TAG, "Erste Temperatur: " + temperaturen.get(0) + "°C");
                    Log.d(TAG, "Letzte Temperatur: " + temperaturen.get(temperaturen.size()-1) + "°C");
                }
                Log.d(TAG, "=== TEMPERATUR DEBUG ENDE ===");

                // Statistiken berechnen
                TemperaturStatistiken statistiken = berechneTemperaturStatistiken(temperaturen);

                // Callback auf Main Thread
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onStatistikenBerechnet(statistiken);
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Berechnen der Temperaturstatistiken: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onFehler("Fehler beim Laden der Temperaturdaten: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    /**
     * WICHTIGE FEHLENDE METHODE: Berechnet die eigentlichen Statistiken
     */
    private TemperaturStatistiken berechneTemperaturStatistiken(List<Float> temperaturen) {
        Log.d(TAG, "Berechne Statistiken für " + temperaturen.size() + " Temperaturwerte");

        // Keine Daten vorhanden
        if (temperaturen.isEmpty()) {
            return new TemperaturStatistiken(
                    0f, 0f, 0f, 0, 0f, false,
                    "Keine Temperaturdaten vorhanden",
                    "Beginnen Sie mit der Aufzeichnung Ihrer Körpertemperatur"
            );
        }

        // Grundstatistiken berechnen
        float summe = 0f;
        float min = temperaturen.get(0);
        float max = temperaturen.get(0);

        for (Float temp : temperaturen) {
            summe += temp;
            if (temp < min) min = temp;
            if (temp > max) max = temp;
        }

        float durchschnitt = summe / temperaturen.size();

        // Standardabweichung berechnen
        float varianzSumme = 0f;
        for (Float temp : temperaturen) {
            varianzSumme += Math.pow(temp - durchschnitt, 2);
        }
        float standardAbweichung = (float) Math.sqrt(varianzSumme / temperaturen.size());

        // Bewertung generieren
        String bewertung = bewerteBedeutung(durchschnitt, min, max, standardAbweichung);
        String empfehlung = generiereEmpfehlung(durchschnitt, min, max, temperaturen.size());

        boolean hatGenugDaten = temperaturen.size() >= 7; // Mindestens eine Woche Daten

        return new TemperaturStatistiken(durchschnitt, min, max, temperaturen.size(),
                standardAbweichung, hatGenugDaten, bewertung, empfehlung);
    }

    /**
     * Bewertet die Bedeutung der Temperaturwerte medizinisch
     */
    private String bewerteBedeutung(float durchschnitt, float min, float max, float standardAbweichung) {
        if (durchschnitt >= TEMP_NORMAL_MIN && durchschnitt <= TEMP_NORMAL_MAX) {
            if (standardAbweichung < 0.3f) {
                return "Ihre Temperaturwerte sind stabil im Normalbereich.";
            } else {
                return "Normale Temperatur mit natürlichen Schwankungen.";
            }
        } else if (durchschnitt > TEMP_NORMAL_MAX) {
            return "Leicht erhöhte Durchschnittstemperatur. Mögliche zyklusbedingte Erhöhung.";
        } else {
            return "Niedrigere Durchschnittstemperatur. Dies kann individuell normal sein.";
        }
    }

    /**
     * Generiert personalisierte Empfehlungen basierend auf den Temperaturwerten
     */
    private String generiereEmpfehlung(float durchschnitt, float min, float max, int anzahlMessungen) {
        StringBuilder empfehlung = new StringBuilder();

        if (anzahlMessungen < 14) {
            empfehlung.append("📊 Sammeln Sie weitere Daten für präzisere Analysen. ");
        }

        if (max - min > 1.0f) {
            empfehlung.append("🌡️ Große Temperaturschwankungen erkannt. Diese können normal sein oder auf Zyklusphasen hinweisen. ");
        }

        if (durchschnitt >= TEMP_NORMAL_MIN && durchschnitt <= TEMP_NORMAL_MAX) {
            empfehlung.append("✅ Ihre Temperaturwerte liegen im gesunden Bereich. ");
        }

        empfehlung.append("💡 Messen Sie regelmäßig zur gleichen Zeit für beste Ergebnisse.");

        return empfehlung.toString();
    }

    /**
     * WICHTIGE FEHLENDE METHODE: Erstellt Liniendiagramm für Temperaturverlauf
     */
    public void erstelleTemperaturDiagramm(LineChart chart, int zeitraumTage) {
        Log.d(TAG, "Erstelle Temperaturdiagramm für " + zeitraumTage + " Tage");

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
                    if (eintrag.getTemperatur() != null && eintrag.getTemperatur() > 0) {
                        entries.add(new Entry(index, eintrag.getTemperatur()));
                        dates.add(eintrag.getDatum().format(DateTimeFormatter.ofPattern("dd.MM")));
                        index++;
                    }
                }

                // Chart auf Main Thread aktualisieren
                mainHandler.post(() -> {
                    aktualisiereChart(chart, entries, dates, "Körpertemperatur (°C)");
                });

            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Erstellen des Temperaturdiagramms: " + e.getMessage());
            }
        }).start();
    }

    /**
     * Aktualisiert das LineChart mit den bereitgestellten Daten
     */
    private void aktualisiereChart(LineChart chart, List<Entry> entries, List<String> dates, String label) {
        if (entries.isEmpty()) {
            chart.setNoDataText("Keine Temperaturdaten verfügbar");
            chart.invalidate();
            return;
        }

        // Dataset erstellen
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(0xFF1976D2); // Material Blue
        dataSet.setCircleColor(0xFF1976D2);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setValueTextSize(10f);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(30);
        dataSet.setFillColor(0xFF1976D2);

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
        leftAxis.setAxisMinimum(35.5f);
        leftAxis.setAxisMaximum(38.5f);
        leftAxis.setGranularity(0.1f);

        chart.getAxisRight().setEnabled(false);

        // Chart aktualisieren
        chart.invalidate();
        Log.d(TAG, "Temperaturdiagramm erfolgreich aktualisiert mit " + entries.size() + " Datenpunkten");
    }

    /**
     * Cleanup-Methode
     */
    public void cleanup() {
        Log.d(TAG, "TemperaturStatistikManager cleanup");
    }
}