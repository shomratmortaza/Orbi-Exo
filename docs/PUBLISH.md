# ORBI — Publish step by step

## 0. One-time: put your GitHub username in
Already set to `shomratmortaza/Orbi-Exo` in `release-config.js`.

## 1. Push to GitHub (VS Code terminal, inside the orbi folder)
```
git init
git add .
git commit -m "ORBI v1.0.0"
git branch -M main
git remote add origin https://github.com/shomratmortaza/Orbi-Exo.git
git push -u origin main
```
Repo: https://github.com/shomratmortaza/Orbi-Exo (if not created yet: New repository → name `Orbi-Exo` → **Public** → no README. Before pushing, check `git status` shows no `.jks` or `.env` files.

## 2. Android signing key (one time, do NOT commit it)
```
keytool -genkeypair -v -keystore orbi.jks -alias orbi -keyalg RSA -keysize 2048 -validity 10000
```
Remember the password. Convert to text: Windows PowerShell `[Convert]::ToBase64String([IO.File]::ReadAllBytes("orbi.jks"))`, Mac/Linux `base64 -w0 orbi.jks`.
GitHub repo → Settings → Secrets and variables → Actions → New secret:
- `ANDROID_KEYSTORE_BASE64` = the long text
- `ANDROID_KEYSTORE_PASSWORD` = your password
- `ANDROID_KEY_ALIAS` = `orbi`

Keep `orbi.jks` backed up privately. If lost, users must uninstall before updating.

## 3. Test build (no public release)
Actions tab → **Release** → Run workflow. Download `windows` / `android` artifacts and test them yourself.

## 4. Public release
```
git tag v1.0.0
git push origin v1.0.0
```
Wait for the Actions run to turn green. The Releases page then has `ORBI-Setup-x64.exe`, `ORBI.apk`, `SHA256SUMS.txt`. Next versions: `v1.0.1` etc. The website updates itself.

## 5. Vercel
Import the repo in Vercel (Framework: Other, no build command, output = root). Every push redeploys. Then open `/download`.

## 6. Verify (open a private/incognito window, not logged in to GitHub)
- `https://github.com/shomratmortaza/Orbi-Exo/releases/latest/download/ORBI-Setup-x64.exe` downloads
- `https://github.com/shomratmortaza/Orbi-Exo/releases/latest/download/ORBI.apk` downloads
- `/download` shows the version and working buttons

## Release a new version
Change nothing in the website. Just `git tag v1.0.1 && git push origin v1.0.1`.

## 7. Song search key (one time, optional but recommended)
ORBI's "search a song" runs on your Vercel site, so users need no key. You add ONE key, privately:
1. console.cloud.google.com -> create a project -> APIs & Services -> Library -> enable **YouTube Data API v3**.
2. Credentials -> Create credentials -> **API key**. Under "API restrictions" choose YouTube Data API v3 only.
3. Vercel -> your project -> Settings -> **Environment Variables** -> name `YOUTUBE_API_KEY`, value = the key -> Save.
4. Deployments -> latest -> **Redeploy**.

Never put the key in the repo. Free quota is 10,000 units per day, one search costs 100 units, so about 100 new searches a day in total. Repeated searches are cached. If the limit is hit, users can still paste a link.
