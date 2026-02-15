# Publishing to GitHub Packages

This guide explains how to publish the Zequent Java Client SDK to GitHub Packages.

## Automated Publishing (GitHub Actions)

The easiest way to publish is using GitHub Actions. All configuration is already set up!

### How It Works

1. Push a tag to GitHub:
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```

2. GitHub Actions automatically:
   - Builds the project
   - Runs tests
   - Deploys to GitHub Packages
   - Creates a release on GitHub

That's it! No manual configuration needed.

### Files Used

- `.github/settings.xml` - Maven configuration with GitHub Packages credentials
- `.github/workflows/publish.yml` - Automated build & publish workflow

The workflow uses GitHub Actions secrets:
- `GITHUB_TOKEN` - Automatically provided by GitHub
- `GITHUB_ACTOR` - Your GitHub username

---

## Manual Publishing (Local)

If you need to publish manually from your computer:

## Prerequisites

1. **GitHub Account** with access to the Zequent organization
2. **Personal Access Token (PAT)** with `write:packages` and `read:packages` permissions (for local dev)
3. **Maven Settings** configured

## Setup for Manual Publishing

### Create GitHub Personal Access Token (optional, for local development)

1. Go to GitHub Settings → **Developer Settings → Personal access tokens → Tokens (classic)**
2. Click **Generate new token (classic)**
3. Select scopes:
   - ✅ `write:packages` - deploy packages
   - ✅ `read:packages` - read packages
4. Copy the token

### Configure Maven Locally

Edit `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_PAT</password>
    </server>
  </servers>
</settings>
```

Or use environment variables (recommended):

```bash
export GITHUB_ACTOR="your-username"
export GITHUB_TOKEN="ghp_xxxxxxxxxxxxx"
```

Then Maven will automatically read from environment variables.

## Deploy Manually

```bash
# Copy GitHub settings to Maven
mkdir -p ~/.m2
cp .github/settings.xml ~/.m2/settings.xml

# Deploy
mvn clean deploy
```

## Verify Publication

Check GitHub Packages:
- https://github.com/Zequent/zequent-framework/packages
- Look for `java-client-sdk` package

Or test in another project:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/Zequent/zequent-framework</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.zequent.framework.client.sdk</groupId>
    <artifactId>java-client-sdk</artifactId>
    <version>1.0.0</version>
</dependency>
```

```bash
mvn dependency:resolve
```
