# ZyklusTracker (FHJ Projekt SS2025)

## Projektbeschreibung

ZyklusTracker ist eine fortschrittliche Android-App zur wissenschaftlich fundierten Selbstbeobachtung des weiblichen Menstruationszyklus. Die Anwendung kombiniert traditionelle Eingabemethoden mit moderner Health Connect-Integration zur automatischen Erfassung von Vitaldaten (Puls, Temperatur, SpO₂) und bietet eine umfassende Analyse der verschiedenen Zyklusphasen.

## Hauptfeatures

### Vollständig implementiert

- Kalenderansicht mit farblicher Darstellung der Zyklusphasen
- Dialogfenster zur Eingabe der Periodentage
- Automatische Zyklusberechnung von:
  - Eisprung (ca. 14 Tage vor nächster Periode)
  - Fruchtbarer Phase (ca. 5 Tage vor Eisprung)
  - Zukünftigen Perioden (prognostische Berechnung)
- Health Connect Integration für automatische Sensordatenerfassung
- Room-Datenbank zur dauerhaften Speicherung aller Daten
- Wohlbefinden-Tracking mit umfassendem Symptom-Logging
- Zyklusanalyse-Engine mit medizinisch fundierter Bewertungslogik
- Statistik-Dashboard mit Trend-Auswertungen

### Wissenschaftliche Grundlage

Die App basiert auf medizinischen Forschungsergebnissen zu Vitaldaten-Schwankungen während des Menstruationszyklus:

- **Menstruation (Tage 1-5)**: Niedrigste Werte für Puls und Temperatur
- **Follikelphase (Tage 6-13)**: Stabile, niedrige Werte
- **Ovulation (Tage 12-16)**: Beginnender Anstieg der Vitaldaten
- **Lutealphase (Tage 17-28)**: Höchste Werte (+3-5% Puls, +0.3-0.7°C Temperatur)

## Architektur & Technologien

### Core Technologies

| Komponente              | Technologie                    |
|------------------------|--------------------------------|
| **Programmiersprache** | Java (Hauptentwicklung)        |
| **UI Framework**       | Kotlin (Health Connect Module) |
| **IDE**                | Android Studio Narwhal (2025.1.2) |
| **Build System**       | Gradle (Kotlin DSL)           |

### Frameworks & Libraries

| Bereich           | Framework/Library              |
|------------------|--------------------------------|
| **UI Design**     | Material Components 3          |
| **Layout**        | ConstraintLayout               |
| **Kalender**      | kizitonwose/calendar-view      |
| **Datenbank**     | Room Database + TypeConverters |
| **Health Data**   | Health Connect API (Kotlin)   |
| **Dokumentation** | Dokka (GitHub Pages)          |


## Funktionsübersicht

### Health Connect Integration

- Automatische Datenerfassung von kompatiblen Wearables
- Validierung der Sensordaten auf medizinische Plausibilität
- Real-time Updates der UI bei neuen Sensordaten
- Fallback-Mechanismen bei fehlenden Berechtigungen

### Zyklusanalyse-Engine

- Phasenerkennung basierend auf Periodendaten
- Bewertungssystem (Normal, Grenzwertig, Auffällig, Kritisch)
- Temperaturabweichungen mit Referenzwert-Vergleich
- Pulsfrequenz-Analyse mit prozentualen Abweichungen
- SpO₂-Monitoring für Atemwegs-/Kreislaufgesundheit

### Benutzeroberfläche

```
ZyklusTracker/
├── ZyklusActivity (Hauptansicht mit Kalender & Live-Daten)
├── WohlbefindenActivity (Symptom-Eingabe & Bewertungen)
├── StatistikActivity (Trend-Auswertungen & Diagramme)
└── Navigation (Material Bottom Navigation)
```

## Implementierungsdetails

### Health Connect Workflow

1. Berechtigungsprüfung für Sensor-Zugriff
2. Datenabfrage der letzten Stunde
3. Validierung auf realistische Werte
4. Room-Speicherung mit Timestamp
5. UI-Update in Real-time


### Voraussetzungen

- Android Studio Narwhal (2025.1.2+)
- Android SDK 34+
- Health Connect App auf dem Zielgerät
- Java 17 für Gradle-Build


## Links & Dokumentation

- **API-Dokumentation**: [https://andreyeskin.github.io/zyklustracker](https://andreyeskin.github.io/zyklustracker)
- **GitHub Repository**: [https://github.com/andreyeskin/zyklustracker](https://github.com/andreyeskin/zyklustracker)
- **Health Connect Developer Docs**: [Android Health Platform](https://developer.android.com/health-and-fitness/guides/health-connect)

## Autor & Projekt

**Andrey Eskin**  
Studiengang: Gesundheitsinformatik  
FH JOANNEUM Graz  
Sommersemester 2025  

**Projekttyp**: Bachelor-Semesterprojekt  
**Schwerpunkt**: Mobile Health (mHealth) & Zyklusmedizin  
**Betreuung**: FH JOANNEUM eHealth

---
*Entwickelt für moderne Frauengesundheit und wissenschaftlich fundierte Zyklusbeobachtung.*

