package de.crazydev22.libbyjar

import net.byteflux.libby.relocation.Relocation
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property

public abstract class LibbyJarExtension(project: Project) {

    @Transient
    internal val relocations = mutableSetOf<Relocation>()
        get() = if (ignoreRelocation.get()) mutableSetOf() else field

    public val ignoreRelocation: Property<Boolean> = project.objects.property(Boolean::class.java)
        .apply(Property<Boolean>::finalizeValueOnRead)
        .apply { set(project.findProperty("ignoreRelocation")?.toString()?.toBoolean() == true) }

    /**
     * Whether to generate checksums or not
     */
    public val checksum: Property<Boolean> = project.objects.property(Boolean::class.java)
        .apply(Property<Boolean>::finalizeValueOnRead)
        .apply { set(true) }

    /**
     * The package of the generated libraries class
     */
    public val packageName: Property<String> = project.objects.property(String::class.java)
        .apply(Property<String>::finalizeValueOnRead)
        .apply { set("de.crazydev22.libbyjar") }

    /**
     * The name of the generated libraries class
     */
    public val className: Property<String> = project.objects.property(String::class.java)
        .apply(Property<String>::finalizeValueOnRead)
        .apply { set("Libraries") }

    /**
     * The Repository to get libby from
     */
    public val repository: Property<String> = project.objects.property(String::class.java)
        .apply(Property<String>::finalizeValueOnRead)
        .apply { set("https://repo.alessiodp.com/releases/") }

    /**
     * The Type of Libby default is 'core'
     */
    public val type: Property<String> = project.objects.property(String::class.java)
        .apply(Property<String>::finalizeValueOnRead)
        .apply { set("core") }

    /**
     * The version of libby default is '1.3.1'
     */
    public val version: Property<String> = project.objects.property(String::class.java)
        .apply(Property<String>::finalizeValueOnRead)
        .apply { set("1.3.1") }

    /**
     * The gradle configuration to add libby to
     */
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