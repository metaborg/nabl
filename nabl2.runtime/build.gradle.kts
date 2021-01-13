plugins {
  id("org.metaborg.gradle.config.java-library")
  id("org.metaborg.devenv.spoofax.gradle.langspec")
  `maven-publish`
}

// Replace compile/source dependencies with overridden/local ones.
fun compositeBuild(name: String) = "$group:$name:$version"
val spoofax2Version: String by ext
val spoofax2BaselineVersion: String by ext
spoofaxLanguageSpecification {
  addCompileDependenciesFromMetaborgYaml.set(false)
  addSourceDependenciesFromMetaborgYaml.set(false)
}
dependencies {
  compileLanguage("org.metaborg:org.metaborg.meta.lang.esv:$spoofax2BaselineVersion")
  compileLanguage("org.metaborg:org.metaborg.meta.lang.template:$spoofax2BaselineVersion")
  compileLanguage(project(":org.metaborg.meta.nabl2.lang"))

  sourceLanguage(compositeBuild("meta.lib.spoofax"))
  sourceLanguage(project(":org.metaborg.meta.nabl2.shared"))
}
