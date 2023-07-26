plugins {
  id("org.metaborg.gradle.config.java-library")
  id("org.metaborg.devenv.spoofax.gradle.langspec")
  `maven-publish`
}

val spoofax2Version: String by ext
val spoofax2BaselineVersion: String by ext
val spoofax2DevenvVersion: String by ext
fun compositeBuild(name: String) = "$group:$name:$version"
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
  sourceLanguage(project(":org.metaborg.meta.nabl2.runtime"))
  sourceLanguage(project(":statix.runtime"))
}

metaborg { // Do not create Java publication; this project is already published as a Spoofax 2 language.
  javaCreatePublication = false
  javaCreateSourcesJar = false
  javaCreateJavadocJar = false
}
