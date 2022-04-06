/*
 * Copyright (C) 2013-2014 Olaf Lessenich
 * Copyright (C) 2014-2015 University of Passau, Germany
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 *
 * Contributors:
 *     Olaf Lessenich <lessenic@fim.uni-passau.de>
 *     Georg Seibt <seibt@fim.uni-passau.de>
 */
import org.ajoberstar.grgit.Grgit

plugins {
    application
    eclipse
    idea

    id("org.ajoberstar.grgit") version ("5.0.0") // Used for interacting with git repositories from the buildscript.
    id("com.github.hierynomus.license") version ("0.16.1")  // Used to ensure that all files contain a license header.
    id("com.scuilion.syntastic") version ("0.3.9")
    id("com.github.ben-manes.versions") version ("0.42.0") // Used to check for new plugin / dependency versions.
}

tasks.withType(JavaCompile::class).configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:all")
}

tasks.javadoc {
    options.encoding = "UTF-8"
}

sourceSets {
    main {
        java.srcDir("src")
        resources.srcDir("res")
    }
    test {
        java.srcDir("test")
        resources.srcDir("testres")
    }
}

val JNM_MAVEN_PROP = "JNM_MAVEN"
val USE_JNM_MAVEN = !project.ext.has(JNM_MAVEN_PROP) || (project.ext.get(JNM_MAVEN_PROP) as String).toBoolean()

val DIST_DIR_PROP = "distDir"

repositories {
    mavenCentral()

    if (USE_JNM_MAVEN) {
        maven {
            // The GitLab Maven endpoint of the JNativeMerge project
            setUrl("https://gitlab.infosun.fim.uni-passau.de/api/v4/projects/199/packages/maven")
            content { includeModuleByRegex("de.uni_passau.fim.seibt", "^jnativemerge.*$") }
        }
    }
}

dependencies {
    implementation("commons-io:commons-io:2.11.0")
    implementation("commons-cli:commons-cli:1.5.0")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("commons-codec:commons-codec:1.15")
    implementation("com.thoughtworks.xstream:xstream:1.4.19")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("de.uni-passau.fim.seibt:kvconfig:1.0")

    if (USE_JNM_MAVEN) {
        implementation("de.uni_passau.fim.seibt:jnativemerge:0.28.1_3")
    } else {
        implementation(project(":JNativeMerge"))
    }

    implementation(files("lib/extendj.jar"))
    testImplementation("junit:junit:4.13.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
        vendor.set(JvmVendorSpec.ADOPTOPENJDK)
    }
}

license {
    header = rootProject.file("LICENSE_HEADER")
    mapping("fxml", "XML_STYLE")
    strictCheck = true

    fileTree("testres").visit {
        exclude(relativePath.pathString)
    }

    exclude("de/fosd/jdime/matcher/unordered/assignmentProblem/HungarianAlgorithm.java")
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

val loggingConfigFile = file("JDimeLogging.properties")
val configFile = file("JDime.properties")

tasks.startScripts {
    applicationName = rootProject.name
    mainClass.set("de.fosd.jdime.Main")
    defaultJvmOpts = listOf("-Xmx2048m", "-ea")
}

distributions {
    main {
        val binDir = "bin"

        contents {
            into(binDir) {
                from(project.projectDir) {
                    include(configFile.name)

                    val getHeadCommit: () -> String = {
                        val grgit = Grgit.open(mapOf("dir" to project.projectDir))
                        val head = grgit.head()

                        grgit.close()
                        head.id
                    }

                    expand("commit" to getHeadCommit)
                }

                from(project.projectDir) {
                    include(loggingConfigFile.name)
                }
            }
        }
    }
}

tasks.installDist {
    if (project.ext.has(DIST_DIR_PROP)) {
        destinationDir = file(project.ext[DIST_DIR_PROP] as String)
    }
}

tasks.named<JavaExec>("run") {
    mainClass.set("de.fosd.jdime.Main")
}

tasks.test {
    systemProperty("java.util.logging.config.file", loggingConfigFile.getAbsolutePath())
    maxHeapSize = "2048m"
    enableAssertions = true
}