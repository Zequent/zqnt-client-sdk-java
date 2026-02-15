# Java Version Compatibility

## ☕ Overview

The Zequent Client SDK is compiled with **Java 21** to ensure maximum compatibility with customer applications.

---

## 🎯 Compatibility Matrix

| SDK Compiled With | Customer's Java Version | Compatible? |
|-------------------|------------------------|-------------|
| Java 21           | Java 21                | ✅ Yes      |
| Java 21           | Java 22                | ✅ Yes      |
| Java 21           | Java 23                | ✅ Yes      |
| Java 21           | Java 24                | ✅ Yes      |
| Java 21           | Java 25                | ✅ Yes      |
| Java 21           | Java 17                | ❌ No       |
| Java 21           | Java 11                | ❌ No       |

---

## 📖 How Java Bytecode Compatibility Works

### The Rule: **Backward Compatible, Not Forward Compatible**

When you compile Java code, you specify a **target version** (e.g., Java 21). The compiled bytecode can run on:
- ✅ **Same version** (Java 21)
- ✅ **Higher versions** (Java 22, 23, 24, 25+)
- ❌ **Lower versions** (Java 17, 11, 8)

### Why?

- Java is **backward compatible**: Newer JVMs can run older bytecode
- Java is **NOT forward compatible**: Older JVMs cannot run newer bytecode

---

## 🔧 Implementation

### SDK Configuration (`pom.xml`)

```xml
<properties>
    <!-- Override parent Java version for better customer compatibility -->
    <!-- SDK compiled with Java 21 works with Java 21, 22, 23, 24, 25+ -->
    <maven.compiler.release>21</maven.compiler.release>
</properties>
```

This configuration:
1. **Overrides** the parent POM's Java 25 setting
2. **Compiles** SDK with Java 21 bytecode
3. **Ensures** compatibility with Java 21+ customers

---

## 📋 Verification

### Check SDK Compiled Version

```bash
# Build the SDK
mvn clean package

# Check compiled bytecode version
javap -v target/classes/com/zequent/framework/client/sdk/ZequentClient.class | grep "major version"
```

Expected output:
```
major version: 65  // Java 21
```

Version mapping:
- `52` = Java 8
- `55` = Java 11
- `61` = Java 17
- `65` = Java 21
- `66` = Java 22
- `69` = Java 25

---

## 🚨 Common Issues

### Problem: Customer Gets `UnsupportedClassVersionError`

**Error:**
```
java.lang.UnsupportedClassVersionError:
com/zequent/framework/client/sdk/ZequentClient has been compiled by
a more recent version of the Java Runtime (class file version 65.0)
```

**Cause:** Customer is using Java 17 or older, but SDK is compiled with Java 21.

**Solution:** Customer must upgrade to Java 21 or higher.

---

## ✅ Best Practices

### For SDK Developers:

1. **Always compile with the LOWEST Java version you want to support**
   - If you want to support Java 17: compile with Java 17
   - If you want to support Java 21: compile with Java 21

2. **Document minimum Java version clearly**
   - In README
   - In release notes
   - In pom.xml properties

3. **Test with multiple Java versions**
   - Test with Java 21 (minimum)
   - Test with Java 25 (latest)
   - Ensure compatibility

### For Customers:

1. **Check your Java version:**
   ```bash
   java -version
   ```

2. **If below Java 21, upgrade:**
   - Download from: https://adoptium.net/
   - Or use SDKMAN: `sdk install java 21.0.1-tem`

---

## 📝 Summary

✅ **SDK compiled with Java 21**
✅ **Works with Java 21, 22, 23, 24, 25+**
✅ **Maximum customer compatibility**
✅ **Clear documentation**

Customers using Java 21+ can use the SDK without issues! 🎉
