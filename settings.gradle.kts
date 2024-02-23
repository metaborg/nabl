rootProject.name = "nabl"

// This is needed to let Gradle find the dependencies of the spoofax2-gradle plugin
pluginManagement {
  repositories {
    maven("https://artifacts.metaborg.org/content/groups/public/")
  }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from("dev.spoofax:spoofax3-catalog:0.0.0-SNAPSHOT")
        }
    }
}

include("nabl2.terms")
include("nabl2.shared")
include("nabl2.lang")
include("nabl2.runtime")

include("scopegraph")
include("p_raffrayi")

include("statix.solver")
include("statix.generator")
include("statix.runtime")
//
//
//include("renaming.java")
include("statix.lang")
