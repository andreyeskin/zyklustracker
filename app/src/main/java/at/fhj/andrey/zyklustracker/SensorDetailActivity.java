package at.fhj.andrey.zyklustracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import at.fhj.andrey.zyklustracker.datenbank.WohlbefindenDao;
import at.fhj.andrey.zyklustracker.datenbank.WohlbefindenEintrag;
import at.fhj.andrey.zyklustracker.datenbank.ZyklusDatenbank;
import at.fhj.andrey.zyklustracker.sensors.sensors.statistik.TemperaturStatistikManager;
import at.fhj.andrey.zyklustracker.sensors.sensors.statistik.PulsStatistikManager;
import at.fhj.andrey.zyklustracker.sensors.sensors.statistik.SpO2StatistikManager;

/**
 * SensorDetailActivity - Vollbild-Dialog für detaillierte Sensor-Statistiken
 *
 * Diese Activity zeigt umfassende Statistiken und Charts für einen spezifischen
 * Sensor-Typ (Temperatur, Puls oder SpO2). Sie wird von ZyklusActivity aufgerufen,
 * wenn der Benutzer auf einen Sensor-Bereich im Tagesbericht klickt.
 *
 * Features:
 * - Statistik-Karten im 2x2 Grid (Durchschnitt, Anzahl, Min, Max)
 * - Liniendiagramm für Verlaufsdarstellung der letzten 30 Tage
 * - Medizinische Bewertung und personalisierte Empfehlungen
 * - Modernes Material Design mit sensor-spezifischen Farben
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class SensorDetailActivity extends AppCompatActivity {

    private static final String TAG = "SensorDetailActivity";

    // Intent-Parameter Konstanten
    public static final String EXTRA_SENSOR_TYP = "sensor_typ";
    public static final String SENSOR_TEMPERATUR = "temperatur";
    public static final String SENSOR_PULS = "puls";
    public static final String SENSOR_SPO2 = "spo2";

    // Standard-Zeitraum für Statistiken (30 Tage)
    private static final int STANDARD_ZEITRAUM_TAGE = 30;

    // UI-Komponenten
    private ImageView sensorIcon;
    private TextView sensorTitel;
    private ImageView closeButton;
    private LineChart sensorChart;
    private MaterialButton schliessenButton;

    // Statistik-Werte TextViews
    private TextView durchschnittWert;
    private TextView anzahlMessungen;
    private TextView minimumWert;
    private TextView maximumWert;
    private TextView medizinischeBewertung;
    private TextView empfehlungen;

    // Sensor-Manager
    private TemperaturStatistikManager temperaturManager;
    private PulsStatistikManager pulsManager;
    private SpO2StatistikManager spO2Manager;

    // Aktueller Sensor-Typ
    private String currentSensorType;

    /**
     * Debug-Methode: Prüft ob Sensordaten in der Datenbank vorhanden sind
     */
    private void debugDatenbankInhalt() {
        Log.d(TAG, "=== DEBUG: Prüfe Datenbank-Inhalt ===");

        new Thread(() -> {
            try {
                ZyklusDatenbank database = ZyklusDatenbank.getInstanz(this);
                WohlbefindenDao dao = database.wohlbefindenDao();

                // Alle Einträge laden
                List<WohlbefindenEintrag> alleEintraege = dao.getAlleEintraege();
                Log.d(TAG, "Gesamt-Einträge in Datenbank: " + alleEintraege.size());

                // Einträge mit Sensordaten zählen
                int mitTemperatur = 0, mitPuls = 0, mitSpO2 = 0;

                for (WohlbefindenEintrag eintrag : alleEintraege) {
                    if (eintrag.getTemperatur() != null && eintrag.getTemperatur() > 0) {
                        mitTemperatur++;
                        Log.d(TAG, "Temperatur gefunden: " + eintrag.getDatum() + " = " + eintrag.getTemperatur() + "°C");
                    }
                    if (eintrag.getPuls() != null && eintrag.getPuls() > 0) {
                        mitPuls++;
                        Log.d(TAG, "Puls gefunden: " + eintrag.getDatum() + " = " + eintrag.getPuls() + "bpm");
                    }
                    if (eintrag.getSpo2() != null && eintrag.getSpo2() > 0) {
                        mitSpO2++;
                        Log.d(TAG, "SpO2 gefunden: " + eintrag.getDatum() + " = " + eintrag.getSpo2() + "%");
                    }
                }

                Log.d(TAG, "Einträge mit Temperatur: " + mitTemperatur);
                Log.d(TAG, "Einträge mit Puls: " + mitPuls);
                Log.d(TAG, "Einträge mit SpO2: " + mitSpO2);
                Log.d(TAG, "=== DEBUG ENDE ===");

            } catch (Exception e) {
                Log.e(TAG, "DEBUG-Fehler: " + e.getMessage(), e);
            }
        }).start();
    }    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_sensor_statistik);

        Log.d(TAG, "SensorDetailActivity gestartet");

        // Intent-Parameter lesen
        currentSensorType = getIntent().getStringExtra(EXTRA_SENSOR_TYP);
        if (currentSensorType == null) {
            Log.e(TAG, "Kein Sensor-Typ in Intent gefunden!");
            finish();
            return;
        }

        // UI-Komponenten initialisieren
        initializeUIComponents();

        // Sensor-Manager initialisieren
        initializeSensorManagers();

        // UI basierend auf Sensor-Typ konfigurieren
        configureSensorSpecificUI();

        // Event-Handler einrichten
        setupEventHandlers();

        // Statistiken laden
        loadSensorStatistics();
        debugDatenbankInhalt();
    }

    /**
     * Initialisiert alle UI-Komponenten mit findViewById
     */
    private void initializeUIComponents() {
        Log.d(TAG, "Initialisiere UI-Komponenten...");

        // Header-Komponenten
        sensorIcon = findViewById(R.id.icon_sensor_type);
        sensorTitel = findViewById(R.id.text_sensor_titel);
        closeButton = findViewById(R.id.btn_close_dialog);

        // Chart
        sensorChart = findViewById(R.id.chart_sensor_verlauf);

        // Statistik-Karten Werte
        durchschnittWert = findViewById(R.id.text_durchschnitt_wert);
        anzahlMessungen = findViewById(R.id.text_anzahl_messungen);
        minimumWert = findViewById(R.id.text_minimum_wert);
        maximumWert = findViewById(R.id.text_maximum_wert);

        // Bewertung und Empfehlungen
        medizinischeBewertung = findViewById(R.id.text_medizinische_bewertung);
        empfehlungen = findViewById(R.id.text_empfehlungen);

        // Schließen-Button
        schliessenButton = findViewById(R.id.btn_dialog_schliessen);

        Log.d(TAG, "UI-Komponenten erfolgreich initialisiert");
    }

    /**
     * Initialisiert die Sensor-Manager
     */
    private void initializeSensorManagers() {
        Log.d(TAG, "Initialisiere Sensor-Manager...");

        temperaturManager = new TemperaturStatistikManager(this);
        pulsManager = new PulsStatistikManager(this);
        spO2Manager = new SpO2StatistikManager(this);

        Log.d(TAG, "Sensor-Manager erfolgreich initialisiert");
    }

    /**
     * Konfiguriert die UI basierend auf dem ausgewählten Sensor-Typ
     */
    private void configureSensorSpecificUI() {
        Log.d(TAG, "Konfiguriere UI für Sensor-Typ: " + currentSensorType);

        switch (currentSensorType) {
            case SENSOR_TEMPERATUR:
                configureTemperaturUI();
                break;
            case SENSOR_PULS:
                configurePulsUI();
                break;
            case SENSOR_SPO2:
                configureSpO2UI();
                break;
            default:
                Log.e(TAG, "Unbekannter Sensor-Typ: " + currentSensorType);
                finish();
                break;
        }
    }

    /**
     * Konfiguriert UI für Temperatur-Statistiken
     */
    private void configureTemperaturUI() {
        sensorIcon.setImageResource(R.drawable.temperature);

        sensorTitel.setText("Temperatur-Statistiken");
        findViewById(R.id.layout_dialog_header).setBackgroundResource(R.drawable.gradient_pink);
        Log.d(TAG, "Temperatur-UI konfiguriert");
    }

    /**
     * Konfiguriert UI für Puls-Statistiken
     */
    private void configurePulsUI() {
        sensorIcon.setImageResource(R.drawable.heart);

        sensorTitel.setText("Puls-Statistiken");
        findViewById(R.id.layout_dialog_header).setBackgroundResource(R.drawable.gradient_green);
        Log.d(TAG, "Puls-UI konfiguriert");
    }

    /**
     * Konfiguriert UI für SpO2-Statistiken
     */
    private void configureSpO2UI() {
        sensorIcon.setImageResource(R.drawable.oxygen);
        sensorTitel.setText("SpO₂-Statistiken");
        findViewById(R.id.layout_dialog_header).setBackgroundResource(R.drawable.gradient_blue);
        Log.d(TAG, "SpO2-UI konfiguriert");
    }

    /**
     * Richtet Event-Handler für Buttons ein
     */
    private void setupEventHandlers() {
        Log.d(TAG, "Richte Event-Handler ein...");

        // Schließen-Button im Header
        closeButton.setOnClickListener(v -> {
            Log.d(TAG, "Schließen-Button (Header) geklickt");
            finish();
        });

        // Schließen-Button am Ende
        schliessenButton.setOnClickListener(v -> {
            Log.d(TAG, "Schließen-Button (Ende) geklickt");
            finish();
        });

        Log.d(TAG, "Event-Handler erfolgreich eingerichtet");
    }

    /**
     * Lädt die Sensor-Statistiken basierend auf dem aktuellen Typ
     */
    private void loadSensorStatistics() {
        Log.d(TAG, "Lade Sensor-Statistiken für: " + currentSensorType);

        switch (currentSensorType) {
            case SENSOR_TEMPERATUR:
                loadTemperaturStatistics();
                break;
            case SENSOR_PULS:
                loadPulsStatistics();
                break;
            case SENSOR_SPO2:
                loadSpO2Statistics();
                break;
            default:
                Log.e(TAG, "Kann Statistiken für unbekannten Sensor-Typ nicht laden: " + currentSensorType);
                showErrorMessage("Unbekannter Sensor-Typ");
                break;
        }
    }

    /**
     * Lädt und zeigt Temperatur-Statistiken
     */
    private void loadTemperaturStatistics() {
        Log.d(TAG, "Lade Temperatur-Statistiken...");

        // Statistiken berechnen
        temperaturManager.berechneStatistiken(STANDARD_ZEITRAUM_TAGE, new TemperaturStatistikManager.TemperaturStatistikCallback() {
            @Override
            public void onStatistikenBerechnet(TemperaturStatistikManager.TemperaturStatistiken statistiken) {
                runOnUiThread(() -> {
                    updateTemperaturUI(statistiken);
                    Log.d(TAG, "Temperatur-Statistiken geladen: " + statistiken.anzahlMessungen + " Messungen");
                });
            }

            @Override
            public void onFehler(String fehlermeldung) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Fehler beim Laden der Temperatur-Statistiken: " + fehlermeldung);
                    showErrorMessage("Fehler beim Laden der Temperatur-Daten: " + fehlermeldung);
                    setNoDataUI();
                });
            }
        });

        // Chart erstellen - KORRIGIERTER METHODENNAME
        temperaturManager.erstelleTemperaturDiagramm(sensorChart, STANDARD_ZEITRAUM_TAGE);
    }

    /**
     * Lädt Puls-Statistiken
     */
    private void loadPulsStatistics() {
        Log.d(TAG, "Lade Puls-Statistiken...");

        // Statistiken berechnen
        pulsManager.berechneStatistiken(STANDARD_ZEITRAUM_TAGE, new PulsStatistikManager.PulsStatistikCallback() {
            @Override
            public void onStatistikenBerechnet(PulsStatistikManager.PulsStatistiken statistiken) {
                runOnUiThread(() -> {
                    updatePulsUI(statistiken);
                    Log.d(TAG, "Puls-Statistiken geladen: " + statistiken.anzahlMessungen + " Messungen");
                });
            }

            @Override
            public void onFehler(String fehlermeldung) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Fehler beim Laden der Puls-Statistiken: " + fehlermeldung);
                    showErrorMessage("Fehler beim Laden der Puls-Daten: " + fehlermeldung);
                    setNoDataUI();
                });
            }
        });

        // Chart erstellen - KORRIGIERTER METHODENNAME
        pulsManager.erstellePulsDiagramm(sensorChart, STANDARD_ZEITRAUM_TAGE);
    }





    /**
     * Lädt und zeigt SpO2-Statistiken
     */
    private void loadSpO2Statistics() {
        Log.d(TAG, "Lade SpO2-Statistiken...");

        // Statistiken berechnen
        spO2Manager.berechneStatistiken(STANDARD_ZEITRAUM_TAGE, new SpO2StatistikManager.SpO2StatistikCallback() {
            @Override
            public void onStatistikenBerechnet(SpO2StatistikManager.SpO2Statistiken statistiken) {
                runOnUiThread(() -> {
                    updateSpO2UI(statistiken);
                    Log.d(TAG, "SpO2-Statistiken geladen: " + statistiken.anzahlMessungen + " Messungen");
                });
            }

            @Override
            public void onFehler(String fehlermeldung) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Fehler beim Laden der SpO2-Statistiken: " + fehlermeldung);
                    showErrorMessage("Fehler beim Laden der SpO2-Daten: " + fehlermeldung);
                    setNoDataUI();
                });
            }
        });

        // Chart erstellen - KORRIGIERTER METHODENNAME
        spO2Manager.erstelleSpO2Diagramm(sensorChart, STANDARD_ZEITRAUM_TAGE);
    }

    /**
     * Aktualisiert die UI mit Temperatur-Statistiken
     */
    private void updateTemperaturUI(TemperaturStatistikManager.TemperaturStatistiken stats) {
        Log.d(TAG, "Aktualisiere Temperatur-UI mit Statistiken");

        if (stats.hatGenugDaten) {
            durchschnittWert.setText(String.format("%.1f°C", stats.durchschnitt));
            minimumWert.setText(String.format("%.1f°C", stats.minimum));
            maximumWert.setText(String.format("%.1f°C", stats.maximum));
            anzahlMessungen.setText(stats.anzahlMessungen + " Tage");

            medizinischeBewertung.setText(stats.bewertung);
            empfehlungen.setText(stats.empfehlung);
        } else {
            setNoDataUI();
        }
    }

    /**
     * Aktualisiert die UI mit Puls-Statistiken
     */
    private void updatePulsUI(PulsStatistikManager.PulsStatistiken stats) {
        Log.d(TAG, "Aktualisiere Puls-UI mit Statistiken");

        if (stats.hatGenugDaten) {
            durchschnittWert.setText(String.format("%.0f bpm", stats.durchschnitt));
            minimumWert.setText(stats.minimum + " bpm");
            maximumWert.setText(stats.maximum + " bpm");
            anzahlMessungen.setText(stats.anzahlMessungen + " Tage");

            medizinischeBewertung.setText(stats.bewertung);
            empfehlungen.setText(stats.empfehlung);
        } else {
            setNoDataUI();
        }
    }

    /**
     * Aktualisiert die UI mit SpO2-Statistiken
     */
    private void updateSpO2UI(SpO2StatistikManager.SpO2Statistiken stats) {
        Log.d(TAG, "Aktualisiere SpO2-UI mit Statistiken");

        if (stats.hatGenugDaten) {
            durchschnittWert.setText(String.format("%.1f%%", stats.durchschnitt));
            minimumWert.setText(stats.minimum + "%");
            maximumWert.setText(stats.maximum + "%");
            anzahlMessungen.setText(stats.anzahlMessungen + " Tage");

            medizinischeBewertung.setText(stats.bewertung);
            empfehlungen.setText(stats.empfehlung);
        } else {
            setNoDataUI();
        }
    }

    /**
     * Setzt UI auf "Keine Daten" Zustand
     */
    private void setNoDataUI() {
        durchschnittWert.setText("--");
        minimumWert.setText("--");
        maximumWert.setText("--");
        anzahlMessungen.setText("0 Tage");

        medizinischeBewertung.setText("Noch nicht genug Daten für aussagekräftige Statistiken.");
        empfehlungen.setText("Sammeln Sie weitere Messwerte für präzisere Analysen.");
    }

    /**
     * Zeigt eine Fehlermeldung als Toast
     */
    private void showErrorMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Cleanup der Manager
        if (temperaturManager != null) {
            temperaturManager.cleanup();
        }
        if (pulsManager != null) {
            pulsManager.cleanup();
        }
        if (spO2Manager != null) {
            spO2Manager.cleanup();
        }

        Log.d(TAG, "SensorDetailActivity cleanup abgeschlossen");
    }
}