# 📦 README для EverWar + инструкция для Git

## 1️⃣ README.md (основной файл)

Создай в корне проекта `EverWar/README.md`:

```markdown
<div align="center">

# ⚔ EverWar

**Advanced Clan, Territory & War System for Minecraft Servers**

*A powerful Bukkit/Spigot plugin designed for realistic war servers with mods support*

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/twiks228/EverWar)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://minecraft.net)
[![Platform](https://img.shields.io/badge/Platform-Bukkit%20%7C%20Spigot%20%7C%20Paper%20%7C%20Arclight-red.svg)](https://spigotmc.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

[Features](#-features) • 
[Installation](#-installation) • 
[Commands](#-commands) • 
[Configuration](#-configuration) • 
[API](#-api) • 
[Contributing](#-contributing)

</div>

---

## 📖 About

**EverWar** is a next-generation clan and warfare plugin created specifically for modded Minecraft servers. Unlike Factions or Towny, EverWar is designed with modern gameplay in mind — full support for mod weapons (SuperbWarfare), aircraft (Immersive Aircraft), Create machinery, TNT, grenades, and drone attacks.

The plugin features a unique **Territory Shield System** — by default territories are vulnerable to any attacks, giving realistic warfare experience. Shields can be activated temporarily (up to 15 minutes) or permanently by administrators, adding strategic depth.

### 🎯 Why EverWar?

- ✅ **Mod-compatible** — works with SuperbWarfare, Immersive Aircraft, Create, and more
- ✅ **No WorldGuard needed** — built-in protection system
- ✅ **Realistic warfare** — territories can be destroyed by TNT, drones, grenades
- ✅ **Multi-language** — Russian & English out of the box
- ✅ **Beautiful GUI** — modern inventory-based menus
- ✅ **SQLite storage** — no external database required
- ✅ **PlaceholderAPI support** — 20+ placeholders
- ✅ **LuckPerms integration** — automatic role-based permissions

---

## ✨ Features

### 🏰 Clan System
- Create clans with custom name (3-24 chars) and tag (4 chars)
- Invite/kick members via commands or GUI
- 5-tier role hierarchy: **Leader → General → Officer → Fighter → Recruit**
- Cost-based clan creation (Vault integration)
- Beautiful clan info display with statistics

### 🗺 Territory Control
- Chunk-based claim system (`/war territory claim`)
- Visual map in chat and GUI (11x11 or 9x4 chunks)
- Territory HP system with damage from explosions
- Upgradeable defense (5 levels, +500 HP per level)
- Base Core designation — clan's main stronghold

### 🛡 Territory Shield System (**UNIQUE!**)
- **Default: SHIELD OFF** — territories are vulnerable (realistic war)
- Enable temporarily: `/war shield on [1-15 minutes]`
- Permanent shield: admin-only permission
- Shield DOESN'T protect during active war (war pierces shield)
- Auto-notification when shield expires

### ⚔ Warfare
- Declare war on enemy clans: `/war war declare <clan>`
- Preparation phase (default 10 min) before combat
- Kill points & territory capture points
- Supply requirements (food + materials) to declare war
- War score tracking with real-time notifications
- Surrender mechanic

### 🏴 Siege System
- Set siege points on enemy territory: `/war siege start`
- Hold zone mechanic (default 5 min to capture)
- Attackers vs Defenders zone control
- Progress bar visible in action bar
- Successful siege = territory transferred to attackers

### 🚨 Deserter Mode (**UNIQUE!**)
- Declare clan "against everyone": `/war deserter on [hours]`
- Attack ANY player including allies and clanmates
- Everyone can attack deserters without consequences
- +10 power bonus for killing deserters
- Auto-expires after set time (max 24h)

### 🤝 Diplomacy
- Alliance system with mutual acceptance
- Enemy declaration
- Neutral relations
- Clickable notifications with accept/reject buttons
- Sound & title alerts for proposals

### 🏛 Country System
- Group multiple clans into a "Country"
- Country-level warfare (planned)
- Country tags and statistics
- Leader clan management

### 📦 Supply System
- Clan warehouse for food, materials, fuel
- Required for war declarations
- Add items directly from hand: `/war supply add food`
- Beautiful GUI management

### 🎮 GUI System (10 windows)
- Main clan menu
- Members list with roles
- Territory map (visual)
- Diplomacy manager
- War & Siege center
- Clan rankings
- Settings (leader-only)
- Supply warehouse
- Country management
- Fully clickable with sounds

### ⚙ Settings Per Clan
- **Friendly Fire** — allow attacking clanmates
- **Attack Allies** — betrayal mode
- **Open Clan** — auto-join without invitation
- **Public Info** — visibility to others

---

## 📥 Installation

### Requirements

- **Server**: Bukkit / Spigot / Paper / Arclight `1.21.1`
- **Java**: 21+
- **Optional plugins**:
  - Vault (for economy)
  - PlaceholderAPI (for placeholders)
  - LuckPerms (for auto role permissions)
  - DecentHolograms (for base core holograms)

### Steps

1. **Download** the latest `EverWar-1.0.0.jar` from [Releases](https://github.com/twiks228/EverWar/releases)

2. **Place** in your server's `plugins/` folder

3. **Restart** the server

4. **Configure** `plugins/EverWar/config.yml` to your needs

5. **Set language** in config: `language: "en"` or `"ru"`

6. **Enjoy!** Type `/war` in-game to open the main menu

### Building from Source

```bash
# Clone repository
git clone https://github.com/twiks228/EverWar.git
cd EverWar

# Build with Gradle Wrapper (Windows)
gradlew.bat jar

# Build with Gradle Wrapper (Linux/Mac)
./gradlew jar

# JAR will be in: build/libs/EverWar-1.0.0.jar
```

---

## 🎮 Commands

### Main Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/war` | Open main GUI menu | `everwar.use` |
| `/war help` | Show all commands | `everwar.use` |
| `/war top [N]` | Top N clans by power | `everwar.use` |
| `/war map` | Show territory map | `everwar.use` |

### Clan Commands

| Command | Description |
|---------|-------------|
| `/war clan create <name> <tag>` | Create clan (costs money if Vault enabled) |
| `/war clan delete` | Delete your clan (Leader only) |
| `/war clan invite <player>` | Invite player |
| `/war clan accept` | Accept invitation |
| `/war clan deny` | Deny invitation |
| `/war clan leave` | Leave clan (not for Leader) |
| `/war clan kick <player>` | Kick member (Generals+) |
| `/war clan info [clan]` | Show clan info |
| `/war clan list` | List all clans |
| `/war clan members` | Open members GUI |
| `/war clan role <player> <role>` | Set member role |

### Territory Commands

| Command | Description |
|---------|-------------|
| `/war territory claim` | Claim current chunk (Officers+) |
| `/war territory unclaim` | Release current chunk |
| `/war territory map` | Text map in chat |
| `/war territory map gui` | Visual GUI map |
| `/war territory setcore` | Set as Base Core (Leader) |
| `/war territory info` | Info about current chunk |
| `/war territory upgrade` | Upgrade defense level |

### Shield Commands

| Command | Description |
|---------|-------------|
| `/war shield on [1-15]` | Enable shield for N minutes |
| `/war shield off` | Disable shield |
| `/war shield status` | Shield status |
| `/war shield permanent` | Permanent shield (Admin) |

### Diplomacy Commands

| Command | Description |
|---------|-------------|
| `/war diplomacy ally <clan>` | Propose alliance |
| `/war diplomacy accept <clan>` | Accept alliance proposal |
| `/war diplomacy reject <clan>` | Reject alliance proposal |
| `/war diplomacy enemy <clan>` | Declare enemy |
| `/war diplomacy neutral <clan>` | Set neutral |
| `/war diplomacy list` | List relations |

### War Commands

| Command | Description |
|---------|-------------|
| `/war war declare <clan>` | Declare war (Generals+) |
| `/war war status` | War status |
| `/war war surrender` | Surrender all wars (Leader) |
| `/war war score` | Current war score |

### Siege Commands

| Command | Description |
|---------|-------------|
| `/war siege start` | Start siege on current chunk |
| `/war siege stop` | Stop siege |
| `/war siege status` | Siege progress |

### Deserter Commands

| Command | Description |
|---------|-------------|
| `/war deserter on [1-24]` | Deserter mode for N hours |
| `/war deserter off` | Disable deserter |
| `/war deserter status` | Deserter status |

### Supply Commands

| Command | Description |
|---------|-------------|
| `/war supply` | Open supply GUI |
| `/war supply status` | Supply status |
| `/war supply add <type> [amount]` | Add resources from hand |

### Country Commands

| Command | Description |
|---------|-------------|
| `/war country create <name> <tag>` | Create country |
| `/war country delete` | Delete country |
| `/war country invite <clan>` | Invite clan |
| `/war country join <country>` | Join country |
| `/war country leave` | Leave country |
| `/war country info [name]` | Country info |
| `/war country list` | List countries |

### Admin Commands (`everwar.admin`)

| Command | Description |
|---------|-------------|
| `/war admin reload` | Reload plugin |
| `/war admin list` | List all clans |
| `/war admin info <clan>` | Detailed clan info |
| `/war admin setpower <player> <amount>` | Set player power |
| `/war admin forcedelete <clan>` | Force delete clan |
| `/war admin shield <clan> on/off/permanent` | Manage shields |
| `/war admin deserter <clan> on/off` | Manage deserter status |
| `/war admin clearwars <clan>` | End all wars |
| `/war admin clearsieges <clan>` | End all sieges |

---

## ⚙ Configuration

### `config.yml`

```yaml
# Language: ru or en
language: "en"

clan:
  min-name-length: 3
  max-name-length: 24
  tag-length: 4
  max-members: 50
  create-cost: 10000.0

territory:
  chunks-per-player: 3
  max-chunks: 150
  claim-cost: 500.0

war:
  preparation-time: 600  # seconds
  min-players: 3
  kill-points: 10
  capture-points: 50

siege:
  capture-time: 300  # seconds
  radius: 10  # blocks
  cost: 2000.0

supply:
  food-per-war: 100
  materials-per-war: 50

power:
  starting-power: 100
  kill-power: 5
  death-power: 3
  max-power: 10000
```

### Messages Files

- `messages_ru.yml` — Russian translations
- `messages_en.yml` — English translations

All messages support color codes (`&a`, `&c`, etc.) and hex colors (`&#RRGGBB`).

---

## 🔌 API

### PlaceholderAPI Placeholders

| Placeholder | Description |
|-------------|-------------|
| `%everwar_clan%` | Player's clan name |
| `%everwar_clan_tag%` | Clan tag |
| `%everwar_clan_leader%` | Clan leader name |
| `%everwar_clan_members%` | Members count |
| `%everwar_clan_online%` | Online members |
| `%everwar_clan_power%` | Total clan power |
| `%everwar_clan_territories%` | Territory count |
| `%everwar_role%` | Player's role |
| `%everwar_role_icon%` | Role icon (⭐🎖🔱⚔🔰) |
| `%everwar_power%` | Player's power |
| `%everwar_kills%` | Player kills |
| `%everwar_deaths%` | Player deaths |
| `%everwar_kdr%` | Kill/Death ratio |
| `%everwar_at_war%` | Yes/No |
| `%everwar_supply_food%` | Clan food |
| `%everwar_supply_materials%` | Clan materials |
| `%everwar_country%` | Player's country |
| `%everwar_territory%` | Current chunk owner |
| `%everwar_rank%` | Clan rank in top |
| `%everwar_has_clan%` | Yes/No |

### LuckPerms Integration

Automatic group assignment based on clan role:
- `everwar_leader`
- `everwar_general`
- `everwar_officer`
- `everwar_fighter`
- `everwar_recruit`

Configure these groups in LuckPerms for role-based permissions.

---

## 🎨 Screenshots

*Coming soon...*

---

## 🐛 Bug Reports

Found a bug? Please report it on our [Issues page](https://github.com/twiks228/EverWar/issues).

Include:
- Server version
- Plugin version
- Steps to reproduce
- Console logs
- Expected vs actual behavior

---

## 🤝 Contributing

Contributions are welcome! Feel free to:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### Development Setup

```bash
git clone https://github.com/twiks228/EverWar.git
cd EverWar

# Open in IntelliJ IDEA (recommended)
# File → Open → EverWar folder
# Wait for Gradle sync

# Build
./gradlew jar
```

---

## 📜 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 👥 Credits

- **Author**: EverTeam
- **Server**: [Ever Server](https://github.com/twiks228/Arclight)
- **Inspiration**: Factions, Towny, but built better for modded servers

---

## ⭐ Support

If you enjoy this plugin, please give it a ⭐ on GitHub!

<div align="center">

Made with ❤ for Minecraft Community

</div>
```

---

## 2️⃣ LICENSE (MIT)

Создай файл `LICENSE` в корне:

```
MIT License

Copyright (c) 2025 EverTeam

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 3️⃣ .gitignore

Создай файл `.gitignore` в корне:

```gitignore
# ==========================================
# EverWar — .gitignore
# ==========================================

# ===== Build outputs =====
build/
out/
target/
*.class
*.jar
!gradle/wrapper/gradle-wrapper.jar

# ===== Gradle =====
.gradle/
!gradle-wrapper.properties

# ===== IntelliJ IDEA =====
.idea/
*.iml
*.ipr
*.iws

# ===== Eclipse =====
.classpath
.project
.settings/
.metadata/
bin/

# ===== VSCode =====
.vscode/
*.code-workspace

# ===== NetBeans =====
nbproject/
nbbuild/
nbdist/
build/
dist/

# ===== Logs =====
*.log
logs/

# ===== SQLite databases (dev/test) =====
*.db
*.db-journal
*.db-wal
*.db-shm
run/plugins/EverWar/everwar.db*

# ===== OS =====
.DS_Store
Thumbs.db
desktop.ini

# ===== Test server folder =====
run/
server/
plugins/

# ===== Python helper scripts (optional — comment if want to commit) =====
# build.py
# setup.py
# check_structure.py
# fix_warnings.py
# copy_to_server.py

# ===== Backup files =====
*.bak
*.backup
*.orig
*~

# ===== Environment =====
.env
.env.local
```

---

## 4️⃣ CHANGELOG.md

Создай `CHANGELOG.md`:

```markdown
# Changelog

All notable changes to EverWar will be documented in this file.

## [1.0.0] - 2025

### 🎉 Initial Release

#### Added
- **Clan System** — create, invite, kick, roles (5 tiers)
- **Territory Control** — chunk-based claiming with HP and defense levels
- **Territory Shield System** — unique temporary/permanent protection
- **Warfare** — declare wars with preparation phase and scoring
- **Siege System** — hold-zone mechanic for capturing territories
- **Deserter Mode** — clan vs everyone (unique feature)
- **Diplomacy** — alliances, enemies, neutrals with clickable notifications
- **Country System** — group clans into larger nations
- **Supply System** — food/materials/fuel for wars
- **10 GUI Menus** — beautiful inventory-based interfaces
- **Multi-language** — Russian & English support
- **SQLite Storage** — auto-migration between versions
- **PlaceholderAPI** — 20+ placeholders
- **LuckPerms Integration** — auto role-based groups
- **Vault Integration** — economy support
- **DecentHolograms Integration** — base core holograms

#### Features
- Support for mod weapons (SuperbWarfare, Immersive Aircraft, etc.)
- TNT, grenades, drones, explosions work as expected
- Works with Arclight (NeoForge + Bukkit hybrid)
- Java 21 support
- Minecraft 1.21.1

---

## Future Plans

### [1.1.0] — Planned
- Clan chat (`/c message`)
- Ally chat (`/a message`)
- Clan bank system
- Daily clan quests
- Achievement titles

### [1.2.0] — Planned
- Dynmap/BlueMap integration
- Clan skins (colors, banners)
- Clan XP and levels
- TAB integration for prefixes
```

---

## 5️⃣ CONTRIBUTING.md

Создай `CONTRIBUTING.md`:

```markdown
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

