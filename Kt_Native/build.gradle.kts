plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
}

group = "me.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

defaultTasks("buildMain")


kotlin {
    val androidTargets = listOf(
        androidNativeArm64("androidArm64") to "arm64",
//        androidNativeArm32("androidArm32") to "arm",
//        androidNativeX64("androidX64") to "x86_64",
//        androidNativeX86("androidX86") to "i386"
    )
    androidTargets.forEach { (target, arch) ->
        target.binaries {
            staticLib {
                baseName = "shared"
                outputDirectory = layout.buildDirectory.dir("bin/android/$arch").get().asFile
                if (buildType.name == "RELEASE") {
                    freeCompilerArgs += listOf("-opt")
                }
                linkerOpts.addAll(listOf("-Wl,--exclude-libs,liblog.so", "-Wl,--exclude-libs,libdl.so"))
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:3.0.0")
                implementation("io.ktor:ktor-client-cio:3.0.0")
                implementation(libs.kotlinxSerializationJson)
            }
        }
    }
}
