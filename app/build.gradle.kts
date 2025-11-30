import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class DownloadGeoFilesTask : DefaultTask() {
    @get:Input
    abstract val assetUrls: MapProperty<String, String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun download() {
        val destinationDir = outputDirectory.get().asFile
        destinationDir.mkdirs()

        assetUrls.get().forEach { (fileName, url) ->
            val outputFile = destinationDir.resolve(fileName)
            runCatching {
                val uri = URI(url)
                uri.toURL().openStream().use { input ->
                    Files.copy(input, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
                logger.lifecycle("$fileName downloaded to ${outputFile.absolutePath}")
            }.onFailure { error ->
                logger.warn("Failed to download $fileName from $url", error)
            }
        }
    }
}



plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("dev.oom-wg.purejoy.mlang")
}


MLang {
    name = null
    configDir = "../lang"
    baseLang = "zh"
    base = true
    compose = true
}

val targetAbi = project.findProperty("android.injected.build.abi") as String?
val mmkvVersion = when (targetAbi) {
    "arm64-v8a", "x86_64" -> libs.versions.mmkv64.get()
    else -> libs.versions.mmkv.get()
}
val mmkvDependency = "com.tencent:mmkv:$mmkvVersion"

val appNamespace = gropify.project.namespace.base
val appName = gropify.project.name
val jvmVersionNumber = gropify.project.jvm
val jvmVersion = jvmVersionNumber.toString()
val javaVersion = JavaVersion.toVersion(jvmVersionNumber)
val appAbiList = gropify.abi.app.list.split(",").map { it.trim() }
val localeList = gropify.locale.app.list.split(",").map { it.trim() }

kotlin {
    androidTarget()
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(jvmVersionNumber))
    }
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.bundles.miuix)
            implementation(libs.haze.materials)
            implementation(mmkvDependency)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.compose.destinations.core)
            implementation(libs.okhttp)
            implementation(libs.timber)
            implementation(libs.javet)
            implementation(libs.pangutext.android)
            implementation(libs.commons.compress)
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.crashlytics.ndk)
            implementation(libs.firebase.analytics)
            implementation(libs.mlkit.barcode.scanning)
            implementation(libs.camera2)
            implementation(libs.camera2Lifecycle)
            implementation(libs.camera2View)
            implementation(libs.cameraCore)
            implementation(libs.cameraVideo)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.android)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
            implementation(libs.coil.svg)
            implementation(libs.aboutlibraries.core)
            implementation(libs.aboutlibraries.compose)
            implementation(libs.aboutlibraries.compose.m3)
        }

        commonMain.dependencies {
            implementation(project(":core"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(jvmVersion))
}

android {
    namespace = appNamespace
    compileSdk = gropify.android.compileSdk

    defaultConfig {
        minSdk = gropify.android.minSdk
        targetSdk = gropify.android.targetSdk
        versionCode = gropify.project.version.code
        versionName = gropify.project.version.name
        manifestPlaceholders["appName"] = appName
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    androidResources {
        localeFilters += localeList
    }

    buildFeatures {
        compose = true
        buildConfig = true
        viewBinding = false
        dataBinding = false
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            isJniDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            isJniDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    splits {
        abi {
            isEnable = gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }
            reset()
            include(*appAbiList.toTypedArray())
            isUniversalApk = false
        }
    }

    packaging {
        jniLibs {
            excludes += listOf(
                "lib/arm64-v8a/libjavet*.so",
                "lib/armeabi-v7a/libjavet*.so",
                "lib/x86_64/libjavet*.so",
                "lib/x86/libjavet*.so",
                "lib/**/libjavet-node-android.v.5.0.1.so",
            )
            useLegacyPackaging = true
        }
        resources {
            excludes += listOf(
                "SubStore/**",
                "kotlin/**",
                "kotlin/**/*",
                "kotlin/**/**",
                "DebugProbesKt.bin",
                "META-INF/*.kotlin_module",
                "META-INF/LICENSE*",
                "META-INF/AL2.0",
                "META-INF/*.version",
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/*.txt",
                "META-INF/index.list",
                "META-INF/io.netty.versions.properties",
                "index.android.bin",
                "index.*.bin",
                "META-INF/spring.*",
                "META-INF/ASL2.0",
                "META-INF/*.index",
            )
        }
    }

    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val abiName = filters.find { it.filterType == "ABI" }?.identifier ?: "universal"
            val buildTypeName = buildType.name
            output.outputFileName = "${appName}-${abiName}-${buildTypeName}.apk"
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    add("kspAndroid", libs.compose.destinations.ksp)
}

ksp {
    arg("compose-destinations.defaultTransitions", "none")
}

val geoFilesDownloadDir = layout.projectDirectory.dir("src/androidMain/assets")

val downloadGeoFilesTask = tasks.register<DownloadGeoFilesTask>("downloadGeoFiles") {
    description = "Download GeoIP and GeoSite databases from MetaCubeX"
    group = "build setup"

    val assets = mapOf(
        "geoip.metadb" to gropify.asset.geoip.url,
        "geosite.dat" to gropify.asset.geosite.url,
        "ASN.mmdb" to gropify.asset.asn.url,
    )
    assetUrls.putAll(assets)
    outputDirectory.set(geoFilesDownloadDir)
}

tasks.configureEach {
    when {
        name.startsWith("assemble") ||
            name.startsWith("lintVitalAnalyze") ||
            (name.startsWith("generate") && name.contains("LintVitalReportModel")) -> {
            dependsOn(downloadGeoFilesTask)
        }
    }
}

tasks.withType<MergeSourceSetFolders>().configureEach {
    dependsOn(downloadGeoFilesTask)
}

tasks.register<Delete>("cleanGeoFiles") {
    description = "Clean downloaded GeoIP and GeoSite databases"
    group = "build setup"
    delete(geoFilesDownloadDir)
}

aboutLibraries {
    export {
        outputFile = file("src/androidMain/resources/aboutlibraries.json")
    }
}
