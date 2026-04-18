#!/usr/bin/env bash
set -e

echo "📦 Génération du changelog..."

# Exécuter depuis la racine du dépôt (comme dans la CI).
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")
VERSION=${1:-"Unreleased"}
DATE=$(date +%Y-%m-%d)

if [ -z "$LAST_TAG" ]; then
  RANGE="HEAD"
else
  RANGE="${LAST_TAG}..HEAD"
fi

write_release_block() {
  local feats fixes others
  feats=$(git log "$RANGE" --pretty=format:"%s" --no-merges | grep '^feat' | sed 's/^/- /' || true)
  fixes=$(git log "$RANGE" --pretty=format:"%s" --no-merges | grep '^fix' | sed 's/^/- /' || true)
  others=$(git log "$RANGE" --pretty=format:"%s" --no-merges | grep -iE '^(chore|docs|refactor|test)(\(|:|$)|^(reformat|delete)\s|^(check for|remove version)' | sed 's/^/- /' || true)

  echo "## $VERSION - $DATE"
  echo ""
  if [ -n "$feats" ]; then
    echo "### 🚀 Features"
    printf '%s\n' "$feats"
    echo ""
  fi
  if [ -n "$fixes" ]; then
    echo "### 🐛 Fixes"
    printf '%s\n' "$fixes"
    echo ""
  fi
  if [ -n "$others" ]; then
    echo "### 🔧 Others"
    printf '%s\n' "$others"
    echo ""
  fi
  echo "---"
  echo ""
}

write_release_block > new_version.md

if [ -f CHANGELOG.md ]; then
  {
    echo "# Changelog"
    echo ""
    cat new_version.md
    if head -n 1 CHANGELOG.md | grep -q '^# Changelog'; then
      tail -n +2 CHANGELOG.md | sed '/./,$!d'
    else
      cat CHANGELOG.md
    fi
  } > temp.md && mv temp.md CHANGELOG.md
else
  {
    echo "# Changelog"
    echo ""
    cat new_version.md
  } > CHANGELOG.md
fi

rm -f new_version.md
