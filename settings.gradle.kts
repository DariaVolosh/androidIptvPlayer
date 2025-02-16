pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "IptvPlayer"
include(":app")
include(":decoder_ffmpeg")
include(":lib-decoder")
include(":lib-exoplayer")
include(":test-utils")
include(":ijkplayer-x86_64")
include(":ijkplayer-java")
include(":ijkplayer-example")
include(":ijkplayer-x86")
include(":ijkplayer-armv5")
include(":ijkplayer-arm64")
include(":ijkplayer-exo")
include(":ijkplayer-armv7a")
