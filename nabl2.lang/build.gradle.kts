plugins {
  id("org.metaborg.gradle.config.java-library")
  id("org.metaborg.devenv.spoofax.gradle.langspec")
  `maven-publish`
}

val spoofax2Version: String by ext
spoofaxLanguageSpecification {
  addSourceDependenciesFromMetaborgYaml.set(false)
}
dependencies {
  sourceLanguage("org.metaborg:meta.lib.spoofax:$spoofax2Version")
  sourceLanguage(project(":org.metaborg.meta.nabl2.shared"))
}

metaborg { // Do not create Java publication; this project is already published as a Spoofax 2 language.
  javaCreatePublication = false
  javaCreateSourcesJar = false
  javaCreateJavadocJar = false
}
