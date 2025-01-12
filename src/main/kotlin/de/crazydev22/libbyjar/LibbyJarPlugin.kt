package de.crazydev22.libbyjar

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import de.crazydev22.libbyjar.exceptions.ShadowNotFoundException
import de.crazydev22.libbyjar.func.addDependency
import de.crazydev22.libbyjar.func.addRepository
import de.crazydev22.libbyjar.func.createConfig
import de.crazydev22.libbyjar.func.findTasks
import de.crazydev22.libbyjar.task.LibbyJarTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.withType

public class LibbyJarPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit = with(project) {
        plugins.apply(JavaPlugin::class.java)

        if (!plugins.hasPlugin(SHADOW_ID)) {
            throw ShadowNotFoundException("LibbyGenerator depends on the Shadow plugin, please apply the plugin. For more information visit: https://imperceptiblethoughts.com/shadow/")
        }

        val libbyJarExtension = extensions.create("libbyJar", LibbyJarExtension::class.java, project)

        val config = createConfig(
            CONFIGURATION_NAME,
            JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
            JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
        )
        if (plugins.hasPlugin("java-library")) {
            createConfig(
                API_CONFIGURATION_NAME,
                JavaPlugin.COMPILE_ONLY_API_CONFIGURATION_NAME,
                JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME
            )
        }

        val libbyJar = tasks.create(JAR_TASK_NAME, LibbyJarTask::class.java, config, libbyJarExtension)
        // Hooks into shadow to inject relocations
        tasks.withType<ShadowJar>().configureEach {
            it
            it.doFirst { _ ->
                libbyJarExtension.relocations.forEach { rule ->
                    it.relocate(rule.pattern, rule.relocatedPattern) { r ->
                        rule.includes.forEach { r.include(it) }
                        rule.excludes.forEach { r.exclude(it) }
                    }
                }
            }
        }

        tasks.findByName("prepareKotlinBuildScriptModel")?.dependsOn(libbyJar)

        afterEvaluate {
            libbyJar.mustRunAfter(
                findTasks(
                    "generateEffectiveLombokConfig",
                    "generateTestEffectiveLombokConfig",
                    "processTestResources",
                    "processResources"
                )
            )

            addRepository(libbyJarExtension)
            addDependency(libbyJarExtension)

            val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
            sourceSets.named("main") {
                it.java.srcDir(provider { libbyJar.outputs })
            }
        }
    }

    public companion object {
        public const val CONFIGURATION_NAME: String = "libby"
        public const val API_CONFIGURATION_NAME: String = "libbyApi"
        public const val JAR_TASK_NAME: String = "libbyJar"
        private const val SHADOW_ID = "com.gradleup.shadow"
    }
}