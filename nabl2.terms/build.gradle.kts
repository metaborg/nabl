plugins {
  id("org.metaborg.gradle.config.java-library")
  id("org.metaborg.gradle.config.junit-testing")
}

dependencies {
  api(platform("org.metaborg:parent:$version"))
  annotationProcessor(platform("org.metaborg:parent:$version"))

  // !! Update dependencies in pom.xml as well

  api("org.metaborg:org.metaborg.util:$version")
  api("org.metaborg:org.spoofax.terms:$version")
  api("org.metaborg:org.spoofax.interpreter.core:$version")

  api("com.google.guava:guava")
  api("io.usethesource:capsule")
  compileOnly("com.google.code.findbugs:jsr305")

  // Annotation processing
  annotationProcessor("org.immutables:value")
  annotationProcessor("org.immutables:serial")
  compileOnly("org.immutables:value")
  compileOnly("org.immutables:serial")
  compileOnly("javax.annotation:javax.annotation-api")

  testCompileOnly("junit:junit")
  testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.1.0")
  testCompileOnly("ch.qos.logback:logback-classic")
  testCompileOnly("com.google.code.findbugs:jsr305")

  // !! Update dependencies in pom.xml as well
}

// Copy test resources into classes directory, to make them accessible as classloader resources at runtime.
val copyTestResourcesTask = tasks.create<Copy>("copyTestResources") {
  from("$projectDir/src/test/resources")
  into("$buildDir/classes/java/test")
}
tasks.getByName("processTestResources").dependsOn(copyTestResourcesTask)
