plugins {
    `java-library`
//    id("org.metaborg.gradle.config.root-project") version "0.5.6"
    id("org.metaborg.gitonium") version "1.2.0"
    id("org.metaborg.devenv.spoofax.gradle.langspec") version "0.1.37" apply false
}

val spoofax2Version: String = System.getProperty("spoofax2Version")
val spoofax2BaselineVersion: String = System.getProperty("spoofax2BaselineVersion")
val spoofax2DevenvVersion: String = System.getProperty("spoofax2DevenvVersion")
allprojects {
    apply(plugin = "java-library")

    group = "org.metaborg.devenv"
    ext["spoofax2Version"] = spoofax2Version
    ext["spoofax2BaselineVersion"] = spoofax2BaselineVersion
    ext["spoofax2DevenvVersion"] = spoofax2DevenvVersion

    repositories {
        maven(url = "https://artifacts.metaborg.org/content/groups/public/")
        mavenCentral() // Backup
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs = options.compilerArgs + listOf("-Xdoclint:none")
    }

    tasks.withType<Javadoc> {
        options {
            this as CoreJavadocOptions
            addStringOption("Xdoclint:none", "-quiet")
            encoding = "UTF-8"
            quiet()
            charset("UTF-8")
        }
    }

    // Ugh, need to encode sourcesJar due to multiple gradle.config plugins
//    metaborg {
//        javaCreateSourcesJar = false
//    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    }

    val sourcesJar = tasks.getByName<Jar>("sourcesJar") {
        dependsOn("classes")
        from(sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).allJava)
        archiveClassifier.set("sources")
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    tasks {
        assemble {
            dependsOn("sourcesJar")
        }
    }

    artifacts {
        add(Dependency.DEFAULT_CONFIGURATION, sourcesJar)
    }

}

//subprojects {
//    metaborg {
//        configureSubProject()
//    }
//}


tasks.register("includedProjects") {
    doLast {
        println("Included projects")
        println("---------------")
        for ((name, subproject) in project.childProjects) {
            println("$name: ${subproject.group}:${subproject.name}:${subproject.version}")
        }
    }
}