plugins {
  id("org.metaborg.spoofax.gradle.langspec")
  `maven-publish`
}

spoofax {
  createPublication = true
}
