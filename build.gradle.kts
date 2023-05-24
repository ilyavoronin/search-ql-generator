import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "searchql-gen"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup:kotlinpoet:1.12.0")
    testImplementation(kotlin("test"))
    implementation(kotlin("reflect"))
}

tasks.test {
    useJUnitPlatform()
    dependsOn("beforeTests")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

tasks {
    task("buildGenerator", type=Jar::class) {
        archiveFileName.set("search-ql-generator.jar")
        manifest {
            attributes(mapOf("Main-Class" to "MainKt"))
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        isZip64 = true
        from (project.configurations.runtimeClasspath.get().map {if (it.isDirectory) it else zipTree(it) })
        with(getByName("jar") as CopySpec)
    }

    task("beforeTests", type=JavaExec::class) {
        classpath = sourceSets["main"].runtimeClasspath
        main = "BeforeTestsKt"
    }
}