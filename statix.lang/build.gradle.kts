plugins {
    `java-library`
    `maven-publish`
    id("dev.spoofax.spoofax2.gradle.langspec")
}

// FIXME: Move this to a common spot
spoofaxLanguageSpecification {
    addCompileDependenciesFromMetaborgYaml.set(false)
    addSourceDependenciesFromMetaborgYaml.set(false)
    addJavaDependenciesFromMetaborgYaml.set(false)
}

// FIXME: Move this to a common spot
repositories {
    mavenCentral()
    maven("https://nexus.usethesource.io/content/repositories/releases/")
    maven("https://artifacts.metaborg.org/content/groups/public/")
}

dependencies {
    // FIXME: Move these platform definitions to a common spot
    compileLanguage(platform(libs.spoofax3.bom))
    sourceLanguage(platform(libs.spoofax3.bom))
    api(platform(libs.spoofax3.bom))
    testImplementation(platform(libs.spoofax3.bom))
    annotationProcessor(platform(libs.spoofax3.bom))
    testAnnotationProcessor(platform(libs.spoofax3.bom))

    // Languages
    compileLanguage(libs.spoofax.lang.esv)
    compileLanguage(libs.spoofax.lang.sdf3)
    compileLanguage(project(":nabl2.lang"))


    sourceLanguage(libs.spoofax2.meta.lib.spoofax)
    sourceLanguage(project(":nabl2.shared"))
    sourceLanguage(project(":nabl2.runtime"))
    sourceLanguage(project(":statix.runtime"))
}

// TODO:
//metaborg { // Do not create Java publication; this project is already published as a Spoofax 2 language.
//  javaCreatePublication = false
//  javaCreateSourcesJar = false
//  javaCreateJavadocJar = false
//}
