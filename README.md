# Zequent Client SDK

> **Java Client Library** for interacting with Zequent Framework Services

## ⚠️ Important: This is a Library, Not a Standalone Application!

The **Zequent Client SDK** is a **Java library/dependency** that customers add to their applications (Spring Boot, Quarkus, etc.). It is **NOT** a standalone service or application that runs by itself.

## 🏗️ Architecture

```
┌─────────────────────────────────────┐
│   CUSTOMER'S APPLICATION            │
│   - Their REST API / Service        │
│   - Their Business Logic            │
│   - @Inject ZequentClient ←──────┐  │
└─────────────────────────────────────┘
                                     │
                            ┌────────┴────────┐
                            │  Zequent SDK    │
                            │  (This Library) │
                            └────────┬────────┘
                                     │
            ┌────────────────────────┼────────────────────────┐
            │                        │                        │
            ▼                        ▼                        ▼
┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐
│ Remote Control      │  │ Mission Autonomy    │  │ Live Data Service   │
│ Service (Port 8002) │  │ Service (Port 8004) │  │ (Port 8003)        │
└─────────────────────┘  └─────────────────────┘  └─────────────────────┘
```

## 🚀 For Customers: How to Use

### 1. Add Dependency to Your Project

```xml
<dependency>
    <groupId>com.zequent.framework.client.sdk</groupId>
    <artifactId>java-client-sdk</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. Setup for Your Framework

#### 🔹 Quarkus (Automatic via CDI)

**application.properties:**
```properties
zequent.remote-control-service.host=localhost
zequent.remote-control-service.port=8002
zequent.live-data-service.host=localhost
zequent.live-data-service.port=8003
zequent.connector-service.host=localhost
zequent.connector-service.port=8010
```

**Usage:**
```java
@ApplicationScoped
public class DroneService {
    @Inject
    ZequentClient zequent;  // ← Automatically configured!

    public void handleTelemetry() {
        zequent.liveData().streamTelemetryData();
    }

    public CompletableFuture<ConnectorResponse> findAsset(String sn) {
        ConnectorRequestContext context = ConnectorRequestContext.builder()
                .sn(sn)
                .tid(UUID.randomUUID().toString())
                .build();
        return zequent.connector().getAssetBySn(
                GetAssetBySnRequest.builder().context(context).build()
        );
    }
}
```

Payload commands use the stable logical command advertised by the Connector; vendor methods stay internal to the edge adapter:

```java
return zequent.connector().getAssetBySn(
        GetAssetBySnRequest.builder().context(context).build()
).thenCompose(assetResponse -> {
    AssetPayloadDTO payload = assetResponse.getAsset().getPayloads().stream()
            .filter(value -> "searchlight-1".equals(value.getExternalId()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Payload is not registered"));

    PayloadCommandDefinitionDTO command = payload.getCommands().stream()
            .filter(value -> "searchlight.mode.set".equals(value.getCommand()))
            .filter(value -> Boolean.TRUE.equals(value.getAvailable()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Command is not available"));

    return zequent.remoteControl().sendCustomCommand(CustomCommandRequest.builder()
            .sn(sn)
            .componentId(payload.getExternalId())
            .commandType(command.getCommand())
            .params(Map.of("mode", 1, "group", 0))
            .build());
});
```

The edge adapter resolves `commandType` to its registered `vendorMethod` and validates the payload again before execution.

**That's it!** The SDK auto-configures via `ZequentClientProducer` (CDI).

---

#### 🔹 Spring Boot (Simple Bean Configuration)

**Create a Configuration class:**
```java
@Configuration
public class ZequentConfig {

    @Bean
    public ZequentClient zequentClient() {
        // Uses defaults: localhost:8002, 8004, 8003, 8010
        return ZequentClient.builder()
                .remoteControl().done()
                .missionAutonomy().done()
                .liveData().done()
                .connector().done()
                .build();
    }
}
```

**Usage:**
```java
@Service
@RequiredArgsConstructor
public class LiveDataService {

    private final ZequentClient zequentClient;  // ← Injected by Spring

    public void handleTelemetry() {
        zequentClient.liveData().streamTelemetryData();
    }
}
```

**Optional - Override with Properties:**
```java
@Bean
public ZequentClient zequentClient(
        @Value("${zequent.remote-control.host:localhost}") String host,
        @Value("${zequent.remote-control.port:8002}") int port) {
    return ZequentClient.builder()
            .remoteControl().host(host).port(port).done()
            .missionAutonomy().done()
            .liveData().done()
            .connector().host("localhost").port(8010).done()
            .build();
}
```

**See detailed guide:** [`SPRING_BOOT_FINAL.md`](../../../docs/client-sdk/SPRING_BOOT_FINAL.md)

## 📚 Customer Documentation

### Quick Start Guides
- **[Spring Boot Integration](../../../docs/client-sdk/SPRING_BOOT_FINAL.md)** - 🔥 **START HERE for Spring Boot** - Simple, defaults-based setup
- **[Bean Configuration Guide](BEAN_CONFIGURATION.md)** - Advanced configuration options

### Reference
- **[Configuration Reference](CONFIGURATION.md)** - All available properties
- **[Quick Start Guide](../../../docs/QUICKSTART.md)** - 5-minute setup for any framework

## 🔧 For Developers: Building the SDK

### Build Library

```bash
mvn clean install
```

This creates a JAR that customers can add as a dependency.

### Run Tests

```bash
mvn test
```

### Deploy to Repository

```bash
mvn deploy
```

## 📦 What's Included

This SDK provides:
- `ZequentClient` - Main client interface
- Service interfaces (RemoteControl, MissionAutonomy, LiveData, Connector)
- Request/Response models
- Auto-configuration via CDI (Quarkus)
- gRPC channel management
- Resilience patterns (retry, circuit breaker)
- Load balancing & service discovery

## ☕ Java Version Compatibility

- **Compiled with:** Java 21
- **Compatible with:** Java 21, 22, 23, 24, 25+
- **Minimum required:** Java 21

The SDK is compiled with Java 21 for maximum customer compatibility. If your application uses Java 21 or higher, the SDK will work seamlessly.

## ✅ Features

✅ **Framework Agnostic** - Works with Spring Boot, Quarkus, Micronaut, etc.
✅ **Sensible Defaults** - Works out-of-the-box (localhost:8002/8004/8003/8010)
✅ **Property-Based Config** - Override via `application.properties` (optional)
✅ **Multi-Service Support** - Remote Control, Mission Autonomy, Live Data, Connector
✅ **Built-in Resilience** - Retry, Circuit Breaker, Timeouts
✅ **Load Balancing** - Round-robin, Least-requests
✅ **Service Discovery** - Stork support for Kubernetes
✅ **Simple Integration** - One `@Bean` method for Spring Boot, zero config for Quarkus

## 🚫 What This Is NOT

This SDK does **NOT**:
- ❌ Run as a standalone application
- ❌ Provide a REST API (customers build that)
- ❌ Include the actual Zequent services
- ❌ Need to be "started" or "deployed" separately

## 📝 License

Copyright © 2025 Zequent Framework
