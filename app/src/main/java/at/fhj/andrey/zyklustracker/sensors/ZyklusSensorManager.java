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
// Убираем импорты kotlinx.coroutines, так как ZyklusSensorManager теперь не запускает корутины сам
// import kotlinx.coroutines.BuildersKt;
// import kotlinx.coroutines.CoroutineScope;
// import kotlinx.coroutines.Dispatchers;
// import kotlinx.coroutines.GlobalScope;
// import kotlinx.coroutines.Job;
// import kotlin.Unit;
// import kotlin.jvm.functions.Function2;

// Используем Kotlin data class SensorData напрямую
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
        Log.d(TAG, "ZyklusSensorManager (Health Connect с коллбэками) инициализирован.");
    }

    public void setzeCallback(SensorCallback callback) {
        this.callback = callback;
    }

    public void starteMessung() {
        Log.d(TAG, "starteMessung() вызван.");
        this.aktuelleHcDaten = null; // Сброс перед новым запросом
        requestAndReadHealthConnectData();
    }

    public void stoppeMessung() {
        Log.d(TAG, "stoppeMessung() вызван. Очистка RealHealthConnectManager.");
        if (realHealthConnectManager != null) {
            realHealthConnectManager.cleanup(); // Важно для отмены корутин в RealHealthConnectManager
        }
    }

    private void requestAndReadHealthConnectData() {
        Log.d(TAG, "requestAndReadHealthConnectData() запущен.");

        int sdkStatus = HealthConnectClient.getSdkStatus(context);

        if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE) {
            Log.w(TAG, "Health Connect SDK недоступен.");
            if (callback != null) {
                callback.keineDatenVerfuegbar("Health Connect недоступен на этом устройстве.");
            }
            return;
        }
        if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            Log.w(TAG, "Требуется обновление Health Connect провайдера.");
            if (callback != null) {
                callback.keineDatenVerfuegbar("Пожалуйста, обновите приложение Health Connect.");
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
            Log.w(TAG, "Не все разрешения Health Connect предоставлены.");
            if (callback instanceof HealthConnectPermissionRequester) {
                ((HealthConnectPermissionRequester) callback).requestHealthConnectPermissions(requiredPermissions);
            } else {
                Log.e(TAG, "Callback не реализует HealthConnectPermissionRequester!");
                if (callback != null) {
                    callback.sensorFehler("Ошибка конфигурации: запрос разрешений HC невозможен.");
                }
            }
            if (callback != null) {
                callback.keineDatenVerfuegbar("Требуются разрешения Health Connect.");
            }
            return null;
        }

        // Замените строки 125-128 на:
        Log.d(TAG, "Разрешения HC есть. Читаю данные из Health Connect...");
        realHealthConnectManager.readLatestSensorData(
                hcData -> onSensorDataReceived(hcData),  // лямбда вместо this::onSensorDataReceived
                error -> onSensorDataError(error)        // лямбда вместо this::onSensorDataError
        );
        return null;
    }

    private @NotNull Unit onSensorDataReceived(SensorData hcData) {
        if (hcData != null) {
            this.aktuelleHcDaten = hcData;
            Log.d(TAG, "Health Connect данные получены: " + hcData.toString());
            if (callback != null) {
                callback.datenVerfuegbar(hcData);
            }
            speichereDatenInDb();
        } else {
            Log.w(TAG, "Данные из Health Connect не получены или пусты.");
            this.aktuelleHcDaten = null;
            if (callback != null) {
                callback.keineDatenVerfuegbar("Нет актуальных данных в Health Connect за последний час.");
            }
        }
        return null;
    }

    private @NotNull Unit onSensorDataError(Exception error) {
        Log.e(TAG, "Ошибка при чтении данных из Health Connect: " + error.getMessage(), error);
        this.aktuelleHcDaten = null;
        if (callback != null) {
            callback.sensorFehler("Health Connect ошибка чтения данных: " + error.getMessage());
            callback.keineDatenVerfuegbar("Ошибка при чтении данных из Health Connect.");
        }
        return null;
    }
    private @NotNull Unit onPermissionsError(Exception error) {
        Log.e(TAG, "Ошибка при проверке разрешений Health Connect: " + error.getMessage(), error);
        this.aktuelleHcDaten = null;
        if (callback != null) {
            callback.sensorFehler("Health Connect ошибка проверки разрешений: " + error.getMessage());
            callback.keineDatenVerfuegbar("Ошибка при проверке разрешений Health Connect.");
        }
        return null;
    }

    private void speichereDatenInDb() {
        if (aktuelleHcDaten == null) {
            Log.d(TAG, "Нет данных HC для сохранения в БД.");
            return;
        }

        LocalDate heute = LocalDate.now();

        // ВАЖНО: Все операции с БД должны быть в фоновом потоке!
        new Thread(() -> {
            try {
                // Получаем eintrag в фоновом потоке
                WohlbefindenEintrag eintrag = wohlbefindenDao.getEintragNachDatum(heute);
                if (eintrag == null) {
                    eintrag = new WohlbefindenEintrag(heute);
                }

                boolean aktualisiert = false;

                // Копируем актуальные данные (они не изменятся, так как final)
                SensorData hcData = aktuelleHcDaten;

                if (hcData.getHeartRate() > 0) {
                    eintrag.setPuls((int) hcData.getHeartRate());
                    aktualisiert = true;
                }
                if (hcData.getOxygenSaturation() > 0) {
                    eintrag.setSpo2((int) hcData.getOxygenSaturation());
                    aktualisiert = true;
                }
                if (hcData.getBodyTemperature() > 0) {
                    eintrag.setTemperatur(hcData.getBodyTemperature());
                    aktualisiert = true;
                }

                if (aktualisiert) {
                    // Сохраняем в БД (уже в фоновом потоке)
                    if (wohlbefindenDao.existiertEintrag(heute) > 0) {
                        wohlbefindenDao.aktualisierenEintrag(eintrag);
                    } else {
                        wohlbefindenDao.einfuegenEintrag(eintrag);
                    }
                    Log.d(TAG, "Данные из Health Connect сохранены в БД для " + heute);
                } else {
                    Log.d(TAG, "Полученные данные HC не содержали значений для обновления в БД.");
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка при сохранении данных из Health Connect в БД", e);

                // Переключаемся на главный поток для callback UI
                if (context != null && callback != null) {
                    android.os.Handler mainHandler = new android.os.Handler(context.getMainLooper());
                    mainHandler.post(() -> {
                        callback.sensorFehler("Ошибка сохранения данных: " + e.getMessage());
                    });
                }
            }
        }).start();
    }
    public SensorData getAktuelleHcDaten() {
        return aktuelleHcDaten;
    }
}