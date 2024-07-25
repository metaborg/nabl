import org.metaborg.convention.Developer
import org.metaborg.convention.MavenPublishConventionExtension

// Workaround for issue: https://youtrack.jetbrains.com/issue/KTIJ-19369
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("org.metaborg.convention.root-project")
    alias(libs.plugins.gitonium)
    alias(libs.plugins.spoofax.gradle.langspec) apply false
}

rootProjectConvention {
    // Add `publishAll` and `publish` tasks that delegate to the subprojects and included builds.
    registerPublishTasks.set(true)
}

allprojects {
    apply(plugin = "org.metaborg.gitonium")

    // Configure Gitonium before setting the version
    gitonium {
        mainBranch.set("master")
    }

    version = gitonium.version
    group = "org.metaborg.devenv"

    pluginManager.withPlugin("org.metaborg.convention.maven-publish") {
        extensions.configure(MavenPublishConventionExtension::class.java) {
            repoOwner.set("metaborg")
            repoName.set("nabl")

            metadata {
                inceptionYear.set("2011")
                developers.set(listOf(
                    Developer("hendrikvanantwerpen", "Hendrik van Antwerpen", "hendrik@van-antwerpen.net"),
                    Developer("AZWN", "Aron Zwaan", "aronzwaan@gmail.com"),
                    Developer("guwac", "Guido Wachsmuth", "g.h.wachsmuth@tudelft.nl"),
                    Developer("Gohla", "Gabriel Konat", "gabrielkonat@gmail.com"),
                    Developer("Virtlink", "Daniel A. A. Pelsmaeker", "developer@pelsmaeker.net"),
                    Developer("Apanatshka", "Jeff Smits", "mail@jeffsmits.net"),
                    Developer("lennartcl", "Lennart Kats", "lclkats@gmail.com"),
                ))
            }
        }
    }
}
