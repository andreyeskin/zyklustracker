package at.fhj.andrey.zyklustracker.zyklusanalyse;

/**
 * AnalyseErgebnis - Datenmodell für Zyklusanalyse-Ergebnisse
 *
 * Diese Klasse kapselt die Ergebnisse der Zyklusanalyse und Sensor-Bewertung.
 * Sie wird von ZyklusPhaseBerechnung verwendet, um strukturierte Rückmeldungen
 * über den aktuellen Zyklusstatus und die Bewertung der Sensor-Werte zu geben.
 *
 * Design-Pattern: Value Object für unveränderliche Analyse-Ergebnisse
 *
 * @author Andrey Eskin
 * @version 1.0
 * @since Mai 2025
 */
public class AnalyseErgebnis {

    /**
     * Bewertungskategorien für Sensor-Werte im Zykluskontext
     */
    public enum Bewertung {
        NORMAL,           // Werte entsprechen der erwarteten Zyklusphase
        GRENZWERTIG,      // Leichte Abweichungen, noch im akzeptablen Bereich
        AUFFAELLIG,       // Deutliche Abweichungen, Beobachtung empfohlen
        KRITISCH          // Starke Abweichungen, ärztliche Konsultation ratsam
    }

    // Grundlegende Analyseergebnisse
    private final ZyklusPhaseBerechnung.ZyklusPhase aktuellePhase;
    private final int zyklusTag;
    private final boolean istZyklusRegular;

    // Sensor-spezifische Bewertungen
    private final Bewertung temperaturBewertung;
    private final Bewertung pulsBewertung;
    private final Bewertung spo2Bewertung;
    private final Bewertung gesamtBewertung;

    // Textuelle Interpretationen für UI
    private final String phasenBeschreibung;
    private final String empfehlung;
    private final String medizinischerHinweis;

    // Numerische Details für weitere Verarbeitung
    private final float temperaturAbweichung;  // in °C vom Phasenmittelwert
    private final float pulsAbweichung;        // in % vom Phasenmittelwert
    private final int spo2Wert;               // aktueller SpO2-Wert

    /**
     * Vollständiger Konstruktor für detaillierte Analyse-Ergebnisse
     */
    public AnalyseErgebnis(ZyklusPhaseBerechnung.ZyklusPhase aktuellePhase,
                           int zyklusTag,
                           boolean istZyklusRegular,
                           Bewertung temperaturBewertung,
                           Bewertung pulsBewertung,
                           Bewertung spo2Bewertung,
                           String phasenBeschreibung,
                           String empfehlung,
                           String medizinischerHinweis,
                           float temperaturAbweichung,
                           float pulsAbweichung,
                           int spo2Wert) {

        this.aktuellePhase = aktuellePhase;
        this.zyklusTag = zyklusTag;
        this.istZyklusRegular = istZyklusRegular;
        this.temperaturBewertung = temperaturBewertung;
        this.pulsBewertung = pulsBewertung;
        this.spo2Bewertung = spo2Bewertung;
        this.phasenBeschreibung = phasenBeschreibung;
        this.empfehlung = empfehlung;
        this.medizinischerHinweis = medizinischerHinweis;
        this.temperaturAbweichung = temperaturAbweichung;
        this.pulsAbweichung = pulsAbweichung;
        this.spo2Wert = spo2Wert;

        // Gesamtbewertung als schlechteste Einzelbewertung
        this.gesamtBewertung = berechneGesamtbewertung();
    }

    /**
     * Vereinfachter Konstruktor für grundlegende Ergebnisse
     */
    public AnalyseErgebnis(ZyklusPhaseBerechnung.ZyklusPhase aktuellePhase,
                           int zyklusTag,
                           String phasenBeschreibung) {
        this(aktuellePhase, zyklusTag, true,
                Bewertung.NORMAL, Bewertung.NORMAL, Bewertung.NORMAL,
                phasenBeschreibung, "Alle Werte im Normalbereich", "",
                0.0f, 0.0f, 98);
    }

    /**
     * Berechnet die Gesamtbewertung basierend auf allen Einzelbewertungen
     */
    private Bewertung berechneGesamtbewertung() {
        Bewertung[] bewertungen = {temperaturBewertung, pulsBewertung, spo2Bewertung};

        // Rückgabe der schlechtesten (höchsten) Bewertung
        for (Bewertung b : new Bewertung[]{Bewertung.KRITISCH, Bewertung.AUFFAELLIG,
                Bewertung.GRENZWERTIG, Bewertung.NORMAL}) {
            for (Bewertung bewertung : bewertungen) {
                if (bewertung == b) {
                    return b;
                }
            }
        }
        return Bewertung.NORMAL;
    }

    // ===== GETTER-METHODEN =====

    public ZyklusPhaseBerechnung.ZyklusPhase getAktuellePhase() {
        return aktuellePhase;
    }

    public int getZyklusTag() {
        return zyklusTag;
    }

    public boolean istZyklusRegular() {
        return istZyklusRegular;
    }

    public Bewertung getTemperaturBewertung() {
        return temperaturBewertung;
    }

    public Bewertung getPulsBewertung() {
        return pulsBewertung;
    }

    public Bewertung getSpo2Bewertung() {
        return spo2Bewertung;
    }

    public Bewertung getGesamtBewertung() {
        return gesamtBewertung;
    }

    public String getPhasenBeschreibung() {
        return phasenBeschreibung;
    }

    public String getEmpfehlung() {
        return empfehlung;
    }

    public String getMedizinischerHinweis() {
        return medizinischerHinweis;
    }

    public float getTemperaturAbweichung() {
        return temperaturAbweichung;
    }

    public float getPulsAbweichung() {
        return pulsAbweichung;
    }

    public int getSpo2Wert() {
        return spo2Wert;
    }

    // ===== HILFSMETHODEN FÜR UI =====

    /**
     * Gibt eine für die UI geeignete Farbe für die Gesamtbewertung zurück
     */
    public String getBewertungsfarbe() {
        switch (gesamtBewertung) {
            case NORMAL:
                return "#4CAF50";      // Grün
            case GRENZWERTIG:
                return "#FF9800";      // Orange
            case AUFFAELLIG:
                return "#F44336";      // Rot
            case KRITISCH:
                return "#D32F2F";      // Dunkelrot
            default:
                return "#9E9E9E";      // Grau
        }
    }

    /**
     * Gibt ein passendes Icon für die aktuelle Zyklusphase zurück
     */
    public String getPhasenIcon() {
        switch (aktuellePhase) {
            case MENSTRUATION:
                return "🩸";
            case FOLLIKELPHASE:
                return "🌱";
            case OVULATION:
                return "🥚";
            case LUTEALPHASE:
                return "🌸";
            default:
                return "📊";
        }
    }

    /**
     * Erstellt eine zusammenfassende Beschreibung für die UI
     */
    public String getZusammenfassung() {
        StringBuilder sb = new StringBuilder();
        sb.append(getPhasenIcon()).append(" ");
        sb.append(aktuellePhase.name()).append(" (Tag ").append(zyklusTag).append(")\n");
        sb.append(phasenBeschreibung);

        if (!empfehlung.isEmpty()) {
            sb.append("\n\n💡 ").append(empfehlung);
        }

        if (!medizinischerHinweis.isEmpty()) {
            sb.append("\n\n⚕️ ").append(medizinischerHinweis);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "AnalyseErgebnis{" +
                "phase=" + aktuellePhase +
                ", tag=" + zyklusTag +
                ", gesamtBewertung=" + gesamtBewertung +
                ", regular=" + istZyklusRegular +
                '}';
    }
}