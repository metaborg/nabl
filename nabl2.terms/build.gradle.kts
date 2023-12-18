plugins {
    `java-library`
//  id("org.metaborg.gradle.config.java-library")
//  id("org.metaborg.gradle.config.junit-testing")
}

// FIXME: Move this to a common spot
repositories {
    mavenCentral()
    maven("https://nexus.usethesource.io/content/repositories/releases/")
    maven("https://artifacts.metaborg.org/content/groups/public/")
}

dependencies {
    // FIXME: Move these platform definitions to a common spot
    api(platform(libs.spoofax3.bom))
    testImplementation(platform(libs.spoofax3.bom))
    annotationProcessor(platform(libs.spoofax3.bom))
    testAnnotationProcessor(platform(libs.spoofax3.bom))

    implementation(libs.spoofax2.metaborg.util)
    api(libs.spoofax2.terms)
    api(libs.spoofax2.interpreter.core)
    api(libs.capsule)

    // Annotation processing
    annotationProcessor(libs.immutables.value)
    annotationProcessor(libs.immutables.serial)
    compileOnly(libs.immutables.value)
    compileOnly(libs.immutables.serial)
    implementation(libs.jakarta.annotation)

    // Tests
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.vintage)
    testImplementation(libs.logback)

    // Test Annotation processing
    testAnnotationProcessor(libs.immutables.value)
    testAnnotationProcessor(libs.immutables.serial)
    testCompileOnly(libs.immutables.value)
    testCompileOnly(libs.immutables.serial)
}

// FIXME: If this is necessary, use testFixtures instead
//// Copy test resources into classes directory, to make them accessible as classloader resources at runtime.
//val copyTestResourcesTask = tasks.create<Copy>("copyTestResources") {
//  from("$projectDir/src/test/resources")
//  into("$buildDir/classes/java/test")
//}
//tasks.getByName("processTestResources").dependsOn(copyTestResourcesTask)
