"""
Generate Android app icons from assets/logo.png
- Replaces legacy mipmap webp files with PNGs at standard densities
- Updates adaptive icon drawables (background + foreground)
"""
from PIL import Image
import os

LOGO_PATH = r"C:\Users\John Victor\Documents\Development\AttentionPanner\assets\logo.png"
RES_DIR = r"C:\Users\John Victor\Documents\Development\AttentionPanner\app\src\main\res"
LOGO = Image.open(LOGO_PATH)

# --- 1. Generate legacy mipmap PNGs ---
# Standard icon sizes (density -> px)
DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Crop to square from center, then resize
w, h = LOGO.size
size = min(w, h)
left = (w - size) // 2
top = (h - size) // 2
cropped = LOGO.crop((left, top, left + size, top + size))

for density, px in DENSITIES.items():
    mipmap_dir = os.path.join(RES_DIR, f"mipmap-{density}")
    os.makedirs(mipmap_dir, exist_ok=True)
    resized = cropped.resize((px, px), Image.LANCZOS)
    for name in ("ic_launcher", "ic_launcher_round"):
        path = os.path.join(mipmap_dir, f"{name}.png")
        resized.save(path, "PNG")
        print(f"  Created {path}")

# --- 2. Update adaptive icon drawables ---

# 2a. Background: solid black (or change to any color)
background_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#000000"
        android:pathData="M0,0h108v108h-108z" />
</vector>
"""
with open(os.path.join(RES_DIR, "drawable", "ic_launcher_background.xml"), "w") as f:
    f.write(background_xml)
print("  Updated drawable/ic_launcher_background.xml")

# 2b. Foreground: reference the PNG bitmap with 10dp inset (72dp safe zone)
foreground_xml = """<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="@drawable/ic_launcher_logo"
    android:gravity="center"
    android:width="72dp"
    android:height="72dp" />
"""
with open(os.path.join(RES_DIR, "drawable", "ic_launcher_logo.xml"), "w") as f:
    f.write(foreground_xml)
print("  Created drawable/ic_launcher_logo.xml")

# 2c. Point the adaptive icon foreground to the new bitmap
adaptive_fg = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_logo" />
</adaptive-icon>
"""
# Update both ic_launcher.xml and ic_launcher_round.xml
for name in ("ic_launcher.xml", "ic_launcher_round.xml"):
    path = os.path.join(RES_DIR, "mipmap-anydpi-v26", name)
    with open(path, "w") as f:
        f.write(adaptive_fg)
    print(f"  Updated {path}")

# --- 3. Save logo copy to drawable-nodpi for bitmap reference ---
nodpi_dir = os.path.join(RES_DIR, "drawable-nodpi")
os.makedirs(nodpi_dir, exist_ok=True)
nodpi_path = os.path.join(nodpi_dir, "ic_launcher_logo.png")
cropped.save(nodpi_path, "PNG")
print(f"  Created {nodpi_path}")

print("\nDone. All icons generated.")
