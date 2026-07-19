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

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ProFlix"

include(":app")

include(":core:common")
include(":core:network")
include(":core:database")
include(":core:designsystem")
include(":core:player")

include(":feature:home")
include(":feature:search")
include(":feature:detail")
include(":feature:player")
include(":feature:history")
include(":feature:favorite")
include(":feature:settings")

include(":provider:data")
include(":provider:domain")
include(":provider:ui")
