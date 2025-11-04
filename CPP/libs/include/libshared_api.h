#ifndef KONAN_LIBSHARED_H
#define KONAN_LIBSHARED_H
#ifdef __cplusplus
extern "C" {
#endif
#ifdef __cplusplus
typedef bool            libshared_KBoolean;
#else
typedef _Bool           libshared_KBoolean;
#endif
typedef unsigned short     libshared_KChar;
typedef signed char        libshared_KByte;
typedef short              libshared_KShort;
typedef int                libshared_KInt;
typedef long long          libshared_KLong;
typedef unsigned char      libshared_KUByte;
typedef unsigned short     libshared_KUShort;
typedef unsigned int       libshared_KUInt;
typedef unsigned long long libshared_KULong;
typedef float              libshared_KFloat;
typedef double             libshared_KDouble;
typedef float __attribute__ ((__vector_size__ (16))) libshared_KVector128;
typedef void*              libshared_KNativePtr;
struct libshared_KType;
typedef struct libshared_KType libshared_KType;

typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Byte;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Short;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Int;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Long;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Float;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Double;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Char;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Boolean;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_Unit;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_UByte;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_UShort;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_UInt;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_kotlin_ULong;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_YuanShen;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_VersionCompareResult;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_VersionCompareResult_LOWER;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_VersionCompareResult_EQUAL;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_VersionCompareResult_HIGHER;
typedef struct {
  libshared_KNativePtr pinned;
} libshared_kref_GameManager;


typedef struct {
  /* Service functions. */
  void (*DisposeStablePointer)(libshared_KNativePtr ptr);
  void (*DisposeString)(const char* string);
  libshared_KBoolean (*IsInstance)(libshared_KNativePtr ref, const libshared_KType* type);
  libshared_kref_kotlin_Byte (*createNullableByte)(libshared_KByte);
  libshared_KByte (*getNonNullValueOfByte)(libshared_kref_kotlin_Byte);
  libshared_kref_kotlin_Short (*createNullableShort)(libshared_KShort);
  libshared_KShort (*getNonNullValueOfShort)(libshared_kref_kotlin_Short);
  libshared_kref_kotlin_Int (*createNullableInt)(libshared_KInt);
  libshared_KInt (*getNonNullValueOfInt)(libshared_kref_kotlin_Int);
  libshared_kref_kotlin_Long (*createNullableLong)(libshared_KLong);
  libshared_KLong (*getNonNullValueOfLong)(libshared_kref_kotlin_Long);
  libshared_kref_kotlin_Float (*createNullableFloat)(libshared_KFloat);
  libshared_KFloat (*getNonNullValueOfFloat)(libshared_kref_kotlin_Float);
  libshared_kref_kotlin_Double (*createNullableDouble)(libshared_KDouble);
  libshared_KDouble (*getNonNullValueOfDouble)(libshared_kref_kotlin_Double);
  libshared_kref_kotlin_Char (*createNullableChar)(libshared_KChar);
  libshared_KChar (*getNonNullValueOfChar)(libshared_kref_kotlin_Char);
  libshared_kref_kotlin_Boolean (*createNullableBoolean)(libshared_KBoolean);
  libshared_KBoolean (*getNonNullValueOfBoolean)(libshared_kref_kotlin_Boolean);
  libshared_kref_kotlin_Unit (*createNullableUnit)(void);
  libshared_kref_kotlin_UByte (*createNullableUByte)(libshared_KUByte);
  libshared_KUByte (*getNonNullValueOfUByte)(libshared_kref_kotlin_UByte);
  libshared_kref_kotlin_UShort (*createNullableUShort)(libshared_KUShort);
  libshared_KUShort (*getNonNullValueOfUShort)(libshared_kref_kotlin_UShort);
  libshared_kref_kotlin_UInt (*createNullableUInt)(libshared_KUInt);
  libshared_KUInt (*getNonNullValueOfUInt)(libshared_kref_kotlin_UInt);
  libshared_kref_kotlin_ULong (*createNullableULong)(libshared_KULong);
  libshared_KULong (*getNonNullValueOfULong)(libshared_kref_kotlin_ULong);

  /* User functions. */
  struct {
    struct {
      struct {
        struct {
          libshared_kref_VersionCompareResult (*get)(); /* enum entry for LOWER. */
        } LOWER;
        struct {
          libshared_kref_VersionCompareResult (*get)(); /* enum entry for EQUAL. */
        } EQUAL;
        struct {
          libshared_kref_VersionCompareResult (*get)(); /* enum entry for HIGHER. */
        } HIGHER;
        libshared_KType* (*_type)(void);
      } VersionCompareResult;
      struct {
        libshared_KType* (*_type)(void);
        libshared_kref_VersionCompareResult (*compareVersion)(libshared_kref_GameManager thiz, const char* localVersion, const char* apkVersion);
        const char* (*getApkVersion)(libshared_kref_GameManager thiz, const char* apkName);
        const char* (*getGameVersion)(libshared_kref_GameManager thiz);
        libshared_KBoolean (*installGame)(libshared_kref_GameManager thiz, const char* installPath);
        libshared_KBoolean (*isGameInstalled)(libshared_kref_GameManager thiz);
        libshared_KBoolean (*isWifi)(libshared_kref_GameManager thiz);
      } GameManager;
      struct {
        libshared_KType* (*_type)(void);
        libshared_kref_YuanShen (*YuanShen)();
        libshared_kref_VersionCompareResult (*compareVersion)(libshared_kref_YuanShen thiz, const char* localVersion, const char* apkVersion);
        const char* (*getApkVersion)(libshared_kref_YuanShen thiz, const char* apkName);
        const char* (*getGameVersion)(libshared_kref_YuanShen thiz);
        libshared_KBoolean (*installGame)(libshared_kref_YuanShen thiz, const char* installPath);
        libshared_KBoolean (*isGameInstalled)(libshared_kref_YuanShen thiz);
        libshared_KBoolean (*isWifi)(libshared_kref_YuanShen thiz);
      } YuanShen;
      libshared_KBoolean (*checkGameInstalled)(libshared_kref_YuanShen manager);
      libshared_KInt (*compareVersion)(libshared_kref_YuanShen manager, const char* localVersion, const char* apkVersion);
      libshared_kref_YuanShen (*createYuanShen)();
      const char* (*executeCommand)(const char* command);
      const char* (*executeCommandWithSELinux)(const char* command);
      const char* (*executeSuCommand)(const char* command);
      const char* (*getApkVersion)(libshared_kref_YuanShen manager, const char* apkName);
      const char* (*getApkVersionStatic)(const char* apkName);
      const char* (*getGameVersion)(libshared_kref_YuanShen manager);
      const char* (*getGameVersionStatic)();
      libshared_KBoolean (*installGame)(libshared_kref_YuanShen manager, const char* installPath);
      libshared_KBoolean (*isGameInstalledStatic)();
    } root;
  } kotlin;
} libshared_ExportedSymbols;
extern libshared_ExportedSymbols* libshared_symbols(void);
#ifdef __cplusplus
}  /* extern "C" */
#endif
#endif  /* KONAN_LIBSHARED_H */
