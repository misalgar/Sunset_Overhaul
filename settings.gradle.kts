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

rootProject.name = "SunsetOverhaul"

include(":app")

// Core
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:ui")
include(":core:network")
include(":core:database")
include(":core:data")
include(":core:player")

// Features
include(":feature:account")
include(":feature:home")
include(":feature:live")
include(":feature:guide")
include(":feature:movies")
include(":feature:series")
include(":feature:details")
include(":feature:search")
include(":feature:mylist")
include(":feature:player")
include(":feature:settings")
include(":feature:parental")
include(":feature:profiles")
