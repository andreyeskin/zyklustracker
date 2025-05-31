package at.fhj.andrey.zyklustracker;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Direkte Weiterleitung zur Hauptfunktion
        Intent intent = new Intent(this, ZyklusActivity.class);
        startActivity(intent);
        finish(); // MainActivity beenden
    }
}