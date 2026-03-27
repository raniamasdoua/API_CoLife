#!/usr/bin/env bash
set -e

echo "🔢 Calcul de la version..."

# Dernier tag (ou 0.0.0 si aucun)
LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "v0.0.0")

COMMITS=$(git log "${LAST_TAG}..HEAD" --pretty=format:%s)

MAJOR=0
MINOR=0
PATCH=0

while IFS= read -r line; do
  if [[ "$line" == *"BREAKING CHANGE"* ]] || [[ "$line" =~ !: ]]; then
    MAJOR=1
  elif [[ "$line" =~ ^feat ]]; then
    MINOR=1
  elif [[ "$line" =~ ^fix ]]; then
    PATCH=1
  fi
done <<< "$COMMITS"

VERSION="${LAST_TAG#v}"
IFS='.' read -r major minor patch <<< "$VERSION"

if [[ $MAJOR -eq 1 ]]; then
  major=$((major + 1))
  minor=0
  patch=0
elif [[ $MINOR -eq 1 ]]; then
  minor=$((minor + 1))
  patch=0
elif [[ $PATCH -eq 1 ]]; then
  patch=$((patch + 1))
fi

NEW_VERSION="v${major}.${minor}.${patch}"

echo "📦 Nouvelle version : $NEW_VERSION"

echo "$NEW_VERSION"