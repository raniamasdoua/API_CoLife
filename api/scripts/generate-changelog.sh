#!/usr/bin/env bash
set -e

echo "📦 Génération du changelog..."

# Dernier tag
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "")

# Définir la range
if [ -z "$LAST_TAG" ]; then
  RANGE="HEAD"
  echo "⚠️ Aucun tag trouvé, génération complète"
else
  RANGE="${LAST_TAG}..HEAD"
  echo "🔎 Depuis le tag : $LAST_TAG"
fi

# Version (temporaire pour test)
VERSION=${1:-"Unreleased"}

# Génération
{
  echo "# Changelog"
  echo ""
  echo "## $VERSION - $(date +%Y-%m-%d)"
  echo ""

  echo "### 🚀 Features"
  git log $RANGE --pretty=format:"%s" | grep "^feat" | sed 's/^/- /' || true
  echo ""

  echo "### 🐛 Fixes"
  git log $RANGE --pretty=format:"%s" | grep "^fix" | sed 's/^/- /' || true
  echo ""

  echo "### 🔧 Others"
  git log $RANGE --pretty=format:"%s" | grep -E "^(chore|docs|refactor|test)" | sed 's/^/- /' || true
  echo ""

} > CHANGELOG.md

echo "✅ CHANGELOG.md généré !"