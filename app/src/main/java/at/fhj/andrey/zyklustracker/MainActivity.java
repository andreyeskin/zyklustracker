/**
 * MainActivity.java - Startaktivität der ZyklusTracker-Anwendung
 *
 * Diese Klasse dient als Einstiegspunkt der Anwendung und leitet
 * automatisch zur Hauptfunktion (ZyklusActivity) weiter.
 *
 * Funktionalität:
 * - Automatische Weiterleitung zur ZyklusActivity
 * - Selbstbeendigung nach Weiterleitung
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai
 * @course SWE04 - Mobile Anwendungen
 * @university FH Joanneum
 */

package at.fhj.andrey.zyklustracker;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;


public class MainActivity extends AppCompatActivity {

    /**
     * Wird beim Erstellen der Aktivität aufgerufen.
     * Führt eine direkte Weiterleitung zur Hauptfunktion durch.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Direkte Weiterleitung zur Hauptfunktion
        Intent intent = new Intent(this, ZyklusActivity.class);
        startActivity(intent);
        finish(); // MainActivity beenden
    }
}