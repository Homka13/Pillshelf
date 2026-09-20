# Pillshelf 💊📦

A modern, offline-first smart medicine cabinet and pill adherence tracker built with Kotlin and Jetpack Compose for Android.

## Features

- **Digital Medicine Cabinet ("Shelf")**:
  - Organize household medications, vitamins, and prescriptions with custom forms (tablets, capsules, syrups, drops, inhalers, topical creams).
  - Track stock levels with automated low-stock warnings and reminders to restock.
  - Expiry date monitoring with visual indicators for valid, expiring soon, and expired medications.
  - Filter by category (Pain Relief, Supplements, Allergy & Sinus, Prescription, First Aid, etc.) and search instantly.
- **Daily Medication Schedule**:
  - Track daily routines (Morning, Afternoon, Evening, Bedtime) and as-needed (PRN) doses.
  - One-tap "Take Dose" action that automatically decrements shelf inventory and logs the intake timestamp.
  - Daily adherence progress bar and completion percentage.
  - Easy "Skip" and "Undo" actions.
- **Activity & Adherence History**:
  - Complete historical log of all taken and skipped medications with timestamps and dosages.
  - Adherence metrics and logs stream.
- **Restock Manager**:
  - Quick-preset restock dialog (+10, +20, +30, +60, +100) or custom quantities.
- **Modern Material 3 Design**:
  - Dynamic theming with light and dark mode support.
  - Adaptive layout with Navigation Rail on tablets / foldables and Navigation Bar on mobile.

## Tech Stack

- **Platform**: Android (SDK 36, Min SDK 26)
- **Language**: Kotlin 2.2
- **UI Toolkit**: Jetpack Compose with Material Design 3
- **Architecture**: MVVM with Clean Architecture principles
- **Local Persistence**: Room Database 2.7 with Kotlin Coroutines & Flow (via KSP)
- **Toolchain**: AGP 9.1.1, Gradle 9.3.1, Java 21
