plugins {
    `java-library`
    id("org.metaborg.convention.java")
    id("org.metaborg.convention.maven-publish")
    id("org.metaborg.convention.junit")
}

dependencies {
    api(platform(libs.metaborg.platform)) { version { require("latest.integration") } }

    // !! Update dependencies in pom.xml as well
    api(project(":nabl2.terms"))
    api(project(":statix.solver"))

    implementation(libs.commons.math3)
    api(libs.capsule)

    // Annotation processing
    annotationProcessor(libs.immutables.value)
    annotationProcessor(libs.immutables.serial)
    compileOnly(libs.immutables.value)
    compileOnly(libs.immutables.serial)
    implementation(libs.jakarta.annotation)

    // Tests
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage)
    testImplementation(libs.logback)

    // Test Annotation processing
    testAnnotationProcessor(libs.immutables.value)
    testAnnotationProcessor(libs.immutables.serial)
    testCompileOnly(libs.immutables.value)
    testCompileOnly(libs.immutables.serial)

    // !! Update dependencies in pom.xml as well
}

// Copy test resources into classes directory, to make them accessible as classloader resources at runtime.
val copyTestResourcesTask = tasks.create<Copy>("copyTestResources") {
    from("$projectDir/src/test/resources")
    into("$buildDir/classes/java/test")
}
tasks.getByName("processTestResources").dependsOn(copyTestResourcesTask)
