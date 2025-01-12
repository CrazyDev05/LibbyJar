package de.crazydev22.libbyjar

import net.byteflux.libby.relocation.Relocation
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.kotlin.dsl.setProperty

public abstract class LibbyJarExtension(project: Project) {

    @Transient
    internal val relocations = mutableSetOf<Relocation>()
        get() = if (ignoreRelocation.get()) mutableSetOf() else field

    /**
     * Sets a global repositories that will be used to resolve dependencies,
     * If not set each dependency will attempt to resolve from one of the projects repositories.
     *
     * When set the global repositories will be the only used repositories.
     */
    public val globalRepositories: SetProperty<String> = project.objects.setProperty<String>()
        .apply(SetProperty<*>::finalizeValueOnRead)

    public val packageName: Property<String> = project.objects.property(String::class.java)
        .apply(Property<String>::finalizeValueOnRead)
        .apply { set("de.crazydev22.libbyjar") }

    public val ignoreRelocation: Property<Boolean> = project.objects.property(Boolean::class.java)
        .apply(Property<Boolean>::finalizeValueOnRead)
        .apply { set(project.findProperty("ignoreRelocation")?.toString()?.toBoolean() == true) }

    public val className: Property<String> = project.objects.property(String::class.java)
        .apply(Property<String>::finalizeValueOnRead)
        .apply { set("Libraries") }

    public val repository: Property<String> = project.objects.property(String::class.java)
        .apply(Property<String>::finalizeValueOnRead)
        .apply { set("https://repo.alessiodp.com/releases/") }

    public val type: Property<String> = project.objects.property(String::class.java)
        .apply(Property<String>::finalizeValueOnRead)
        .apply { set("core") }

    public val version: Property<String> = project.objects.property(String::class.java)
        .apply(Property<String>::finalizeValueOnRead)
        .apply { set("1.3.1") }

    public val config: Property<String> = project.objects.property(String::class.java)
        .apply(Property<String>::finalizeValueOnRead)
        .apply { set("implementation") }

    /**
     * @receiver the original path
     * @param target the prefixed path to relocate to.
     */
    @JvmName("relocateInfix")
    public infix fun String.relocate(target: String) {
        relocate(this, target) {}
    }

    public fun relocate(original: String, target: String): LibbyJarExtension {
        return relocate(original, target) {}
    }

    public fun relocate(
        original: String,
        relocated: String,
        configure: Action<Relocation.Builder>
    ): LibbyJarExtension {
        val builder = Relocation.builder()
            .pattern(original)
            .relocatedPattern(relocated)
        configure.execute(builder)
        relocations.add(builder.build())
        return this
    }
}