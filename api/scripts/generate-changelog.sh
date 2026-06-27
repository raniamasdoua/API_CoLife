#!/usr/bin/env bash
set -e

echo "📦 Génération du changelog..."

# Dernier tag (pour ne lister que les commits depuis la dernière release)
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
VERSION=${1:-"Unreleased"}
DATE=$(date +%Y-%m-%d)

if [ -z "$LAST_TAG" ]; then
  RANGE="HEAD"
else
  RANGE="${LAST_TAG}..HEAD"
fi

# Récupération des commits par type (exécutées, et non écrites littéralement).
# `|| true` évite que `set -e` ne stoppe le script quand grep ne trouve rien.
FEATURES=$(git log "$RANGE" --pretty=format:"%s" | grep -E "^feat" | sed 's/^/- /' || true)
FIXES=$(git log "$RANGE" --pretty=format:"%s" | grep -E "^fix" | sed 's/^/- /' || true)
OTHERS=$(git log "$RANGE" --pretty=format:"%s" | grep -E "^(chore|docs|refactor|test|perf|ci)" | sed 's/^/- /' || true)

# Génération du bloc de la nouvelle version
{
  echo "## $VERSION - $DATE"
  echo ""
  echo "### 🚀 Features"
  echo "${FEATURES:-- _Aucune_}"
  echo ""
  echo "### 🐛 Fixes"
  echo "${FIXES:-- _Aucun_}"
  echo ""
  echo "### 🔧 Others"
  echo "${OTHERS:-- _Aucun_}"
  echo ""
  echo "---"
  echo ""
} > new_version.md

# Insère la nouvelle version juste sous le titre "# Changelog" (qui reste en tête),
# la plus récente en haut.
if [ -f CHANGELOG.md ]; then
  { head -n 1 CHANGELOG.md; echo ""; cat new_version.md; tail -n +2 CHANGELOG.md; } > temp.md
  mv temp.md CHANGELOG.md
else
  { echo "# Changelog"; echo ""; cat new_version.md; } > CHANGELOG.md
fi

rm new_version.md
