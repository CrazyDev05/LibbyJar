package de.crazydev22.libbyjar.data

import net.byteflux.libby.relocation.Relocation
import java.util.*
import java.util.stream.Collectors

public data class DependencyData public constructor(
    public val repositories: Collection<String>,
    public val dependencies: MutableSet<Dependency>,
    public val relocations: Collection<Relocation>
) {

    internal fun toSourceFile(packageName: String, className: String = "Libraries"): String {
        return """package $packageName;

import net.byteflux.libby.Library;
import net.byteflux.libby.LibraryManager;
import net.byteflux.libby.classloader.IsolatedClassLoader;
import net.byteflux.libby.relocation.Relocation;

import java.util.List;

public final class $className {
    public static final List<String> REPOSITORIES = unescape(${repositories.toValue { it.escaped() }});
    public static final List<Relocation> RELOCATIONS = ${relocations.toValue { it.toValue() }};
    public static final List<Library> LIBRARIES = ${dependencies.toValue()};

    public static <T extends LibraryManager> void load(T manager) {
        REPOSITORIES.forEach(manager::addRepository);
        LIBRARIES.forEach(manager::loadLibrary);
    }

    public static <T extends LibraryManager> IsolatedClassLoader loadIsolated(T manager) {
        REPOSITORIES.forEach(manager::addRepository);
        IsolatedClassLoader loader = new IsolatedClassLoader();
        LIBRARIES.forEach(lib -> loader.addPath(manager.downloadLibrary(lib)));
        return loader;
    }

    private static Relocation relocation(String pattern, String relocatedPattern, List<String> includes, List<String> excludes) {
        return new Relocation(unescape(pattern), unescape(relocatedPattern), unescape(includes), unescape(excludes));
    }

    private static Library library(String groupId, String artifactId, String version, String sha256) {
        groupId = unescape(groupId);
        Library.Builder builder = Library.builder()
                .id(groupId + ":" + artifactId + ":" + version)
                .groupId(groupId)
                .artifactId(artifactId)
                .version(version);
        if (sha256 != null && !sha256.isEmpty())
            builder.checksum(sha256);
        RELOCATIONS.forEach(builder::relocate);
        return builder.build();
    }
    
    private static List<String> unescape(List<String> raw) {
        return raw.stream().map($className::unescape).toList();
    }
    
    private static String unescape(String raw) {
        return raw.replace("{}", ".").replace("{/}", "/");
    }
}"""
    }

    private fun String.escaped(replace: Boolean = true): String =
        if (replace) "\"${replace(".", "{}").replace("/", "{/}")}\""
        else "\"${this}\""

    private fun <T> Collection<T>.toValue(transform: (T) -> String): String {
        return this.joinToString(prefix = "List.of(", postfix = ")", separator = ", ", transform = transform)
    }

    private fun Relocation.toValue(): String {
        return "relocation(${pattern.escaped()}, ${relocatedPattern.escaped()}, ${includes.toValue { it.escaped() }}, ${excludes.toValue { it.escaped() }})"
    }

    private fun MutableSet<Dependency>.toValue(): String {
        val flat = mutableSetOf<Dependency>()
        val queue = LinkedList<Dependency>(this)
        while (queue.isNotEmpty()) {
            val dep = queue.removeFirst()
            if (!flat.add(dep))
                continue
            queue.addAll(dep.transitive)
        }

        return flat.stream()
            .map { "library(${it.groupId.escaped()}, ${it.artifactId.escaped(false)}, ${it.version.escaped(false)}, ${it.sha256?.escaped(false)})" }
            .collect(Collectors.joining(", ", "List.of(", ")"))
    }
}
