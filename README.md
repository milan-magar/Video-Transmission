# Live Video Streaming (Pure Java)

Android phone → Wi-Fi → Java Swing desktop.

- 📱 Android: CameraX captures frames → JPEG → OkHttp POST
- 💻 Laptop: HttpServer receives → ImageIO decodes → Swing displays

## Build & Run

### Server
    cd laptop-video-server
    javac --add-modules jdk.httpserver -d out src/main/java/com/stream/server/*.java
    java --add-modules jdk.httpserver -cp out com.stream.server.Main

### Android
    cd mobile-camera-app
    gradlew.bat assembleDebug
    adb install -r app/build/outputs/apk/debug/app-debug.apk

## Setup

1. Both devices on the same Wi-Fi
2. Allow Java through Windows Firewall (Private networks)
3. Enter laptop IP + port 8080 in the app → START
