<div align="center">
  <p>An AI-powered, free and open-source anti-cheat for Minecraft servers.</p>

  <p>
    <a href="README.md"><b>English</b></a>
    ·
    <a href="README.ru.md">Русский</a>
    ·
    <a href="https://mkac.fun">mkac.fun</a>
  </p>

  <p>
    <a href="https://github.com/korpys667/MKAC/actions/workflows/ci.yml">
      <img alt="CI" src="https://github.com/korpys667/MKAC/actions/workflows/ci.yml/badge.svg">
    </a>
    <a href="https://app.codacy.com/gh/korpys667/MKAC/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade">
      <img alt="Codacy" src="https://app.codacy.com/project/badge/Grade/fb90bca03b36460faab2cf86fc2178bd?branch=main">
    <a href="https://github.com/korpys667/MKAC/">
      <img alt="Views" src="https://api.visitorbadge.io/api/visitors?path=https%3A%2F%2Fgithub.com%2Fkorpys667%2FMKAC%2FREADME.md&label=Views&countColor=%23555555&style=flat&labelStyle=none">
    </a>
  </p>
</div>

## What MKAC is

MKAC is an open-source combat anti-cheat for Minecraft servers. An AI model checks the player's mouse movement and detects killaura and aim assist.

## Important before you install

MKAC's AI check uses the official MKAC API. Create an account at [mkac.fun](https://mkac.fun) to get access.

If API access is not available yet, disable the AI check for now.

## Requirements

- Java 17 or newer to run the plugin
- JDK 21 or newer if you want to build from source
- A Paper or Folia-based server

## Installation

1. Download the latest release from [GitHub Releases](https://github.com/korpys667/MKAC/releases).
2. Place the main `MKAC-<version>.jar` in the server `plugins/` directory.
3. Start the server once so MKAC can generate its configuration files.
4. If needed, configure storage:
   - SQLite is the default
   - MySQL and MariaDB are also supported
5. If WorldGuard is installed, specific regions can be excluded from the AI check.
6. Restart the server or reload the MKAC configuration.

## Configuration files

- [`config.yml`](src/main/resources/config.yml): AI, database, Redis and cross-server alerts, alerts, duplicate packet handling
- [`monitor.yml`](src/main/resources/monitor.yml): outputs, limits, themes and formatting for `/mkac monitor` and `/mkac view`
- [`punishments.yml`](src/main/resources/punishments.yml): punishment rules
- [`messages/messages_en.yml`](src/main/resources/messages/messages_en.yml): English messages
- [`messages/messages_ru.yml`](src/main/resources/messages/messages_ru.yml): Russian messages

## Main commands

| Command                                   | Purpose                                                                 |
|-------------------------------------------|-------------------------------------------------------------------------|                                      |                                    |
| `/mkac alerts`                           | Toggle violation alerts                                                 |
| `/mkac suspicious <list\|top\|flagged>`  | Review suspicious or previously flagged online players                  |
| `/mkac profile <player>`                 | Open a player's live profile                                            |
| `/mkac monitor <player>`                 | Watch AI data for one player in real time                               |
| `/mkac monitor add\|remove\|clear`       | Watch several players at once                                           |
| `/mkac monitor output <type>`            | Draw the monitor on the action bar, boss bar, sidebar, chat or tab list |
| `/mkac monitor settings`                 | Show and edit your monitor settings                                     |
| `/mkac view`                             | Toggle observation mode for nearby players                              |
| `/mkac logs [page]`                      | View recent violations                                                  |
| `/mkac history <player> [page]`          | View a player's stored violation history                                |
| `/mkac stats`                            | View server-side anti-cheat stats                                       |
| `/mkac dc <start\|stop\|cancel\|status>` | Manage labeled data collection sessions                                 |
| `/mkac reload`                           | Reload MKAC configuration                                               |

For the full command list, use `/mkac help` in game.

## Building from source

```bash
git clone https://github.com/korpys667/MKAC.git
cd MKAC
./gradlew shadowJar
```

The main plugin jar lands in `build/libs/MKAC-<version>.jar`.

## Help, bugs, and discussion

- Bug reports: [GitHub Issues](https://github.com/korpys667/MKAC/issues)

Issue reports should include:

- server version
- Java version
- plugin version
- relevant config values
- logs, stack traces, and steps to reproduce

## Credits

MKAC has its own, independently developed codebase. That said, some parts of its code are adapted from the open-source [GrimAC](https://github.com/GrimAnticheat/Grim) project, and MKAC builds on ideas developed by GrimAC, DefineOutside, and other GrimAC contributors - full credit and thanks to them for their work.

## License

MKAC is distributed under the terms of the [GNU General Public License v3.0](LICENSE).

<img src="https://api.visitorbadge.io/api/visitors?path=mkac&label=&countColor=%23555555&style=flat&labelStyle=none" alt="" width="1" height="1">

