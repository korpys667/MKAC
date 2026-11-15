<div align="center">
  <p>Свободный AI-античит с открытым исходным кодом для Minecraft-серверов.</p>

  <p>
    <a href="README.md">English</a>
    ·
    <a href="README.ru.md"><b>Русский</b></a>
    ·
    <a href="https://mkac.fun">mkac.fun</a>
  </p>

  <p>
    <a href="https://github.com/korpys667/MKAC/actions/workflows/ci.yml">
      <img alt="CI" src="https://github.com/korpys667/MKAC/actions/workflows/ci.yml/badge.svg">
    </a>
    <a href="https://app.codacy.com/gh/korpys667/MKAC/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade">
      <img alt="Codacy" src="https://app.codacy.com/project/badge/Grade/fb90bca03b36460faab2cf86fc2178bd?branch=main">
    </a>
    <a href="https://github.com/korpys667/MKAC/">
      <img alt="Views" src="https://api.visitorbadge.io/api/visitors?path=https%3A%2F%2Fgithub.com%2Fkorpys667%2FMKAC%2FREADME.md&label=Views&countColor=%23555555&style=flat&labelStyle=none">
    </a>
  </p>
</div>

## Что такое MKAC

MKAC - combat-античит с открытым исходным кодом для Minecraft-серверов. AI-модель проверяет движения мыши игрока и обнаруживает killaura и aim assist.

## Важный момент перед установкой

AI-проверка MKAC использует официальный MKAC API. Зарегистрируйтесь на [mkac.fun](https://mkac.fun), чтобы получить доступ.

Чтобы подключить сервер к API, укажите в config.yml API-ключ.

При отсутствии доступа к API AI-проверку следует временно отключить.

## Требования

- Java 17+ для запуска плагина
- JDK 21+ для сборки проекта
- сервер на Paper или Folia

## Установка

1. Скачать актуальный релиз из [GitHub Releases](https://github.com/korpys667/MKAC/releases).
2. Поместить основной `MKAC-<version>.jar` в каталог `plugins/`.
3. Один раз запустить сервер, чтобы MKAC создал конфиги.
4. Указать API-ключ в config.yml.
5. При необходимости настроить хранилище:
   - SQLite используется по умолчанию
   - MySQL и MariaDB тоже поддерживаются
6. При использовании WorldGuard можно исключить нужные регионы из AI-проверки.
7. Перезапустить сервер или перезагрузить конфигурацию плагина.

## Файлы конфигурации

- [`config.yml`](src/main/resources/config.yml): AI, база данных, Redis, межсерверные оповещения, алерты и обработка дублирующихся пакетов движения
- [`monitor.yml`](src/main/resources/monitor.yml): выводы, лимиты, темы и формат `/mkac monitor` и `/mkac view`
- [`punishments.yml`](src/main/resources/punishments.yml): правила наказаний
- [`messages/messages_en.yml`](src/main/resources/messages/messages_en.yml): английская локализация
- [`messages/messages_ru.yml`](src/main/resources/messages/messages_ru.yml): русская локализация

## Основные команды

| Команда | Что делает                                                          |
| --- |---------------------------------------------------------------------|| |
| `/mkac alerts` | Включает и выключает уведомления о нарушениях                       |
| `/mkac suspicious <list\|top\|flagged>` | Показывает подозрительных игроков и онлайн-игроков с флагами        |
| `/mkac profile <player>` | Открывает профиль игрока                                            |
| `/mkac monitor <player>` | Показывает AI-данные игрока в реальном времени                      |
| `/mkac monitor add\|remove\|clear` | Отслеживает нескольких игроков сразу                                |
| `/mkac monitor output <type>` | Рисует монитор в action bar, боссбаре, сайдбаре, чате или таб-листе |
| `/mkac monitor settings` | Показывает и меняет настройки монитора                              |
| `/mkac view` | Переключает режим наблюдения за игроками                            |
| `/mkac logs [page]` | Показывает недавние нарушения                                       |
| `/mkac history <player> [page]` | Показывает историю нарушений игрока                                 |
| `/mkac stats` | Показывает статистику античита по серверу                           |
| `/mkac dc <start\|stop\|cancel\|status>` | Управляет сессиями сбора данных                                     |
| `/mkac reload` | Перезагружает конфигурацию MKAC                                     |

Полный список команд доступен через `/mkac help`.

## Сборка из исходников

```bash
git clone https://github.com/korpys667/MKAC.git
cd MKAC
./gradlew shadowJar
```

Готовый jar появится в `build/libs/MKAC-<version>.jar`.

## Помощь, баг-репорты и обсуждение

- Баг-репорты: [GitHub Issues](https://github.com/korpys667/MKAC/issues)

При создании issue рекомендуется приложить:

- версию сервера
- версию Java
- версию плагина
- важные фрагменты конфига
- логи, stack trace и шаги воспроизведения

## Благодарности

У MKAC собственная, независимо разработанная кодовая база. Тем не менее часть его кода адаптирована из open-source проекта [GrimAC](https://github.com/GrimAnticheat/Grim), и MKAC опирается на идеи, разработанные GrimAC, DefineOutside и другими участниками проекта GrimAC - им полная признательность и благодарность за работу.

## Лицензия

MKAC распространяется на условиях лицензии [GNU General Public License v3.0](LICENSE).

<img src="https://api.visitorbadge.io/api/visitors?path=mkac&label=&countColor=%23555555&style=flat&labelStyle=none" alt="" width="1" height="1">

