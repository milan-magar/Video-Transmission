"""
Object detector that pulls frames from the Java server's /latest endpoint
and runs YOLOv8 on them in real time.
"""

import cv2
import numpy as np
import requests
import time
from ultralytics import YOLO

# ============================================================
#  CONFIGURATION
# ============================================================
SERVER_URL       = "http://localhost:8080/latest"
MODEL_PATH       = "yolov8n.pt"
CONFIDENCE       = 0.35
SHOW_WINDOW      = True
PRINT_DETECTIONS = True
WINDOW_NAME      = "Phone Camera - YOLO Detection"
INITIAL_W        = 960       # initial window width
INITIAL_H        = 540       # initial window height
# ============================================================


def fetch_latest_frame(url, timeout=1.0):
    try:
        r = requests.get(url, timeout=timeout)
    except requests.RequestException:
        return None
    if r.status_code != 200 or not r.content:
        return None
    arr = np.frombuffer(r.content, np.uint8)
    frame = cv2.imdecode(arr, cv2.IMREAD_COLOR)
    return frame


def resize_to_fit(image, window_w, window_h):
    """
    Scale image to fit inside a window of size (window_w, window_h),
    preserving aspect ratio and adding black letterbox bars when needed.
    """
    if window_w <= 0 or window_h <= 0:
        return image

    h, w = image.shape[:2]
    scale = min(window_w / w, window_h / h)
    new_w = max(1, int(w * scale))
    new_h = max(1, int(h * scale))

    resized = cv2.resize(image, (new_w, new_h), interpolation=cv2.INTER_AREA)

    # Create a black canvas and center the resized image on it
    canvas = np.zeros((window_h, window_w, 3), dtype=np.uint8)
    x_off = (window_w - new_w) // 2
    y_off = (window_h - new_h) // 2
    canvas[y_off:y_off + new_h, x_off:x_off + new_w] = resized
    return canvas


def main():
    print(f"[detect] Loading model: {MODEL_PATH}")
    model = YOLO(MODEL_PATH)
    print(f"[detect] Model loaded. Polling {SERVER_URL}")
    print("[detect] Controls:  q = quit   f = toggle fullscreen")

    # --- Window setup (resizable, aspect-ratio kept) ---
    if SHOW_WINDOW:
        cv2.namedWindow(WINDOW_NAME, cv2.WINDOW_NORMAL)
        cv2.resizeWindow(WINDOW_NAME, INITIAL_W, INITIAL_H)

    frames_processed = 0
    last_fps_print   = time.time()
    fps_window_start = time.time()
    is_fullscreen    = False

    while True:
        # --- Grab latest frame ---
        frame = fetch_latest_frame(SERVER_URL)
        if frame is None:
            time.sleep(0.05)
            continue

        # --- Detect ---
        results = model(frame, conf=CONFIDENCE, verbose=False)[0]
        annotated = results.plot()

        if PRINT_DETECTIONS and results.boxes is not None:
            for box in results.boxes:
                cls_id   = int(box.cls[0])
                cls_name = model.names[cls_id]
                conf     = float(box.conf[0])
                x1, y1, x2, y2 = box.xyxy[0].cpu().numpy().astype(int)
                print(f"[detect] {cls_name:12s} {conf:.2f}  box=({x1},{y1})-({x2},{y2})")

        frames_processed += 1

        # --- Display: read window size, scale image to fit, show ---
        if SHOW_WINDOW:
            try:
                # Returns (x, y, width, height) of the *client* area of the window
                _, _, win_w, win_h = cv2.getWindowImageRect(WINDOW_NAME)
            except Exception:
                win_w, win_h = INITIAL_W, INITIAL_H

            if win_w > 0 and win_h > 0:
                display = resize_to_fit(annotated, win_w, win_h)
            else:
                display = annotated

            cv2.imshow(WINDOW_NAME, display)

            key = cv2.waitKey(1) & 0xFF
            if key == ord('q'):
                break
            elif key == ord('f'):
                is_fullscreen = not is_fullscreen
                prop = cv2.WINDOW_FULLSCREEN if is_fullscreen else cv2.WINDOW_NORMAL
                cv2.setWindowProperty(WINDOW_NAME, cv2.WND_PROP_FULLSCREEN, prop)

        # --- FPS report once per second ---
        now = time.time()
        if now - last_fps_print >= 1.0:
            elapsed = now - fps_window_start
            fps = frames_processed / elapsed if elapsed > 0 else 0
            print(f"[detect] FPS: {fps:.1f}    frames: {frames_processed}")
            frames_processed = 0
            fps_window_start = now
            last_fps_print = now

    cv2.destroyAllWindows()
    print("[detect] Exited.")


if __name__ == "__main__":
    main()