package at.fhj.andrey.zyklustracker;

import static android.content.ContentValues.TAG;

import at.fhj.andrey.zyklustracker.datenbank.*;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.Pair;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.kizitonwose.calendarview.model.CalendarDay;
import com.kizitonwose.calendarview.model.CalendarMonth;
import com.kizitonwose.calendarview.CalendarView;
import com.kizitonwose.calendarview.model.DayOwner;
import com.kizitonwose.calendarview.ui.DayBinder;
import com.kizitonwose.calendarview.ui.ViewContainer;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import kotlin.Unit;

// Sensor-Integration
import at.fhj.andrey.zyklustracker.sensors.ZyklusSensorManager;
import at.fhj.andrey.zyklustracker.sensors.SensorData;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.pm.PackageManager;
import android.widget.Toast;
import android.util.Log;
import at.fhj.andrey.zyklustracker.statistik.StatistikManager;
import at.fhj.andrey.zyklustracker.zyklusanalyse.AnalyseErgebnis;
import at.fhj.andrey.zyklustracker.zyklusanalyse.ZyklusPhaseBerechnung;

/**
 * ZyklusActivity - Hauptaktivität für die Zyklusanzeige und -verwaltung
 *
 * Diese Aktivität stellt das Herzstück der App dar und bietet:
 *
 * Funktionalitäten:
 * - Kalenderansicht mit farblicher Markierung der Zyklusphasen
 * - Eingabe neuer Periodentage über Material DatePicker
 * - Automatische Berechnung von Eisprung und fruchtbaren Tagen
 * - Prognose zukünftiger Perioden basierend auf historischen Daten
 * - Verwaltung bestehender Periodentage (Löschen möglich)
 *
 * Kalender-Farbschema:
 * - Rot: Menstruationstage (echte Daten)
 * - Violett: Eisprung (14 Tage vor nächster Periode)
 * - Blau: Fruchtbare Phase (5 Tage vor Eisprung)
 *
 * Datenbank-Integration:
 * - Speicherung in Room-Datenbank (PeriodeEintrag-Entitäten)
 * - Unterscheidung zwischen echten Daten und Prognosen
 * - Automatische Neuberechnung bei Datenänderungen
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class ZyklusActivity extends AppCompatActivity
        implements ZyklusSensorManager.SensorCallback,
        ZyklusSensorManager.HealthConnectPermissionRequester
{

    // Datenbankzugriff
    private ZyklusDatenbank database;
    private PeriodeDao periodDao;

    // UI-Komponenten für die Kalenderanzeige
    private CalendarView calendarView;
    private YearMonth currentMonth = YearMonth.now();
    private TextView monthTitleText;
    private ImageView previousMonthButton, nextMonthButton;

    // Datenlisten für verschiedene Zyklusphasen
    private final List<LocalDate> menstruationDays = new ArrayList<>();
    private final List<LocalDate> ovulationDays = new ArrayList<>();
    private final List<LocalDate> fertileDays = new ArrayList<>();
    private final List<LocalDate> predictedMenstruation = new ArrayList<>();

    // Aktuell ausgewähltes Datum im Kalender
    private LocalDate selectedDate = null;
    // Sensor-Management
    private ZyklusSensorManager sensorManager;
    private TextView temperatureValueText;
    private TextView pulseValueText;
    private TextView spo2ValueText;
    // ===== NEUE ZYKLUSPHASEN-INTEGRATION =====
    private StatistikManager statistikManager;
    private TextView aktuellePhaseText;
    private TextView zyklusTagText;

    // Berechtigungen verwalten
    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zyklus);

        // UI-Komponenten initialisieren
        initializeUIComponents();

        // Datenbank initialisieren
        initializeDatabase();

        // Bottom Navigation konfigurieren
        setupBottomNavigation();

        // Kalender konfigurieren und einrichten
        setupCalendar();

        // Floating Action Button für Periodeneingabe konfigurieren
        setupPeriodInputButton();
        // Sensor-Click-Handler konfigurieren
        setupSensorClickHandlers();


        // Sensor-Integration initialisieren
        initializeSensors();
        setupPermissionLauncher();
        fordereBerechtigungenAn();
        starteSensorMessung();

        //  Diese beiden Aufrufe am Ende, da sie asynchron sind
        loadMenstruationDataFromDatabase();  // Lädt Daten im Background
        ladeSensordatenFuerAnzeige();        // Lädt Sensordaten im Background

        // StatistikManager für Zyklusphasen-Analyse initialisieren
        statistikManager = new StatistikManager(this);

// Aktuelle Zyklusphase laden und anzeigen
        ladeAktuelleZyklusphase();
    }

    /**
     * Initialisiert alle UI-Komponenten und weist sie den entsprechenden Variablen zu.
     */
    private void initializeUIComponents() {
        monthTitleText = findViewById(R.id.text_month_title);
        previousMonthButton = findViewById(R.id.btn_month_previous);
        nextMonthButton = findViewById(R.id.btn_month_next);
        calendarView = findViewById(R.id.calendar_view);
        // Sensor-UI-Komponenten initialisieren
        temperatureValueText = findViewById(R.id.text_temperature_value);
        pulseValueText = findViewById(R.id.text_pulse_value);
        spo2ValueText = findViewById(R.id.text_spo2_value);
        // Neue UI-Komponenten für Zyklusphase
        aktuellePhaseText = findViewById(R.id.text_cycle_phase_info);
        zyklusTagText = findViewById(R.id.text_cycle_day_info);
    }

    /**
     * Initialisiert die Room-Datenbank und die entsprechenden DAOs.
     */
    private void initializeDatabase() {
        database = ZyklusDatenbank.getInstanz(this);
        periodDao = database.periodeDao();
    }
    /**
     * Initialisiert den Sensor-Manager für Gesundheitsdaten
     */
    private void initializeSensors() {
        sensorManager = new ZyklusSensorManager(this);
        sensorManager.setzeCallback(this);
    }
    private void setupPermissionLauncher() {
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean alleErteilt = true;
                    for (Boolean erteilt : result.values()) {
                        if (!erteilt) {
                            alleErteilt = false;
                            break;
                        }
                    }

                    if (alleErteilt) {
                        starteSensorMessung();
                    } else {
                        zeigeBerechtigungVerweigertDialog();
                    }
                }
        );
    }
    /**
     * Fordert die notwendigen Sensor-Berechtigungen an
     */
    private void fordereBerechtigungenAn() {
        String[] berechtigungen = {
                android.Manifest.permission.BODY_SENSORS,
        };

        // Prüfen welche Berechtigungen noch fehlen
        List<String> zuFordernde = new ArrayList<>();
        for (String berechtigung : berechtigungen) {
            if (ContextCompat.checkSelfPermission(this, berechtigung)
                    != PackageManager.PERMISSION_GRANTED) {
                zuFordernde.add(berechtigung);
            }
        }

        if (zuFordernde.isEmpty()) {
            // Alle Berechtigungen bereits vorhanden
            starteSensorMessung();
        } else {
            // Berechtigungen anfordern
            permissionLauncher.launch(zuFordernde.toArray(new String[0]));
        }
    }

    /**
     * Startet die Sensor-Messungen
     */
    private void starteSensorMessung() {
        if (sensorManager != null) {
            // ZyklusSensorManager.starteMessung()
            sensorManager.starteMessung();

            Toast.makeText(this, "Sensor-Messung gestartet", Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "Health Connect-Integration aktiv",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Zeigt Dialog bei verweigerten Berechtigungen
     */
    private void zeigeBerechtigungVerweigertDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Berechtigungen erforderlich")
                .setMessage("Für die Health Connect-Funktionen werden spezielle Berechtigungen benötigt. " +
                        "Sie können diese in Health Connect nachträglich erteilen.")
                .setPositiveButton("OK", null)
                .setNegativeButton("Einstellungen", (dialog, which) -> {
                    // Öffne Health Connect oder App-Einstellungen
                    try {
                        Intent intent = new Intent("androidx.health.ACTION_REQUEST_PERMISSIONS");
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "Health Connect nicht gefunden", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /**
     * Lädt alle Menstruationsdaten aus der Room-Datenbank.
     * Filtert nur echte Periodendaten (keine Prognosen).
     * WICHTIG: Läuft jetzt im Background Thread!
     */
    private void loadMenstruationDataFromDatabase() {
        // Background Thread für Datenbankzugriff
        new Thread(() -> {
            try {
                Log.d("ZyklusActivity", "Lade Menstruationsdaten aus der Datenbank...");

                // Bestehende Liste leeren (auf Background Thread sicher)
                menstruationDays.clear();

                // Alle echten Periodeneinträge aus der Datenbank abrufen (Background Thread)
                List<PeriodeEintrag> entries = periodDao.getAlleEchtenPerioden();

                // PeriodeEintrag-Objekte in LocalDate umwandeln und zur Liste hinzufügen
                for (PeriodeEintrag entry : entries) {
                    menstruationDays.add(entry.getDatum());
                }

                Log.d("ZyklusActivity", "Menstruationsdaten geladen: " + menstruationDays.size() + " Einträge");

                // Fruchtbarkeitsberechnungen durchführen (bleibt im Background Thread!)
                // calculateFertilityData() verwaltet seine eigenen Threads für DB-Operationen
                calculateFertilityData();

                // NUR Kalender-Update auf UI Thread (OHNE weitere DB-Operationen)
                runOnUiThread(() -> {
                    calendarView.notifyCalendarChanged();
                    Log.d("ZyklusActivity", "Kalender-UI erfolgreich aktualisiert");
                });

            } catch (Exception e) {
                Log.e("ZyklusActivity", "Fehler beim Laden der Menstruationsdaten: " + e.getMessage(), e);

                // Fehler-Behandlung auf UI Thread
                runOnUiThread(() -> {
                    Toast.makeText(ZyklusActivity.this,
                            "Fehler beim Laden der Zyklusdaten",
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
    /**
     * Berechnet Eisprung, fruchtbare Tage und zukünftige Perioden basierend auf
     * den vorhandenen Menstruationsdaten.
     *
     * WICHTIG: Diese Methode führt Datenbankoperationen aus und muss im Background Thread laufen!
     * Wird automatisch von loadMenstruationDataFromDatabase() im Background aufgerufen.
     *
     * Algorithmus:
     * 1. Bestehende Prognosedaten löschen
     * 2. Zykluslänge aus letzten beiden Perioden berechnen
     * 3. Standardwert 28 Tage verwenden wenn unrealistisch
     * 4. Eisprung = 14 Tage vor nächster Periode
     * 5. Fruchtbare Phase = 5 Tage vor Eisprung
     * 6. Prognose für 3 zukünftige Zyklen
     */
    /**
     * Berechnet Eisprung, fruchtbare Phase und zukünftige Perioden basierend auf
     * den vorhandenen Menstruationsdaten.
     *
     * Korrigierte Berechnung:
     * - Eisprung = 14 Tage vor der NÄCHSTEN prognostizierten Periode
     * - Fruchtbare Phase = 5 Tage vor bis 2 Tage nach Eisprung (7 Tage total)
     * - Realistische Zykluslängen (21-35 Tage, Standard 28)
     */
    private void calculateFertilityData() {
        clearPredictionData();

        List<LocalDate> sortedDays = new ArrayList<>(menstruationDays);
        Collections.sort(sortedDays);

        if (sortedDays.size() < 2) {
            runOnUiThread(() -> calendarView.notifyCalendarChanged());
            return;
        }

        // Finde den letzten Periodenstart (nicht einzelnen Tag)
        LocalDate letzterPeriodenstart = findeLetztenPeriodenstart(sortedDays);
        LocalDate vorletzterPeriodenstart = findeVorletztenPeriodenstart(sortedDays, letzterPeriodenstart);

        if (vorletzterPeriodenstart == null) {
            runOnUiThread(() -> calendarView.notifyCalendarChanged());
            return;
        }
        // Korrekte Zykluslänge berechnen
        int zyklusLaenge = (int) ChronoUnit.DAYS.between(vorletzterPeriodenstart, letzterPeriodenstart);

// Validierung und Normalisierung
        if (zyklusLaenge < 21 || zyklusLaenge > 35) {
            zyklusLaenge = 28; // Medizinischer Standard
            Log.w(TAG, "Unrealistische Zykluslänge erkannt (" + zyklusLaenge +
                    " Tage), verwende Standard: 28 Tage");
        }

        //  PROGNOSE für die nächsten 3 Zyklen
        for (int zyklus = 1; zyklus <= 3; zyklus++) {
            // Nächster Periodenstart
            LocalDate naechsterPeriodenstart = letzterPeriodenstart.plusDays(zyklus * zyklusLaenge);

            //Eisprung = 14 Tage NACH dem Start jedes Zyklus
            LocalDate zyklusStart = letzterPeriodenstart.plusDays((zyklus - 1) * zyklusLaenge);
            LocalDate eisprung = zyklusStart.plusDays(13);

            // Füge Eisprung hinzu
            ovulationDays.add(eisprung);

            //Fruchtbare Phase = 5 Tage vor bis 2 Tage nach Eisprung
            for (int tag = -5; tag <= 2; tag++) {
                LocalDate fruchtbarerTag = eisprung.plusDays(tag);
                fertileDays.add(fruchtbarerTag);
            }

            // Prognostizierte Menstruation (5-6 Tage)
            for (int tag = 0; tag < 6; tag++) {
                LocalDate menstruationsTag = naechsterPeriodenstart.plusDays(tag);
                predictedMenstruation.add(menstruationsTag);
            }
        }

// UI aktualisieren
        runOnUiThread(() -> {
            calendarView.notifyCalendarChanged();
            Log.d(TAG, "Fertilitätsdaten korrekt berechnet: " + ovulationDays.size() + " Eisprünge, " + fertileDays.size() + " fruchtbare Tage");
        });
    }

    /**
     * Findet den letzten Periodenstart (ersten Tag einer zusammenhängenden Periode)
     */
    private LocalDate findeLetztenPeriodenstart(List<LocalDate> sortierteTage) {
        if (sortierteTage.isEmpty()) return null;

        // Gruppiere zusammenhängende Tage zu Perioden
        List<List<LocalDate>> perioden = gruppiereZuPerioden(sortierteTage);

        if (perioden.isEmpty()) return null;

        // Letzter Periodenstart = erster Tag der letzten Periode
        List<LocalDate> letzteperiode = perioden.get(perioden.size() - 1);
        Collections.sort(letzteperiode);
        return letzteperiode.get(0);
    }

    /**
     * Findet den vorletzten Periodenstart für Zykluslängen-Berechnung
     */
    private LocalDate findeVorletztenPeriodenstart(List<LocalDate> sortierteTage, LocalDate letzterStart) {
        List<List<LocalDate>> perioden = gruppiereZuPerioden(sortierteTage);

        if (perioden.size() < 2) return null;

        // Vorletzte Periode
        List<LocalDate> vorletzteperiode = perioden.get(perioden.size() - 2);
        Collections.sort(vorletzteperiode);
        return vorletzteperiode.get(0);
    }

    /**
     * Gruppiert einzelne Menstruationstage zu zusammenhängenden Perioden
     */
    private List<List<LocalDate>> gruppiereZuPerioden(List<LocalDate> alleTage) {
        List<List<LocalDate>> perioden = new ArrayList<>();
        List<LocalDate> aktuelleperiode = new ArrayList<>();

        for (int i = 0; i < alleTage.size(); i++) {
            LocalDate aktuellerTag = alleTage.get(i);

            if (aktuelleperiode.isEmpty()) {
                // Erste Periode beginnen
                aktuelleperiode.add(aktuellerTag);
            } else {
                LocalDate letzterTag = Collections.max(aktuelleperiode);

                // Wenn mehr als 2 Tage Abstand, neue Periode beginnen
                if (ChronoUnit.DAYS.between(letzterTag, aktuellerTag) > 2) {
                    perioden.add(new ArrayList<>(aktuelleperiode));
                    aktuelleperiode.clear();
                    aktuelleperiode.add(aktuellerTag);
                } else {
                    aktuelleperiode.add(aktuellerTag);
                }
            }
        }

        // Letzte Periode hinzufügen
        if (!aktuelleperiode.isEmpty()) {
            perioden.add(aktuelleperiode);
        }

        return perioden;
    }

    /**
     * Löscht alle Prognosedaten aus den lokalen Listen.
     */
    private void clearPredictionData() {
        predictedMenstruation.clear();
        ovulationDays.clear();
        fertileDays.clear();
    }

    /**
     * Konfiguriert die Bottom Navigation mit entsprechenden Event-Handlern.
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.nav_cycle);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_wellbeing) {
                startActivity(new Intent(this, WohlbefindenActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_statistics) {
                startActivity(new Intent(this, StatistikActivity.class));
                finish();
                return true;
            }
            return true;
        });
    }

    /**
     * Konfiguriert die CalendarView mit DayBinder und Event-Handlern.
     * Setzt Monatsnavigation und Kalenderanzeige auf.
     */
    private void setupCalendar() {
        // DayBinder für die Darstellung einzelner Kalendertage
        calendarView.setDayBinder(new DayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }

            @Override
            public void bind(DayViewContainer container, CalendarDay day) {
                LocalDate heute = LocalDate.now(); // Heutiges Datum
                LocalDate date = day.getDate();
                container.textView.setText(String.valueOf(date.getDayOfMonth()));
                container.dotView.setVisibility(View.GONE); // Standardmäßig versteckt

                // Textfarbe für Tage außerhalb des aktuellen Monats
                if (day.getOwner() != DayOwner.THIS_MONTH) {
                    container.textView.setTextColor(Color.parseColor("#CCCCCC"));
                } else {
                    container.textView.setTextColor(Color.BLACK);
                }

                Context context = container.textView.getContext();

                // Prioritätsreihenfolge: Echte Menstruation > Prognose > Eisprung > Fruchtbare Tage
                if (menstruationDays.contains(date)) {
                    container.dotView.setVisibility(View.VISIBLE);
                    container.dotView.setBackground(ContextCompat.getDrawable(context, R.drawable.dot_menstruation));
                } else if (predictedMenstruation.contains(date)) {
                    container.dotView.setVisibility(View.VISIBLE);
                    container.dotView.setBackground(ContextCompat.getDrawable(context, R.drawable.dot_predicted_period));
                } else if (ovulationDays.contains(date)) {
                    container.dotView.setVisibility(View.VISIBLE);
                    container.dotView.setBackground(ContextCompat.getDrawable(context, R.drawable.triangle_ovulation));
                } else if (fertileDays.contains(date)) {
                    container.dotView.setVisibility(View.VISIBLE);
                    container.dotView.setBackground(ContextCompat.getDrawable(context, R.drawable.dot_fertile));
                }

                // Hervorhebung des ausgewählten Tages
                // Hervorhebung: Priorität - ausgewählt > heute > normal
                if (date.equals(selectedDate)) {
                    // Ausgewählter Tag hat höchste Priorität
                    container.textView.setBackgroundResource(R.drawable.selected_day_background);
                    container.textView.setTextColor(Color.WHITE);
                } else if (date.equals(heute)) {
                    // Heutiger Tag wird hervorgehoben
                    container.textView.setBackgroundResource(R.drawable.bg_today);
                    container.textView.setTextColor(Color.BLACK);
                } else {
                    // Normaler Tag
                    container.textView.setBackground(null);
                    // Textfarbe für Tage des aktuellen Monats wiederherstellen
                    if (day.getOwner() == DayOwner.THIS_MONTH) {
                        container.textView.setTextColor(Color.BLACK);
                    }
                }

                // Click-Handler für Tagesauswahl
                container.textView.setOnClickListener(v -> {
                    // Toggle-Logik: bei erneutem Klick auf denselben Tag → Auswahl zurücksetzen
                    if (date.equals(selectedDate)) {
                        selectedDate = null; // Auswahl zurücksetzen
                    } else {
                        selectedDate = date; // Neuen Tag auswählen
                    }

                    calendarView.notifyCalendarChanged();

                    // Info-Karte aktualisieren
                    ladeAktuelleZyklusphase();
                });
            }
        });

        // Kalender-Setup und Navigation
        calendarView.setup(currentMonth, currentMonth, DayOfWeek.MONDAY);
        calendarView.scrollToMonth(currentMonth);
        updateMonthTitle(currentMonth);

        // Initialer Monatstitel
        CalendarMonth initialMonth = calendarView.findFirstVisibleMonth();
        if (initialMonth != null) {
            updateMonthTitle(initialMonth.getYearMonth());
        } else {
            updateMonthTitle(currentMonth);
        }

        // Listener für Monatsscroll
        calendarView.setMonthScrollListener(calendarMonth -> {
            currentMonth = calendarMonth.getYearMonth();
            updateMonthTitle(currentMonth);
            return Unit.INSTANCE;
        });

        // Navigation Buttons
        previousMonthButton.setOnClickListener(v -> {
            currentMonth = currentMonth.minusMonths(1);
            calendarView.setup(currentMonth, currentMonth, DayOfWeek.MONDAY);
            updateMonthTitle(currentMonth);
        });

        nextMonthButton.setOnClickListener(v -> {
            currentMonth = currentMonth.plusMonths(1);
            calendarView.setup(currentMonth, currentMonth, DayOfWeek.MONDAY);
            updateMonthTitle(currentMonth);
        });
    }

    /**
     * Konfiguriert die Click-Handler für die Sensor-Bereiche im Tagesbericht.
     * Öffnet jeweils die entsprechende SensorDetailActivity mit dem spezifischen Sensor-Typ.
     */
    private void setupSensorClickHandlers() {
        Log.d(TAG, "Konfiguriere Sensor Click-Handler...");

        // Temperatur-Bereich Click-Handler
        LinearLayout temperatureLayout = findViewById(R.id.layout_temperature_sensor);
        if (temperatureLayout != null) {
            temperatureLayout.setOnClickListener(v -> {
                Log.d(TAG, "Temperatur-Bereich geklickt");
                openSensorDetailDialog(SensorDetailActivity.SENSOR_TEMPERATUR);
            });

            // Visuelles Feedback für Klickbarkeit
            temperatureLayout.setBackground(ContextCompat.getDrawable(this,
                    R.drawable.sensor_clickable_background));
            temperatureLayout.setClickable(true);
            temperatureLayout.setFocusable(true);
        }

        // Puls-Bereich Click-Handler
        LinearLayout pulseLayout = findViewById(R.id.layout_pulse_sensor);
        if (pulseLayout != null) {
            pulseLayout.setOnClickListener(v -> {
                Log.d(TAG, "Puls-Bereich geklickt");
                openSensorDetailDialog(SensorDetailActivity.SENSOR_PULS);
            });

            // Visuelles Feedback für Klickbarkeit
            pulseLayout.setBackground(ContextCompat.getDrawable(this,
                    R.drawable.sensor_clickable_background));
            pulseLayout.setClickable(true);
            pulseLayout.setFocusable(true);
        }

        // SpO₂-Bereich Click-Handler
        LinearLayout spo2Layout = findViewById(R.id.layout_spo2_sensor);
        if (spo2Layout != null) {
            spo2Layout.setOnClickListener(v -> {
                Log.d(TAG, "SpO₂-Bereich geklickt");
                openSensorDetailDialog(SensorDetailActivity.SENSOR_SPO2);
            });

            // Visuelles Feedback für Klickbarkeit
            spo2Layout.setBackground(ContextCompat.getDrawable(this,
                    R.drawable.sensor_clickable_background));
            spo2Layout.setClickable(true);
            spo2Layout.setFocusable(true);
        }

        Log.d(TAG, "Sensor Click-Handler erfolgreich konfiguriert");
    }

    /**
     * Öffnet die SensorDetailActivity für den angegebenen Sensor-Typ
     *
     * @param sensorType Der Typ des Sensors (SENSOR_TEMPERATUR, SENSOR_PULS, SENSOR_SPO2)
     */
    private void openSensorDetailDialog(String sensorType) {
        try {
            Log.d(TAG, "Öffne Sensor-Detail-Dialog für: " + sensorType);

            Intent intent = new Intent(this, SensorDetailActivity.class);
            intent.putExtra(SensorDetailActivity.EXTRA_SENSOR_TYP, sensorType);

            // Mit Animation starten für bessere UX
            startActivity(intent);

            // Schöne Übergangsanimation
            overridePendingTransition(R.anim.slide_in_up, R.anim.fade_out);

            Log.d(TAG, "Sensor-Detail-Dialog erfolgreich gestartet");

        } catch (Exception e) {
            Log.e(TAG, "Fehler beim Öffnen des Sensor-Detail-Dialogs: " + e.getMessage(), e);
            Toast.makeText(this, "Fehler beim Öffnen der Sensor-Statistiken",
                    Toast.LENGTH_SHORT).show();
        }
    }



    /**
     * Konfiguriert den Floating Action Button für die Periodeneingabe.
     * Öffnet einen Dialog mit Material DatePicker und Verwaltungsoptionen.
     */
    private void setupPeriodInputButton() {
        ExtendedFloatingActionButton addButton = findViewById(R.id.btn_period);

        addButton.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_periode, null);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            Button selectNewDatesButton = dialogView.findViewById(R.id.btn_select_new_dates);
            LinearLayout oldDatesLayout = dialogView.findViewById(R.id.layout_old_dates);
            Button finishButton = dialogView.findViewById(R.id.btn_dialog_finish);

            // Bestehende Datumsangaben laden
            refreshOldDates(oldDatesLayout, dialog);

            // Neue Periode hinzufügen
            selectNewDatesButton.setOnClickListener(v2 -> {
                MaterialDatePicker<Pair<Long, Long>> picker =
                        MaterialDatePicker.Builder.dateRangePicker()
                                .setTitleText("Periode auswählen")
                                .build();
                picker.show(getSupportFragmentManager(), "PeriodePicker");

                picker.addOnPositiveButtonClickListener(selection -> {
                    if (selection != null) {
                        handleDateRangeSelection(selection, oldDatesLayout, dialog);
                    }
                });
            });

            finishButton.setOnClickListener(v4 -> dialog.dismiss());
            dialog.show();
        });
    }

    /**
     * Verarbeitet die Auswahl eines Datumsbereichs aus dem Material DatePicker.
     *
     * @param selection Das ausgewählte Datumspaar (Start, Ende)
     * @param oldDatesLayout Layout für die Aktualisierung der Anzeige
     * @param dialog Der Dialog für die Aktualisierung
     */
    private void handleDateRangeSelection(Pair<Long, Long> selection, LinearLayout oldDatesLayout, AlertDialog dialog) {
        Long startMillis = selection.first;
        Long endMillis = selection.second;

        if (startMillis != null && endMillis != null) {
            LocalDate startDate = Instant.ofEpochMilli(startMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            LocalDate endDate = Instant.ofEpochMilli(endMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();

            // Neue Tage sammeln (nur noch nicht vorhandene)
            List<LocalDate> newDays = new ArrayList<>();
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                if (!menstruationDays.contains(current)) {
                    newDays.add(current);
                }
                current = current.plusDays(1);
            }

            // Zur lokalen Liste hinzufügen
            menstruationDays.addAll(newDays);

            // In Datenbank speichern
            saveMenstruationDaysToDatabase(newDays);

            // Prognosen neu berechnen
            clearPredictionData();
            calculateFertilityData();

            // Kalender aktualisieren
            calendarView.notifyCalendarChanged();

            // Dialog-Liste aktualisieren
            refreshOldDates(oldDatesLayout, dialog);
        }
    }

    /**
     * Speichert neue Menstruationstage in der Room-Datenbank.
     *
     * @param newDays Liste der neuen Menstruationstage
     */
    /**
     * Speichert neue Menstruationstage in der Room-Datenbank.
     * VERBESSERT: Läuft jetzt im Background Thread
     *
     * @param newDays Liste der neuen Menstruationstage
     */
    private void saveMenstruationDaysToDatabase(List<LocalDate> newDays) {
        if (newDays.isEmpty()) {
            Log.d("ZyklusActivity", "Keine neuen Menstruationstage zum Speichern");
            return;
        }

        Log.d("ZyklusActivity", "Speichere " + newDays.size() + " neue Menstruationstage...");

        // Background-Thread für Datenbankoperationen
        new Thread(() -> {
            try {
                // PeriodeEintrag-Objekte für neue Daten erstellen
                List<PeriodeEintrag> newEntries = new ArrayList<>();
                for (LocalDate date : newDays) {
                    PeriodeEintrag entry = new PeriodeEintrag(date, false); // false = echte Menstruation
                    newEntries.add(entry);
                }

                // In der Datenbank speichern
                periodDao.einfuegenMehrerePerioden(newEntries);

                Log.i("ZyklusActivity", "Menstruationstage erfolgreich gespeichert");

                // UI über Erfolg benachrichtigen
                runOnUiThread(() -> {
                    Toast.makeText(ZyklusActivity.this,
                            newDays.size() + " Periodentage hinzugefügt",
                            Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e("ZyklusActivity", "Fehler beim Speichern der Menstruationstage", e);
                runOnUiThread(() -> {
                    Toast.makeText(ZyklusActivity.this,
                            "Fehler beim Speichern: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Aktualisiert die Anzeige der bestehenden Menstruationstage im Dialog.
     * Erstellt für jeden Tag eine Zeile mit Löschbutton.
     *
     * @param layout Das Layout-Container für die Datumsanzeige
     * @param dialog Der Dialog für Kalender-Updates
     */
    private void refreshOldDates(LinearLayout layout, AlertDialog dialog) {
        layout.removeAllViews();

        // Liste kopieren und sortieren (neueste zuerst)
        List<LocalDate> sortedDates = new ArrayList<>(menstruationDays);
        sortedDates.sort((a, b) -> b.compareTo(a));

        for (LocalDate date : sortedDates) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            // Datum anzeigen
            TextView dateText = new TextView(this);
            dateText.setText(date.toString());
            row.addView(dateText);

            // Lösch-Icon
            ImageView deleteIcon = new ImageView(this);
            deleteIcon.setImageResource(android.R.drawable.ic_menu_delete);
            deleteIcon.setPadding(16, 0, 0, 0);
            deleteIcon.setOnClickListener(v -> {
                Log.d("ZyklusActivity", "Lösche Menstruationsdatum: " + date);

                // Sofort aus lokaler Liste entfernen (UI-responsiv)
                menstruationDays.remove(date);

                // Background Thread für Datenbankoperation
                new Thread(() -> {
                    try {
                        // Aus Datenbank löschen (Background Thread!)
                        periodDao.loeschenPeriodeNachDatum(date);

                        Log.d("ZyklusActivity", "Datum erfolgreich aus DB gelöscht: " + date);

                        // UI-Updates auf Main Thread
                        runOnUiThread(() -> {
                            // Kalender aktualisieren
                            calendarView.notifyCalendarChanged();

                            // Dialog-Liste aktualisieren
                            refreshOldDates(layout, dialog);

                            // Benutzer-Feedback
                            Toast.makeText(ZyklusActivity.this,
                                    "Datum gelöscht: " + date,
                                    Toast.LENGTH_SHORT).show();
                        });

                    } catch (Exception e) {
                        Log.e("ZyklusActivity", "Fehler beim Löschen des Datums: " + e.getMessage(), e);

                        // Fehler-Behandlung
                        runOnUiThread(() -> {
                            // Datum wieder zur lokalen Liste hinzufügen bei Fehler
                            menstruationDays.add(date);

                            // Benutzer über Fehler informieren
                            Toast.makeText(ZyklusActivity.this,
                                    "Fehler beim Löschen: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show();

                            // Dialog aktualisieren
                            refreshOldDates(layout, dialog);
                        });
                    }
                }).start();
            });
            row.addView(deleteIcon);

            layout.addView(row);
        }
    }

    /**
     * Aktualisiert den Monatstitel in der Kopfzeile.
     *
     * @param month Der anzuzeigende Monat
     */
    private void updateMonthTitle(YearMonth month) {
        String monthName = month.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL,
                java.util.Locale.GERMAN
        );
        // Ersten Buchstaben großschreiben
        monthName = monthName.substring(0,1).toUpperCase() + monthName.substring(1);
        String title = monthName + " " + month.getYear();
        monthTitleText.setText(title);
    }
    /**
     * Wird aufgerufen wenn die Activity wieder sichtbar wird
     * Reaktiviert die Sensoren falls Berechtigungen vorhanden
     */
    @Override
    protected void onResume() {
        super.onResume();

        // Sensoren wieder aktivieren wenn Berechtigungen vorhanden
        if (sensorManager != null) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.BODY_SENSORS)
                    == PackageManager.PERMISSION_GRANTED) {
                sensorManager.starteMessung();
            }
        }
        ladeSensordatenFuerAnzeige();
    }

    /**
     * Wird aufgerufen wenn die Activity in den Hintergrund geht
     * Stoppt die Sensoren um Akku zu schonen
     */
    @Override
    protected void onPause() {
        super.onPause();

        // Sensoren pausieren um Akku zu schonen
        // if (sensorManager != null) {
        //    sensorManager.stoppeMessung();
        // }
    }

    /**
     * Wird aufgerufen wenn die Activity zerstört wird
     * Gibt alle Sensor-Ressourcen frei
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Sensor-Ressourcen freigeben
        if (sensorManager != null) {
            sensorManager.stoppeMessung();
        }
    }

    /**
     * Implementierung des HealthConnectPermissionRequester-Interfaces
     * Wird vom ZyklusSensorManager aufgerufen um Health Connect Berechtigungen anzufordern
     */
    // ===== IMPLEMENTIERUNG DES HEALTHCONNECTPERMISSIONREQUESTER INTERFACES =====


    @Override
    public void requestHealthConnectPermissions(Set<String> permissions) {
        try {
            Intent intent = new Intent("androidx.health.ACTION_REQUEST_PERMISSIONS");
            startActivity(intent);
            Toast.makeText(this, "Öffnen Sie Health Connect für die Berechtigungen\"",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Health Connect nicht gefunden. Bitte installieren Sie Health Connect",
                    Toast.LENGTH_LONG).show();
        }
    }


// ===== IMPLEMENTIERUNG DES SENSORCALLBACK INTERFACES =====

    /**
     * Implementierung der SensorCallback-Methoden für Health Connect Integration
     */
    @Override
    public void datenVerfuegbar(SensorData daten) {
        runOnUiThread(() -> {
            if (daten != null) {
                Log.i("ZyklusActivity", "Neue Sensordaten empfangen: " + daten.toString());

                // Sofortige Anzeige der neuen Daten
                updateSensorUI(daten);

                // Nach kurzer Verzögerung auch gespeicherte Daten neu laden
                // (um sicherzustellen, dass Anzeige mit DB synchron ist)
                new android.os.Handler().postDelayed(() -> {
                    ladeSensordatenFuerAnzeige();
                }, 2000); // 2 Sekunden Verzögerung

                Toast.makeText(this, "Sensordaten aktualisiert und gespeichert!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void keineDatenVerfuegbar(String grund) {
        runOnUiThread(() -> {
            Log.w("ZyklusActivity", "Keine Daten verfügbar: " + grund);
            temperatureValueText.setText("Temperatur: N/A");
            pulseValueText.setText("Puls: N/A");
            spo2ValueText.setText("SpO₂: N/A");
            Toast.makeText(ZyklusActivity.this, "Keine Sensordaten: " + grund, Toast.LENGTH_LONG).show();
        });
    }


    @Override
    public void sensorFehler(String fehlermeldung) {
        runOnUiThread(() -> {
            Log.e("ZyklusActivity", "Sensor Fehler: " + fehlermeldung);
            Toast.makeText(ZyklusActivity.this, "Sensorfehler: " + fehlermeldung, Toast.LENGTH_LONG).show();
        });
    }

    /**
     * Aktualisiert die Sensor-UI mit aktuellen Live-Daten (ohne Datenbankzugriff)
     * Wird aufgerufen wenn neue Daten direkt von Health Connect kommen
     */
    private void updateSensorUI(SensorData daten) {
        Log.d("ZyklusActivity", "Aktualisiere UI mit Live-Sensordaten: " + daten.toString());

        // Temperatur anzeigen
        if (daten.getBodyTemperature() > 0) {
            String tempText = String.format("Temperatur: %.1f°C", daten.getBodyTemperature());
            temperatureValueText.setText(tempText);
            temperatureValueText.setTextColor(getColor(R.color.text_primary));
            Log.d("ZyklusActivity", "Live-Temperatur angezeigt: " + tempText);
        } else {
            temperatureValueText.setText("Temperatur: N/A");
            temperatureValueText.setTextColor(getColor(R.color.text_disabled));
        }

        // Pulsfrequenz anzeigen
        if (daten.getHeartRate() > 0) {
            String pulsText = String.format("Puls: %.0f bpm", daten.getHeartRate());
            pulseValueText.setText(pulsText);
            pulseValueText.setTextColor(getColor(R.color.text_primary));
            Log.d("ZyklusActivity", "Live-Puls angezeigt: " + pulsText);
        } else {
            pulseValueText.setText("Puls: N/A");
            pulseValueText.setTextColor(getColor(R.color.text_disabled));
        }

        // Sauerstoffsättigung anzeigen
        if (daten.getOxygenSaturation() > 0) {
            String spo2Text = String.format("SpO₂: %.0f%%", daten.getOxygenSaturation());
            spo2ValueText.setText(spo2Text);
            spo2ValueText.setTextColor(getColor(R.color.text_primary));
            Log.d("ZyklusActivity", "Live-SpO₂ angezeigt: " + spo2Text);
        } else {
            spo2ValueText.setText("SpO₂: N/A");
            spo2ValueText.setTextColor(getColor(R.color.text_disabled));
        }
    }

    /**
     * ViewContainer-Klasse für die Darstellung einzelner Kalendertage.
     * Enthält Referenzen auf TextView (Tagesnummer) und View (Markierungspunkt).
     */
    public static class DayViewContainer extends ViewContainer {
        public final TextView textView;
        public final View dotView;
        public DayViewContainer(View view) {
            super(view);
            textView = view.findViewById(R.id.text_calendar_day);
            dotView = view.findViewById(R.id.view_day_marker);
        }
    }

    /**
     * Lädt und zeigt die aktuellsten Sensordaten an
     */
    private void ladeSensordatenFuerAnzeige() {
        // Background-Thread für Datenbankzugriff
        new Thread(() -> {
            try {
                WohlbefindenDao dao = database.wohlbefindenDao();
                WohlbefindenEintrag letzteWerte = dao.getLetztenSensordaten();

                // Zurück zum UI-Thread für Anzeige-Updates
                runOnUiThread(() -> {
                    if (letzteWerte != null) {
                        aktualisiereSensordatenAnzeige(letzteWerte);
                    } else {
                        zeigePlatzhalterFuerSensordaten();
                    }
                });

            } catch (Exception e) {
                Log.e("ZyklusActivity", "Fehler beim Laden der Sensordaten", e);
                runOnUiThread(() -> zeigeSensordatenFehler());
            }
        }).start();
    }

    /**
     * Aktualisiert die UI mit den geladenen Sensordaten
     */
    private void aktualisiereSensordatenAnzeige(WohlbefindenEintrag eintrag) {
        Log.d("ZyklusActivity", "Aktualisiere Sensordaten-Anzeige für: " + eintrag.getDatum());

        // Temperatur anzeigen
        if (eintrag.getTemperatur() != null) {
            String tempText = String.format("Temperatur: %.1f°C", eintrag.getTemperatur());
            temperatureValueText.setText(tempText);
            temperatureValueText.setTextColor(getColor(R.color.text_primary));
            Log.d("ZyklusActivity", "Temperatur angezeigt: " + tempText);
        } else {
            temperatureValueText.setText("Temperatur: Keine Daten");
            temperatureValueText.setTextColor(getColor(R.color.text_disabled));
        }

        // Pulsfrequenz anzeigen
        if (eintrag.getPuls() != null) {
            String pulsText = String.format("Puls: %d bpm", eintrag.getPuls());
            pulseValueText.setText(pulsText);
            pulseValueText.setTextColor(getColor(R.color.text_primary));
            Log.d("ZyklusActivity", "Puls angezeigt: " + pulsText);
        } else {
            pulseValueText.setText("Puls: Keine Daten");
            pulseValueText.setTextColor(getColor(R.color.text_disabled));
        }

        // Sauerstoffsättigung anzeigen
        if (eintrag.getSpo2() != null) {
            String spo2Text = String.format("SpO₂: %d%%", eintrag.getSpo2());
            spo2ValueText.setText(spo2Text);
            spo2ValueText.setTextColor(getColor(R.color.text_primary));
            Log.d("ZyklusActivity", "SpO₂ angezeigt: " + spo2Text);
        } else {
            spo2ValueText.setText("SpO₂: Keine Daten");
            spo2ValueText.setTextColor(getColor(R.color.text_disabled));
        }

        // Zeitstempel der letzten Messung anzeigen
        String zeitstempel = "Letzte Messung: " + eintrag.getDatum().toString();
        // Sie können ein zusätzliches TextView für den Zeitstempel hinzufügen
        Log.i("ZyklusActivity", zeitstempel);
    }

    /**
     * Zeigt Platzhalter wenn keine Sensordaten vorhanden
     */
    private void zeigePlatzhalterFuerSensordaten() {
        temperatureValueText.setText("Temperatur: Noch keine Messung");
        temperatureValueText.setTextColor(getColor(R.color.text_disabled));

        pulseValueText.setText("Puls: Noch keine Messung");
        pulseValueText.setTextColor(getColor(R.color.text_disabled));

        spo2ValueText.setText("SpO₂: Noch keine Messung");
        spo2ValueText.setTextColor(getColor(R.color.text_disabled));

        Log.i("ZyklusActivity", "Keine Sensordaten in der Datenbank gefunden");
    }

    /**
     * Zeigt Fehlermeldung bei Problemen mit Sensordaten
     */
    private void zeigeSensordatenFehler() {
        temperatureValueText.setText("Temperatur: Fehler beim Laden");
        temperatureValueText.setTextColor(getColor(R.color.error_red));

        pulseValueText.setText("Puls: Fehler beim Laden");
        pulseValueText.setTextColor(getColor(R.color.error_red));

        spo2ValueText.setText("SpO₂: Fehler beim Laden");
        spo2ValueText.setTextColor(getColor(R.color.error_red));

        Log.e("ZyklusActivity", "Fehler beim Anzeigen der Sensordaten");
    }
    /**
     * Lädt und zeigt die aktuelle Zyklusphase an
     */
    private void ladeAktuelleZyklusphase() {
        Log.d(TAG, "Lade aktuelle Zyklusphase...");

        // Prüfe ob UI-Komponenten verfügbar sind
        if (aktuellePhaseText == null || zyklusTagText == null) {
            Log.w(TAG, "UI-Komponenten für Zyklusphase nicht verfügbar");
            return;
        }

        // Wenn ein Tag ausgewählt ist → zeige den, sonst zeige heute
        LocalDate zielDatum = (selectedDate != null) ? selectedDate : LocalDate.now();

        statistikManager.analysiereZyklusphaseFürDatum(zielDatum,
                new StatistikManager.ZyklusPhasenCallback() {
                    @Override
                    public void onZyklusPhasenAnalyseBerechnet(AnalyseErgebnis analyseErgebnis) {
                        runOnUiThread(() -> {
                            aktualisiereZyklusphaseUI(analyseErgebnis);
                        });
                    }

                    @Override
                    public void onZyklusFehler(String fehlermeldung) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Zyklusphasen-Fehler: " + fehlermeldung);
                            zeigeZyklusphasenFehler(fehlermeldung);
                        });
                    }
                });
    }

    /**
     * Aktualisiert die UI mit Zyklusphasen-Informationen
     */
    private void aktualisiereZyklusphaseUI(AnalyseErgebnis ergebnis) {
        Log.d(TAG, "Aktualisiere Zyklusphase UI: " + ergebnis.getAktuellePhase());

        // Phase und Tag anzeigen
        String phaseText = ergebnis.getPhasenIcon() + " " + ergebnis.getAktuellePhase().getDisplayName();
        aktuellePhaseText.setText(phaseText);

        String tagText = "Tag " + ergebnis.getZyklusTag();
        zyklusTagText.setText(tagText);

        // Farbe basierend auf Phase setzen
        int phasenFarbe = getPhasenFarbe(ergebnis.getAktuellePhase());
        aktuellePhaseText.setTextColor(phasenFarbe);
    }

    /**
     * Gibt Farbe für Zyklusphase zurück
     */
    private int getPhasenFarbe(ZyklusPhaseBerechnung.ZyklusPhase phase) {
        switch (phase) {
            case MENSTRUATION:
                return getColor(R.color.fuchsia);          // Rosa für Menstruation
            case FOLLIKELPHASE:
                return getColor(android.R.color.holo_green_dark);   // Grün für Follikelphase
            case OVULATION:
                return getColor(android.R.color.holo_purple);       // Lila für Eisprung
            case LUTEALPHASE:
                return getColor(android.R.color.holo_orange_dark);  // Orange für Lutealphase
            default:
                return getColor(android.R.color.darker_gray);       // Grau für unbekannt
        }
    }

    /**
     * Zeigt Fehler bei Zyklusphasen-Analyse
     */
    private void zeigeZyklusphasenFehler(String fehlermeldung) {
        if (aktuellePhaseText != null) {
            aktuellePhaseText.setText("❓ Phase unbekannt");
            aktuellePhaseText.setTextColor(getColor(android.R.color.darker_gray));
        }
        if (zyklusTagText != null) {
            zyklusTagText.setText("Mehr Daten sammeln");
        }

        Toast.makeText(this, "Zyklusphase: " + fehlermeldung, Toast.LENGTH_SHORT).show();
    }

    /**
     * Analysiert Sensor-Daten im Zykluskontext (wird aufgerufen wenn neue Sensor-Daten verfügbar)
     */
    private void analysiereSensorDatenMitZyklus(float temperatur, int puls, int spo2) {
        Log.d(TAG, "Analysiere Sensor-Daten mit Zyklusphase...");

        statistikManager.analysiereAktuelleZyklusphaseUndSensoren(temperatur, puls, spo2,
                new StatistikManager.ZyklusPhasenCallback() {
                    @Override
                    public void onZyklusPhasenAnalyseBerechnet(AnalyseErgebnis analyseErgebnis) {
                        runOnUiThread(() -> {
                            // UI mit detaillierter Analyse aktualisieren
                            aktualisiereZyklusphaseUI(analyseErgebnis);
                            zeigeSensorBewertung(analyseErgebnis);
                        });
                    }

                    @Override
                    public void onZyklusFehler(String fehlermeldung) {
                        runOnUiThread(() -> {
                            Log.w(TAG, "Sensor-Zyklus-Analyse Fehler: " + fehlermeldung);
                        });
                    }
                });
    }

    /**
     * Zeigt Bewertung der Sensor-Werte im Zykluskontext
     */
    private void zeigeSensorBewertung(AnalyseErgebnis ergebnis) {
        // Wenn Empfehlungen vorhanden, als Toast anzeigen
        if (!ergebnis.getEmpfehlung().isEmpty()) {
            String bewertung = ergebnis.getBewertungsfarbe().equals("#4CAF50")
                    ? "✅ Werte normal für " + ergebnis.getAktuellePhase().getDisplayName()
                    : "⚠️ " + ergebnis.getEmpfehlung();

            Toast.makeText(this, bewertung, Toast.LENGTH_LONG).show();
        }
    }
    /**
     * Berechnet die durchschnittliche Periodenlänge aus echten Daten
     */
    private int berechneDurchschnittlichePeriodenlänge(List<LocalDate> sortedDays) {
        if (sortedDays.size() < 4) return 4; // Mindestens 4 Tage als Fallback

        // Finde zusammenhängende Periodengruppen
        List<Integer> periodenlängen = new ArrayList<>();
        int aktuelleLength = 1;

        for (int i = 1; i < sortedDays.size(); i++) {
            long tageZwischen = ChronoUnit.DAYS.between(sortedDays.get(i-1), sortedDays.get(i));

            if (tageZwischen == 1) {
                // Aufeinanderfolgender Tag
                aktuelleLength++;
            } else {
                // Lücke gefunden - Periode beendet
                periodenlängen.add(aktuelleLength);
                aktuelleLength = 1;
            }
        }
        // Letzte Periode hinzufügen
        periodenlängen.add(aktuelleLength);

        // Durchschnitt berechnen
        if (periodenlängen.isEmpty()) return 4;

        double durchschnitt = periodenlängen.stream().mapToInt(Integer::intValue).average().orElse(4.0);
        return Math.max(3, Math.min(7, (int) Math.round(durchschnitt))); // Zwischen 3-7 Tagen
    }
}