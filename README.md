---

# Chat to Command (C2C)

Chat to Command (C2C) is a lightweight utility mod for Minecraft that detects specific chat messages and automatically executes assigned commands or actions. Easily create custom triggers for global use or configure separate profiles for specific servers through an intuitive in-game interface.

---

## 🌟 Features

* **Chat Detection:** Automatically detects specified messages in the game chat.
* **Auto Command Execution:** Executes assigned commands instantly when a trigger is detected.
* **Chat Automation:** Sends normal chat messages automatically.
* **Multi-Command Triggers:** Supports binding multiple actions/commands to a single trigger.
* **Custom Delays:** Configure individual execution delays for each command.
* **Shift Confirmation:** Requires rapid Shift presses before executing sensitive commands.
* **Action Bar Feedback:** Displays clear confirmation progress directly in the Action Bar.
* **Audio Cues:** Plays sound effects when confirmation is requested, pressed, completed, or cancelled.
* **Auto-Cancel Timeout:** Automatically cancels pending actions if Shift is not pressed within 5 seconds.
* **Profile Management:**
* **Global Profile:** Works across all worlds and servers.
* **Server Profiles:** Custom triggers for specific servers.


* **In-Game GUI:** Fully configurable via a user-friendly menu.
* **Management Options:** Add, remove, enable, disable, expand, and edit triggers on the fly.
* **Quick Access:** Use the `/c2c` command or a customizable keybind to open the menu.

---

## ⚡ Supported Actions

Each trigger can execute multiple actions sequentially:

* `/home` — Executes a console command.
* `Hello everyone` — Sends a standard chat message.
* `notify:Task complete` — Displays an in-game notification.
* `title:Ready` — Displays a title message on screen.
* `sound:any` — Plays a confirmation or custom sound.

---

## 🛡️ Shift Confirmation System

For sensitive triggers requiring safety confirmation, C2C displays a prompt in the Action Bar:

> **Press Shift rapidly to confirm**

Press **Shift three times** in quick succession to confirm the action. A 10-segment progress bar updates in real time as you press Shift. If confirmation is not completed within **5 seconds**, the pending action is cancelled, and C2C alerts you with a red cancellation message.

---

## ⚙️ Configuration & Commands

Open the C2C configuration menu using the command:

```text
/c2c

```

Alternatively, you can assign a keybind in Minecraft's standard **Controls** menu.

### Profiles

C2C maintains separate profiles for global settings and individual servers. Server profiles automatically identify servers using their domain/IP address (excluding port numbers) for seamless switching.

---

## 📋 Requirements

* **Minecraft:** 1.21.x / 26.2
* **Loader:** Fabric Loader
* **Dependencies:**
* Fabric API
* Fabric Language Kotlin



---

## 👨‍💻 Credits

Created by **Losoler**
