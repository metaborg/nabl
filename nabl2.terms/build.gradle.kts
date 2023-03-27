plugins {
  id("org.metaborg.gradle.config.java-library")
  id("org.metaborg.gradle.config.junit-testing")
}

val spoofax2Version: String by ext
fun compositeBuild(name: String) = "org.metaborg:$name:$spoofax2Version"

dependencies {
  api(platform(compositeBuild("parent")))
  testImplementation(platform(compositeBuild("parent")))
  annotationProcessor(platform(compositeBuild("parent")))
  testAnnotationProcessor(platform(compositeBuild("parent")))

  // !! Update dependencies in pom.xml as well

  implementation(compositeBuild("org.metaborg.util"))
  api(compositeBuild("org.spoofax.terms"))
  api(compositeBuild("org.spoofax.interpreter.core"))

  api("com.google.guava:guava")
  // Required for Guava >= 27.0:
  // api("com.google.guava:failureaccess")
  api("io.usethesource:capsule")
  compileOnly("com.google.code.findbugs:jsr305")

  // Annotation processing
  annotationProcessor("org.immutables:value")
  annotationProcessor("org.immutables:serial")
  compileOnly("org.immutables:value")
  compileOnly("org.immutables:serial")
  compileOnly("javax.annotation:javax.annotation-api")

  // Tests
  testImplementation("junit:junit")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
  testImplementation("ch.qos.logback:logback-classic")
  testCompileOnly("com.google.code.findbugs:jsr305")

  // Test Annotation processing
  testAnnotationProcessor("org.immutables:value")
  testAnnotationProcessor("org.immutables:serial")
  testCompileOnly("org.immutables:value")
  testCompileOnly("org.immutables:serial")
  testCompileOnly("javax.annotation:javax.annotation-api")

  // !! Update dependencies in pom.xml as well
}

// Copy test resources into classes directory, to make them accessible as classloader resources at runtime.
val copyTestResourcesTask = tasks.create<Copy>("copyTestResources") {
  from("$projectDir/src/test/resources")
  into("$buildDir/classes/java/test")
}
tasks.getByName("processTestResources").dependsOn(copyTestResourcesTask)
