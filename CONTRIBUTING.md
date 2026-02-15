# Contributing to Zequent Java Client SDK

Thank you for your interest in contributing! This document outlines the guidelines for contributing to the Zequent Java Client SDK.

## Getting Started

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR-USERNAME/zqnt-client-sdk-java.git
   cd zqnt-client-sdk-java
   ```
3. **Create a branch** for your feature:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Setup

### Prerequisites
- Java 21+
- Maven 3.8.0+

### Building the Project
```bash
./mvnw clean install
```

### Running Tests
```bash
./mvnw test
```

## Code Style

- Follow standard Java naming conventions
- Use 4-space indentation
- Maximum line length: 120 characters
- Add JavaDoc to public APIs
- Write meaningful commit messages

## Creating a Pull Request

1. **Commit your changes** with clear messages:
   ```bash
   git commit -m "feat: add feature description"
   git commit -m "fix: resolve issue #123"
   ```

2. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

3. **Open a Pull Request** with:
   - Clear title and description
   - Reference to any related issues
   - Tests for new functionality
   - Updated documentation

## Commit Message Format

```
<type>: <subject>

<body>

<footer>
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

## Reporting Issues

Please use GitHub Issues for bug reports and feature requests. Include:
- Clear description of the issue
- Steps to reproduce (for bugs)
- Expected vs. actual behavior
- Java version and environment details

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

## Questions?

Feel free to reach out to the development team at dev@zequent.com
