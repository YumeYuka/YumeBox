cmake_minimum_required(VERSION 3.0)

# 获取git hash

message(STATUS "CMAKE_CURRENT_SOURCE_DIR= ${CMAKE_CURRENT_SOURCE_DIR}")

# 尝试从子模块获取 commit hash
execute_process(
  COMMAND git rev-parse HEAD
  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
  OUTPUT_VARIABLE COMMIT_HASH
  ERROR_QUIET
  OUTPUT_STRIP_TRAILING_WHITESPACE
)

if(COMMIT_HASH)
  string(SUBSTRING ${COMMIT_HASH} 0 7 COMMIT_HASH)
else()
  # 备用方案：从 submodule foreach 获取
  execute_process(
    COMMAND git submodule foreach git log -1 --format=%H   
    WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}/..
    OUTPUT_VARIABLE COMMIT_HASH
    ERROR_QUIET
  )
  string(REPLACE "\n" ";" COMMIT_HASH "${COMMIT_HASH}")
  list(LENGTH COMMIT_HASH HASH_COUNT)
  if(HASH_COUNT GREATER 1)
    list(GET COMMIT_HASH 1 COMMIT_HASH)
    if(COMMIT_HASH)
      string (REGEX REPLACE "[\n\t\r]" "" COMMIT_HASH "${COMMIT_HASH}")
      string(SUBSTRING ${COMMIT_HASH} 0 7 COMMIT_HASH)
    else()
      set(COMMIT_HASH "unknown")
    endif()
  else()
    set(COMMIT_HASH "unknown")
  endif()
endif()
message(STATUS "git hash= ${COMMIT_HASH}")

# 获取分支名称
# 方法1: 尝试从 HEAD 获取分支名
execute_process(
  COMMAND git rev-parse --abbrev-ref HEAD
  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
  OUTPUT_VARIABLE CURRENT_BRANCH
  ERROR_QUIET
  OUTPUT_STRIP_TRAILING_WHITESPACE
)

# 如果是 HEAD (detached state) 或为空，尝试其他方法
if(NOT CURRENT_BRANCH OR CURRENT_BRANCH STREQUAL "HEAD")
  # 方法2: 尝试从环境变量获取 (CI 环境)
  if(DEFINED ENV{GITHUB_REF_NAME})
    set(CURRENT_BRANCH "$ENV{GITHUB_REF_NAME}")
  elseif(DEFINED ENV{CI_COMMIT_BRANCH})
    set(CURRENT_BRANCH "$ENV{CI_COMMIT_BRANCH}")
  else()
    # 方法3: 尝试从远程分支获取
    execute_process(
      COMMAND git branch -r --contains HEAD
      WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
      OUTPUT_VARIABLE BRANCH_OUTPUT
      ERROR_QUIET
      OUTPUT_STRIP_TRAILING_WHITESPACE
    )
    if(BRANCH_OUTPUT)
      string(REGEX REPLACE ".*origin/([^ \n]+).*" "\\1" CURRENT_BRANCH "${BRANCH_OUTPUT}")
      string(REGEX REPLACE "[\n\t\r]" "" CURRENT_BRANCH "${CURRENT_BRANCH}")
    else()
      set(CURRENT_BRANCH "detached")
    endif()
  endif()
endif()

# 清理分支名
if(CURRENT_BRANCH)
  string(REGEX REPLACE "^origin/" "" CURRENT_BRANCH "${CURRENT_BRANCH}")
  string(REGEX REPLACE "[\n\t\r ]" "" CURRENT_BRANCH "${CURRENT_BRANCH}")
else()
  set(CURRENT_BRANCH "unknown")
endif()

message(STATUS "git current branch = ${CURRENT_BRANCH}")

# 获取生成时间
string(TIMESTAMP COMPILE_TIME "%y%m%d")
if(COMPILE_TIME)
  string (REGEX REPLACE "[\n\t\r]" "" COMPILE_TIME "${COMPILE_TIME}")
  string(REGEX REPLACE "\"" "" COMPILE_TIME "${COMPILE_TIME}")
else()
  set(COMPILE_TIME "000000")
endif()

# 生成版本信息
set(GIT_VERSION "${CURRENT_BRANCH}_${COMMIT_HASH}_${COMPILE_TIME}")
message(STATUS "version info = ${GIT_VERSION}")

# 去除空格
if(GIT_VERSION)
  string(REGEX REPLACE "[ ]+" "" GIT_VERSION "${GIT_VERSION}")
endif()

# 保存变量到文件
configure_file(${CMAKE_CURRENT_SOURCE_DIR}/version.h.in ${CMAKE_CURRENT_SOURCE_DIR}/version.h @ONLY)

project(clash-bridge C)


set(CMAKE_POSITION_INDEPENDENT_CODE on)
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -O3")

set(GO_OUTPUT_BASE ${GO_OUTPUT}/${FLAVOR_NAME})

if ("${CMAKE_BUILD_TYPE}" STREQUAL "Debug")
    set(GO_OUTPUT_BASE "${GO_OUTPUT_BASE}Debug")
elseif ("${CMAKE_BUILD_TYPE}" STREQUAL "Release")
    set(GO_OUTPUT_BASE "${GO_OUTPUT_BASE}Release")
elseif ("${CMAKE_BUILD_TYPE}" STREQUAL "RelWithDebInfo")
    set(GO_OUTPUT_BASE "${GO_OUTPUT_BASE}Release")
else ()
    message(FATAL_ERROR "Unknown build type ${CMAKE_BUILD_TYPE}")
endif ()

include_directories("${GO_OUTPUT_BASE}/${CMAKE_ANDROID_ARCH_ABI}")
include_directories("${GO_SOURCE}")

link_directories("${GO_OUTPUT_BASE}/${CMAKE_ANDROID_ARCH_ABI}")

add_library(bridge SHARED main.c jni_helper.c bridge_helper.c)
target_link_libraries(bridge log clash)
