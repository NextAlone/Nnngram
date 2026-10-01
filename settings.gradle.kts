/*
 * Copyright (C) 2019-2024 qwq233 <qwq233@qwq2333.top>
 * https://github.com/qwq233/Nullgram
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this software.
 *  If not, see
 * <https://www.gnu.org/licenses/>
 */

@file:Suppress("UnstableApiUsage")
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        maven("https://jitpack.io")
    }
}

plugins {
    id("com.gradle.develocity") version "3.19.2"
}

develocity {
    buildScan {
        publishing.onlyIf {
            System.getenv("GITHUB_ACTIONS") == "true" || it.buildResult.failures.isNotEmpty()
        }
        termsOfUseAgree.set("yes")
        termsOfUseUrl.set("https://gradle.com/terms-of-service")
    }
}

rootProject.name = "Nnngram"
include(
    ":TMessagesProj",
    ":jlatexmath",
    ":libs:tcp2ws",
    ":libs:pangu",
    ":libs:ksp",
)
project(":jlatexmath").projectDir = file("TMessagesProj/lib/jlatexmath/jlatexmath")

gradle.extra["androidxMediaModulePrefix"] = "TMessagesProj_Modules:media3:media-"
apply(from = file("TMessagesProj_Modules/media/core_settings.gradle"))
// Gradle 9 rejects projects whose directory does not exist; the implicit ":TMessagesProj_Modules:media3"
// parent created by the prefix above has none, so point it at the directory holding the modules.
project(":TMessagesProj_Modules:media3").projectDir = file("TMessagesProj_Modules/media/libraries")
