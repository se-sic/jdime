import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.internal.impldep.aQute.bnd.maven.MavenRepository
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.idea.IdeaPlugin

//buildscript {
//    repositories.maven {
//        setUrl("https://plugins.gradle.org/m2/")
//    }
//
//    dependencies {
//        classpath("org.ajoberstar:gradle-git:1.6.0")
//        classpath("gradle.plugin.nl.javadude.gradle.plugins:license-gradle-plugin:0.13.1")
//        classpath("com.scuilion.syntastic:gradle-syntastic-plugin:0.3.8")
//    }
//}

apply {
    plugin<ApplicationPlugin>()
    plugin<EclipsePlugin>()
    plugin<IdeaPlugin>()
//    plugin("org.ajoberstar.grgit")
//    plugin("com.github.hierynomus.license")
//    plugin("com.scuilion.syntastic")
}

configure<JavaPluginConvention> {
    sourceSets.apply {
        getByName("main").apply {
            java.srcDir("src")
            resources.srcDir("res")
        }

        getByName("test").apply {
            java.srcDir("test")
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compile("commons-io:commons-io:2.5")
    compile("commons-cli:commons-cli:1.3.1")
    compile("org.apache.commons:commons-math3:3.6.1")
    compile("com.thoughtworks.xstream:xstream:1.4.9")
    compile("de.uni-passau.fim.seibt:kvconfig:1.0")
    compile(files("lib/JJ7.jar"))
    testCompile("junit:junit:4.12")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}