plugins {
  id("org.metaborg.gradle.config.java-library") version("0.4.7")
  id("org.metaborg.gradle.config.junit-testing") version("0.4.7")
}

// Used for refsyn: disabled by default
fun kaptEnabled() = extra.has("kaptEnabled") && extra["kaptEnabled"] as Boolean
val spoofax2Version: String by ext
val spoofax2DevenvVersion: String by ext
dependencies {
  api(platform("org.metaborg:parent:$spoofax2Version"))
  testImplementation(platform("org.metaborg:parent:$spoofax2Version"))
  annotationProcessor(platform("org.metaborg:parent:$spoofax2Version"))
  testAnnotationProcessor(platform("org.metaborg:parent:$spoofax2Version"))
  if(kaptEnabled()) {
    kapt(platform("org.metaborg:parent:$version"))
    kaptTest(platform("org.metaborg:parent:$version"))
  }

  // !! Update dependencies in pom.xml as well

  api("org.metaborg:org.metaborg.util:$spoofax2Version") // API to expose logger framework.
  api("org.metaborg:org.spoofax.terms:$spoofax2Version")
  api("org.metaborg:org.spoofax.interpreter.core:$spoofax2Version")
  api(project(":nabl2.terms"))
  api(project(":scopegraph"))
  api(project(":p_raffrayi"))

  api("io.usethesource:capsule")
  compileOnly("com.google.code.findbugs:jsr305")

  // Annotation processing
  annotationProcessor("org.immutables:value")
  annotationProcessor("org.immutables:serial")
  if(kaptEnabled()) {
    kapt("org.immutables:value")
    kapt("org.immutables:serial")
  }
  compileOnly("org.immutables:value")
  compileOnly("org.immutables:serial")
  compileOnly("javax.annotation:javax.annotation-api")
  compileOnly("javax.inject:javax.inject:1")

  // Tests
  testImplementation("junit:junit")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
  testImplementation("ch.qos.logback:logback-classic")
  testCompileOnly("com.google.code.findbugs:jsr305")

  // Test Annotation processing
  testAnnotationProcessor("org.immutables:value")
  testAnnotationProcessor("org.immutables:serial")
  if(kaptEnabled()) {
    kaptTest("org.immutables:value")
    kaptTest("org.immutables:serial")
  }
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
