package at.fhj.andrey.zyklustracker;

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
import androidx.room.parser.expansion.Position;

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
import java.util.stream.Collectors;



import kotlin.Unit;
import kotlin.jvm.functions.Function1;


public class ZyklusActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private YearMonth currentMonth = YearMonth.now();
    private TextView textMonthTitle;
    private ImageView btnMonthPrev, btnMonthNext;

    private final List<LocalDate> menstruationstage = new ArrayList<>();
    private final List<LocalDate> eisprungstage = new ArrayList<>();
    private final List<LocalDate> fruchtbareTage = new ArrayList<>();
    private final List<LocalDate> prognostizierteMenstruation = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zyklus);
        textMonthTitle = findViewById(R.id.textMonthTitle);
        btnMonthPrev = findViewById(R.id.btnMonthPrev);
        btnMonthNext = findViewById(R.id.btnMonthNext);

        calendarView = findViewById(R.id.calendarView); // ← теперь это не локальная переменная!


        loadMenstruationstage(); // ← добавь сюда!
        berechneFruchtbarkeit();

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_zyklus);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_wohlbefinden) {
                startActivity(new Intent(this, WohlbefindenActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_statistik) {
                startActivity(new Intent(this, StatistikActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return true;
        });


        calendarView.setDayBinder(new DayBinder<DayViewContainer>() {
            @Override
            public DayViewContainer create(View view) {
                return new DayViewContainer(view);
            }


            @Override
            public void bind(DayViewContainer container, CalendarDay day) {
                LocalDate date = day.getDate();
                container.textView.setText(String.valueOf(date.getDayOfMonth()));
                container.dotView.setVisibility(View.GONE); // по умолчанию

                // серый текст для соседних месяцев
                if (day.getOwner() != DayOwner.THIS_MONTH) {
                    container.textView.setTextColor(Color.parseColor("#CCCCCC"));
                } else {
                    container.textView.setTextColor(Color.BLACK);
                }

                Context context = container.textView.getContext();

                // ✅ Порядок важен: сначала реальные, потом прогноз
                if (menstruationstage.contains(date)) {
                    container.dotView.setVisibility(View.VISIBLE);
                    container.dotView.setBackground(ContextCompat.getDrawable(context, R.drawable.dot_menstruation));
                } else if (prognostizierteMenstruation.contains(date)) {
                    container.dotView.setVisibility(View.VISIBLE);
                    container.dotView.setBackground(ContextCompat.getDrawable(context, R.drawable.dot_predicted_period));
                } else if (eisprungstage.contains(date)) {
                    container.dotView.setVisibility(View.VISIBLE);
                    container.dotView.setBackground(ContextCompat.getDrawable(context, R.drawable.triangle_ovulation));
                } else if (fruchtbareTage.contains(date)) {
                    container.dotView.setVisibility(View.VISIBLE);
                    container.dotView.setBackground(ContextCompat.getDrawable(context, R.drawable.dot_fertile));
                } else {
                    container.dotView.setVisibility(View.GONE);
                }

                // выделение выбранного дня
                if (date.equals(selectedDate)) {
                    container.textView.setBackgroundResource(R.drawable.selected_day_background);
                    container.textView.setTextColor(Color.WHITE);
                } else {
                    container.textView.setBackground(null);
                }

                container.textView.setOnClickListener(v -> {
                    selectedDate = date;
                    calendarView.notifyCalendarChanged();
                });
            }





        });

        calendarView.setup(
                currentMonth,
                currentMonth,
                DayOfWeek.MONDAY
        );

        calendarView.scrollToMonth(currentMonth);
        updateMonthTitle(currentMonth); // Сразу показываем текущий месяц
// 1. Устанавливаем название месяца при запуске:
        CalendarMonth initialMonth = calendarView.findFirstVisibleMonth();
        if (initialMonth != null) {
            updateMonthTitle(initialMonth.getYearMonth());
        } else {
            updateMonthTitle(currentMonth);
        }

// 2. Подписываемся на смену месяца:
        calendarView.setMonthScrollListener(calendarMonth -> {
            currentMonth = calendarMonth.getYearMonth();
            updateMonthTitle(currentMonth);
            return Unit.INSTANCE; // <-- обязательно!
        });

        btnMonthPrev.setOnClickListener(v -> {
            currentMonth = currentMonth.minusMonths(1);
            calendarView.setup(currentMonth, currentMonth, DayOfWeek.MONDAY);
            updateMonthTitle(currentMonth);
        });

        btnMonthNext.setOnClickListener(v -> {
            currentMonth = currentMonth.plusMonths(1);
            calendarView.setup(currentMonth, currentMonth, DayOfWeek.MONDAY);
            updateMonthTitle(currentMonth);
        });

        ExtendedFloatingActionButton btnAdd = findViewById(R.id.btnPeriode);

        btnAdd.setOnClickListener(v -> {
            View dialogView = getLayoutInflater().inflate(R.layout.dialog_periode, null);
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            Button btnSelectNewDates = dialogView.findViewById(R.id.btnSelectNewDates);
            LinearLayout layoutOldDates = dialogView.findViewById(R.id.layoutOldDates);
            Button btnDialogFertig = dialogView.findViewById(R.id.btnDialogFertig); // ← Вот эта строка
            // Функция обновления списка существующих дней
            refreshOldDates(layoutOldDates, dialog);

            btnSelectNewDates.setOnClickListener(v2 -> {
                MaterialDatePicker<Pair<Long, Long>> picker =
                        MaterialDatePicker.Builder.dateRangePicker()
                                .setTitleText("Periode auswählen")
                                .build();
                picker.show(getSupportFragmentManager(), "PeriodePicker");

                picker.addOnPositiveButtonClickListener(selection -> {
                    if (selection != null) {
                        Long startMillis = selection.first;
                        Long endMillis = selection.second;
                        if (startMillis != null && endMillis != null) {
                            LocalDate startDate = Instant.ofEpochMilli(startMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                            LocalDate endDate = Instant.ofEpochMilli(endMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate();
                            List<LocalDate> neueTage = new ArrayList<>();
                            LocalDate current = startDate;
                            while (!current.isAfter(endDate)) {
                                if (!menstruationstage.contains(current)) {
                                    neueTage.add(current);
                                }
                                current = current.plusDays(1);
                            }
                            menstruationstage.addAll(neueTage);
                            saveMenstruationstage();
                            clearPrognoseDaten();
                            berechneFruchtbarkeit();
                            calendarView.notifyCalendarChanged();

                            refreshOldDates(layoutOldDates, dialog); // обновить список
                        }
                    }
                });
            });
            btnDialogFertig.setOnClickListener(v4 -> dialog.dismiss()); // ← Вот эта строка

            dialog.show();
        });
    }
    private LocalDate selectedDate = null;

    public static class DayViewContainer extends ViewContainer {
        public final TextView textView;
        public final View dotView;

        public DayViewContainer(View view) {
            super(view);
            textView = view.findViewById(R.id.calendarDayText);
            dotView = view.findViewById(R.id.dayDot);
        }
    }

    // Сохраняем даты в SharedPreferences
    private void saveMenstruationstage() {
        List<String> stringDates = menstruationstage.stream()
                .map(LocalDate::toString)
                .collect(Collectors.toList());

        getSharedPreferences("zyklus", MODE_PRIVATE)
                .edit()
                .putStringSet("tage", new HashSet<>(stringDates))
                .apply();
    }

    // Загружаем даты из SharedPreferences
    private void loadMenstruationstage() {
        Set<String> stringSet = getSharedPreferences("zyklus", MODE_PRIVATE)
                .getStringSet("tage", new HashSet<>());

        for (String s : stringSet) {
            menstruationstage.add(LocalDate.parse(s));
        }
    }
    private void clearPrognoseDaten() {
        prognostizierteMenstruation.clear();
        eisprungstage.clear();
        fruchtbareTage.clear();
    }

    private void berechneFruchtbarkeit() {
        clearPrognoseDaten();

        List<LocalDate> sortierteTage = new ArrayList<>(menstruationstage);
        Collections.sort(sortierteTage);

        if (sortierteTage.size() < 2) return;

        // Найдём длину последнего цикла
        LocalDate letzter = sortierteTage.get(sortierteTage.size() - 1);
        LocalDate vorletzter = null;

        for (int i = sortierteTage.size() - 2; i >= 0; i--) {
            if (!sortierteTage.get(i).isAfter(letzter.minusDays(1))) {
                vorletzter = sortierteTage.get(i);
                break;
            }
        }

        if (vorletzter == null) return;

        long zyklusLaenge = ChronoUnit.DAYS.between(vorletzter, letzter);
        if (zyklusLaenge < 20 || zyklusLaenge > 40) zyklusLaenge = 28; // стандартное значение

        // Прогноз на 3 месяца = 3 цикла
        for (int i = 1; i <= 3; i++) {
            LocalDate prognoseStart = letzter.plusDays(i * zyklusLaenge);
            LocalDate eisprung = prognoseStart.minusDays(14);

            eisprungstage.add(eisprung);
            for (int j = -5; j <= 0; j++) {
                fruchtbareTage.add(eisprung.plusDays(j));
            }

            for (int d = 0; d < 5; d++) {
                prognostizierteMenstruation.add(prognoseStart.plusDays(d));
            }
        }
    }



    private void refreshOldDates(LinearLayout layout, AlertDialog dialog) {
        layout.removeAllViews();
        // 1. Копируем и сортируем список
        List<LocalDate> sortedDates = new ArrayList<>(menstruationstage);
        // Новые сверху — убывание
        sortedDates.sort((a, b) -> b.compareTo(a));
        for (LocalDate date : sortedDates) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);

            TextView tv = new TextView(this);
            tv.setText(date.toString());
            row.addView(tv);

            ImageView delIcon = new ImageView(this);
            delIcon.setImageResource(android.R.drawable.ic_menu_delete);
            delIcon.setPadding(16, 0, 0, 0);
            delIcon.setOnClickListener(v -> {
                menstruationstage.remove(date);
                saveMenstruationstage();
                calendarView.notifyCalendarChanged();
                refreshOldDates(layout, dialog);
            });
            row.addView(delIcon);

            layout.addView(row);
        }
    }
    private void updateMonthTitle(YearMonth month) {
        String monthName = month.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.GERMAN);
        monthName = monthName.substring(0,1).toUpperCase() + monthName.substring(1);
        String title = monthName + " " + month.getYear();
        textMonthTitle.setText(title);
    }

    }

