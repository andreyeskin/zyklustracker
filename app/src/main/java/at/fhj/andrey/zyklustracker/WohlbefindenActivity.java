package at.fhj.andrey.zyklustracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class WohlbefindenActivity extends AppCompatActivity {

    private MaterialButton selectedSchmerz = null;
    private MaterialButton selectedStimmung = null;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_wohlbefinden);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.scrollView), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        RadioButton[] radioButtons = new RadioButton[] {
                findViewById(R.id.rbSehrLeicht),
                findViewById(R.id.rbLeicht),
                findViewById(R.id.rbMittel),
                findViewById(R.id.rbStark)
        };

        final RadioButton[] selected = {null}; // активная кнопка

        for (RadioButton b : radioButtons) {
            b.setOnClickListener(v -> {
                if (b.equals(selected[0])) {
                    b.setChecked(false);       // снять выбор
                    selected[0] = null;
                } else {
                    for (RadioButton other : radioButtons) {
                        other.setChecked(false);
                    }
                    b.setChecked(true);        // установить выбор
                    selected[0] = b;
                }
            });
        }
        setupSchmerzenButtons();
        setupStimmungButtons();
        TextView btnSpeichern = findViewById(R.id.btnSpeichern);
        TextView textDummy = findViewById(R.id.textDummyEintrag);
        btnSpeichern.setOnClickListener(v -> {
            String datum = "28.05.2025"; // пока фиксированная дата

            // Характер крови
            String blutung = "";
            if (((RadioButton) findViewById(R.id.rbSehrLeicht)).isChecked()) blutung = "Sehr leicht";
            else if (((RadioButton) findViewById(R.id.rbLeicht)).isChecked()) blutung = "Leicht";
            else if (((RadioButton) findViewById(R.id.rbMittel)).isChecked()) blutung = "Mittel";
            else if (((RadioButton) findViewById(R.id.rbStark)).isChecked()) blutung = "Stark";

            // Боль и настроение
            String schmerz = selectedSchmerz != null ? selectedSchmerz.getText().toString() : "–";
            String stimmung = selectedStimmung != null ? selectedStimmung.getText().toString() : "–";

            // Чипы симптомов
            Chip chipKopf = findViewById(R.id.chipKopf);
            Chip chipÜbelkeit = findViewById(R.id.chipÜbelkeit);
            Chip chipMüdigkeit = findViewById(R.id.chipMüdigkeit);
            Chip chipRücken = findViewById(R.id.chipRücken);
            Chip chipBrüste = findViewById(R.id.chipBrüste);
            Chip[] chips = new Chip[] { chipKopf, chipÜbelkeit, chipMüdigkeit, chipRücken, chipBrüste };

            List<String> symptome = new ArrayList<>();
            for (Chip chip : chips) {
                if (chip.isChecked()) symptome.add(chip.getText().toString());
            }

            // Вывод текста
            String zusammenfassung = datum + "\n" +
                    "Blutung: " + blutung + "\n" +
                    "Schmerzen: " + schmerz + "\n" +
                    "Stimmung: " + stimmung + "\n" +
                    "Begleitsymptome: " + String.join(", ", symptome);

            textDummy.setText(zusammenfassung);
        });

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_wohlbefinden); // активный пункт

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_zyklus) {
                startActivity(new Intent(this, ZyklusActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (id == R.id.nav_statistik) {
                startActivity(new Intent(this, StatistikActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return true;
        });

    }
    private void setupSchmerzenButtons() {

        MaterialButton[] buttons = new MaterialButton[] {
                findViewById(R.id.btnKeine),
                findViewById(R.id.btnLeicht),
                findViewById(R.id.btnMittel),
                findViewById(R.id.btnStark),
                findViewById(R.id.btnKrampfartig)
        };

        final MaterialButton[] selected = {null}; // хранит текущую активную кнопку

        for (MaterialButton b : buttons) {
            b.setOnClickListener(v -> {
                if (b.equals(selected[0])) {
                    // повторный клик → снять выделение
                    b.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                    b.setTextColor(Color.BLACK);
                    selected[0] = null;
                } else {
                    // снять выбор со всех
                    for (MaterialButton other : buttons) {
                        other.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                        other.setTextColor(Color.BLACK);
                    }
                    // установить новую активную
                    b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D81B60"))); // pink
                    b.setTextColor(Color.WHITE);
                    selected[0] = b;
                    selectedSchmerz = b; // ✅ сохранить выбранную кнопку
                }
            });
        }

    }
    private void setupStimmungButtons() {

        MaterialButton[] buttons = new MaterialButton[] {
                findViewById(R.id.btnSehrGut),
                findViewById(R.id.btnGut),
                findViewById(R.id.btnMittelStimmung),
                findViewById(R.id.btnSchlecht)
        };

        final MaterialButton[] selected = {null};

        for (MaterialButton b : buttons) {
            b.setOnClickListener(v -> {
                if (b.equals(selected[0])) {
                    b.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                    b.setTextColor(Color.BLACK);
                    selected[0] = null;
                } else {
                    for (MaterialButton other : buttons) {
                        other.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
                        other.setTextColor(Color.BLACK);
                    }
                    b.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D81B60")));
                    b.setTextColor(Color.WHITE);
                    selected[0] = b;
                    selectedStimmung = b; // ✅ сохранить выбранную кнопку
                }
            });
        }
    }


}