# Alldebrid Tool for Android

A native Android app for the [AllDebrid](https://alldebrid.com) service, built with **Kotlin**, **Jetpack Compose**, and **Material 3**.

![Platform](https://img.shields.io/badge/platform-Android-3DDC84?logo=android)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin)
![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)
![Made with AI](https://img.shields.io/badge/made%20with-AI-blueviolet)

> 🤖 **A fun little AI-built app.** This project was built with the help of AI as a personal/hobby project — not a polished commercial product, just something made for fun and to learn along the way.

## ✨ Features

- 🔑 **Login with API key** — verified live against AllDebrid's `/v4/user` endpoint and stored securely on-device.
- 🔗 **Unlock Link tab** — paste direct hoster links (Mega, 1fichier, etc.) to get direct high-speed download links.
- 🧲 **Add Magnet tab** — paste a magnet link and watch it download with a live progress bar and percentage.
- 📁 **My Files tab** — access your entire library, including saved links and processing history.
- 📺 **Stream Support** — open videos directly in external players like VLC or MX Player.
- 🚪 **One-Tap Actions** — instant "Get" menu for downloading, streaming, or copying links.
- 🌗 **Light / Dark / System theme** toggle, persisted across launches.
- ⚡ **High Performance** — supports 120Hz+ refresh rates for buttery smooth swiping and scrolling.
- 🎨 Built entirely with **Material 3** — modern design with an edge-to-edge immersive experience.

## 🏗️ Project structure

```
app/src/main/java/com/example/alldebrid/
├── data/            # Retrofit API interface, models, repository, DataStore prefs
├── viewmodel/       # AppViewModel — all app state & business logic
├── ui/              # General UI components and theme
└── ui/screens/      # LoginScreen, HomeScreen, UnlockTab, MagnetTab, FilesTab, HostsScreen
```

## 🚀 Getting started

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Ladybug or newer recommended)
- An Android device or emulator running **API 24+**
- An [AllDebrid](https://alldebrid.com) account and API key

### Setup
1. Clone the repo:
   ```bash
   git clone https://github.com/<your-username>/<your-repo>.git
   cd <your-repo>
   ```
2. Open the project folder in Android Studio and let Gradle sync.
3. Run on a device or emulator.
4. On first launch, paste your AllDebrid API key — get it from **AllDebrid → Account → My Apps** (or the API keys page on alldebrid.com) — and tap **Log in**.

## 🔧 Configuration notes

- **API base URL**: `https://api.alldebrid.com/v4/` (using v4.1 for magnets).
- **High Refresh Rate**: Automatically requests the highest refresh rate available on the device for fluid animations.
- **Downloads**: handled by Android's built-in `DownloadManager`, saving to the public Downloads folder.
- **Persistent Trash**: Deleted history items are filtered out locally to ensure they don't reappear after app restarts.

## 🤝 Contact

Created with ❤️ and AI. 
Reach out on Telegram: [@hjil_l](https://t.me/hjil_l)

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

## ⚠️ Disclaimer

This is an unofficial, community-built client and is not affiliated with or endorsed by AllDebrid. You are responsible for complying with AllDebrid's terms of service and all applicable laws in your jurisdiction.
