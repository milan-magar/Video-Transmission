# 📹 Live Video Streaming + Object Detection (Pure Java)

An end-to-end system that turns an **Android phone into a wireless camera**, streams frames to a **Java desktop server over Wi-Fi**, and optionally feeds them to a **Python YOLO object detector** — all without third-party streaming libraries.

No Node.js. No FFmpeg. No WebRTC. Just the JDK, CameraX, OkHttp, and Python for detection.

---

## 🏗️ System Architecture

```
┌─────────────────┐   HTTP POST /upload    ┌──────────────────┐   GET /latest   ┌───────────────────┐
│  ANDROID PHONE  │  ───────────────────►  │   JAVA SERVER    │  ◄────────────  │  PYTHON DETECTOR  │
│  (CameraX)      │   JPEG @ 15 FPS        │  (HttpServer +   │   polling       │  (YOLOv8 +        │
│                 │                        │   Swing or       │                 │   OpenCV)         │
│                 │                        │   headless)      │                 │                   │
└─────────────────┘                        └──────────────────┘                 └───────────────────┘
                                                    │
                                                    ▼
                                            ┌──────────────┐
                                            │  Swing GUI   │
                                            │  (mode 1)    │
                                            └──────────────┘
```

**Three components:**

| Component | Language | Purpose |
|---|---|---|
| **Android App** | Java + CameraX + OkHttp | Capture frames, compress to JPEG, POST to laptop |
| **Java Server** | Java + Swing + `com.sun.net.httpserver` | Receive frames, display in Swing, expose `/latest` |
| **Python Detector** | Python + Ultralytics YOLO + OpenCV | Fetch frames from `/latest`, detect objects, display annotated video |

---

## 📂 Repository Structure

```
VideoTransmission/
├── README.md
├── .gitignore
│
├── laptop-video-server/                     ← Module 1: Java server
│   └── src/main/java/com/stream/server/
│       ├── Main.java                        (entry + mode prompt + Python launcher)
│       ├── StreamServer.java                (HTTP + /upload + /latest)
│       └── VideoWindow.java                 (Swing rendering with auto-scale)
│
├── mobile-camera-app/                       ← Module 2: Android app
│   ├── build.gradle
│   ├── settings.gradle
│   ├── gradle.properties
│   ├── gradlew / gradlew.bat
│   └── app/
│       ├── build.gradle
│       └── src/main/
│           ├── AndroidManifest.xml
│           ├── res/layout/activity_main.xml
│           └── java/com/stream/app/
│               ├── MainActivity.java        (UI + runtime permissions)
│               ├── CameraManager.java       (CameraX → JPEG bytes)
│               └── NetworkStreamer.java     (OkHttp POST with backpressure)
│
└── object-detector/                         ← Module 3: Python YOLO detector
    ├── detect.py
    └── requirements.txt
```

---

## ✨ Features

- 📱 **Android CameraX** capture with live preview and throttled frame analysis (15 FPS)
- 🔒 **Runtime permissions** — camera requested on first use
- 🌐 **Configurable target IP** — change the laptop IP from the app, no rebuild
- 🚦 **Backpressure-aware** — one-in-flight HTTP policy drops frames instead of queuing
- 🖥️ **Java Swing viewer** with automatic aspect-ratio-preserving scaling
- 🐍 **Optional Python detector** — YOLOv8 in real time, auto-launched by the server
- 🎛️ **Two server modes** — Swing GUI, or headless feeding the Python detector
- 📸 **MJPEG-friendly** — server exposes `/latest` for any client (browser, Python, curl)

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| **JDK** | 11+ (26 tested) | Run Java server |
| **JDK 17** | 17.x | Required to build the Android APK |
| **Android SDK** | Platform 33, Build-Tools 33.0.1 | Android build |
| **Gradle Wrapper** | 8.1.1 (auto-downloaded) | Android build driver |
| **Python** | 3.9+ | Object detection (optional) |

### 1. Clone

```bash
git clone https://github.com/<yourname>/video-transmission.git
cd video-transmission
```

### 2. Build & run the Java server

```bash
cd laptop-video-server
javac --add-modules jdk.httpserver -d out src/main/java/com/stream/server/*.java
java  --add-modules jdk.httpserver -cp out com.stream.server.Main
```

You'll be prompted:

```
Select display mode:
  1 - Java Swing window
  2 - Python YOLO (auto-launches detect.py)
Enter choice [1/2]:
```

- Choose **1** for the Swing viewer.
- Choose **2** to skip Swing and launch the Python detector automatically.

**Note the IPv4 address** printed in the banner — you'll enter it into the phone app.

### 3. Build & install the Android app

```bash
cd mobile-camera-app
gradlew.bat assembleDebug          # Windows
./gradlew assembleDebug            # Linux/macOS
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> Requires JDK 17 for the Android build. On Windows:
> ```cmd
> set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.x.x-hotspot
> ```

### 4. Run the Python detector (optional, if using mode 2)

```bash
cd object-detector
pip install -r requirements.txt
python detect.py
```

### 5. Stream

1. Open the Android app.
2. Enter the laptop's IP and port `8080`.
3. Tap **START**, allow camera access.
4. Watch the Swing window (mode 1) or Python YOLO window (mode 2).

---

## ⚙️ Configuration

### Android (`CameraManager.java`)

| Constant | Default | Effect |
|---|---|---|
| `TARGET_FPS` | 15 | Frames per second sent to server |
| `JPEG_QUALITY` | 60 | Image quality (higher = larger frames) |

### Java Server (`StreamServer.java`)

| Constant | Default | Effect |
|---|---|---|
| `MAX_FRAME_BYTES` | 4 MB | Reject oversized uploads |
| Port | 8080 | Override with `Main <port>` |

### Python Detector (`detect.py`)

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_URL` | `http://localhost:8080/latest` | Frame source |
| `MODEL_PATH` | `yolov8n.pt` | YOLO model (any `yolov8*.pt` works) |
| `CONFIDENCE` | 0.35 | Minimum detection confidence |
| `SHOW_WINDOW` | `True` | Show OpenCV window |
| `PRINT_DETECTIONS` | `True` | Log detections to console |

**Model options:**

| Model | Size | Speed | Accuracy |
|---|---|---|---|
| `yolov8n.pt` | 6 MB | Fastest | Low |
| `yolov8s.pt` | 22 MB | Fast | Medium |
| `yolov8m.pt` | 50 MB | Medium | Good |
| `yolov8l.pt` | 84 MB | Slow | Better |

---

## 🔌 HTTP API

The Java server exposes two endpoints:

| Endpoint | Method | Purpose |
|---|---|---|
| `/upload` | POST | Receives JPEG frames from the phone |
| `/latest` | GET | Returns the newest JPEG frame |

Example:

```bash
# Fetch the current frame to a file
curl http://localhost:8080/latest -o frame.jpg

# From Python
import requests
r = requests.get("http://localhost:8080/latest")
open("frame.jpg", "wb").write(r.content)
```

Any client can consume `/latest` — Python, browser, curl, another Java app, etc.

---

## 🐍 Using the Detector Standalone

The Python detector works with **any** server that exposes a `/latest` JPEG endpoint. You don't need the Java server — replace the URL in `detect.py`:

```python
SERVER_URL = "http://192.168.1.42:8080/latest"
```

---

## 🧪 How It Works — Data Flow

1. **Android:** CameraX binds the rear camera to a `PreviewView`. In parallel, an `ImageAnalysis` analyzer receives frames with `STRATEGY_KEEP_ONLY_LATEST`.
2. **Throttle:** Frames are limited to ~15 FPS by comparing timestamps.
3. **Encode:** The RGBA buffer is copied to a `Bitmap`, compressed to JPEG (quality 60), producing ~30–80 KB.
4. **POST:** `NetworkStreamer` sends the JPEG to `http://<laptop-ip>:8080/upload`. An `AtomicBoolean` ensures only one request is in flight.
5. **Server receives:** `HttpServer` reads the body (up to 4 MB), decodes it via `ImageIO`.
6. **Cache:** The JPEG bytes are stored in `latestJpeg` for the `/latest` endpoint.
7. **Display:** The image is drawn in Swing (mode 1) on the Event Dispatch Thread.
8. **Detect (mode 2):** Python polls `/latest`, runs YOLO, draws boxes, and shows the result.

---

## 🐛 Troubleshooting

| Problem | Cause | Fix |
|---|---|---|
| `package com.sun.net.httpserver does not exist` | JDK 9+ module isolation | Add `--add-modules jdk.httpserver` to both `javac` and `java` |
| `Unsupported class file major version 70` | Gradle on JDK 26 | Install JDK 17, set `JAVA_HOME` for the Android build |
| `INSTALL_FAILED_USER_RESTRICTED` | Phone blocks adb installs | Enable **"Install via USB"** in Developer Options |
| `SDK location not found` | `local.properties` missing | Create it with `sdk.dir=<path>` |
| `local.properties` trailing space | `echo ... >` added a space | Use Notepad or `(echo ...)>file` |
| `Malformed \uxxxx encoding` | Single backslashes in `.properties` | Use forward slashes: `C:/Users/...` |
| `android.useAndroidX property is not enabled` | Missing `gradle.properties` | Add `android.useAndroidX=true` |
| Laptop shows black, no frames | Firewall or wrong IP | Allow Java on Private networks; verify IP in app |
| Python `ModuleNotFoundError` | Missing dependencies | `pip install -r requirements.txt` |
| Python `/latest` returns 204 | No frame received yet | Start streaming from the phone first |

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Android UI | Java, XML layouts, AppCompat |
| Android camera | CameraX (core, camera2, lifecycle, view) |
| Android HTTP | OkHttp |
| Server HTTP | `com.sun.net.httpserver.HttpServer` (built into JDK) |
| Server GUI | Java Swing |
| Image decoding | `javax.imageio.ImageIO` |
| Object detection | Ultralytics YOLOv8 |
| Python imaging | OpenCV, NumPy |

---

## 📄 License

MIT — see [LICENSE](LICENSE) if included.

---

## 🙏 Acknowledgements

Built as a learning exercise in end-to-end Java systems — from Android camera pipeline to desktop rendering — with zero third-party streaming libraries.
