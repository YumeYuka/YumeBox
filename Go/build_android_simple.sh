#!/bin/bash
set -e

NDK_ROOT="Y:/DevEnv/Android/ndk/29.0.13846066"

[ ! -d "$NDK_ROOT" ] && echo "Error: NDK not found" && exit 1

export GOOS=android GOARCH=arm64 CGO_ENABLED=1
export CC="$NDK_ROOT/toolchains/llvm/prebuilt/windows-x86_64/bin/aarch64-linux-android21-clang.cmd"
export CGO_CFLAGS="-O2 -fdata-sections -ffunction-sections"
export CGO_LDFLAGS="-Wl,--gc-sections -static-libgcc -static-libstdc++"

mkdir -p build/android/arm64-v8a

echo "Building ARM64 shared library..."
go build -buildmode=c-shared \
  -ldflags="-buildid= -linkmode=external" \
  -trimpath \
  -o build/android/arm64-v8a/libyuanshen.so \
  main.go

SIZE=$(stat -c%s build/android/arm64-v8a/libyuanshen.so 2>/dev/null || stat -f%z build/android/arm64-v8a/libyuanshen.so 2>/dev/null || echo "0")
SIZE_MB=$(echo "scale=2; $SIZE/1048576" | bc 2>/dev/null || echo "?")

cp -f build/android/arm64-v8a/libyuanshen.so ../CPP/libs/lib/
cp -f build/android/arm64-v8a/libyuanshen.h ../CPP/libs/include/
echo "Done: ${SIZE_MB}MB"
