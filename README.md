# ZenUtility

> **ZenUtility** is a modern Minecraft Fabric client utility & quality-of-life mod featuring custom visual indicators, tool protection, dropped item scaling, inventory counters, and customizable settings.

[![Minecraft](https://img.shields.io/badge/Minecraft-26.2%20%7C%201.21.11-brightgreen.svg)](https://fabricmc.net/)
[![Fabric](https://img.shields.io/badge/ModLoader-Fabric-blue.svg)](https://fabricmc.net/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

---

## Features

- **Spotify In-Air 3D Floating Lyrics:** Displays synchronized real-time lyrics floating seamlessly in 3D world space in front of your crosshair (or as a 2D HUD card). Automatically detects Spotify playback, fetches synced lyrics with zero external native DLLs, features smooth inertia lerping, customizable distance & height, color presets (Cyan, Gold, Pink, Emerald, Rainbow, etc.), sync timing offset, and demo song mode.
- **Low Durability Tool Protection:** Warns when holding tools under 10 durability (customizable threshold, sound, and screen title) and cancels block mining to prevent tools from breaking.
- **Custom Damage Flash (Hit Color):** Fully customizable damage flash indicator (Red, Gold, Cyan, Purple, Rainbow, or any custom HEX code).
- **Custom Totem Pop Flash & Particles:** Custom particle color & flash effect on totem activation with presets and custom HEX input.
- **1:1 Vanilla Glint Tinting:** Real-time enchanted item glint color customization without shaders or external textures.
- **Dropped Item Scale:** Scale individual dropped items and weapons on the ground with an in-game item search bar and interactive steppers.
- **Inventory Counter HUD:** Compact on-screen HUD tracking items in your inventory with custom search, count badges, and dragging.
- **Keybindings & Slash Commands:** Easily access features via `/zen`, `/pvputils`, `/utility`, `/lyrics`, or configurable hotkeys in a dedicated ZenUtility category.

---

## Controls & Keybinds

| Action | Default Key / Command | Configurable in Settings |
|---|---|---|
| Open ZenUtility GUI | `U` or `/zen`, `/pvputils`, `/utility` | Yes (Controls -> Key Binds) |
| Song Lyrics GUI | `/lyrics` | Yes |
| Toggle Song Lyrics | Not bound by default | Yes |
| Toggle Hit Color | `H` | Yes |
| Toggle Totem Pop Color | `J` | Yes |
| Toggle Dura Protection | Not bound by default | Yes |

---

## Building from Source

### Prerequisites
- **JDK 21+**
- Git

### Build Instructions
```bash
git clone https://github.com/Z3ntrix603/ZenUtility.git
cd ZenUtility
./gradlew build
```
The compiled `.jar` file will be located in `build/libs/`.

---

## License
This project is licensed under the [MIT License](LICENSE).
