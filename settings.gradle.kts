pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "ArtificientGPSTracking"

include(":app")
include(":core-data")
include(":core-database")
include(":core-testing")
include(":core-ui")
include(":feature-tracking")
include(":test-app")
