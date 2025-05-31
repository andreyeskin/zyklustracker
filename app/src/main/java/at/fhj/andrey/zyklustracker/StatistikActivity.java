package at.fhj.andrey.zyklustracker;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StatistikActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistik);

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_statistik);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_zyklus) {
                startActivity(new Intent(this, ZyklusActivity.class));
                return true;
            } else if (id == R.id.nav_wohlbefinden) {
                startActivity(new Intent(this, WohlbefindenActivity.class));
                return true;
            }
            return true;
        });
    }
}