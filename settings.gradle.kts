rootProject.name = "nabl"

pluginManagement {
  repositories {
    maven("https://artifacts.metaborg.org/content/groups/public/")
  }
}

include("nabl2.terms")  // <-
include("nabl2.lang")
include("nabl2.runtime")
include("nabl2.shared") // <-

include("statix.solver")
include("statix.generator")
include("statix.lang")
include("statix.runtime")
