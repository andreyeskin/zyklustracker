package at.fhj.andrey.zyklustracker;

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
import com.google.android.material.button.MaterialButton;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

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
 * - Rosa: Prognostizierte Menstruation
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
public class ZyklusActivity extends AppCompatActivity {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zyklus);

        // UI-Komponenten initialisieren
        initializeUIComponents();

        // Datenbank initialisieren
        initializeDatabase();

        // Menstruationsdaten aus der Datenbank laden
        loadMenstruationDataFromDatabase();

        // Fruchtbarkeitsberechnungen durchführen
        calculateFertilityData();

        // Bottom Navigation konfigurieren
        setupBottomNavigation();

        // Kalender konfigurieren und einrichten
        setupCalendar();

        // Floating Action Button für Periodeneingabe konfigurieren
        setupPeriodInputButton();
    }

    /**
     * Initialisiert alle UI-Komponenten und weist sie den entsprechenden Variablen zu.
     */
    private void initializeUIComponents() {
        monthTitleText = findViewById(R.id.text_month_title);
        previousMonthButton = findViewById(R.id.btn_month_previous);
        nextMonthButton = findViewById(R.id.btn_month_next);
        calendarView = findViewById(R.id.calendar_view);
    }

    /**
     * Initialisiert die Room-Datenbank und die entsprechenden DAOs.
     */
    private void initializeDatabase() {
        database = ZyklusDatenbank.getInstanz(this);
        periodDao = database.periodeDao();
    }

    /**
     * Lädt alle Menstruationsdaten aus der Room-Datenbank.
     * Filtert nur echte Periodendaten (keine Prognosen).
     */
    private void loadMenstruationDataFromDatabase() {
        menstruationDays.clear(); // Bestehende Liste leeren

        // Alle echten Periodeneinträge aus der Datenbank abrufen
        List<PeriodeEintrag> entries = periodDao.getAlleEchtenPerioden();

        // PeriodeEintrag-Objekte in LocalDate umwandeln und zur Liste hinzufügen
        for (PeriodeEintrag entry : entries) {
            menstruationDays.add(entry.getDatum());
        }
    }

    /**
     * Berechnet Eisprung, fruchtbare Tage und zukünftige Perioden basierend auf
     * den vorhandenen Menstruationsdaten.
     *
     * Algorithmus:
     * 1. Bestehende Prognosedaten löschen
     * 2. Zykluslänge aus letzten beiden Perioden berechnen
     * 3. Standardwert 28 Tage verwenden wenn unrealistisch
     * 4. Eisprung = 14 Tage vor nächster Periode
     * 5. Fruchtbare Phase = 5 Tage vor Eisprung
     * 6. Prognose für 3 zukünftige Zyklen
     */
    private void calculateFertilityData() {
        clearPredictionData();

        // Alte Prognosen aus der Datenbank löschen
        periodDao.loeschenAllePrognosen();

        List<LocalDate> sortedDays = new ArrayList<>(menstruationDays);
        Collections.sort(sortedDays);

        if (sortedDays.size() < 2) return; // Nicht genug Daten für Berechnung

        // Länge des letzten Zyklus ermitteln
        LocalDate lastPeriod = sortedDays.get(sortedDays.size() - 1);
        LocalDate previousPeriod = null;

        // Suche nach dem vorletzten Periodenstart
        for (int i = sortedDays.size() - 2; i >= 0; i--) {
            if (!sortedDays.get(i).isAfter(lastPeriod.minusDays(1))) {
                previousPeriod = sortedDays.get(i);
                break;
            }
        }

        if (previousPeriod == null) return;

        // Zykluslänge berechnen
        long cycleLength = ChronoUnit.DAYS.between(previousPeriod, lastPeriod);
        if (cycleLength < 20 || cycleLength > 40) {
            cycleLength = 28; // Standardwert für unrealistische Zykluslängen
        }

        // Liste für Prognose-Einträge in der Datenbank
        List<PeriodeEintrag> predictionEntries = new ArrayList<>();

        // Prognose für die nächsten 3 Zyklen erstellen
        for (int i = 1; i <= 3; i++) {
            LocalDate predictedStart = lastPeriod.plusDays(i * cycleLength);
            LocalDate ovulation = predictedStart.minusDays(14);

            // Eisprung hinzufügen
            ovulationDays.add(ovulation);

            // Fruchtbare Phase (5 Tage vor Eisprung)
            for (int j = -5; j <= 0; j++) {
                fertileDays.add(ovulation.plusDays(j));
            }

            // Prognostizierte Menstruation (5 Tage)
            for (int d = 0; d < 5; d++) {
                LocalDate predictionDay = predictedStart.plusDays(d);
                predictedMenstruation.add(predictionDay);

                // Zur Datenbank-Speicherung hinzufügen
                predictionEntries.add(new PeriodeEintrag(predictionDay, true));
            }
        }

        // Prognosen in der Datenbank speichern
        if (!predictionEntries.isEmpty()) {
            periodDao.einfuegenMehrerePerioden(predictionEntries);
        }
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
                if (date.equals(selectedDate)) {
                    container.textView.setBackgroundResource(R.drawable.selected_day_background);
                    container.textView.setTextColor(Color.WHITE);
                } else {
                    container.textView.setBackground(null);
                }

                // Click-Handler für Tagesauswahl
                container.textView.setOnClickListener(v -> {
                    selectedDate = date;
                    calendarView.notifyCalendarChanged();
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
    private void saveMenstruationDaysToDatabase(List<LocalDate> newDays) {
        // PeriodeEintrag-Objekte für neue Daten erstellen
        List<PeriodeEintrag> newEntries = new ArrayList<>();
        for (LocalDate date : newDays) {
            PeriodeEintrag entry = new PeriodeEintrag(date, false); // false = echte Menstruation
            newEntries.add(entry);
        }

        // In der Datenbank speichern
        periodDao.einfuegenMehrerePerioden(newEntries);
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
                // Aus lokaler Liste entfernen
                menstruationDays.remove(date);

                // Aus Datenbank löschen
                periodDao.loeschenPeriodeNachDatum(date);

                // Kalender aktualisieren
                calendarView.notifyCalendarChanged();

                // Dialog-Liste aktualisieren
                refreshOldDates(layout, dialog);
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
}