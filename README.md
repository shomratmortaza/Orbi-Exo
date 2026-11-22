# ORBI

**One Ecosystem. Every Device.**

ORBI is an installable web app (PWA) with a Liquid Glass "Dynamic Island" at the top of the screen, a tiny animated pixel pet inside it, and a set of everyday productivity tools. It runs in any modern browser and can be added to the home screen on Windows, Mac, Android and iPhone.

It is a static site plus one small serverless function. There is no build step and no database.

## What's included

| Module | What it does |
|---|---|
| Island | Floating glass pill with resting, hover, timer, music, notification and expanded states. Click to open, click outside or press Esc to close. |
| Pixel pet | Canvas-drawn pet inside the island. 7 characters (cat, dog, bunny, panda, fox, bear, orbi). Blinks, looks at your cursor, sleeps when idle, wakes, dances to music, reacts to clicks and notifications. Feed, play and pet actions with saved stats. |
| Music | Plays audio files from your device. Visualizer, seek, volume, playlist, synced `.lrc` lyrics, system media keys. |
| Clipboard | History of text you copy or paste on the page, plus a capture button. Search, pin, pause, private mode. Card-number-like text is never saved. |
| File shelf | Drag and drop, previews, open, pin, and share through the browser share sheet. Files stay in memory for the session. |
| Home widgets | Clock, timer, stopwatch, weather (Open-Meteo), battery, device and storage info, notification history. |
| Notes and tasks | Saved locally. |
| Launcher | `Ctrl/⌘ + K` searches tabs, notes, tasks and clips, runs `timer 10`, opens websites. |
| Settings | Width, blur, transparency, corner radius, animation speed, pet size, accent color, light/dark, reduced motion, export/import, reset. |

## Project structure

```
orbi/
  index.html        the whole app (HTML, CSS, JS)
  manifest.json     install metadata
  sw.js             service worker (offline support)
  vercel.json       cache header for the service worker
  icons/            app icons (192, 512, maskable, Apple touch)
```

## Deploy to Vercel

1. Put the `orbi` folder in a Git repo, or use the CLI from inside the folder:
   ```
   npm i -g vercel
   vercel --prod
   ```
2. That is all. No environment variables or keys are needed.

The AI chat tab is switched off for now. Its server code is kept in `_disabled/chat.js` (Gemini). To bring it back, move it to `api/chat.js`, add `GEMINI_API_KEY` in Vercel, and add the AI tab back in `index.html`.

## Install on a device

- **Windows / Mac (Chrome or Edge):** open the site, click the Install icon in the address bar, or the Install button on the welcome card. ORBI opens in its own window with a Start menu or Dock icon.
- **Android (Chrome):** menu → Add to Home screen (or Install app).
- **iPhone (Safari):** Share → Add to Home Screen.

The site must be served over HTTPS, which Vercel does by default.

## Privacy

- No accounts, no analytics, no tracking.
- Notes, tasks, clipboard history, settings and pet stats are stored in your browser's local storage on your own device.
- Network calls: weather (only after you press "Use my location"). Nothing else leaves the device.
- Use Settings → Export data for a backup, or Reset everything to erase it.

## Known limitations

A browser app cannot do these. They need native apps.

- Float above other apps or the Windows desktop (always-on-top overlay), tray icon, global shortcuts.
- Real CPU, GPU, FPS or temperature readings. The Home card shows only what the browser exposes (core count, approximate memory).
- Android system overlay, iOS Live Activities, or the real iPhone Dynamic Island.
- Read other apps' playback (Spotify etc.) or other apps' notifications.

Browser support varies:

- Battery info: Chromium browsers only (not Safari or Firefox).
- Notifications on iPhone: iOS 16.4 or later, and only after adding ORBI to the home screen.
- Clipboard capture: needs a tap and permission on most browsers.
- The island and music stop when the app is closed.

## Not built yet

AI agent center (Codex, Claude Code, Gemini activity), gaming center, creator center (OBS, Twitch, YouTube chat), web drawer, plugin system, phone-PC sync with QR pairing and file transfer, pet outfits and accessories, and a full onboarding flow. Sync and QR transfer would need a backend; the native-only items above would need Windows, Android and iOS apps.

## Local development

Serve the folder with any static server, for example `npx serve .`. The service worker and install prompt need `localhost` or HTTPS.

## Status

Syntax-checked only. It has not yet been tested across browsers or on real devices, so test on your target devices before sharing it widely.

## License

Copyright (c) 2026 Shomrat Mortaza. All rights reserved.

This project is proprietary. You may not use, copy, modify, distribute, host or fork any part of it without prior written permission from Shomrat Mortaza. See [LICENSE](LICENSE).

## Desktop app (Windows)

The `desktop/` folder wraps ORBI in Electron so the island floats at the top center of the screen above other windows, like NotchPixel.

- Transparent, frameless, always-on-top window. Clicks pass through to your apps except on the island itself.
- Tray icon (open/close, hide, choose display, start with Windows, quit) and the global shortcut `Ctrl+Alt+O`.
- Loads the live site (`orbi-exo.vercel.app`), so redeploying updates the desktop app too. It needs the internet on first launch, then works from cache.

Build the installer: push this repo to GitHub, open **Actions → Build Windows app → Run workflow**, and download the `.exe` from the run's artifacts. Or on Windows: `cd desktop && npm install && npm start` (test) / `npm run dist` (installer in `desktop/dist`).

Notes: the installer is unsigned, so Windows SmartScreen will warn ("More info → Run anyway"). Because the window is transparent, the glass is a tinted translucent pill; it cannot blur the desktop behind it. The app does not inject into other processes, so it does not interfere with games or anti-cheat, but it will appear above fullscreen apps unless you hide it from the tray.

## Downloads (Windows and Android)

Both apps load the live ORBI site (a Vercel redeploy updates them). The website has a real download page at `/download` that reads the latest public GitHub Release.

Full publishing steps: [docs/PUBLISH.md](docs/PUBLISH.md). Asset names must stay `ORBI-Setup-x64.exe` and `ORBI.apk`.

iPhone cannot install apps outside the App Store; use Safari → Share → Add to Home Screen.

New in this version: your own name next to the pet (Pet tab or the setup card), Alarms tab, Calendar tab with timed reminders.
