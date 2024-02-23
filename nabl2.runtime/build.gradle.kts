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

    sourceLanguage(libs.spoofax2.meta.lib)
    sourceLanguage(project(":nabl2.shared"))
}

// TODO: This uses the gradle.config plugin
//metaborg { // Do not create Java publication; this project is already published as a Spoofax 2 language.
//    javaCreatePublication = false
//    javaCreateSourcesJar = false
//    javaCreateJavadocJar = false
//}