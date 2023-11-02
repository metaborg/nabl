plugins {
  id("org.metaborg.gradle.config.java-library")
  id("org.metaborg.gradle.config.junit-testing")
}

val spoofax2Version: String by ext
val spoofax2DevenvVersion: String by ext
dependencies {
  api(platform("org.metaborg:parent:$spoofax2Version"))
  testImplementation(platform("org.metaborg:parent:$spoofax2Version"))
  annotationProcessor(platform("org.metaborg:parent:$spoofax2Version"))
  testAnnotationProcessor(platform("org.metaborg:parent:$spoofax2Version"))

  // !! Update dependencies in pom.xml as well

  implementation("org.metaborg:org.metaborg.util:$spoofax2Version")
  api("org.metaborg:org.spoofax.terms:$spoofax2Version")
  api("org.metaborg:org.spoofax.interpreter.core:$spoofax2Version")
  api("io.usethesource:capsule")
  compileOnly(libs.jsr305)

  // Annotation processing
  annotationProcessor("org.immutables:value")
  annotationProcessor("org.immutables:serial")
  compileOnly("org.immutables:value")
  compileOnly("org.immutables:serial")
  compileOnly(libs.javax.annotation)

  // Tests
  testImplementation("junit:junit")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
  testImplementation("ch.qos.logback:logback-classic")
  testCompileOnly(libs.jsr305)

  // Test Annotation processing
  testAnnotationProcessor("org.immutables:value")
  testAnnotationProcessor("org.immutables:serial")
  testCompileOnly("org.immutables:value")
  testCompileOnly("org.immutables:serial")
  testCompileOnly(libs.javax.annotation)

  // !! Update dependencies in pom.xml as well
}

// Copy test resources into classes directory, to make them accessible as classloader resources at runtime.
val copyTestResourcesTask = tasks.create<Copy>("copyTestResources") {
  from("$projectDir/src/test/resources")
  into("$buildDir/classes/java/test")
}
tasks.getByName("processTestResources").dependsOn(copyTestResourcesTask)
