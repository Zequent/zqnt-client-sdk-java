# Public Release Checklist

Verwende diese Checkliste vor der Veröffentlichung der SDK als öffentlich.

## 📋 Pre-Release Checks

### Code Quality
- [ ] Alle Tests bestanden (`./mvnw test`)
- [ ] Build erfolgreich (`./mvnw clean package`)
- [ ] Keine Compiler-Warnings
- [ ] Code-Review durchgeführt
- [ ] Code-Style konsistent (Formatierung, Naming)

### Documentation
- [ ] README.md aktualisiert und vollständig
- [ ] CONTRIBUTING.md für Contributor:innen
- [ ] CHANGELOG.md mit Release-Notes
- [ ] Alle öffentlichen APIs haben JavaDoc
- [ ] Beispiele funktionieren und sind aktuell
- [ ] API-Dokumentation aktuell

### Dependencies
- [ ] Alle Dependencies haben zulässige Lizenzen
- [ ] Keine bekannten Security-Issues (`mvn dependency-check:check`)
- [ ] Abhängigkeiten von `com.zequent.framework` als `<optional>true</optional>` gekennzeichnet
- [ ] Keine internen/private Dependencies

### Security & Privacy
- [ ] Keine hartcodierten Credentials/Secrets im Code
- [ ] Keine internen URLs/Pfade in öffentlichen Klassen
- [ ] Keine Testdaten mit echten Credentials
- [ ] LICENSE-Datei vorhanden und korrekt
- [ ] SECURITY.md eingerichtet
- [ ] .gitignore korrekt konfiguriert

### Repository Setup
- [ ] Repository öffentlich auf GitHub/GitLab
- [ ] Repository-Beschreibung aussagekräftig
- [ ] README auf Hauptseite sichtbar
- [ ] Topics/Tags gesetzt (java, grpc, sdk, etc.)
- [ ] Branching-Strategie definiert (main/develop/feature branches)

### Build & Release
- [ ] Maven GPG-Plugin konfiguriert
- [ ] Maven Source & Javadoc Plugin konfiguriert
- [ ] pom.xml vollständig ausgefüllt:
  - [ ] GroupId, ArtifactId, Version
  - [ ] Name, Description
  - [ ] URL, License, Developers, SCM
  - [ ] DistributionManagement konfiguriert
- [ ] Version-Nummer in pom.xml aktuell
- [ ] Tag in Git erstellt (v1.0.0 format)

### POM Configuration Verification
```bash
# Checklist-Punkte prüfen:
./mvnw help:describe # Verify pom.xml structure
./mvnw dependency:tree # Check dependency tree
./mvnw validate # Validate POM syntax
```

## 🚀 Release Steps

1. [ ] **Version aktualisieren**
   ```bash
   ./mvnw versions:set -DnewVersion=1.0.0
   ```

2. [ ] **Tests ausführen**
   ```bash
   ./mvnw clean test
   ```

3. [ ] **Build erstellen**
   ```bash
   ./mvnw clean package
   ```

4. [ ] **Commit & Tag erstellen**
   ```bash
   git add .
   git commit -m "Release 1.0.0"
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin main --tags
   ```

5. [ ] **Zu Maven Central deployen**
   ```bash
   ./mvnw clean deploy
   ```

6. [ ] **Staging Repository freigeben**
   - Login zu https://s01.oss.sonatype.org
   - Repository "Close" → "Release"

7. [ ] **Publication verifizieren**
   - [ ] Nach 30min in Maven Central sichtbar
   - [ ] `mvn dependency:copy` funktioniert mit neuer Version
   - [ ] GitHub Release erstellen mit Changelog

## 🔍 Häufige Fehler vermeiden

### ❌ Fehler: Interne Dependencies sichtbar gemacht
**Fehler**: `com.zequent.framework:utils` ist nicht öffentlich
```xml
<!-- FALSCH -->
<dependency>
    <groupId>com.zequent.framework</groupId>
    <artifactId>utils</artifactId>
</dependency>
```
**Lösung**: Als optional kennzeichnen
```xml
<!-- RICHTIG -->
<dependency>
    <groupId>com.zequent.framework</groupId>
    <artifactId>utils</artifactId>
    <optional>true</optional>
</dependency>
```

### ❌ Fehler: Interne URLs/Konfiguration in Code
**Fehler**: Hardcodierte interne IPs oder Hosts
```java
// FALSCH - nie commiten!
String serviceUrl = "http://internal-zequent-server:8080";
```
**Lösung**: Konfigurierbar machen
```java
// RICHTIG
@ConfigProperty(name = "zequent.service.url")
String serviceUrl;
```

### ❌ Fehler: Keine Quellen/Javadoc JAR
**Deploy schlägt fehl**: Maven Central verlangt Source + Javadoc
**Lösung**: Plugins in build konfigurieren (bereits in pom.xml gemacht)

### ❌ Fehler: GPG-Signatur fehlt
**Deploy schlägt fehl**: Keine signed JAR
**Lösung**:
```bash
export GPG_TTY=$(tty)  # Linux/Mac
gpg-agent  # Start GPG Agent
./mvnw deploy  # Wird nach Passwort fragen
```

### ❌ Fehler: Wrong License
**Fehler**: Private interne License statt Apache 2.0
**Lösung**: COPYING-Datei checkent oder LICENSE prüfen

## 📊 Nach Release

- [ ] GitHub Release Notes erstellen
- [ ] Maven Central URL dokumentiert
- [ ] Installation-Dokumentation aktualisiert
- [ ] ChangeLog/Release Notes aktualisiert
- [ ] Interne Teams über Public Release informiert
- [ ] Monitoring für Bug-Reports aufsetzen

## 🎯 Multi-Maven-Module zu SDK umwandeln

Falls du **nur einen Teil** des Multi-Module-Projekts publishen möchtest:

1. **Neues Repository erstellen**: `zqnt-client-sdk-java`
2. **Modul separieren**: Nur SDK Code kopieren, nicht die anderen Module
3. **Dependencies neu evaluieren**: Was ist wirklich notwendig?
4. **Interne Refs löschen**: Alle Imports zu anderen Modulen prüfen
5. **Integration Tests anpassen**: Nur die SDK Tests inkludieren

Beispielstruktur für monolit → SDK:
```
Original Projekt (Multi-Module):
├── core/
├── sdk/          ← THIS
├── examples/
└── tests/

SDK Repository:
└── zqnt-client-sdk-java/  (nur SDK Inhalte)
    ├── src/
    ├── pom.xml
    ├── LICENSE
    └── README.md
```

## 📞 Support & Kontakt

Bei Problemen:
- GitHub Issues für Bug Reports
- security@zequent.com für Security Issues
- dev@zequent.com für allgemeine Fragen
