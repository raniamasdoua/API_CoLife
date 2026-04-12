#!/usr/bin/env bash
set -e

echo "📦 Génération du changelog..."

# Dernier tag
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
VERSION=${1:-"Unreleased"}
DATE=$(date +%Y-%m-%d)

if [ -z "$LAST_TAG" ]; then
  RANGE="HEAD"
else
  RANGE="${LAST_TAG}..HEAD"
fi


# Génération
cat <<EOF > new_version.md
  ## $VERSION - $DATE

  ### 🚀 Features
  git log $RANGE --pretty=format:"%s" | grep "^feat" | sed 's/^/- /' || true

  ### 🐛 Fixes
  git log $RANGE --pretty=format:"%s" | grep "^fix" | sed 's/^/- /' || true
  

  ### 🔧 Others
  git log $RANGE --pretty=format:"%s" | grep -E "^(chore|docs|refactor|test)" | sed 's/^/- /' || true

  ---

EOF

# si fichier existe → prepend
if [ -f CHANGELOG.md ]; then
  cat new_version.md CHANGELOG.md > temp.md && mv temp.md CHANGELOG.md
else
  echo "# Changelog" > CHANGELOG.md
  cat new_version.md >> CHANGELOG.md
fi

rm new_version.md
