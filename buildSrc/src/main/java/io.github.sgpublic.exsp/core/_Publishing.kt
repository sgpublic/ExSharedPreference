package io.github.sgpublic.exsp.core

import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.configurationcache.extensions.capitalized
import org.gradle.plugins.signing.SigningExtension
import java.net.URI

fun Project.applyJavaPublishing() {
    applyPublishing(this, "java")
}

fun Project.applyAndroidPublishing() {
    applyPublishing(this, "release")
}

private fun applyPublishing(project: Project, type: String) {
    val publishing_username = (project.findProperty("publising.username") ?: return).toString()
    val publishing_password = (project.findProperty("publising.password") ?: return).toString()
    project.properties.keys.filter { it.startsWith("signing.") }.let {
        if (it.isEmpty()) return
    }

    project.rootProject.applyPublishingTask()

    val rootName = "exsp"
    val taskName = rootName + project.name.capitalized()
    val projectName = rootName + "-" + project.name

    project.extensions.configure<PublishingExtension>("publishing") {
        publications {
            register(taskName, MavenPublication::class.java) {
                groupId = project.group.toString()
                artifactId = projectName
                version = project.version.toString()

                pom {
                    name.set(projectName)
                    description.set(projectName)
                    url.set("https://github.com/sgpublic/ExSharedPreference")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    issueManagement {
                        system.set("GitHub")
                        url.set("https://github.com/sgpublic/ExSharedPreference/issues")
                    }
                    scm {
                        connection.set("scm:git:git://github.com/sgpublic/ExSharedPreference.git")
                        developerConnection.set("scm:git:git@github.com:sgpublic/ExSharedPreference.git")
                        url.set("https://github.com/sgpublic/ExSharedPreference")
                    }
                    developers {
                        developer {
                            id.set("mdmzx")
                            name.set("Madray Haven")
                            email.set("sgpublic2002@gmail.com")
                        }
                    }
                }
                project.afterEvaluate {
                    from(project.components.getByName(type))
                }
            }
        }
        repositories {
            maven {
                name = "ossrh"
                url = if (project.version.toString().endsWith("-SNAPSHOT")) {
                    URI.create("https://oss.sonatype.org/content/repositories/snapshots")
                } else {
                    URI.create("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                }
                credentials {
                    username = publishing_username
                    password = publishing_password
                }
            }
        }
    }

    project.extensions.configure<SigningExtension>("signing") {
        val publishing = project.extensions.getByName("publishing") as PublishingExtension
        sign(publishing.publications.getByName(taskName))
    }
}