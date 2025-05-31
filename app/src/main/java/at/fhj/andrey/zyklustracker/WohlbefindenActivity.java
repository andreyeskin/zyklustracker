package at.fhj.andrey.zyklustracker;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import at.fhj.andrey.zyklustracker.datenbank.*;
import java.time.LocalDate;
import android.widget.LinearLayout;

/**
 * WohlbefindenActivity - Aktivität für die Eingabe von Wohlbefindensdaten
 *
 * Diese Aktivität ermöglicht es der Nutzerin, täglich folgende Daten zu erfassen:
 * - Charakter der Blutung (sehr leicht bis stark)
 * - Schmerzlevel (keine bis krampfartig)
 * - Stimmung (sehr gut bis schlecht)
 * - Begleitsymptome (Kopfschmerzen, Übelkeit, etc.)
 *
 * Die Daten werden automatisch in der Room-Datenbank gespeichert.
 * Pro Tag kann nur ein Eintrag existieren - bei wiederholter Eingabe wird aktualisiert.
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class WohlbefindenActivity extends AppCompatActivity {

    // UI-Referenzen für die aktuell ausgewählten Buttons
    private MaterialButton selectedPainButton = null;
    private MaterialButton selectedMoodButton = null;

    // Datenbankzugriff
    private ZyklusDatenbank database;
    private WohlbefindenDao wellbeingDao;

    // Aktuelles Datum für den Eintrag
    private LocalDate currentDate = LocalDate.now();

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wohlbefinden);

        // Datenbank initialisieren
        database = ZyklusDatenbank.getInstanz(this);
        wellbeingDao = database.wohlbefindenDao();



        // RadioButton-Gruppe für Blutungsstärke einrichten
        setupBleedingRadioButtons();

        // Schmerzlevel-Buttons einrichten
        setupPainButtons();

        // Stimmungs-Buttons einrichten
        setupMoodButtons();

        // Speichern-Button konfigurieren
        setupSaveButton();

        // Bottom Navigation einrichten
        setupBottomNavigation();

        // Letzte Einträge laden und anzeigen
        loadAndDisplayLastEntries();

        // Heutigen Eintrag laden (falls vorhanden)
        loadTodaysEntry();
    }

    /**
     * Konfiguriert die Tröpfchen-Auswahl für die Blutungsstärke.
     * Ermöglicht das Abwählen durch erneutes Klicken.
     */
    private void setupBleedingRadioButtons() {
        LinearLayout[] dropletButtons = new LinearLayout[] {
                findViewById(R.id.btn_very_light),
                findViewById(R.id.btn_light),
                findViewById(R.id.btn_medium),
                findViewById(R.id.btn_heavy)
        };

        final LinearLayout[] selectedDroplet = {null}; // Aktuell ausgewählter Tropfen

        for (LinearLayout button : dropletButtons) {
            button.setOnClickListener(v -> {
                if (button.equals(selectedDroplet[0])) {
                    // Erneuter Klick auf bereits ausgewählten Button -> Abwählen
                    resetDropletAppearance(button);
                    selectedDroplet[0] = null;
                } else {
                    // Alle anderen zurücksetzen, aktuellen hervorheben
                    for (LinearLayout other : dropletButtons) {
                        resetDropletAppearance(other);
                    }
                    highlightDroplet(button);
                    selectedDroplet[0] = button;
                }
            });
        }
    }

    /**
     * Konfiguriert die MaterialButton-Gruppe für Schmerzlevel.
     * Unterstützt Single-Selection mit visueller Hervorhebung.
     */
    private void setupPainButtons() {
        MaterialButton[] buttons = new MaterialButton[] {
                findViewById(R.id.btn_no_pain),
                findViewById(R.id.btn_light_pain),
                findViewById(R.id.btn_medium_pain),
                findViewById(R.id.btn_heavy_pain),
                findViewById(R.id.btn_cramp_pain)
        };

        final MaterialButton[] selectedButton = {null}; // Aktuell ausgewählter Button

        for (MaterialButton button : buttons) {
            button.setOnClickListener(v -> {
                if (button.equals(selectedButton[0])) {
                    // Erneuter Klick -> Auswahl aufheben
                    resetButtonAppearance(button);
                    selectedButton[0] = null;
                    selectedPainButton = null;
                } else {
                    // Alle Buttons zurücksetzen
                    for (MaterialButton other : buttons) {
                        resetButtonAppearance(other);
                    }
                    // Aktuellen Button hervorheben
                    highlightButton(button);
                    selectedButton[0] = button;
                    selectedPainButton = button;
                }
            });
        }
    }

    /**
     * Konfiguriert die MaterialButton-Gruppe für Stimmung.
     * Funktioniert analog zu den Schmerzlevel-Buttons.
     */
    private void setupMoodButtons() {
        MaterialButton[] buttons = new MaterialButton[] {
                findViewById(R.id.btn_very_good),
                findViewById(R.id.btn_good),
                findViewById(R.id.btn_medium_mood),
                findViewById(R.id.btn_bad)
        };

        final MaterialButton[] selectedButton = {null};

        for (MaterialButton button : buttons) {
            button.setOnClickListener(v -> {
                if (button.equals(selectedButton[0])) {
                    // Auswahl aufheben
                    resetButtonAppearance(button);
                    selectedButton[0] = null;
                    selectedMoodButton = null;
                } else {
                    // Alle zurücksetzen, aktuellen hervorheben
                    for (MaterialButton other : buttons) {
                        resetButtonAppearance(other);
                    }
                    highlightButton(button);
                    selectedButton[0] = button;
                    selectedMoodButton = button;
                }
            });
        }
        // Symptom-Buttons einrichten (statt setupSymptomChips)
        setupSymptomButtons();
    }


    /**
     * Konfiguriert den Speichern-Button mit Datensammlung und Datenbankoperationen.
     */
    /**
     * Konfiguriert den Speichern-Button mit Datensammlung und Datenbankoperationen.
     */
    private void setupSaveButton() {
        TextView saveButton = findViewById(R.id.btn_save);

        saveButton.setOnClickListener(v -> {
            // Prüfen, ob bereits ein Eintrag für heute existiert
            WohlbefindenEintrag existingEntry = wellbeingDao.getEintragNachDatum(currentDate);

            // Neuen Eintrag erstellen oder bestehenden verwenden
            WohlbefindenEintrag entry;
            if (existingEntry != null) {
                entry = existingEntry;
            } else {
                entry = new WohlbefindenEintrag(currentDate);
            }

            // Blutungsstärke sammeln
            String bleeding = collectBleedingData();
            entry.setBlutungsstaerke(bleeding);

            // Schmerzlevel sammeln
            String pain = selectedPainButton != null ? selectedPainButton.getText().toString() : "";
            entry.setSchmerzLevel(pain);

            // Stimmung sammeln
            String mood = selectedMoodButton != null ? selectedMoodButton.getText().toString() : "";
            entry.setStimmung(mood);

            // Symptome sammeln
            List<String> symptoms = collectSymptoms();
            entry.setSymptome(symptoms);

            // In Datenbank speichern
            saveToDatabaseWithFeedback(entry, existingEntry != null);
        });
    }

    /**
     * Sammelt die ausgewählte Blutungsstärke aus den Tröpfchen-Buttons.
     * @return String der ausgewählten Blutungsstärke oder leerer String
     */
    private String collectBleedingData() {
        if (isDropletSelected(R.id.btn_very_light)) {
            return "Sehr leicht";
        } else if (isDropletSelected(R.id.btn_light)) {
            return "Leicht";
        } else if (isDropletSelected(R.id.btn_medium)) {
            return "Mittel";
        } else if (isDropletSelected(R.id.btn_heavy)) {
            return "Stark";
        }
        return "";
    }

    /**
     * Sammelt alle ausgewählten Symptome aus den MaterialButtons.
     * @return Liste der ausgewählten Symptome
     */
    private List<String> collectSymptoms() {
        MaterialButton[] buttons = new MaterialButton[] {
                findViewById(R.id.btn_headache),
                findViewById(R.id.btn_nausea),
                findViewById(R.id.btn_fatigue),
                findViewById(R.id.btn_back_pain),
                findViewById(R.id.btn_breast_tenderness)
        };

        List<String> symptoms = new ArrayList<>();
        for (MaterialButton button : buttons) {
            if (button != null && button.getTag() != null && button.getTag().equals("selected")) {
                symptoms.add(button.getText().toString());
            }
        }
        return symptoms;
    }

    /**
     * Speichert den Eintrag in die Datenbank und zeigt entsprechendes Feedback.
     * @param entry Der zu speichernde Eintrag
     * @param isUpdate true wenn es ein Update ist, false für neuen Eintrag
     */
    private void saveToDatabaseWithFeedback(WohlbefindenEintrag entry, boolean isUpdate) {
        try {
            if (isUpdate) {
                wellbeingDao.aktualisierenEintrag(entry);
                Toast.makeText(this, "Daten aktualisiert!", Toast.LENGTH_SHORT).show();
            } else {
                wellbeingDao.einfuegenEintrag(entry);
                Toast.makeText(this, "Daten gespeichert!", Toast.LENGTH_SHORT).show();
            }

            // Anzeige der letzten Einträge aktualisieren
            loadAndDisplayLastEntries();

        } catch (Exception e) {
            Toast.makeText(this, "Fehler beim Speichern: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Konfiguriert die Bottom Navigation mit entsprechenden Intents.
     */
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setSelectedItemId(R.id.nav_wellbeing);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_cycle)  {
                startActivity(new Intent(this, ZyklusActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_statistics)  {
                startActivity(new Intent(this, StatistikActivity.class));
                finish();
                return true;
            }
            return true;
        });
    }

    /**
     * Lädt die letzten 5 Einträge aus der Datenbank und aktualisiert die Anzeige.
     */
    private void loadAndDisplayLastEntries() {
        TextView entriesDisplay = findViewById(R.id.text_recent_entries);

        List<WohlbefindenEintrag> recentEntries = wellbeingDao.getLetzteEintraege(5);

        if (recentEntries.isEmpty()) {
            entriesDisplay.setText("Keine Einträge vorhanden");
            return;
        }

        // StringBuilder für formatierte Ausgabe
        StringBuilder displayText = new StringBuilder();
        for (WohlbefindenEintrag entry : recentEntries) {
            appendEntryToDisplay(displayText, entry);
        }

        entriesDisplay.setText(displayText.toString());
    }

    /**
     * Hilfsmethode zum Formatieren eines Eintrags für die Anzeige.
     * @param sb StringBuilder für die Ausgabe
     * @param entry Der anzuzeigende Eintrag
     */
    private void appendEntryToDisplay(StringBuilder sb, WohlbefindenEintrag entry) {
        // Datum
        sb.append(entry.getDatum().toString()).append("\n");

        // Blutung
        if (isNotEmpty(entry.getBlutungsstaerke())) {
            sb.append("Blutung: ").append(entry.getBlutungsstaerke()).append("\n");
        }

        // Schmerzen
        if (isNotEmpty(entry.getSchmerzLevel())) {
            sb.append("Schmerzen: ").append(entry.getSchmerzLevel()).append("\n");
        }

        // Stimmung
        if (isNotEmpty(entry.getStimmung())) {
            sb.append("Stimmung: ").append(entry.getStimmung()).append("\n");
        }

        // Symptome
        if (entry.getSymptome() != null && !entry.getSymptome().isEmpty()) {
            sb.append("Begleitsymptome: ").append(String.join(", ", entry.getSymptome())).append("\n");
        }

        sb.append("\n"); // Leerzeile zwischen Einträgen
    }

    /**
     * Lädt den heutigen Eintrag (falls vorhanden) und stellt die UI entsprechend ein.
     */
    private void loadTodaysEntry() {
        WohlbefindenEintrag todaysEntry = wellbeingDao.getEintragNachDatum(currentDate);

        if (todaysEntry == null) {
            return; // Kein Eintrag für heute vorhanden
        }

        // Blutungsstärke wiederherstellen
        restoreBleedingSelection(todaysEntry.getBlutungsstaerke());

        // Schmerzlevel wiederherstellen
        restorePainSelection(todaysEntry.getSchmerzLevel());

        // Stimmung wiederherstellen
        restoreMoodSelection(todaysEntry.getStimmung());

        // Symptome wiederherstellen
        restoreSymptomSelection(todaysEntry.getSymptome());
    }

    /**
     * Stellt die Auswahl der Blutungsstärke wieder her.
     * @param bleeding Die gespeicherte Blutungsstärke
     */
    private void restoreBleedingSelection(String bleeding) {
        if (bleeding == null) return;

        switch (bleeding) {
            case "Sehr leicht":
                highlightDroplet(findViewById(R.id.btn_very_light));
                break;
            case "Leicht":
                highlightDroplet(findViewById(R.id.btn_light));
                break;
            case "Mittel":
                highlightDroplet(findViewById(R.id.btn_medium));
                break;
            case "Stark":
                highlightDroplet(findViewById(R.id.btn_heavy));
                break;
        }
    }

    /**
     * Stellt die Auswahl des Schmerzlevels wieder her.
     * @param pain Das gespeicherte Schmerzlevel
     */
    private void restorePainSelection(String pain) {
        if (isNotEmpty(pain)) {
            MaterialButton[] painButtons = {
                    findViewById(R.id.btn_no_pain),
                    findViewById(R.id.btn_light_pain),
                    findViewById(R.id.btn_medium_pain),
                    findViewById(R.id.btn_heavy_pain),
                    findViewById(R.id.btn_cramp_pain)
            };

            for (MaterialButton btn : painButtons) {
                if (btn.getText().toString().equals(pain)) {
                    btn.performClick(); // Klick simulieren
                    break;
                }
            }
        }
    }

    /**
     * Stellt die Auswahl der Stimmung wieder her.
     * @param mood Die gespeicherte Stimmung
     */
    private void restoreMoodSelection(String mood) {
        if (isNotEmpty(mood)) {
            MaterialButton[] moodButtons = {
                    findViewById(R.id.btn_very_good),
                    findViewById(R.id.btn_good),
                    findViewById(R.id.btn_medium_mood),
                    findViewById(R.id.btn_bad)
            };

            for (MaterialButton btn : moodButtons) {
                if (btn.getText().toString().equals(mood)) {
                    btn.performClick(); // Klick simulieren
                    break;
                }
            }
        }
    }

    /**
     * Stellt die Auswahl der Symptome wieder her.
     * @param symptoms Die gespeicherten Symptome
     */
    private void restoreSymptomSelection(List<String> symptoms) {
        if (symptoms != null) {
            MaterialButton[] buttons = new MaterialButton[] {
                    findViewById(R.id.btn_headache),
                    findViewById(R.id.btn_nausea),
                    findViewById(R.id.btn_fatigue),
                    findViewById(R.id.btn_back_pain),
                    findViewById(R.id.btn_breast_tenderness)
            };

            for (MaterialButton button : buttons) {
                if (button != null && symptoms.contains(button.getText().toString())) {
                    highlightButton(button);
                    button.setTag("selected");
                }
            }
        }
    }

    /**
     * Setzt das Aussehen eines MaterialButtons auf den Standardzustand zurück.
     * @param button Der zurückzusetzende Button
     */
    private void resetButtonAppearance(MaterialButton button) {
        button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
        button.setTextColor(Color.BLACK);
    }

    /**
     * Hebt einen MaterialButton visuell hervor (für Auswahl).
     * @param button Der hervorzuhebende Button
     */
    private void highlightButton(MaterialButton button) {
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D81B60")));
        button.setTextColor(Color.WHITE);
    }

    /**
     * Hilfsmethode zur Prüfung ob ein String nicht null und nicht leer ist.
     * @param text Der zu prüfende String
     * @return true wenn der String Inhalt hat
     */
    private boolean isNotEmpty(String text) {
        return text != null && !text.isEmpty();
    }
    /**
     * Prüft, ob ein Tröpfchen-Button ausgewählt ist.
     */
    private boolean isDropletSelected(int dropletId) {
        LinearLayout droplet = findViewById(dropletId);
        return droplet.getTag() != null && droplet.getTag().equals("selected");
    }

    /**
     * Hebt einen Tröpfchen-Button visuell hervor - nur Text mit rosa Hintergrund.
     */
    private void highlightDroplet(LinearLayout droplet) {
        // Finde nur den TextView (Text-Element)
        TextView textView = (TextView) droplet.getChildAt(1);

        // Rosa Hintergrund nur für Text
        textView.setBackgroundResource(R.drawable.circle_highlight_pink);
        textView.setTextColor(Color.WHITE);
        textView.setPadding(12, 4, 12, 4); // Padding für besseres Aussehen
        textView.setTextSize(12);
        droplet.setTag("selected");
    }

    /**
     * Setzt das Aussehen eines Tröpfchen-Buttons zurück.
     */
    private void resetDropletAppearance(LinearLayout droplet) {
        TextView textView = (TextView) droplet.getChildAt(1);

        // Entferne Hintergrund und setze Text zurück
        textView.setBackground(null);
        textView.setTextColor(Color.BLACK);
        textView.setPadding(0, 0, 0, 0);

        droplet.setTag(null);
    }

    /**
     * Konfiguriert die MaterialButton-Gruppe für Symptome.
     * Unterstützt Multiple-Selection mit visueller Hervorhebung.
     */
    private void setupSymptomButtons() {
        MaterialButton[] buttons = new MaterialButton[] {
                findViewById(R.id.btn_headache),
                findViewById(R.id.btn_nausea),
                findViewById(R.id.btn_fatigue),
                findViewById(R.id.btn_back_pain),
                findViewById(R.id.btn_breast_tenderness)
        };

        for (MaterialButton button : buttons) {
            button.setOnClickListener(v -> {
                // Toggle-Verhalten - mehrere können ausgewählt sein
                if (button.getTag() != null && button.getTag().equals("selected")) {
                    // Abwählen
                    resetButtonAppearance(button);
                    button.setTag(null);
                } else {
                    // Auswählen
                    highlightButton(button);
                    button.setTag("selected");
                }
            });
        }
    }
}