# Framework Independence

Das Zequent Client SDK ist jetzt **framework-agnostisch** und funktioniert mit jedem Java-Framework.

---

## ✅ Was wurde geändert

### Vorher (Framework-spezifisch):
- ❌ `quarkus-arc` (Quarkus CDI)
- ❌ `quarkus-grpc` (Quarkus gRPC wrapper)
- ❌ `quarkus-smallrye-stork` (Quarkus Stork integration)
- ❌ `quarkus-junit5` (Quarkus Tests)

**Problem:** Funktionierte nur mit Quarkus!

### Nachher (Framework-agnostisch):
- ✅ `jakarta.inject-api` (Standard CDI Annotations)
- ✅ `jakarta.enterprise.cdi-api` (Standard CDI API)
- ✅ `grpc-netty` (Pure gRPC, kein Framework)
- ✅ `grpc-protobuf` (Standard Protobuf)
- ✅ `grpc-stub` (Standard gRPC stubs)
- ✅ `stork-core` (Standalone, optional)
- ✅ `junit-jupiter` (Standard JUnit 5)

**Ergebnis:** Funktioniert mit **allen** Java-Frameworks!

---

## 🎯 Kompatibilität

### Unterstützte Frameworks:

| Framework | Status | Notes |
|-----------|--------|-------|
| Spring Boot | ✅ | Bean Configuration via `@Bean` |
| Quarkus | ✅ | Auto-Configuration via CDI Producer |
| Micronaut | ✅ | Bean Configuration via `@Factory` |
| Jakarta EE | ✅ | CDI `@Produces` |
| Plain Java | ✅ | Manual `ZequentClient.builder()` |

---

## 📦 Dependencies Breakdown

### Runtime Dependencies (kompiliert ins JAR):
```xml
<!-- gRPC (Pure Java) -->
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty</artifactId>
    <version>1.62.2</version>
</dependency>
```

**Warum:** gRPC braucht kein Framework, funktioniert überall.

### Provided Dependencies (vom Kunden bereitgestellt):
```xml
<!-- Jakarta CDI API -->
<dependency>
    <groupId>jakarta.inject</groupId>
    <artifactId>jakarta.inject-api</artifactId>
    <version>2.0.1</version>
    <scope>provided</scope>
</dependency>
```

**Warum:** Jedes Framework (Spring, Quarkus, etc.) bringt seine eigene CDI-Implementierung mit.

### Optional Dependencies (bei Bedarf):
```xml
<!-- Stork für Service Discovery -->
<dependency>
    <groupId>io.smallrye.stork</groupId>
    <artifactId>stork-core</artifactId>
    <version>2.7.1</version>
    <optional>true</optional>
</dependency>
```

**Warum:** Nur wenn Kunde Service Discovery braucht.

---

## 🔍 Annotations Verwendung

### Jakarta Standard Annotations (Framework-agnostisch):

```java
import jakarta.inject.Inject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
```

Diese Annotations funktionieren in:
- ✅ Quarkus (via Quarkus Arc)
- ✅ Spring Boot (via Spring Context)
- ✅ Micronaut (via Micronaut Inject)
- ✅ Jakarta EE (via Weld, OpenWebBeans, etc.)

---

## 💡 Wie funktioniert das?

### 1. Standard Annotations
Das SDK verwendet **nur** Jakarta Standard Annotations (`@Inject`, `@ApplicationScoped`, `@Produces`).

### 2. Provided Scope
Diese Annotations sind `scope=provided`, d.h.:
- SDK kompiliert gegen die API
- Kunde bringt die Implementierung mit (Spring, Quarkus, etc.)

### 3. Framework-spezifische Implementierung
Jedes Framework hat seine eigene CDI-Implementierung:
- **Quarkus**: Arc
- **Spring Boot**: Spring Context (interpretiert Jakarta Annotations)
- **Micronaut**: Micronaut Inject
- **Jakarta EE**: Weld, OpenWebBeans

---

## 📋 Verification

### Check Dependencies (keine Framework-spezifischen):
```bash
mvn dependency:tree | grep -E "quarkus|spring|micronaut"
```

Expected: **Keine** framework-spezifischen Dependencies außer `scope=provided` oder `scope=test`

### Build SDK:
```bash
mvn clean install
```

### Test in verschiedenen Frameworks:
```bash
# Quarkus Projekt
mvn quarkus:dev

# Spring Boot Projekt
mvn spring-boot:run

# Plain Java
java -jar customer-app.jar
```

---

## ✅ Benefits

### Für SDK Entwickler:
- ✅ Weniger Framework-spezifischer Code
- ✅ Einfachere Wartung
- ✅ Breitere Kundenbasis

### Für Kunden:
- ✅ Funktioniert in jedem Framework
- ✅ Keine Framework-Lock-in
- ✅ Kleinere Dependency-Tree (keine redundanten Framework-Dependencies)

---

## 🚨 Breaking Changes

### Migration Guide für existierende Kunden:

**Vorher (Quarkus-spezifisch):**
```xml
<!-- Quarkus stellt diese Dependencies automatisch bereit -->
```

**Nachher (Framework-agnostisch):**
```xml
<!-- Kein Unterschied für Quarkus Kunden! -->
<!-- Quarkus stellt weiterhin jakarta.inject-api bereit -->
```

**Spring Boot Kunden müssen nichts ändern:**
```xml
<!-- Spring Boot stellt jakarta.inject-api automatisch bereit -->
```

**Plain Java Kunden benötigen:**
```xml
<dependency>
    <groupId>jakarta.inject</groupId>
    <artifactId>jakarta.inject-api</artifactId>
    <version>2.0.1</version>
</dependency>
```

---

## 📝 Summary

✅ **Framework-agnostisch**
✅ **Pure Java gRPC**
✅ **Jakarta Standard Annotations**
✅ **Funktioniert überall**

Das SDK ist jetzt ein **echtes framework-unabhängiges Library**! 🎉
