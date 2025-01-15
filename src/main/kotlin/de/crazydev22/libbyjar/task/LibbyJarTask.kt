package de.crazydev22.libbyjar.task

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import de.crazydev22.libbyjar.LibbyJarExtension
import de.crazydev22.libbyjar.LibbyJarPlugin.Companion.API_CONFIGURATION_NAME
import de.crazydev22.libbyjar.data.Dependency
import de.crazydev22.libbyjar.data.DependencyData
import de.crazydev22.libbyjar.func.checksum
import de.crazydev22.libbyjar.func.createDirectory
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.tasks.*
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableDependency
import org.gradle.api.tasks.diagnostics.internal.graph.nodes.RenderableModuleResult
import java.io.File
import java.nio.charset.StandardCharsets
import javax.inject.Inject

@CacheableTask
public abstract class LibbyJarTask @Inject constructor(
    @Transient private val config: Configuration,
    @Transient private val extension: LibbyJarExtension
) : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public val buildDirectory: File = project.layout.buildDirectory.asFile.get()

    @get:OutputDirectory
    public val outputDirectory: File = buildDirectory.resolve("generated/libbyjar").createDirectory()

    init {
        group = "libbyJar"
        inputs.files(config)
    }

    /**
     * Action to generate the json file inside the jar
     */
    @TaskAction
    internal fun createSource() = with(project) {
        val repositories = repositories.getMavenRepos()
        val dependencies = config.incoming
            .getLibbyDependencies()
            .toMutableSet()

        // If api config is present map dependencies from it as well.
        project.configurations
            .findByName(API_CONFIGURATION_NAME)
            ?.incoming
            ?.getLibbyDependencies()
            ?.toCollection(dependencies)

        val outputFile = outputDirectory.resolve(
            "${extension.packageName.get()}.${extension.className.get()}".replace(
                ".",
                "/"
            ) + ".java"
        )
        if (!outputFile.parentFile.exists()) outputFile.parentFile.mkdirs()

        outputFile.writeText(
            DependencyData(repositories, dependencies, extension.relocations)
                .toSourceFile(extension.packageName.get(), extension.className.get()),
            StandardCharsets.UTF_8
        )
    }

    /**
     * Turns a [RenderableDependency] into a [Dependency] with all its transitives.
     */
    private fun RenderableDependency.toLibbyDependency(): Dependency? {
        val transitive = mutableSetOf<Dependency>()
        collectTransitive(transitive, children)
        return id.toString().toDependency(transitive)
    }

    /**
     * Recursively flattens the transitive dependencies.
     */
    private fun collectTransitive(
        transitive: MutableSet<Dependency>,
        dependencies: Set<RenderableDependency>
    ) {
        for (dependency in dependencies) {
            val dep = dependency.id.toString().toDependency(emptySet()) ?: continue
            if (dep in transitive) continue
            if (dep.artifactId.endsWith("-bom")) continue

            transitive.add(dep)
            collectTransitive(transitive, dependency.children)
        }
    }

    /**
     * Creates a [Dependency] based on a string
     * group:artifact:version:snapshot - The snapshot is the only nullable value.
     */
    private fun String.toDependency(transitive: Set<Dependency>): Dependency? {
        val array = arrayOfNulls<Any>(6)
        array[4] = sha256()
        array[5] = transitive

        split(":").takeIf { it.size >= 3 }?.forEachIndexed { index, s ->
            array[index] = s
        } ?: return null

        return Dependency::class.java.constructors.first().newInstance(*array) as Dependency
    }

    private fun String.sha256(): String? {
        if (!extension.checksum.get() || count { it == ':' } == 3)
            return null
        return project.configurations
            .detachedConfiguration(project.dependencyFactory.create(this))
            .setTransitive(false)
            .resolve()
            .firstOrNull()
            ?.checksum()
    }

    private fun RepositoryHandler.getMavenRepos() = this.filterIsInstance<MavenArtifactRepository>()
        .map { it.url.toString() }
        .filterNot { it.startsWith("file") }
        .toSet()

    private fun ResolvableDependencies.getLibbyDependencies(): List<Dependency> =
        RenderableModuleResult(this.resolutionResult.root).children
            .mapNotNull { it.toLibbyDependency() }
            .filterNot { it.artifactId.endsWith("-bom") }

    protected open fun withShadowTask(
        action: ShadowJar.() -> Unit
    ): ShadowJar? = (project.tasks.findByName("shadowJar") as? ShadowJar)?.apply(action)
}
