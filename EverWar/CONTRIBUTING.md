# Contributing to EverWar

Thank you for your interest in contributing to EverWar! 🎉

## How to Contribute

### 🐛 Reporting Bugs

1. Check if the bug is already reported in [Issues](https://github.com/twiks228/EverWar/issues)
2. If not, create a new issue with:
   - **Title**: Clear, descriptive
   - **Server version**: Bukkit/Spigot/Paper/Arclight version
   - **Plugin version**: EverWar version
   - **Steps to reproduce**: Detailed steps
   - **Expected behavior**: What should happen
   - **Actual behavior**: What happens instead
   - **Console logs**: Include relevant error messages
   - **Screenshots**: If applicable

### ✨ Suggesting Features

1. Open an issue with the `[Feature Request]` prefix
2. Describe the feature clearly
3. Explain why it would be useful
4. Provide examples if possible

### 💻 Code Contributions

#### Setup

1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/EverWar.git
   cd EverWar
   ```
3. Create a branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

#### Development Guidelines

- **Java Version**: Use Java 21 features
- **Code Style**: Follow existing patterns
- **Comments**: Comment complex logic
- **Naming**: 
  - Classes: `PascalCase`
  - Methods/variables: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
- **Language**: All code comments and commit messages in English

#### Testing

Before submitting:
1. Build: `./gradlew jar`
2. Test on a real server
3. Ensure no console errors
4. Test with different plugin combinations (Vault, PAPI, etc.)

#### Pull Request

1. Push your changes:
   ```bash
   git add .
   git commit -m "Add: your feature description"
   git push origin feature/your-feature-name
   ```
2. Open a Pull Request on GitHub
3. Fill in the PR template
4. Wait for review

## Commit Message Format

Use conventional commits:

- `Add: new feature`
- `Fix: bug fix`
- `Update: change existing feature`
- `Remove: delete feature`
- `Docs: documentation changes`
- `Refactor: code improvements without feature changes`

Example: `Add: deserter mode for clan warfare`

## Code of Conduct

- Be respectful
- Be constructive
- Help others learn
- No spam or self-promotion

