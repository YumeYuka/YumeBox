cmake_minimum_required(VERSION 3.31)

set(ANDROID_ABI "arm64-v8a")
set(CMAKE_TOOLCHAIN_FILE "Y:/DevEnv/Android/ndk/29.0.13846066/build/cmake/android.toolchain.cmake")
set(CMAKE_SYSTEM_NAME Android)
set(CMAKE_SYSTEM_VERSION 21)
set(CMAKE_ANDROID_NDK "Y:/DevEnv/Android/ndk/29.0.13846066")

project(YuanShen)
set(CMAKE_CXX_STANDARD 26)


set(COMMON_CXX_FLAGS "-Wall -Oz -g0 -fdata-sections -ffunction-sections -fomit-frame-pointer")
set(COMMON_LINKER_FLAGS "-Wl,--strip-all,--gc-sections")


add_executable(YuanShen src/main.cpp)

include_directories(${CMAKE_SOURCE_DIR}/libs/include)

add_library(shared STATIC IMPORTED)
set_target_properties(shared PROPERTIES
        IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/libs/lib/libshared.a")

add_library(yuanshen SHARED IMPORTED)
set_target_properties(yuanshen PROPERTIES
        IMPORTED_LOCATION "${CMAKE_SOURCE_DIR}/libs/lib/libyuanshen.so"
        IMPORTED_NO_SONAME TRUE)


set_target_properties(YuanShen PROPERTIES
        CXX_FLAGS "${COMMON_CXX_FLAGS}"
        LINK_FLAGS "${COMMON_LINKER_FLAGS} -Wl,-rpath,\$ORIGIN"
        BUILD_RPATH_USE_ORIGIN TRUE
)
target_link_libraries(YuanShen PRIVATE shared yuanshen)

file(MAKE_DIRECTORY "${CMAKE_SOURCE_DIR}/../src")

add_custom_command(TARGET YuanShen POST_BUILD
    COMMAND ${CMAKE_COMMAND} -E copy_if_different
    $<TARGET_FILE:YuanShen>
    "${CMAKE_SOURCE_DIR}/../src/YuanShen"
    COMMENT "Copying YuanShen binary to src/bin directory"
)

