package at.fhj.andrey.zyklustracker.sensors;

import android.content.Context;
import android.util.Log;

import androidx.health.connect.client.HealthConnectClient;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.util.Set;

import at.fhj.andrey.zyklustracker.datenbank.WohlbefindenDao;
import at.fhj.andrey.zyklustracker.datenbank.WohlbefindenEintrag;
import at.fhj.andrey.zyklustracker.datenbank.ZyklusDatenbank;
import kotlin.Unit;

public class ZyklusSensorManager {

    private static final String TAG = "ZyklusSensorManager";

    private Context context;
    private ZyklusDatenbank datenbank;
    private WohlbefindenDao wohlbefindenDao;

    private SensorData aktuelleHcDaten;

    private RealHealthConnectManager realHealthConnectManager;
    private SensorCallback callback;

    public interface SensorCallback {
        void datenVerfuegbar(SensorData daten);

        void keineDatenVerfuegbar(String grund);

        void sensorFehler(String fehlermeldung);
    }

    public interface HealthConnectPermissionRequester {
        void requestHealthConnectPermissions(Set<String> permissions);
    }

    public ZyklusSensorManager(Context context) {
        this.context = context.getApplicationContext();
        this.datenbank = ZyklusDatenbank.getInstanz(this.context);
        this.wohlbefindenDao = datenbank.wohlbefindenDao();
        this.aktuelleHcDaten = null;
        this.realHealthConnectManager = new RealHealthConnectManager(this.context);
        Log.d(TAG, "ZyklusSensorManager (Health Connect mit Callbacks) erfolgreich initialisiert");
    }

    public void setzeCallback(SensorCallback callback) {
        this.callback = callback;
    }

    public void starteMessung() {
        Log.d(TAG, "Neue Sensordaten-Messung gestartet");
        this.aktuelleHcDaten = null; //Vorherige Daten zurücksetzen für neue Messung
        requestAndReadHealthConnectData();
    }

    public void stoppeMessung() {
        Log.d(TAG, "Sensordaten-Erfassung gestoppt - Cleanup wird durchgeführt");
        if (realHealthConnectManager != null) {
            realHealthConnectManager.cleanup(); // Wichtig: Beendet laufende Koroutinen im Health Connect Manager
        }
    }

    private void requestAndReadHealthConnectData() {
        Log.d(TAG, "Health Connect Datenerfassung wird gestartet");

        int sdkStatus = HealthConnectClient.getSdkStatus(context);

        if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            Log.w(TAG, "Health Connect SDK ist auf diesem Gerät nicht verfügbar");
            if (callback != null) {
                callback.keineDatenVerfuegbar("Health Connect ist auf diesem Gerät nicht verfügbar");
            }
            return;
        }
        if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            Log.w(TAG, "Health Connect Provider benötigt ein Update");
            if (callback != null) {
                callback.keineDatenVerfuegbar("Bitte aktualisieren Sie die Health Connect App");
            }
            return;
        }

        Set<String> requiredPermissions = realHealthConnectManager.getRequiredPermissionsSet();

        realHealthConnectManager.checkGrantedPermissions(
                grantedPermissions -> onPermissionsChecked(grantedPermissions),
                error -> onPermissionsError(error)
        );
    }

    private @NotNull Unit onPermissionsChecked(Set<String> grantedPermissions) {
        Set<String> requiredPermissions = realHealthConnectManager.getRequiredPermissionsSet();

        if (!grantedPermissions.containsAll(requiredPermissions)) {
            Log.w(TAG, "Nicht alle erforderlichen Health Connect Berechtigungen erteilt");
            if (callback instanceof HealthConnectPermissionRequester) {
                ((HealthConnectPermissionRequester) callback).requestHealthConnectPermissions(requiredPermissions);
            } else {
                Log.e(TAG, "Callback implementiert nicht HealthConnectPermissionRequester Interface");
                if (callback != null) {
                    callback.sensorFehler("Konfigurationsfehler: Health Connect Berechtigungs-Anfrage nicht möglich");
                }
            }
            if (callback != null) {
                callback.keineDatenVerfuegbar("Health Connect Berechtigungen erforderlich");
            }
            return null;
        }
        Log.d(TAG, "Health Connect Berechtigungen verfügbar - Sensordaten werden abgerufen");
        realHealthConnectManager.readLatestSensorData(
                hcData -> onSensorDataReceived(hcData),
                error -> onSensorDataError(error)
        );
        return null;
    }

    private @NotNull Unit onSensorDataReceived(SensorData hcData) {
        if (hcData != null) {
            this.aktuelleHcDaten = hcData;
            Log.d(TAG, "Health Connect Sensordaten erfolgreich empfangen: " + hcData.toString());
            if (callback != null) {
                callback.datenVerfuegbar(hcData);
            }
            speichereDatenInDb();
        } else {
            Log.w(TAG, "Keine Health Connect Sensordaten empfangen oder Daten sind leer");
            this.aktuelleHcDaten = null;
            if (callback != null) {
                callback.keineDatenVerfuegbar("Keine aktuellen Sensordaten in Health Connect verfügbar");
            }
        }
        return null;
    }

    private @NotNull Unit onSensorDataError(Exception error) {
        Log.e(TAG, "Fehler beim Abrufen der Health Connect Sensordaten: " + error.getMessage(), error);
        this.aktuelleHcDaten = null;
        if (callback != null) {
            callback.sensorFehler("Health Connect Datenlesefehler: " + error.getMessage());
            callback.keineDatenVerfuegbar("Fehler beim Abrufen der Health Connect Sensordaten");
        }
        return null;
    }
    private @NotNull Unit onPermissionsError(Exception error) {
        Log.e(TAG, "Fehler bei Health Connect Berechtigungs-Prüfung: " + error.getMessage(), error);
        this.aktuelleHcDaten = null;
        if (callback != null) {
            callback.sensorFehler("Health Connect ошибка проверки разрешений: " + error.getMessage());
            callback.keineDatenVerfuegbar("Fehler bei der Health Connect Berechtigungs-Prüfung");
        }
        return null;
    }

    /**
     * 2. SPEICHER-PROZESS (bereits implementiert):
     * ============================================
     */

// In ZyklusSensorManager.java - speichereDatenInDb() Methode:
    private void speichereDatenInDb() {
        if (aktuelleHcDaten == null) {
            Log.d(TAG, "Keine Health Connect Daten zum Speichern verfügbar.");
            return;
        }

        LocalDate heute = LocalDate.now();
        Log.d(TAG, "Starte Speicherung der Sensordaten für: " + heute);

        // WICHTIG: Background-Thread für Datenbankoperationen
        new Thread(() -> {
            try {
                // Schritt 1: Bestehenden oder neuen Eintrag holen
                WohlbefindenEintrag eintrag = wohlbefindenDao.getEintragNachDatum(heute);
                boolean istNeuerEintrag = (eintrag == null);

                if (istNeuerEintrag) {
                    eintrag = new WohlbefindenEintrag(heute);
                    Log.d(TAG, "Neuer Wohlbefinden-Eintrag erstellt für: " + heute);
                } else {
                    Log.d(TAG, "Bestehender Eintrag gefunden für: " + heute);
                }

                boolean datenAktualisiert = false;
                StringBuilder updateLog = new StringBuilder("Aktualisierte Sensordaten: ");

                // Schritt 2: Validierung und Speicherung der Sensordaten

                // 2a) Herzfrequenz validieren und speichern
                if (istValidePulsfrequenz(aktuelleHcDaten.getHeartRate())) {
                    int neuePulsfrequenz = (int) aktuelleHcDaten.getHeartRate();
                    eintrag.setPuls(neuePulsfrequenz);
                    updateLog.append("Puls=").append(neuePulsfrequenz).append("bpm ");
                    datenAktualisiert = true;
                    Log.d(TAG, "Pulsfrequenz gespeichert: " + neuePulsfrequenz + " bpm");
                } else {
                    Log.w(TAG, "Ungültige Pulsfrequenz ignoriert: " + aktuelleHcDaten.getHeartRate());
                }

                // 2b) Sauerstoffsättigung validieren und speichern
                if (istValideSpO2(aktuelleHcDaten.getOxygenSaturation())) {
                    int neueSpO2 = (int) aktuelleHcDaten.getOxygenSaturation();
                    eintrag.setSpo2(neueSpO2);
                    updateLog.append("SpO2=").append(neueSpO2).append("% ");
                    datenAktualisiert = true;
                    Log.d(TAG, "Sauerstoffsättigung gespeichert: " + neueSpO2 + "%");
                } else {
                    Log.w(TAG, "Ungültige SpO2-Werte ignoriert: " + aktuelleHcDaten.getOxygenSaturation());
                }

                // 2c) Körpertemperatur validieren und speichern
                if (istValideTemperatur(aktuelleHcDaten.getBodyTemperature())) {
                    float neueTemperatur = aktuelleHcDaten.getBodyTemperature();
                    eintrag.setTemperatur(neueTemperatur);
                    updateLog.append("Temp=").append(String.format("%.1f", neueTemperatur)).append("°C ");
                    datenAktualisiert = true;
                    Log.d(TAG, "Körpertemperatur gespeichert: " + String.format("%.1f", neueTemperatur) + "°C");
                } else {
                    Log.w(TAG, "Ungültige Temperatur ignoriert: " + aktuelleHcDaten.getBodyTemperature());
                }

                // Schritt 3: In Datenbank speichern falls Daten aktualisiert wurden
                if (datenAktualisiert) {
                    if (istNeuerEintrag) {
                        wohlbefindenDao.einfuegenEintrag(eintrag);
                        Log.i(TAG, "Neuer Wohlbefinden-Eintrag mit Sensordaten erstellt");
                    } else {
                        wohlbefindenDao.aktualisierenEintrag(eintrag);
                        Log.i(TAG, "Bestehender Eintrag mit neuen Sensordaten aktualisiert");
                    }

                    Log.i(TAG, updateLog.toString());

                    // UI über erfolgreiche Speicherung informieren
                    benachrichtigeUeberErfolgreicheSpeicherung();

                } else {
                    Log.w(TAG, "Keine gültigen Sensordaten zum Speichern gefunden");
                    benachrichtigeUeberUngueltigeDaten();
                }

            } catch (Exception e) {
                Log.e(TAG, "Fehler beim Speichern der Sensordaten in die Datenbank", e);
                benachrichtigeUeberSpeicherFehler(e.getMessage());
            }
        }).start();
    }

    /**
     * Validiert die Pulsfrequenz auf realistische Werte
     */
    private boolean istValidePulsfrequenz(float puls) {
        return puls > 0 && puls >= 40 && puls <= 200; // Realistischer Bereich für Ruhepuls
    }

    /**
     * Validiert die Sauerstoffsättigung auf medizinisch sinnvolle Werte
     */
    private boolean istValideSpO2(float spo2) {
        return spo2 > 0 && spo2 >= 80 && spo2 <= 100; // Medizinisch relevanter Bereich
    }

    /**
     * Validiert die Körpertemperatur auf physiologisch mögliche Werte
     */
    private boolean istValideTemperatur(float temperatur) {
        return temperatur > 0 && temperatur >= 30.0f && temperatur <= 45.0f; // Überlebensfähiger Bereich
    }

    /**
     * Benachrichtigt die UI über erfolgreiche Datenspeicherung
     */
    private void benachrichtigeUeberErfolgreicheSpeicherung() {
        if (context != null) {
            android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
            mainHandler.post(() -> {
                if (callback != null) {
                    // Wir können den Callback erweitern oder einfach loggen
                    Log.i(TAG, "Sensordaten erfolgreich in Datenbank gespeichert");
                }
            });
        }
    }

    /**
     * Benachrichtigt über ungültige Sensordaten
     */
    private void benachrichtigeUeberUngueltigeDaten() {
        if (context != null && callback != null) {
            android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
            mainHandler.post(() -> {
                callback.keineDatenVerfuegbar("Sensordaten außerhalb gültiger Bereiche");
            });
        }
    }

    /**
     * Benachrichtigt über Datenbankfehler
     */
    private void benachrichtigeUeberSpeicherFehler(String fehlerNachricht) {
        if (context != null && callback != null) {
            android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
            mainHandler.post(() -> {
                callback.sensorFehler("Datenbank-Speicherfehler: " + fehlerNachricht);
            });
        }
    }






    public SensorData getAktuelleHcDaten() {
        return aktuelleHcDaten;
    }
}