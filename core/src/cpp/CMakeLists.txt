cmake_minimum_required(VERSION 3.22.1)

# 设置 CMake 策略以支持 IN_LIST 操作符
if(POLICY CMP0057)
    cmake_policy(SET CMP0057 NEW)
endif()

# Version info from Gradle (optimized - no git commands in CMake)
# Variables: GIT_COMMIT_HASH, GIT_BRANCH, GIT_SUFFIX, BUILD_TIMESTAMP passed from Gradle

if(NOT DEFINED GIT_COMMIT_HASH)
    set(GIT_COMMIT_HASH "unknown")
endif()

if(NOT DEFINED GIT_BRANCH)
    set(GIT_BRANCH "unknown")
endif()

if(NOT DEFINED GIT_SUFFIX)
    set(GIT_SUFFIX "")
endif()

# Only generate timestamp if BUILD_TIMESTAMP is not empty
if(NOT DEFINED BUILD_TIMESTAMP OR "${BUILD_TIMESTAMP}" STREQUAL "")
    # 如果没有时间戳参数，则不生成
    set(BUILD_TIMESTAMP "")
endif()

message(STATUS "Git Hash: ${GIT_COMMIT_HASH}")
message(STATUS "Git Branch: ${GIT_BRANCH}")
message(STATUS "Git Suffix: ${GIT_SUFFIX}")
message(STATUS "Build Timestamp: ${BUILD_TIMESTAMP}")

# 生成版本信息
if("${BUILD_TIMESTAMP}" STREQUAL "")
    # 没有时间戳：Alpha-smart-7571c87a
    set(GIT_VERSION "${GIT_BRANCH}${GIT_SUFFIX}-${GIT_COMMIT_HASH}")
else()
    # 有时间戳：Alpha-smart-7571c87a-251126
    set(GIT_VERSION "${GIT_BRANCH}${GIT_SUFFIX}-${GIT_COMMIT_HASH}-${BUILD_TIMESTAMP}")
endif()

string(REGEX REPLACE "[ ]+" "" GIT_VERSION "${GIT_VERSION}")
message(STATUS "Version Info: ${GIT_VERSION}")

# 保存变量到文件
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/version.h.in ${CMAKE_CURRENT_SOURCE_DIR}/version.h @ONLY)

set(YUMEBOX_LINKER_FLAG_LIST "")
if(DEFINED YUMEBOX_LINKER_FLAGS AND NOT "${YUMEBOX_LINKER_FLAGS}" STREQUAL "")
    separate_arguments(YUMEBOX_LINKER_FLAG_LIST NATIVE_COMMAND "${YUMEBOX_LINKER_FLAGS}")
endif()

project(clash-bridge C)


set(CMAKE_POSITION_INDEPENDENT_CODE on)
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -O3")

set(GO_OUTPUT_BASE "${GO_OUTPUT}/${FLAVOR_NAME}/${CMAKE_ANDROID_ARCH_ABI}")
if(NOT DEFINED FLAVOR_NAME OR "${FLAVOR_NAME}" STREQUAL "" OR NOT EXISTS("${GO_OUTPUT_BASE}"))
    set(GO_OUTPUT_BASE "${GO_OUTPUT}/${CMAKE_ANDROID_ARCH_ABI}")
    message(STATUS "Using shared Golang output folder: ${GO_OUTPUT_BASE}")
endif()

include_directories("${GO_OUTPUT_BASE}")
include_directories("${GO_SOURCE}")

link_directories("${GO_OUTPUT_BASE}")

add_library(bridge SHARED main.c jni_helper.c bridge_helper.c)
target_link_libraries(bridge log clash)
if(YUMEBOX_LINKER_FLAG_LIST)
    target_link_options(bridge PRIVATE ${YUMEBOX_LINKER_FLAG_LIST})
endif()
