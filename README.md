# 📹 Live Video Streaming System (Pure Java)

A complete end-to-end live video streaming system built entirely in Java —
an **Android phone** captures camera frames and streams them over **Wi-Fi**
to a **Java desktop application** that displays the feed live.

No third-party streaming libraries. No Node.js. No Python. Just the JDK,
CameraX, and OkHttp.

## ✨ Features

- 📱 **Android camera app** using CameraX (preview + frame analysis)
- 🖥️ **Laptop server** using built-in `com.sun.net.httpserver.HttpServer`
- 🎨 **Swing GUI** for flicker-free real-time rendering
- 🔧 **Runtime-configurable target IP** (no rebuild to change laptops)
- 🚦 **Backpressure-aware** — drops frames instead of piling up
- 📦 **Zero server dependencies** — JDK only

## 🏗️ Architecture

```
┌──────────────┐   HTTP POST /upload    ┌──────────────┐
│   ANDROID    │  ─────────────────►    │    LAPTOP    │
│              │   JPEG byte array      │              │
│  CameraX     │   ~15 fps              │  HttpServer  │
│  → JPEG      │                        │  → ImageIO   │
│  → OkHttp    │                        │  → Swing     │
└──────────────┘                        └──────────────┘
```

## 📂 Project Structure

See [Folder Structure](#) in the docs (or the tree above).

## 🚀 Quick Start

### Prerequisites

| Tool | Version |
|---|---|
| JDK (server) | 11+ (tested with 26) |
| JDK (Android build) | 17 |
| Android SDK | Platform 33, Build-Tools 33.0.1 |
| Gradle Wrapper | 8.1.1 (auto-downloaded) |

### 1. Run the laptop server

```bash
cd laptop-video-server

# Compile
javac --add-modules jdk.httpserver -d out src/main/java/com/stream/server/*.java

# Run
java --add-modules jdk.httpserver -cp out com.stream.server.Main
```

Note the printed IPv4 address (e.g. `192.168.1.42`).
Allow Java through Windows Firewall on **Private networks**.

### 2. Build the Android APK

```bash
cd mobile-camera-app

# Create local.properties pointing to your Android SDK:
echo sdk.dir=/path/to/Android/Sdk > local.properties

# Build
./gradlew assembleDebug    # Linux/macOS
gradlew.bat assembleDebug  # Windows
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### 3. Install and stream

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

1. Ensure phone and laptop are on the **same Wi-Fi**
2. Open the app, enter the laptop IP + port `8080`
3. Tap **START** → allow camera
4. Watch the Swing window go live

## ⚙️ Configuration

| Where | Constant | Default | Effect |
|---|---|---|---|
| `CameraManager.java` | `TARGET_FPS` | 15 | Frame rate cap |
| `CameraManager.java` | `JPEG_QUALITY` | 60 | Image quality (lower = less bandwidth) |
| `StreamServer.java` | `MAX_FRAME_BYTES` | 4 MB | Server-side size limit |
| `Main.java` | `DEFAULT_PORT` | 8080 | HTTP port |

## 🐛 Troubleshooting

| Symptom | Fix |
|---|---|
| Laptop stays black | Allow Java through Windows Firewall (Private networks) |
| `INSTALL_FAILED_USER_RESTRICTED` | Enable "Install via USB" in Developer Options |
| `Unsupported class file major version 70` | Use JDK 17 for the Android build |
| No frames arriving | Both devices on same Wi-Fi? Check laptop IP |

## 📸 Screenshots

<!-- Add a screenshot of the app + laptop window here -->
| Android App | Laptop Receiver |
|---|---|
| _(add screenshot)_ | _(add screenshot)_ |

## 🛠️ Tech Stack

- **Server**: Java 11+, Swing, `com.sun.net.httpserver`, `javax.imageio`
- **Android**: Java, CameraX, OkHttp, AndroidX

## 📄 License

MIT — see [LICENSE](LICENSE).

## 🙏 Acknowledgements

Built as a learning exercise in end-to-end Java systems — from Android
camera pipeline to desktop rendering, with no frameworks in between.
