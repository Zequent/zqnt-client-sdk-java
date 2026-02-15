# Publishing to Maven Central

This guide explains how to publish the Zequent Java Client SDK to Maven Central Repository.

## Prerequisites

1. **GPG Key** for signing artifacts
2. **OSSRH Account** (Sonatype) - sign up at https://issues.sonatype.org
3. **Maven Settings** configured with credentials
4. Sontype account namespace approved for `com.zequent.framework.client.sdk`

## Step 1: Setup GPG

### Generate GPG Key (if you don't have one)
```bash
gpg --full-generate-key
# Select: RSA and RSA (4096 bits)
# Email: dev@zequent.com
```

### Export your keys
```bash
# List your keys
gpg --list-keys

# Export public key
gpg --armor --export YOUR_KEY_ID > public.asc

# Export private key (KEEP THIS SAFE!)
gpg --armor --export-secret-keys YOUR_KEY_ID > private.asc
```

### Upload public key to keyserver
```bash
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

## Step 2: Configure Maven

Edit `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <!-- OSSRH (Sonatype) credentials -->
    <server>
      <id>ossrh</id>
      <username>YOUR_SONATYPE_USERNAME</username>
      <password>YOUR_SONATYPE_PASSWORD</password>
    </server>
  </servers>

  <profiles>
    <profile>
      <id>ossrh</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg</gpg.executable>
        <gpg.passphrase>YOUR_GPG_PASSPHRASE</gpg.passphrase>
        <!-- Better: Use GPG Agent without passphrase in settings -->
      </properties>
    </profile>
  </profiles>
</settings>
```

**IMPORTANT**: Never commit credentials to git! Use Maven environment variables instead:
```bash
export MAVEN_OPTS="-Dgpg.passphrase=YOUR_PASSPHRASE"
```

## Step 3: Prepare Release

### Update version in pom.xml
```bash
./mvnw versions:set -DnewVersion=1.0.0
./mvnw versions:commit
```

### Build and test locally
```bash
./mvnw clean package
```

### Create release commit
```bash
git add pom.xml
git commit -m "Release 1.0.0"
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin main --tags
```

## Step 4: Deploy to Maven Central

```bash
# Deploy artifacts (signs with GPG automatically)
./mvnw clean deploy -P ossrh

# Or if using release-plugin:
./mvnw release:prepare
./mvnw release:perform
```

## Step 5: Release from Staging

After deployment, login to https://s01.oss.sonatype.org and:

1. Go to **Staging Repositories**
2. Find your repository (should start with `comzequent`)
3. Click **Close** (validates artifacts)
4. Once closed, click **Release** (promotes to Maven Central)

The artifact will appear in Maven Central within 30 minutes to 2 hours.

## Verify Publication

```bash
# Check Maven Central
curl https://central.sonatype.com/api/v1/search/artifact?name=java-client-sdk

# Or search: https://mvnrepository.com/artifact/com.zequent.framework.client.sdk/java-client-sdk
```

## Troubleshooting

### GPG Errors
```bash
# Check GPG is working
echo "test" | gpg --sign

# Configure Maven to use GPG:
gpg-agent
export GPG_TTY=$(tty)
```

### Staging Repository Closed
Check the staging repository log in https://s01.oss.sonatype.org for validation errors:
- Missing javadoc JAR
- Missing sources JAR
- Missing POM
- Invalid signature

### Nexus Connection Issues
```bash
# Test connection
mvn help:active-profiles
```

## Snapshot Releases (for testing)

For pre-release versions (1.0.0-SNAPSHOT):

```bash
./mvnw deploy -P ossrh
```

Snapshot artifacts are deployed to:
```
https://s01.oss.sonatype.org/content/repositories/snapshots
```

## Resources

- [OSSRH Guide](https://central.sonatype.org/publish/publish-guide/)
- [Maven GPG Plugin](https://maven.apache.org/plugins/maven-gpg-plugin/)
- [Apache Maven Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/)
