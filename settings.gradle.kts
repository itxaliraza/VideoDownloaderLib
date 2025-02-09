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


        maven { url = uri("https://www.jitpack.io" ) }

    }
}

rootProject.name = "VideoDownloaderLib"
include(":app")
include(":adm_downloader")

include(":downloaderr:main")
include(":downloaderr:entities")
include(":downloaderr:framework")
include(":downloaderr:domain")
include(":downloaderr:persistence")
include(":downloaderr:data")
include(":downloaderr:sdk")
