plugins {
    `java-library`
    `maven-publish`
    id("org.metaborg.convention.java")
    id("org.metaborg.convention.maven-publish")
    id("org.metaborg.devenv.spoofax.gradle.langspec")
}

spoofaxLanguageSpecification {
    addCompileDependenciesFromMetaborgYaml.set(false)
    addSourceDependenciesFromMetaborgYaml.set(false)

    // We add the dependency manually and don't change the repositories
    // Eventually, this functionality should be removed from spoofax.gradle
    addSpoofaxCoreDependency.set(false)
    addSpoofaxRepository.set(false)
}
dependencies {
    compileLanguage(libs.esv.lang)
    compileLanguage(libs.sdf3.lang)
    compileLanguage(project(":org.metaborg.meta.nabl2.lang"))

    sourceLanguage(libs.meta.lib.spoofax)
    sourceLanguage(project(":org.metaborg.meta.nabl2.shared"))
    sourceLanguage(project(":org.metaborg.meta.nabl2.runtime"))
    sourceLanguage(project(":statix.runtime"))

    compileOnly(libs.spoofax.core)
}
