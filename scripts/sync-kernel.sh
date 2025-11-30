#!/usr/bin/env sh
set -eu

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
KERNEL_PROPERTIES="$PROJECT_ROOT/kernel.properties"
GOLANG_ROOT="$PROJECT_ROOT/core/src/foss/golang"
GOLANG_MAIN="$PROJECT_ROOT/core/src/golang"
MIHOMO_DIR="$GOLANG_ROOT/mihomo"

usage() {
  echo "Usage: $(basename "$0") <alpha|meta|smart>"
  exit 1
}

CHOICE="${1:-}"
case "$CHOICE" in
  alpha|Alpha)
    REPO_URL="https://github.com/MetaCubeX/mihomo.git"
    BRANCH_NAME="Alpha"
    VERSION_SUFFIX=""
    ;;
  meta|Meta)
    REPO_URL="https://github.com/MetaCubeX/mihomo.git"
    BRANCH_NAME="Meta"
    VERSION_SUFFIX=""
    ;;
  smart|Smart)
    REPO_URL="https://github.com/vernesong/mihomo.git"
    BRANCH_NAME="Alpha"
    VERSION_SUFFIX="-Smart"
    ;;
  *)
    usage
    ;;
esac

require_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

require_cmd git
require_cmd go

update_kernel_properties() {
  tmp_file="$(mktemp 2>/dev/null || mktemp -t switch_mihomo)"
  awk -v repo="$REPO_URL" -v branch="$BRANCH_NAME" -v suffix="$VERSION_SUFFIX" '
    BEGIN {
      target["external.mihomo.repo"]=repo
      target["external.mihomo.branch"]=branch
      target["external.mihomo.suffix"]=suffix
    }
    {
      if ($0 ~ /^[ \t]*#/ || $0 !~ /=/) { print; next }
      split($0, kv, "=")
      key = kv[1]
      if (key in target) {
        print key "=" target[key]
        seen[key]=1
      } else {
        print
      }
    }
    END {
      for (k in target) {
        if (!(k in seen)) {
          print k "=" target[k]
        }
      }
    }
  ' "$KERNEL_PROPERTIES" > "$tmp_file"
  mv "$tmp_file" "$KERNEL_PROPERTIES"
  echo "Updated kernel.properties -> repo=$REPO_URL branch=$BRANCH_NAME suffix=$VERSION_SUFFIX"
}

sync_repo() {
  if [ -d "$MIHOMO_DIR" ]; then
    echo "Removing existing directory $MIHOMO_DIR"
    rm -rf "$MIHOMO_DIR"
  fi
  echo "Cloning $REPO_URL (branch $BRANCH_NAME) -> $MIHOMO_DIR"
  git clone --branch "$BRANCH_NAME" --single-branch "$REPO_URL" "$MIHOMO_DIR"
}

run_tidy() {
  if [ ! -f "$1/go.mod" ]; then
    echo "Skipping tidy for $1 (no go.mod found)"
    return
  fi
  echo "Running go mod tidy in $1"
  (
    cd "$1"
    go mod tidy
  )
}

update_kernel_properties
sync_repo
run_tidy "$GOLANG_ROOT"
run_tidy "$GOLANG_MAIN"

echo "Done: selected $CHOICE"
