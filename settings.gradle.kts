rootProject.name = "nabl"

// This allows us to use plugins from Metaborg Artifacts
pluginManagement {
    repositories {
        maven("https://artifacts.metaborg.org/content/groups/public/")
    }
}

// This allows us to use the catalog in dependencies
dependencyResolutionManagement {
    repositories {
        maven("https://artifacts.metaborg.org/content/groups/public/")
    }
    versionCatalogs {
        create("libs") {
            from("org.metaborg.spoofax3:catalog:0.3.3")
        }
    }
}

// This downloads an appropriate JVM if not already available
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("nabl2.terms")
include("org.metaborg.meta.nabl2.lang")
project(":org.metaborg.meta.nabl2.lang").projectDir = file("nabl2.lang")
include("org.metaborg.meta.nabl2.runtime")
project(":org.metaborg.meta.nabl2.runtime").projectDir = file("nabl2.runtime")
include("org.metaborg.meta.nabl2.shared")
project(":org.metaborg.meta.nabl2.shared").projectDir = file("nabl2.shared")

include("statix.solver")
include("statix.generator")
include("statix.lang")
include("statix.runtime")

include("scopegraph")
include("p_raffrayi")

include("renaming.java")
