package at.fhj.andrey.zyklustracker.datenbank;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import java.time.LocalDate;
import java.util.List;

/**
 * WohlbefindenEintrag - Entitätsklasse für tägliche Gesundheits- und Befindlichkeitsdaten
 *
 * Diese Klasse speichert alle Informationen, die eine Nutzerin täglich über ihr
 * Wohlbefinden und ihre Symptome erfassen kann. Pro Tag kann nur ein Eintrag existieren.
 *
 * Datenstruktur:
 * - Tabelle: "wohlbefinden_eintraege"
 * - Eindeutigkeit: Ein Datum = Ein Eintrag
 * - Update-Verhalten: Bestehende Einträge werden überschrieben
 *
 * Gespeicherte Informationen:
 * 1. Menstruationsdaten: Blutungsstärke und Charakter
 * 2. Schmerzbewertung: Von "Keine" bis "Krampfartig"
 * 3. Stimmung: Emotionale Verfassung mit Emoji-Unterstützung
 * 4. Begleitsymptome: Multiple-Choice-Liste häufiger Beschwerden
 * 5. Vitaldaten: Temperatur, Puls, SpO₂ (für zukünftige Sensor-Integration)
 *
 * Datenkonvertierung:
 * - LocalDate: Automatische String-Konvertierung via DatumKonverter
 * - List<String>: JSON-Serialisierung für Symptom-Listen
 *
 * Verwendung in der App:
 * - WohlbefindenActivity: Haupteingabe-Interface
 * - StatistikActivity: Auswertung und Trends
 * - Zukünftig: Integration mit Wearables (Amazfit Band 5)
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
@Entity(tableName = "wohlbefinden_eintraege")
@TypeConverters(DatumKonverter.class)
public class WohlbefindenEintrag {

    /**
     * Primärschlüssel der Entität.
     * Auto-generiert von Room, nicht manuell setzen.
     */
    @PrimaryKey(autoGenerate = true)
    private int id;

    /**
     * Datum des Eintrags - fungiert als logischer Primärschlüssel.
     *
     * Geschäftslogik:
     * - Pro Tag kann nur ein Eintrag existieren
     * - Bei erneuter Eingabe wird der bestehende Eintrag aktualisiert
     * - Standardwert: Aktuelles Datum bei Erstellung
     */
    private LocalDate datum;

    /**
     * Intensität der Menstruationsblutung.
     *
     * Mögliche Werte:
     * - "Sehr leicht": Minimale Blutung, Slipeinlage ausreichend
     * - "Leicht": Schwache Blutung, normale Binde/Tampon
     * - "Mittel": Normale Blutungsstärke
     * - "Stark": Starke Blutung, häufiger Wechsel nötig
     * - null/empty: Keine Angabe oder keine Blutung
     */
    private String blutungsstaerke;

    /**
     * Subjektive Schmerzintensität während der Menstruation.
     *
     * Schmerzskala:
     * - "Keine": Keine spürbaren Schmerzen
     * - "Leicht": Erträgliche, gelegentliche Schmerzen
     * - "Mittel": Spürbare Schmerzen, beeinträchtigen leicht
     * - "Stark": Starke Schmerzen, deutliche Beeinträchtigung
     * - "Krampfartig": Intensive Krämpfe, starke Beeinträchtigung
     * - null/empty: Keine Angabe
     */
    private String schmerzLevel;

    /**
     * Emotionale Verfassung und Stimmung.
     *
     * Stimmungsskala mit Emojis:
     * - "😀 Sehr gut": Sehr positive Stimmung
     * - "🙂 Gut": Gute Stimmung
     * - "😐 Mittel": Neutrale Stimmung
     * - "🙁 Schlecht": Schlechte Stimmung
     * - null/empty: Keine Angabe
     *
     * Hinweis: Emojis werden bei Statistiken automatisch entfernt
     */
    private String stimmung;

    /**
     * Liste der Begleitsymptome als JSON-Array.
     *
     * Häufige Symptome:
     * - "Kopfschmerzen": Cephalgie verschiedener Intensität
     * - "Übelkeit": Nausea, Unwohlsein
     * - "Müdigkeit": Fatigue, Energiemangel
     * - "Rückenschmerzen": Dorsalgie, unterer Rücken
     * - "empfindliche Brüste": Mastalgie, Spannungsgefühl
     *
     * Technische Umsetzung:
     * - Speicherung als JSON-String: ["Kopfschmerzen", "Übelkeit"]
     * - Automatische Konvertierung durch DatumKonverter
     * - Mehrfachauswahl möglich
     */
    private List<String> symptome;

    // ===== VITALDATEN FÜR SENSOR-INTEGRATION =====
    // Diese Felder sind für die zukünftige Integration mit dem Amazfit Band 5 vorgesehen

    /**
     * Körpertemperatur in Grad Celsius.
     *
     * Zukünftige Verwendung:
     * - Automatische Erfassung via Amazfit Band 5
     * - Basaltemperatur für Eisprung-Erkennung
     * - Trend-Analysen für Zyklusphase-Bestimmung
     *
     * Normbereich: 36.0 - 37.5°C
     * Speicherung: Float für Dezimalstellen (z.B. 36.7°C)
     */
    private Float temperatur;

    /**
     * Ruhepuls in Schlägen pro Minute.
     *
     * Zukünftige Verwendung:
     * - Kontinuierliche Messung via Wearable
     * - Zyklusabhängige Schwankungen erkennen
     * - Stressindikator und Gesundheitsmonitoring
     *
     * Normbereich: 60-100 bpm (altersabhängig)
     * Speicherung: Integer für ganze Schläge
     */
    private Integer puls;

    /**
     * Sauerstoffsättigung im Blut (SpO₂) in Prozent.
     *
     * Zukünftige Verwendung:
     * - Pulsoxymetrie via Amazfit Band 5
     * - Früherkennung von Atemwegsinfekten
     * - Allgemeine Gesundheitsüberwachung
     *
     * Normbereich: 95-100%
     * Speicherung: Integer für Prozent-Werte
     */
    private Integer spo2;

    // ===== KONSTRUKTOREN =====

    /**
     * Leerer Konstruktor - erforderlich für Room.
     * Room verwendet Reflection zur Objekterstellung.
     */
    public WohlbefindenEintrag() {}

    /**
     * Konstruktor für die Erstellung mit Datum.
     * Standardkonstruktor für neue Einträge.
     *
     * @param datum Das Datum des Wohlbefinden-Eintrags
     */
    public WohlbefindenEintrag(LocalDate datum) {
        this.datum = datum;
    }

    // ===== GETTER UND SETTER METHODEN =====
    // Room verwendet diese Methoden für Datenbankoperationen

    /**
     * Gibt die eindeutige ID des Eintrags zurück.
     *
     * @return Auto-generierte Datenbank-ID
     */
    public int getId() {
        return id;
    }

    /**
     * Setzt die ID des Eintrags.
     * Normalerweise nur von Room verwendet.
     *
     * @param id Die zu setzende ID
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Gibt das Datum des Eintrags zurück.
     *
     * @return LocalDate des Wohlbefinden-Eintrags
     */
    public LocalDate getDatum() {
        return datum;
    }

    /**
     * Setzt das Datum des Eintrags.
     *
     * @param datum Das zu setzende Datum
     */
    public void setDatum(LocalDate datum) {
        this.datum = datum;
    }

    /**
     * Gibt die Blutungsstärke zurück.
     *
     * @return String mit Blutungsintensität oder null
     */
    public String getBlutungsstaerke() {
        return blutungsstaerke;
    }

    /**
     * Setzt die Blutungsstärke.
     *
     * @param blutungsstaerke Die Blutungsintensität
     */
    public void setBlutungsstaerke(String blutungsstaerke) {
        this.blutungsstaerke = blutungsstaerke;
    }

    /**
     * Gibt das Schmerzlevel zurück.
     *
     * @return String mit Schmerzintensität oder null
     */
    public String getSchmerzLevel() {
        return schmerzLevel;
    }

    /**
     * Setzt das Schmerzlevel.
     *
     * @param schmerzLevel Die Schmerzintensität
     */
    public void setSchmerzLevel(String schmerzLevel) {
        this.schmerzLevel = schmerzLevel;
    }

    /**
     * Gibt die Stimmung zurück.
     *
     * @return String mit Stimmungsbezeichnung (inklusive Emoji) oder null
     */
    public String getStimmung() {
        return stimmung;
    }

    /**
     * Setzt die Stimmung.
     *
     * @param stimmung Die Stimmungsbezeichnung
     */
    public void setStimmung(String stimmung) {
        this.stimmung = stimmung;
    }

    /**
     * Gibt die Liste der Symptome zurück.
     *
     * @return Liste von Symptom-Strings oder null
     */
    public List<String> getSymptome() {
        return symptome;
    }

    /**
     * Setzt die Liste der Symptome.
     *
     * @param symptome Die Liste der Begleitsymptome
     */
    public void setSymptome(List<String> symptome) {
        this.symptome = symptome;
    }

    /**
     * Gibt die Körpertemperatur zurück.
     *
     * @return Temperatur in °C oder null
     */
    public Float getTemperatur() {
        return temperatur;
    }

    /**
     * Setzt die Körpertemperatur.
     *
     * @param temperatur Temperatur in °C
     */
    public void setTemperatur(Float temperatur) {
        this.temperatur = temperatur;
    }

    /**
     * Gibt den Puls zurück.
     *
     * @return Herzfrequenz in bpm oder null
     */
    public Integer getPuls() {
        return puls;
    }

    /**
     * Setzt den Puls.
     *
     * @param puls Herzfrequenz in bpm
     */
    public void setPuls(Integer puls) {
        this.puls = puls;
    }

    /**
     * Gibt die Sauerstoffsättigung zurück.
     *
     * @return SpO₂ in % oder null
     */
    public Integer getSpo2() {
        return spo2;
    }

    /**
     * Setzt die Sauerstoffsättigung.
     *
     * @param spo2 SpO₂ in %
     */
    public void setSpo2(Integer spo2) {
        this.spo2 = spo2;
    }

    /**
     * Überschreibt toString() für bessere Debugging-Ausgabe.
     *
     * @return String-Repräsentation des Objekts
     */
    @Override
    public String toString() {
        return "WohlbefindenEintrag{" +
                "id=" + id +
                ", datum=" + datum +
                ", blutungsstaerke='" + blutungsstaerke + '\'' +
                ", schmerzLevel='" + schmerzLevel + '\'' +
                ", stimmung='" + stimmung + '\'' +
                ", symptome=" + symptome +
                ", temperatur=" + temperatur +
                ", puls=" + puls +
                ", spo2=" + spo2 +
                '}';
    }
}