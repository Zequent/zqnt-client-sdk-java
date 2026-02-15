# Security Policy

## Reporting Security Vulnerabilities

**Do not** open a public GitHub issue for security vulnerabilities.

Instead, please email security concerns to: **security@zequent.com**

Please include:
- Description of the vulnerability
- Steps to reproduce (if applicable)
- Potential impact
- Suggested fix (if you have one)

We will acknowledge receipt within 24 hours and work to resolve the issue promptly.

## Supported Versions

| Version | Status | Support Until |
|---------|--------|---------------|
| 1.x     | Active | Latest minor version supported |

## Dependency Scanning

This project uses Maven to manage dependencies. We recommend regular scanning for vulnerabilities using:
- `mvn dependency-check:check`
- OWASP Dependency-Check
- GitHub Dependabot

## Best Practices

When using this SDK:
- Keep your JDK updated to Java 21+
- Regularly update dependencies: `./mvnw versions:display-dependency-updates`
- Don't commit sensitive data (API keys, credentials) in your code
- Use environment variables or secure vaults for credentials
