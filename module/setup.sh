#!/data/data/com.termux/files/usr/bin/bash

clear
echo ""
echo "╔════════════════════════════════════════╗"
echo "║    RootmasterBD Saned — GitHub Setup   ║"
echo "╚════════════════════════════════════════╝"
echo ""

# Install deps
pkg install -y git curl unzip 2>/dev/null | grep -E "install|already"

echo ""
echo "[1/4] GitHub Username:"
read -r GH_USER

echo "[2/4] GitHub Token (ghp_...):"
read -r GH_TOKEN

echo "[3/4] Repo name (press Enter for: RootmasterBD-Saned):"
read -r GH_REPO
[ -z "$GH_REPO" ] && GH_REPO="RootmasterBD-Saned"

echo "[4/4] Commit message (press Enter for: Initial release):"
read -r GH_MSG
[ -z "$GH_MSG" ] && GH_MSG="RootmasterBD Saned - Initial release"

echo ""
echo "[*] Configuring git..."
git config --global user.name "$GH_USER"
git config --global user.email "$GH_USER@users.noreply.github.com"

# Create GitHub repo
echo "[*] Creating GitHub repo..."
HTTP=$(curl -s -o /dev/null -w "%{http_code}" -X POST \
  -H "Authorization: token $GH_TOKEN" \
  -H "Accept: application/vnd.github.v3+json" \
  https://api.github.com/user/repos \
  -d "{\"name\":\"$GH_REPO\",\"private\":false,\"description\":\"RootmasterBD Saned Performance Module\"}")

[ "$HTTP" = "201" ] && echo "[✓] Repo created!" || echo "[!] Repo: $HTTP (may already exist)"

# Setup project
WORKDIR="$HOME/rmbd_saned_build"
rm -rf "$WORKDIR"
mkdir -p "$WORKDIR"
cd "$WORKDIR"

# Extract project zip from module
ZIP="/data/adb/modules/rootmasterbd_saned/BGGuard_project.zip"
[ ! -f "$ZIP" ] && ZIP="/data/adb/modules/android_optimization_module/BGGuard_project.zip"

if [ -f "$ZIP" ]; then
    echo "[*] Extracting project..."
    unzip -o "$ZIP" -d "$WORKDIR" > /dev/null
    [ -d "$WORKDIR/BGGuardApp" ] && cp -r "$WORKDIR/BGGuardApp/." "$WORKDIR/" && rm -rf "$WORKDIR/BGGuardApp"
    echo "[✓] Project ready"
else
    echo "[!] Project zip not found!"
    exit 1
fi

# Git push
echo "[*] Initializing git..."
git init
git add .
git commit -m "$GH_MSG"
git branch -M main
git remote add origin "https://$GH_TOKEN@github.com/$GH_USER/$GH_REPO.git"

echo "[*] Pushing to GitHub..."
git push -u origin main --force

if [ $? -eq 0 ]; then
    # Trigger Actions
    sleep 3
    curl -s -o /dev/null -X POST \
      -H "Authorization: token $GH_TOKEN" \
      -H "Accept: application/vnd.github.v3+json" \
      "https://api.github.com/repos/$GH_USER/$GH_REPO/actions/workflows/build.yml/dispatches" \
      -d '{"ref":"main"}'

    echo ""
    echo "╔════════════════════════════════════════╗"
    echo "║  [✓] SUCCESS!                          ║"
    echo "║                                        ║"
    echo "║  Repo: github.com/$GH_USER/$GH_REPO"
    echo "║  Actions → Build running...            ║"
    echo "║  Wait 5 min → Releases → Download APK ║"
    echo "║                                        ║"
    echo "║  APK name: RootmasterBD_Saned.apk     ║"
    echo "╚════════════════════════════════════════╝"
else
    echo "[✗] Push failed — check token & internet"
fi
