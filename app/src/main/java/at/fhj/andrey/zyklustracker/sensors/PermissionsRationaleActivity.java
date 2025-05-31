package at.fhj.andrey.zyklustracker.sensors;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import at.fhj.andrey.zyklustracker.R;

/**
 * PermissionsRationaleActivity - Aktivität zur Anzeige der Datenschutzerklärung
 *
 * Diese Aktivität wird von Health Connect aufgerufen, wenn der Nutzer auf den
 * "Datenschutz"-Link in den Health Connect Berechtigungen klickt.
 *
 * Zweck:
 * - Erklärung warum die App gesundheitsdaten benötigt
 * - Datenschutz und Verwendung der Daten erläutern
 * - Vertrauen des Nutzers gewinnen
 *
 * Integration:
 * - Wird automatisch von Health Connect über Intent aufgerufen
 * - Intent Filter: androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE
 * - Für Android 14+: android.intent.action.VIEW_PERMISSION_USAGE
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class PermissionsRationaleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_rationale);

        // Title und Inhalt setzen
        setupPrivacyContent();

        // Schließen-Button konfigurieren
        findViewById(R.id.btn_close_privacy).setOnClickListener(v -> finish());
    }

    /**
     * Konfiguriert den Inhalt der Datenschutzerklärung
     */
    private void setupPrivacyContent() {
        TextView titleText = findViewById(R.id.text_privacy_title);
        TextView contentText = findViewById(R.id.text_privacy_content);

        titleText.setText("Datenschutz & Gesundheitsdaten");

        String privacyContent =
                "ZyklusTracker verwendet Ihre Gesundheitsdaten ausschließlich für folgende Zwecke:\n\n" +

                        "• ZYKLUSBEOBACHTUNG\n" +
                        "Herzfrequenz, Körpertemperatur und Sauerstoffsättigung helfen bei der " +
                        "Erkennung verschiedener Zyklusphasen und unterstützen die natürliche " +
                        "Familienplanung.\n\n" +

                        "• LOKALE SPEICHERUNG\n" +
                        "Alle Daten werden nur lokal auf Ihrem Gerät gespeichert. Es erfolgt " +
                        "keine Übertragung an externe Server oder Dritte.\n\n" +

                        "• WISSENSCHAFTLICHE GRUNDLAGE\n" +
                        "Die App basiert auf wissenschaftlichen Erkenntnissen über zyklische " +
                        "Schwankungen der Vitalparameter während des Menstruationszyklus.\n\n" +

                        "• IHRE KONTROLLE\n" +
                        "Sie können jederzeit in Health Connect entscheiden, welche Daten " +
                        "ZyklusTracker lesen darf. Berechtigungen können jederzeit widerrufen werden.\n\n" +

                        "• KEINE WERBUNG\n" +
                        "Ihre Gesundheitsdaten werden niemals für Werbezwecke verwendet oder " +
                        "an Werbetreibende weitergegeben.\n\n" +

                        "Bei Fragen zum Datenschutz wenden Sie sich an: privacy@zyklustracker.app";

        contentText.setText(privacyContent);
    }
}