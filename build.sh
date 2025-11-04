#!/bin/bash
set -e

cd Go && ./build_android_simple.sh && cd ..

cp -f Kt_Native/build/bin/android/arm64/libshared_api.h CPP/libs/include/
cp -f Kt_Native/build/bin/android/arm64/libshared.a CPP/libs/lib/
cp -f CPP/libs/lib/libyuanshen.so src/

cd Vue && pnpm build && cd ..