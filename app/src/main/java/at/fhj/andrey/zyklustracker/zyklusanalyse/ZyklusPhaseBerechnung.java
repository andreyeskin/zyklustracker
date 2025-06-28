package at.fhj.andrey.zyklustracker.zyklusanalyse;

import android.util.Log;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ZyklusPhaseBerechnung - Zentrale Logik für Zyklusanalyse und Phasenerkennung
 *
 * Diese Klasse implementiert die wissenschaftlich fundierten Algorithmen zur:
 * - Berechnung der aktuellen Zyklusphase basierend auf Periodendaten
 * - Bewertung von Sensor-Werten im Kontext der jeweiligen Zyklusphase
 * - Erkennung von Zyklusunregelmäßigkeiten und Anomalien
 *
 * Medizinische Grundlage (basierend auf Ihrer Forschung):
 * - Menstruation (Tage 1-5): Niedrigste Werte für Puls und Temperatur
 * - Follikelphase (Tage 6-13): Stabile, niedrige Werte
 * - Ovulation (Tage 12-16): Beginnender Anstieg, Überlappung mit Follikelphase
 * - Lutealphase (Tage 17-28): Höchste Werte (+3-5% Puls, +0.3-0.7°C Temperatur)
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class ZyklusPhaseBerechnung {

    private static final String TAG = "ZyklusPhaseBerechnung";

    /**
     * Enum für die vier Hauptphasen des Menstruationszyklus
     */
    public enum ZyklusPhase {
        MENSTRUATION("Menstruation", "Monatliche Blutung"),
        FOLLIKELPHASE("Follikelphase", "Ei reift heran"),
        OVULATION("Ovulation", "Eisprung - höchste Fruchtbarkeit"),
        LUTEALPHASE("Lutealphase", "Gelbkörperphase nach Eisprung"),
        UNBEKANNT("Unbekannt", "Phase konnte nicht bestimmt werden");

        private final String displayName;
        private final String beschreibung;

        ZyklusPhase(String displayName, String beschreibung) {
            this.displayName = displayName;
            this.beschreibung = beschreibung;
        }

        public String getDisplayName() { return displayName; }
        public String getBeschreibung() { return beschreibung; }
    }

    // ===== MEDIZINISCHE REFERENZWERTE (aus Ihrer Forschung) =====

    // Temperatur-Referenzbereiche (°C)
    private static final float TEMP_FOLLIKEL_MIN = 36.0f;
    private static final float TEMP_FOLLIKEL_MAX = 36.5f;
    private static final float TEMP_LUTEAL_MIN = 36.5f;
    private static final float TEMP_LUTEAL_MAX = 37.0f;
    private static final float TEMP_ANSTIEG_SCHWELLE = 0.3f;  // Mindest-Anstieg für Eisprung

    // Puls-Referenzbereiche (bpm und prozentuale Änderungen)
    private static final int PULS_NORMAL_MIN = 60;
    private static final int PULS_NORMAL_MAX = 100;
    private static final float PULS_LUTEAL_ANSTIEG_MIN = 0.03f;  // 3% Anstieg
    private static final float PULS_LUTEAL_ANSTIEG_MAX = 0.05f;  // 5% Anstieg

    // SpO2-Referenzbereiche (%)
    private static final int SPO2_NORMAL_MIN = 95;
    private static final int SPO2_OPTIMAL = 98;

    // Zyklus-Parameter
    private static final int ZYKLUSLÄNGE_MIN = 21;
    private static final int ZYKLUSLÄNGE_MAX = 35;
    private static final int ZYKLUSLÄNGE_STANDARD = 28;

    /**
     * Ermittelt die aktuelle Zyklusphase für ein gegebenes Datum
     *
     * @param datum Das zu analysierende Datum
     * @param periodendaten Liste aller Periodenstartdaten (chronologisch sortiert)
     * @return ZyklusPhase für das angegebene Datum
     */
    public ZyklusPhase berechneAktuellePhase(LocalDate datum, List<LocalDate> periodendaten) {
        if (periodendaten == null || periodendaten.isEmpty()) {
            Log.w(TAG, "Keine Periodendaten verfügbar für Phasenberechnung");
            return ZyklusPhase.UNBEKANNT;
        }

        // Daten sortieren (neueste zuerst für effiziente Suche)
        List<LocalDate> sortiertePerioden = new ArrayList<>(periodendaten);
        Collections.sort(sortiertePerioden, Collections.reverseOrder());

        // Finde den ersten Tag der letzten Periode vor dem angegebenen Datum
        LocalDate letzterPeriodenstart = findeErstenPeriodentag(datum, sortiertePerioden);

        if (letzterPeriodenstart == null) {
            Log.w(TAG, "Keine passende Periode für Datum gefunden: " + datum);
            return ZyklusPhase.UNBEKANNT;
        }

        // Berechne Zyklustag (1-basiert)
        int zyklusTag = (int) ChronoUnit.DAYS.between(letzterPeriodenstart, datum) + 1;

        // Bestimme durchschnittliche Zykluslänge für präzisere Phasengrenzen
        int durchschnittlicheZykluslänge = berechneDurchschnittlicheZykluslänge(sortiertePerioden);

        Log.d(TAG, "Datum: " + datum + ", Letzter Periodenstart: " + letzterPeriodenstart +
                ", Zyklustag: " + zyklusTag + ", Durchschnittslänge: " + durchschnittlicheZykluslänge);

        return bestimmePhaseNachTag(zyklusTag, durchschnittlicheZykluslänge);
    }

    /**
     * Bestimmt die Zyklusphase basierend auf dem Zyklustag und der individuellen Zykluslänge
     *
     * KORRIGIERTE medizinische Phasen:
     * - Menstruation: Tage 1-6
     * - Follikelphase: Tage 7-12
     * - Fruchtbare Phase: Tage 10-16 (überlappend!)
     * - Eisprung: Tag 14 (in fruchtbarer Phase)
     * - Lutealphase: Tage 15-28
     */
    private ZyklusPhase bestimmePhaseNachTag(int zyklusTag, int zykluslänge) {
        if (zyklusTag <= 5) {
            return ZyklusPhase.MENSTRUATION;
        } else if (zyklusTag >= 10 && zyklusTag <= 14) {
            // Fruchtbare Phase überschreibt andere Phasen (wichtigste Info)
            if (zyklusTag == 14) {
                return ZyklusPhase.OVULATION; // Eisprung hat höchste Priorität
            }
            return ZyklusPhase.FOLLIKELPHASE;
        } else if (zyklusTag <= 13) {
            return ZyklusPhase.FOLLIKELPHASE;
        } else {
            return ZyklusPhase.LUTEALPHASE;
        }
    }

    /**
     * Berechnet die durchschnittliche Zykluslänge aus historischen Daten
     */
    private int berechneDurchschnittlicheZykluslänge(List<LocalDate> sortiertePerioden) {
        if (sortiertePerioden.size() < 2) {
            return ZYKLUSLÄNGE_STANDARD;
        }

        List<Long> zykluslängen = new ArrayList<>();
        for (int i = 0; i < sortiertePerioden.size() - 1; i++) {
            LocalDate aktuelle = sortiertePerioden.get(i);
            LocalDate vorherige = sortiertePerioden.get(i + 1);
            long länge = ChronoUnit.DAYS.between(vorherige, aktuelle);

            // Nur realistische Zykluslängen berücksichtigen
            if (länge >= ZYKLUSLÄNGE_MIN && länge <= ZYKLUSLÄNGE_MAX) {
                zykluslängen.add(länge);
            }
        }

        if (zykluslängen.isEmpty()) {
            return ZYKLUSLÄNGE_STANDARD;
        }

        // Durchschnitt berechnen
        double durchschnitt = zykluslängen.stream().mapToLong(Long::longValue).average().orElse(ZYKLUSLÄNGE_STANDARD);
        return (int) Math.round(durchschnitt);
    }

    /**
     * Analysiert Sensor-Werte im Kontext der aktuellen Zyklusphase
     *
     * @param datum Datum der Messung
     * @param temperatur Körpertemperatur in °C
     * @param puls Ruhepuls in bpm
     * @param spo2 Sauerstoffsättigung in %
     * @param periodendaten Historische Periodendaten
     * @return Detailliertes AnalyseErgebnis mit Bewertung und Empfehlungen
     */
    public AnalyseErgebnis analysiereSensorWerte(LocalDate datum, float temperatur, int puls, int spo2,
                                                 List<LocalDate> periodendaten) {

        // Aktuelle Phase bestimmen
        ZyklusPhase aktuellePhase = berechneAktuellePhase(datum, periodendaten);
        int zyklusTag = berechneZyklusTag(datum, periodendaten);
        boolean istRegular = pruefeZyklusRegularität(periodendaten);

        Log.d(TAG, "Analysiere Sensor-Werte für Phase: " + aktuellePhase +
                ", Tag: " + zyklusTag + ", Werte: Temp=" + temperatur + "°C, Puls=" + puls + "bpm, SpO2=" + spo2 + "%");

        // Einzelbewertungen für jeden Parameter
        AnalyseErgebnis.Bewertung tempBewertung = bewerteTemperatur(temperatur, aktuellePhase);
        AnalyseErgebnis.Bewertung pulsBewertung = bewertePuls(puls, aktuellePhase);
        AnalyseErgebnis.Bewertung spo2Bewertung = bewerteSpo2(spo2);

        // Beschreibungen und Empfehlungen generieren
        String phasenBeschreibung = generierePhasenBeschreibung(aktuellePhase, zyklusTag);
        String empfehlung = generiereEmpfehlung(aktuellePhase, tempBewertung, pulsBewertung, spo2Bewertung);
        String medizinischerHinweis = generiereMedizinischenHinweis(aktuellePhase, tempBewertung, pulsBewertung, istRegular);

        // Abweichungen berechnen für UI-Darstellung
        float tempAbweichung = berechneTemperaturAbweichung(temperatur, aktuellePhase);
        float pulsAbweichung = berechnePulsAbweichung(puls, aktuellePhase);

        return new AnalyseErgebnis(
                aktuellePhase, zyklusTag, istRegular,
                tempBewertung, pulsBewertung, spo2Bewertung,
                phasenBeschreibung, empfehlung, medizinischerHinweis,
                tempAbweichung, pulsAbweichung, spo2
        );
    }

    /**
     * Bewertet die Temperatur im Kontext der Zyklusphase
     */
    private AnalyseErgebnis.Bewertung bewerteTemperatur(float temperatur, ZyklusPhase phase) {
        if (temperatur <= 0) {
            return AnalyseErgebnis.Bewertung.NORMAL; // Keine Daten verfügbar
        }

        switch (phase) {
            case MENSTRUATION:
            case FOLLIKELPHASE:
                if (temperatur >= TEMP_FOLLIKEL_MIN && temperatur <= TEMP_FOLLIKEL_MAX) {
                    return AnalyseErgebnis.Bewertung.NORMAL;
                } else if (temperatur < TEMP_FOLLIKEL_MIN - 0.5f || temperatur > TEMP_FOLLIKEL_MAX + 0.3f) {
                    return AnalyseErgebnis.Bewertung.AUFFAELLIG;
                } else {
                    return AnalyseErgebnis.Bewertung.GRENZWERTIG;
                }

            case OVULATION:
                // Übergangsphase - größere Toleranz
                if (temperatur >= TEMP_FOLLIKEL_MIN && temperatur <= TEMP_LUTEAL_MAX) {
                    return AnalyseErgebnis.Bewertung.NORMAL;
                } else {
                    return AnalyseErgebnis.Bewertung.GRENZWERTIG;
                }

            case LUTEALPHASE:
                if (temperatur >= TEMP_LUTEAL_MIN && temperatur <= TEMP_LUTEAL_MAX) {
                    return AnalyseErgebnis.Bewertung.NORMAL;
                } else if (temperatur < TEMP_FOLLIKEL_MAX) {
                    // Fehlender Temperaturanstieg in Lutealphase könnte auf Anovulation hindeuten
                    return AnalyseErgebnis.Bewertung.AUFFAELLIG;
                } else if (temperatur > TEMP_LUTEAL_MAX + 0.5f) {
                    return AnalyseErgebnis.Bewertung.KRITISCH;
                } else {
                    return AnalyseErgebnis.Bewertung.GRENZWERTIG;
                }

            default:
                return AnalyseErgebnis.Bewertung.NORMAL;
        }
    }

    /**
     * Bewertet den Puls im Kontext der Zyklusphase
     */
    private AnalyseErgebnis.Bewertung bewertePuls(int puls, ZyklusPhase phase) {
        if (puls <= 0) {
            return AnalyseErgebnis.Bewertung.NORMAL; // Keine Daten verfügbar
        }

        // Grundlegende Puls-Bewertung
        if (puls < 50) {
            return AnalyseErgebnis.Bewertung.AUFFAELLIG; // Bradykardie
        } else if (puls > 100) {
            return AnalyseErgebnis.Bewertung.KRITISCH; // Tachykardie
        }

        // Phasenspezifische Bewertung (vereinfacht, da Baseline individuell ist)
        switch (phase) {
            case MENSTRUATION:
            case FOLLIKELPHASE:
                // In frühen Phasen erwarten wir niedrigere Werte
                if (puls >= PULS_NORMAL_MIN && puls <= 80) {
                    return AnalyseErgebnis.Bewertung.NORMAL;
                } else if (puls <= 90) {
                    return AnalyseErgebnis.Bewertung.GRENZWERTIG;
                } else {
                    return AnalyseErgebnis.Bewertung.AUFFAELLIG;
                }

            case LUTEALPHASE:
                // In Lutealphase sind leicht erhöhte Werte normal
                if (puls >= PULS_NORMAL_MIN && puls <= 85) {
                    return AnalyseErgebnis.Bewertung.NORMAL;
                } else if (puls <= 95) {
                    return AnalyseErgebnis.Bewertung.GRENZWERTIG;
                } else {
                    return AnalyseErgebnis.Bewertung.AUFFAELLIG;
                }

            default:
                // Ovulation: Übergangsphase
                if (puls >= PULS_NORMAL_MIN && puls <= 85) {
                    return AnalyseErgebnis.Bewertung.NORMAL;
                } else {
                    return AnalyseErgebnis.Bewertung.GRENZWERTIG;
                }
        }
    }

    /**
     * Bewertet SpO2 (unabhängig von Zyklusphase, da keine signifikanten Schwankungen)
     */
    private AnalyseErgebnis.Bewertung bewerteSpo2(int spo2) {
        if (spo2 <= 0) {
            return AnalyseErgebnis.Bewertung.NORMAL; // Keine Daten verfügbar
        }

        if (spo2 >= SPO2_OPTIMAL) {
            return AnalyseErgebnis.Bewertung.NORMAL;
        } else if (spo2 >= SPO2_NORMAL_MIN) {
            return AnalyseErgebnis.Bewertung.GRENZWERTIG;
        } else if (spo2 >= 90) {
            return AnalyseErgebnis.Bewertung.AUFFAELLIG;
        } else {
            return AnalyseErgebnis.Bewertung.KRITISCH;
        }
    }

    // ===== HILFSMETHODEN =====

    private int berechneZyklusTag(LocalDate datum, List<LocalDate> periodendaten) {
        if (periodendaten == null || periodendaten.isEmpty()) {
            return 1;
        }

        List<LocalDate> sortiert = new ArrayList<>(periodendaten);
        Collections.sort(sortiert, Collections.reverseOrder());

        for (LocalDate periodenstart : sortiert) {
            if (!periodenstart.isAfter(datum)) {
                return (int) ChronoUnit.DAYS.between(periodenstart, datum) + 1;
            }
        }
        return 1;
    }

    private boolean pruefeZyklusRegularität(List<LocalDate> periodendaten) {
        if (periodendaten == null || periodendaten.size() < 3) {
            return true; // Zu wenig Daten für Bewertung
        }

        List<Long> zykluslängen = new ArrayList<>();
        List<LocalDate> sortiert = new ArrayList<>(periodendaten);
        Collections.sort(sortiert);

        for (int i = 1; i < sortiert.size(); i++) {
            long länge = ChronoUnit.DAYS.between(sortiert.get(i-1), sortiert.get(i));
            if (länge >= ZYKLUSLÄNGE_MIN && länge <= ZYKLUSLÄNGE_MAX) {
                zykluslängen.add(länge);
            }
        }

        if (zykluslängen.size() < 2) {
            return true;
        }

        // Berechne Standardabweichung
        double durchschnitt = zykluslängen.stream().mapToLong(Long::longValue).average().orElse(0);
        double varianz = zykluslängen.stream()
                .mapToDouble(l -> Math.pow(l - durchschnitt, 2))
                .average().orElse(0);
        double standardAbweichung = Math.sqrt(varianz);

        // Zyklus gilt als regular wenn Standardabweichung < 5 Tage
        return standardAbweichung < 5.0;
    }

    private float berechneTemperaturAbweichung(float temperatur, ZyklusPhase phase) {
        if (temperatur <= 0) return 0.0f;

        float referenz;
        switch (phase) {
            case MENSTRUATION:
            case FOLLIKELPHASE:
                referenz = (TEMP_FOLLIKEL_MIN + TEMP_FOLLIKEL_MAX) / 2;
                break;
            case LUTEALPHASE:
                referenz = (TEMP_LUTEAL_MIN + TEMP_LUTEAL_MAX) / 2;
                break;
            default:
                referenz = (TEMP_FOLLIKEL_MAX + TEMP_LUTEAL_MIN) / 2;
                break;
        }
        return temperatur - referenz;
    }

    private float berechnePulsAbweichung(int puls, ZyklusPhase phase) {
        if (puls <= 0) return 0.0f;

        float referenz = 70.0f; // Durchschnittlicher Ruhepuls
        switch (phase) {
            case LUTEALPHASE:
                referenz = 73.0f; // Leicht erhöht in Lutealphase
                break;
            default:
                referenz = 68.0f; // Baseline für andere Phasen
                break;
        }
        return ((float) puls - referenz) / referenz * 100; // Prozentuale Abweichung
    }

    private String generierePhasenBeschreibung(ZyklusPhase phase, int zyklusTag) {
        switch (phase) {
            case MENSTRUATION:
                return "Sie befinden sich in der Menstruationsphase (Tag " + zyklusTag +
                        "). Niedrige Hormonwerte sind normal, Puls und Temperatur sollten am niedrigsten sein.";
            case FOLLIKELPHASE:
                return "Sie sind in der Follikelphase (Tag " + zyklusTag +
                        "). Stabile, niedrige Werte für Puls und Temperatur sind zu erwarten.";
            case OVULATION:
                return "Sie befinden sich um den Eisprung (Tag " + zyklusTag +
                        "). Leichte Anstiege von Puls und Temperatur können beginnen.";
            case LUTEALPHASE:
                return "Sie sind in der Lutealphase (Tag " + zyklusTag +
                        "). Erhöhte Werte sind normal: +3-5% Puls, +0,3-0,7°C Temperatur.";
            default:
                return "Zyklusphase konnte nicht bestimmt werden. Sammeln Sie weitere Periodendaten.";
        }
    }

    private String generiereEmpfehlung(ZyklusPhase phase, AnalyseErgebnis.Bewertung temp,
                                       AnalyseErgebnis.Bewertung puls, AnalyseErgebnis.Bewertung spo2) {
        StringBuilder empfehlung = new StringBuilder();

        // Phasenspezifische Empfehlungen
        switch (phase) {
            case MENSTRUATION:
                empfehlung.append("Achten Sie auf ausreichend Ruhe und Flüssigkeitszufuhr. ");
                break;
            case FOLLIKELPHASE:
                empfehlung.append("Ideale Zeit für Sport und neue Aktivitäten. ");
                break;
            case OVULATION:
                empfehlung.append("Körper bereitet sich auf mögliche Schwangerschaft vor. ");
                break;
            case LUTEALPHASE:
                empfehlung.append("Leicht erhöhte Werte sind normal. Bei PMS-Symptomen: Entspannung bevorzugen. ");
                break;
        }

        // Bewertungsbasierte Empfehlungen
        if (temp == AnalyseErgebnis.Bewertung.AUFFAELLIG || puls == AnalyseErgebnis.Bewertung.AUFFAELLIG) {
            empfehlung.append("Bei anhaltenden Auffälligkeiten ärztlichen Rat einholen. ");
        }
        if (spo2 == AnalyseErgebnis.Bewertung.KRITISCH) {
            empfehlung.append("Niedrige Sauerstoffsättigung - sofort ärztliche Hilfe suchen! ");
        }

        return empfehlung.toString();
    }

    private String generiereMedizinischenHinweis(ZyklusPhase phase, AnalyseErgebnis.Bewertung temp,
                                                 AnalyseErgebnis.Bewertung puls, boolean istRegular) {
        StringBuilder hinweis = new StringBuilder();

        if (!istRegular) {
            hinweis.append("Unregelmäßiger Zyklus erkannt - Gynäkologe konsultieren. ");
        }

        if (phase == ZyklusPhase.LUTEALPHASE && temp == AnalyseErgebnis.Bewertung.AUFFAELLIG) {
            hinweis.append("Fehlender Temperaturanstieg könnte auf anovulatorischen Zyklus hindeuten. ");
        }

        if (puls == AnalyseErgebnis.Bewertung.KRITISCH) {
            hinweis.append("Stark erhöhter Ruhepuls - kardiologische Abklärung empfohlen. ");
        }

        return hinweis.toString();
    }

    /**
     * Findet den ersten Tag einer zusammenhängenden Periodensequenz
     */
    private LocalDate findeErstenPeriodentag(LocalDate datum, List<LocalDate> sortiertePerioden) {
        // Finde alle Periodentage vor dem gegebenen Datum
        List<LocalDate> relevantePeriodenTage = new ArrayList<>();
        for (LocalDate tag : sortiertePerioden) {
            if (!tag.isAfter(datum)) {
                relevantePeriodenTage.add(tag);
            }
        }

        if (relevantePeriodenTage.isEmpty()) return null;

        // Sortiere aufsteigend
        Collections.sort(relevantePeriodenTage);

        // Finde den ersten Tag der letzten zusammenhängenden Periode
        LocalDate letzterTag = relevantePeriodenTage.get(relevantePeriodenTage.size() - 1);
        LocalDate ersterTag = letzterTag;

        // Gehe rückwärts und finde aufeinanderfolgende Tage
        for (int i = relevantePeriodenTage.size() - 2; i >= 0; i--) {
            LocalDate vorheriger = relevantePeriodenTage.get(i);
            if (ChronoUnit.DAYS.between(vorheriger, ersterTag) == 1) {
                ersterTag = vorheriger;
            } else {
                break; // Lücke gefunden, stoppe
            }
        }

        return ersterTag;
    }
}