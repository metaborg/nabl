plugins {
    `java-library`
    `maven-publish`
    id("org.metaborg.convention.java")
    id("org.metaborg.convention.maven-publish")
}

fun compositeBuild(name: String) = "$group:$name:$version"
val spoofax2Version: String by ext
val spoofax2DevenvVersion: String by ext
dependencies {
    api(platform(libs.metaborg.platform)) { version { require("latest.integration") } }

    // !! Update dependencies in pom.xml as well
    api(compositeBuild("org.metaborg.util"))
    api("io.usethesource:capsule")

    // Annotation processing
    annotationProcessor(libs.immutables.value)
    annotationProcessor(libs.immutables.serial)
    compileOnly(libs.immutables.value)
    compileOnly(libs.immutables.serial)
    implementation("jakarta.annotation:jakarta.annotation-api")

    // Tests
    testImplementation("junit:junit")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    testImplementation("ch.qos.logback:logback-classic")

    // Test Annotation processing
    testAnnotationProcessor("org.immutables:value")
    testAnnotationProcessor("org.immutables:serial")
    testCompileOnly("org.immutables:value")
    testCompileOnly("org.immutables:serial")

    // !! Update dependencies in pom.xml as well
}

// Copy test resources into classes directory, to make them accessible as classloader resources at runtime.
val copyTestResourcesTask = tasks.create<Copy>("copyTestResources") {
    from("$projectDir/src/test/resources")
    into("$buildDir/classes/java/test")
}
tasks.getByName("processTestResources").dependsOn(copyTestResourcesTask)
