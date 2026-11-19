#!/usr/bin/env bash
# Firma la extensión con Mozilla (sin publicarla) y deja el .xpi en browserExtension/dist.
# Las claves se leen de local.properties: amo.issuer y amo.secret.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
prop() { grep -E "^$1=" "$ROOT/local.properties" | cut -d= -f2- ; }
ISSUER="$(prop amo.issuer)"; SECRET="$(prop amo.secret)"
[ -n "$ISSUER" ] && [ -n "$SECRET" ] || { echo "Faltan amo.issuer / amo.secret en local.properties"; exit 1; }
cd "$ROOT/browserExtension"
npx --yes web-ext@8 sign --channel=unlisted --api-key="$ISSUER" --api-secret="$SECRET" \
    --ignore-files rules.test.js dist --artifacts-dir dist
ls -1 dist/*.xpi
