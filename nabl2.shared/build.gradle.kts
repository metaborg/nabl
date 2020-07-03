plugins {
  id("org.metaborg.spoofax.gradle.langspec")
  `maven-publish`
}

spoofaxLanguageSpecification {
  createPublication.set(true)
}
