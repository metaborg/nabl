plugins {
    `java-library`
    id("org.metaborg.devenv.spoofax.gradle.langspec")
    `maven-publish`
}

spoofaxLanguageSpecification {
    addSourceDependenciesFromMetaborgYaml.set(false)
}
dependencies {
    sourceLanguage(libs.spoofax2.meta.lib.spoofax)

metaborg { // Do not create Java publication; this project is already published as a Spoofax 2 language.
    javaCreatePublication = false
    javaCreateSourcesJar = false
    javaCreateJavadocJar = false
}
