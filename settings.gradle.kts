rootProject.name = "nabl"

dependencyResolutionManagement {
    repositories {
        maven("https://artifacts.metaborg.org/content/groups/public/")
        mavenCentral()
    }
}

pluginManagement {
    repositories {
        maven("https://artifacts.metaborg.org/content/groups/public/")
        gradlePluginPortal()
    }
}

plugins {
    id("org.metaborg.convention.settings") version "latest.integration"
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
