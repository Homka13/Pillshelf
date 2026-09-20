# Pillshelf 💊📦

<p align="center">
  <img src="app/src/main/res/drawable/img_pillshelf_banner.jpg" alt="Pillshelf Banner" width="100%" style="max-height: 320px; object-fit: cover; border-radius: 12px;" />
</p>

<p align="center">
  <b>Розумна аптечка, трекер прийому ліків та моніторинг цін в аптеках України</b><br>
  <i>Smart medicine cabinet, pill adherence tracker, and Ukrainian pharmacy price monitor</i>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android" alt="Android" />
  <img src="https://img.shields.io/badge/Kotlin-2.3-7F52FF.svg?style=flat&logo=kotlin" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4.svg?style=flat" alt="Compose" />
  <img src="https://img.shields.io/badge/Room%20DB-2.7-009688.svg?style=flat" alt="Room" />
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat" alt="License" />
</p>

<p align="center">
  <a href="#-українська-версія">🇺🇦 Українська версія</a> •
  <a href="#-english-version">🇬🇧 English Version</a>
</p>

---

## 🇺🇦 Українська версія

### 📖 Огляд проекту (Project Overview)
**Pillshelf** — це сучасний, приватний та повністю автономний (offline-first) Android-застосунок для контролю домашньої аптечки, розкладу прийому медикаментів і відстеження термінів придатності. Застосунок не має власних серверів: єдиний мережевий контакт — необов'язковий пошук цін на сайті Tabletki.ua (див. нижче).

**Приватність.** Усі персональні дані, історія прийомів та списки медикаментів зберігаються виключно на вашому пристрої в локальній базі даних (Room). Хмарне резервне копіювання вимкнене (`allowBackup="false"`); натомість є ручний експорт усіх даних у JSON-файл. Виняток щодо мережі: якщо ви **самі** увімкнули фоновий моніторинг цін (він вимкнений за замовчуванням), назва препарату надсилається на tabletki.ua для пошуку. Жодних аналітиків, рекламних SDK чи трекерів немає.

---

### 📱 Скріншоти інтерфейсу (Screenshots Placeholder)

| 🏠 Аптечка (Shelf) | ⏰ Розклад (Schedule) | 💰 Ціни (Prices) | 📜 Журнал (History) |
| :---: | :---: | :---: | :---: |
| _[Скріншот списку ліків, пошуку та категорій]_ | _[Скріншот карток прийому на сьогодні з прогресом]_ | _[Скріншот динаміки цін, трендів та Tabletki.ua]_ | _[Скріншот журналу прийомів та статистики комплаєнсу]_ |

---

### ✨ Функціональні можливості (Feature List)

#### 1. 💊 Домашня аптечка (Shelf Management)
- **Детальний каталог препаратів**: збереження торгової назви, форми випуску (таблетки, капсули, сироп, краплі, спрей, мазь тощо), діючої речовини, дозування та категорії.
- **Розумний контроль залишків**: миттєве сповіщення про низький запас ($\le 5$ од.) та закінчення препарату ($0$ од.).
- **Швидке поповнення запасу**: зручне меню поповнення зі швидкими пресетами (+10, +20, +30, +60 од.) або довільним значенням.
- **Моніторинг термінів придатності**: підсвічування прострочених препаратів червоним та попередження про ліки, строк яких спливає найближчим часом ($< 30$ днів).
- **Колірна диференціація**: вибір кольорової мітки для швидкої візуальної навігації.
- **Фільтрація та живий пошук**: миттєвий пошук за назвою або діючою речовиною, фільтри за категоріями («Знеболювальні», «Застуда та грип», «Вітаміни», «ШКТ», «Антибіотики» тощо).

#### 2. ⏰ Щоденний розклад та контроль прийому (Schedule & Adherence)
- **Гнучкі режими прийому**:
  - `DAILY` — добовий графік за часом дня (Ранок 08:00, День 14:00, Вечір 20:00).
  - `EVERY_N_HOURS` — регулярні інтервали (наприклад, кожні 4, 6 або 8 годин).
  - `COURSE` — фіксовані курси лікування (наприклад, антибіотики на 7–10 днів).
  - `AS_NEEDED` — прийом за вимогою чи симптоматично.
- **Контекст вживання їжі**: інструкції «До їжі», «Під час їжі», «Після їжі» або «Незалежно від їжі».
- **Швидкі дії в один дотик**:
  - **«Прийняти»**: автоматично списує 1 дозу з аптечки та зберігає факт прийому в журналі з точним часом.
  - **«Пропустити»**: реєструє пропуск без списання залишку.
- **Індикатор прогресу дня**: прогрес-бар виконання щоденного плану та відсоток завершення.

#### 3. 💰 Ціни та аптеки України (Prices & Tabletki.ua)
- **Чесний підхід до даних**: застосунок показує лише ціни, реально отримані від сервісу Tabletki.ua. Вигадані чи «орієнтовні» ціни не генеруються ніколи.
- **Поточне обмеження (вересень 2026)**: API Tabletki.ua блокує запити з не-браузерних клієнтів (Cloudflare, HTTP 403), тому автоматичне порівняння цін недоступне — екран чесно показує «ціни недоступні».
- **Швидкий перехід на Tabletki.ua**: кнопка «Відкрити в tabletki.ua» для кожного препарату — пошук наявності та цін у браузері, де обмежень немає.
- **Опціональна фонова перевірка**: вимикач у застосунку, вимкнений за замовчуванням. Якщо сервіс знову почне відповідати, моніторинг поновиться; тренди рахуються лише з реально отриманих записів.

#### 4. ⚠️ Підказки щодо лікарських взаємодій (кілька вбудованих правил)
> **Це НЕ повна перевірка взаємодій.** Перевірено лише кілька простих правил — завжди показується, що це підказки, а не медичний висновок.

- **Захист від дублювання парацетамолу**: виявлення одночасного прийому комбінованих чаїв від застуди (Фервекс, Терафлю тощо) та парацетамолу в таблетках для запобігання гепатотоксичності.
- **Попередження про НПЗП**: виявлення одночасного вживання кількох нестероїдних протизапальних засобів (ібупрофен, аспірин, диклофенак).
- **Правила прийому антибіотиків**: нагадування про проходження повного курсу лікування.
- **Медичний дисклеймер**: застосунок — засіб обліку і не замінює лікаря чи фармацевта.

#### 5. 📜 Журнал прийомів та аналітика (History Log)
- Хронологічна стрічка всіх записів прийомів із часовими мітками.
- Розрахунок відсотка дотримання схеми лікування (Adherence rate).
- Функція **«Скасувати»**: скасування помилкового запису з поверненням списаної дози в аптечку.
- **Прострочені дози**: якщо на нагадування не відповіли, доза позначається «Прострочено» в графіку і не зникає мовчки.
- **Відмітка заднім числом**: можливість відмітити прийом реальним часом («випив вчасно»), а не моментом натискання кнопки.
- **Експорт даних**: кнопка «Експорт» у журналі — JSON-копія аптечки та історії прийомів через системне діалогове вікно збереження.

#### 6. 🔔 Нагадування (точні будильники + WorkManager)
- **Точні будильники `setAlarmClock()`**: сповіщення про дозу в потрібну хвилину — не підпадають під обмеження Doze, показують системну іконку будильника.
- **Фіналізація пропущених доз**: якщо прийом не відмічено протягом 30 хвилин після нагадування, у журналі з'являється запис «Прострочено».
- **Статус дозволу**: екран розкладу показує, чи дозволені точні будильники, і веде на `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
- `ReminderWorker`: страховий перегляд розкладу кожні 15 хвилин.
- `PriceCheckWorker`: фонова перевірка цін кожні 12 годин — **вимкнена за замовчуванням**, вмикається вручну.
- Сповіщення про низький або вичерпаний запас ліків.
- Відновлення будильників після перезавантаження пристрою (`BootReceiver`).

---

### 🛠️ Інструкція зі збирання та запуску (Build Instructions)

#### Системні вимоги
- **JDK**: Java 21 LTS
- **Android Studio**: Ladybug (2024.2.1) або новіша
- **Android SDK**: API 36 (Android 16), мінімальна версія API 26 (Android 8.0)
- **Gradle**: 9.3.1 (Android Gradle Plugin 9.1.1)

#### Клонування та збирання
1. Клонуйте репозиторій:
   ```bash
   git clone https://github.com/your-username/pillshelf.git
   cd pillshelf
   ```
2. Зберіть проект за допомогою Gradle:
   ```bash
   ./gradlew assembleDebug
   ```
3. Запустіть юніт-тести:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Встановіть APK на підключений пристрій або емулятор:
   ```bash
   ./gradlew installDebug
   ```

---

### 🗺️ План розвитку проекту (Roadmap)
- [x] **Базовий облік аптечки**: додавання ліків, відстеження залишків та термінів придатності.
- [x] **Розклад та нагадування**: прийом ліків за розкладом, списання кількості, точні будильники (`setAlarmClock`).
- [x] **Швидкий перехід до Tabletki.ua**: чесна робота з цінами — тільки реальні дані, без вигадок.
- [x] **Підказки щодо взаємодій**: попередження про дублювання парацетамолу та комбінації НПЗП (кілька вбудованих правил).
- [x] **Експорт даних у JSON**: ручна копія аптечки та журналу (єдиний бекап, бо хмарний вимкнено).
- [ ] **Сканування штрих-кодів та QR-кодів**: швидке додавання препаратів за допомогою камери пристрою.
- [ ] **Підтримка NFC-міток**: прийом ліків шляхом піднесення смартфону до флакона з NFC-стікером.
- [ ] **Експорт медичного звіту у PDF**: формування звіту прийому та залишків для сімейного лікаря.
- [ ] **Опціональне зашифроване резервне копіювання**: експорт/імпорт бази даних у Google Drive або WebDAV.
- [ ] **Додаток-компаньйон для Wear OS**: швидкі відмітки про прийом таблетки з годинника.

---

<br>

## 🇬🇧 English Version

### 📖 Project Overview
**Pillshelf** is a modern, private, offline-first Android application for home medicine cabinet organization, medication scheduling, and expiration monitoring. The app runs no servers of its own: its only network contact is the optional price search on Tabletki.ua (see below).

**Privacy.** All personal health data, medication inventory, and intake logs remain on your device in a local Room database. Cloud backup is disabled (`allowBackup="false"`); instead, the app offers a manual export of all data to a JSON file. Network exception: if **you** enable background price monitoring (disabled by default), medication names are sent to tabletki.ua for search. There are no analytics, ad SDKs, or trackers.

---

### 📱 UI Screenshots (Screenshots Placeholder)

| 🏠 Shelf Screen | ⏰ Schedule Screen | 💰 Price Monitor | 📜 History & Analytics |
| :---: | :---: | :---: | :---: |
| _[Screenshot of cabinet inventory, search & categories]_ | _[Screenshot of today's schedule cards & progress]_ | _[Screenshot of price trends & Tabletki.ua shortcuts]_ | _[Screenshot of intake logs & adherence statistics]_ |

---

### ✨ Feature List

#### 1. 💊 Digital Medicine Cabinet ("Shelf")
- **Comprehensive Medicine Registry**: Trade name, dosage form (tablets, capsules, syrups, drops, inhalers, creams, etc.), active substance, strength, and therapeutic category.
- **Automated Stock Monitoring**: Visual alerts and notifications for low stock ($\le 5$ units) and out-of-stock items ($0$ units).
- **One-Tap Restock Dialog**: Convenient quick presets (+10, +20, +30, +60 units) or custom amount input.
- **Expiry Date Tracking**: Badges for active medications, items expiring within 30 days, and expired drugs.
- **Color Identification**: Visual color markers for immediate recognition of pill boxes.
- **Live Search & Filter**: Real-time search across names and active ingredients, plus category chip filters.

#### 2. ⏰ Daily Schedule & Adherence
- **Flexible Regimens**:
  - `DAILY`: Routine daily schedule (Morning 08:00, Afternoon 14:00, Evening 20:00).
  - `EVERY_N_HOURS`: Cyclic interval timing (e.g. every 4, 6, or 8 hours).
  - `COURSE`: Fixed duration therapy courses (e.g., 7-day antibiotic course).
  - `AS_NEEDED`: Symptomatic or PRN intake.
- **Meal-time Context**: Instructions for taking "Before meal", "With meal", "After meal", or "Anytime".
- **One-Tap Actions**:
  - **"Take"**: Decrements 1 dose from your inventory and records the intake timestamp.
  - **"Skip"**: Records a skipped dose without decrementing shelf stock.
- **Daily Adherence Indicator**: Visual progress bar reflecting completed daily doses.

#### 3. 💰 Prices & Ukrainian Pharmacies (Tabletki.ua)
- **Honest data policy**: the app only shows prices actually received from Tabletki.ua. Fabricated or "estimated" prices are never generated.
- **Current limitation (September 2026)**: the Tabletki.ua API blocks non-browser clients (Cloudflare, HTTP 403), so automated price comparison is unavailable — the screen honestly shows "prices unavailable".
- **Quick Tabletki.ua shortcut**: a "Open in tabletki.ua" button per medication — availability and prices open in the browser, where no restrictions apply.
- **Optional background check**: an in-app toggle, disabled by default. If the service becomes reachable again, monitoring resumes; trends are computed only from actually received records.

#### 4. ⚠️ Medication Interaction Hints (a few built-in rules)
> **This is NOT a full interaction checker.** Only a handful of simple rules are checked — the UI always makes clear these are hints, not medical advice.

- **Paracetamol Duplication Guard**: Detects concurrent usage of paracetamol powders/hot drinks (Fervex, Theraflu) and standard tablets to prevent overdose.
- **NSAID Co-Administration Warning**: Warns against taking multiple non-steroidal anti-inflammatory drugs simultaneously.
- **Antibiotic Course Guidance**: Full course completion reminders.
- **Medical disclaimer**: the app is a tracking tool and does not replace a doctor or pharmacist.

#### 5. 📜 Intake History & Compliance Analytics
- Chronological timeline of all intake events (taken, skipped).
- Overall treatment compliance percentage (Adherence rate).
- **Undo Action**: Revert accidental intake logs, restoring shelf stock immediately.
- **Overdue doses**: if a reminder goes unanswered, the dose is marked "Overdue" in the schedule instead of silently disappearing.
- **Retroactive logging**: mark a dose with its real time ("taken on time"), not the moment the button was pressed.
- **Data export**: an "Export" button in the history screen — a JSON copy of the cabinet and intake log via the system save dialog.

#### 6. 🔔 Reminders (Exact Alarms + WorkManager)
- **Exact alarms via `setAlarmClock()`**: dose notifications fire on time — exempt from Doze restrictions, show the system alarm icon.
- **Missed-dose finalization**: if an intake is not confirmed within 30 minutes of the reminder, an "Overdue" entry is written to the history.
- **Permission status**: the schedule screen shows whether exact alarms are allowed and links to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`.
- `ReminderWorker`: a 15-minute safety-net schedule sweep.
- `PriceCheckWorker`: 12-hour background price check — **disabled by default**, enabled manually.
- Low stock and out-of-stock notification triggers.
- Alarm restoration after device reboot (`BootReceiver`).

---

### 🛠️ Build & Installation Instructions

#### Prerequisites
- **JDK**: Java 21 LTS
- **Android Studio**: Ladybug (2024.2.1) or newer
- **Android SDK**: API 36 (Android 16), Minimum SDK API 26 (Android 8.0 Oreo)
- **Gradle**: 9.3.1 (Android Gradle Plugin 9.1.1)

#### Cloning and Building
1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/pillshelf.git
   cd pillshelf
   ```
2. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run unit tests:
   ```bash
   ./gradlew testDebugUnitTest
   ```
4. Install onto a connected device or emulator:
   ```bash
   ./gradlew installDebug
   ```

---

### 🗺️ Project Roadmap
- [x] **Core Medicine Cabinet**: Inventory management, stock levels, and expiration monitoring.
- [x] **Intake Schedule & Reminders**: Daily dose tracking, stock auto-decrement, exact alarms (`setAlarmClock`).
- [x] **Tabletki.ua Shortcut**: Honest price handling — only real data, never fabricated.
- [x] **Interaction Hints**: Paracetamol duplication and multi-NSAID warnings (a few built-in rules).
- [x] **JSON Data Export**: Manual backup of the cabinet and intake log (the only backup, since cloud is disabled).
- [ ] **Barcode & QR Code Scanner**: Swift medication entry via device camera.
- [ ] **NFC Tag Logging**: Tap medicine box NFC stickers to mark doses as taken.
- [ ] **PDF Medical Report Export**: Generate comprehensive adherence reports for doctors.
- [ ] **Encrypted Backup & Restore**: Optional synchronization via WebDAV or Google Drive.
- [ ] **Wear OS Companion App**: Log doses directly from your smartwatch.

---

## 📄 Ліцензія / License Attribution

Розповсюджується на умовах ліцензії **Apache License, Version 2.0**.

Distributed under the **Apache License, Version 2.0**.

```
Copyright 2026 Yukhym Shulha

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
