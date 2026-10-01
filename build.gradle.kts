/*
 * Copyright (C) 2019-2025 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.serialization) apply false
    alias(libs.plugins.ksp) apply false
    //alias(libs.plugins.rust) apply false
}

tasks.register<Delete>("clean").configure {
    delete(layout.buildDirectory.asFile.get())
}

val apiCode by extra(93)
val verCode = Common.getBuildVersionCode(rootProject)

val verName = "v" + properties["APP_VERSION_NAME"] + "-" + (Common.getGitHeadRefsSuffix(rootProject))

val androidTargetSdkVersion by extra(36)
val androidMinSdkVersion by extra(27)
val androidCompileSdkVersion by extra(36)
val androidBuildToolsVersion = Version.findBuildToolsVersion()
val androidCompileNdkVersion = "28.2.13676358"

fun Project.configureBaseExtension() {
    extensions.findByType(com.android.build.gradle.BaseExtension::class)?.run {
        compileSdkVersion(androidCompileSdkVersion)
        ndkVersion = androidCompileNdkVersion
        buildToolsVersion = androidBuildToolsVersion

        defaultConfig {
            minSdk = androidMinSdkVersion
            targetSdk = androidTargetSdkVersion
            versionCode = verCode
            versionName = verName
        }

        compileOptions {
            sourceCompatibility = Version.java
            targetCompatibility = Version.java
        }

        packagingOptions.jniLibs.useLegacyPackaging = false
    }
}

// Strip JLatexMath fonts we don't ship (transitive AAR assets from
// io.noties.markwon:ext-latex -> ru.noties:jlatexmath-android, ~764 KB total under
// assets/org/scilab/forge/jlatexmath/fonts/). Has to live in the APK modules — a
// library's androidResources.ignoreAssetsPattern doesn't filter packaging in apps
// that consume it. Using subprojects keeps it in one place.
//   keep:  base/, latin/, maths/, euler/  (\mathbb, \mathcal, \mathfrak still work)
//   drop:  latin/optional/, maths/optional/  (~290 KB; \textbf/\textit/\mathds fall back)
//          licences/                          (~20 KB; pure license text files)
subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<com.android.build.api.dsl.ApplicationExtension>("android") {
            androidResources {
                ignoreAssetsPattern =
                    "!.svn:!.git:!.ds_store:!*.scc:!CVS:!thumbs.db:!picasa.ini:!*~:!optional:!licences:!*stmary*:!*eufb*"
            }
        }
    }
    plugins.withId("com.android.application") {
        configureBaseExtension()
    }
    plugins.withId("com.android.library") {
        configureBaseExtension()
    }

    // The media3 source modules (TMessagesProj_Modules/media) may still declare their package only
    // in the manifest; derive the namespace from it before the module's own script runs.
    if (projectDir.absolutePath.contains("/TMessagesProj_Modules/media/")) {
        plugins.withId("com.android.library") {
            extensions.configure<com.android.build.gradle.LibraryExtension>("android") {
                if (namespace == null) {
                    val manifestFile = file("src/main/AndroidManifest.xml")
                    if (manifestFile.exists()) {
                        val manifestPackage = javax.xml.parsers.DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .parse(manifestFile)
                            .documentElement
                            .getAttribute("package")
                        if (manifestPackage.isNotEmpty()) {
                            namespace = manifestPackage
                        }
                    }
                }
            }
        }
    }
}
