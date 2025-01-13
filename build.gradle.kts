
plugins {
    id("java-gradle-plugin")
    id("com.gradle.plugin-publish") version "1.3.0"
    id("com.gradleup.shadow") version "8.3.5"
    kotlin("jvm") version "2.0.21"
}

group = "de.crazydev22"
version = "1.0.0"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.alessiodp.com/releases/")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.21")
    implementation("net.byteflux:libby-core:1.3.1")

    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())
    compileOnly("com.gradleup.shadow:shadow-gradle-plugin:8.3.5")

    testImplementation(kotlin("test"))
    testImplementation("com.gradleup.shadow:shadow-gradle-plugin:8.3.5")
    testImplementation(gradleApi())
    testImplementation(gradleKotlinDsl())
    testImplementation(gradleTestKit())
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

val ensureDependenciesAreInlined by tasks.registering {
    description = "Ensures all declared dependencies are inlined into shadowed jar"
    group = HelpTasksPlugin.HELP_GROUP
    dependsOn(tasks.shadowJar)

    doLast {
        val nonInlinedDependencies = mutableListOf<String>()
        zipTree(tasks.shadowJar.flatMap { it.archiveFile }).visit {
            if (!isDirectory) return@visit

            val path = relativePath
            if (
                !path.startsWith("META-INF") &&
                path.lastName.endsWith(".class") &&
                !path.pathString.startsWith("de/crazydev22/libbyjar")
            ) nonInlinedDependencies.add(path.pathString)
        }

        if (nonInlinedDependencies.isEmpty()) return@doLast
        throw GradleException("Found non inlined dependencies: $nonInlinedDependencies")
    }
}

tasks {
    named("check") {
        dependsOn(ensureDependenciesAreInlined)
        dependsOn(validatePlugins)
    }

    shadowJar {
        archiveClassifier.set("")

        mapOf(
            "kotlin" to ".kotlin",
            "org.intellij" to ".intellij",
            "org.jetbrains" to ".jetbrains"
        ).forEach { relocate(it.key, "de.crazydev22.libbyjar${it.value}") }
    }

    test {
        useJUnitPlatform()
    }
}

@Suppress("UnstableApiUsage")
gradlePlugin {
    website.set("https://github.com/CrazyDev05/LibbyJar")
    vcsUrl.set("https://github.com/CrazyDev05/LibbyJar")

    plugins {
        create("libbyjar") {
            id = "de.crazydev22.libbyjar"
            displayName = "LibbyJar"
            description = "Libby Configuration Generator."
            implementationClass = "de.crazydev22.libbyjar.LibbyJarPlugin"
            tags.set(listOf("runtime dependency", "relocation"))
        }
    }
}