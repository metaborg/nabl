plugins {
    `java-library`
    id("org.metaborg.devenv.spoofax.gradle.langspec")
    `maven-publish`
}

val spoofax2Version: String by ext
val spoofax2BaselineVersion: String by ext
val spoofax2DevenvVersion: String by ext
spoofaxLanguageSpecification {
    addCompileDependenciesFromMetaborgYaml.set(false)
    addSourceDependenciesFromMetaborgYaml.set(false)
}
dependencies {
    compileLanguage(libs.spoofax2.esv.lang)     // Bootstrap using Spoofax 2 artifact
    compileLanguage(libs.spoofax2.sdf3.lang)    // Bootstrap using Spoofax 2 artifact
    compileLanguage(project(":org.metaborg.meta.nabl2.lang"))

    sourceLanguage(libs.spoofax2.meta.lib.spoofax)
    sourceLanguage(project(":org.metaborg.meta.nabl2.shared"))

metaborg { // Do not create Java publication; this project is already published as a Spoofax 2 language.
    javaCreatePublication = false
    javaCreateSourcesJar = false
    javaCreateJavadocJar = false
}
