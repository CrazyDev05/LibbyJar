package de.crazydev22.libbyjar.data

import net.byteflux.libby.Library
import net.byteflux.libby.relocation.Relocation
import java.util.*

public data class Dependency public constructor(
    public val groupId: String,
    public val artifactId: String,
    public val version: String,
    public val snapshotId: String?,
    public val sha256: String?,
    public val transitive: MutableCollection<Dependency>
) : Comparable<Dependency> {
    public fun toLibrary(relocations: MutableCollection<Relocation>): Library? {
        val builder = Library.builder()
            .id(toString())
            .groupId(groupId)
            .artifactId(artifactId)
            .version(version)
        if (sha256 != null && !sha256.isEmpty()) builder.checksum(sha256)
        relocations.forEach { builder.relocate(it) }
        return builder.build()
    }

    public fun isSimilar(dependency: Dependency): Boolean {
        return groupId == dependency.groupId && artifactId == dependency.artifactId
    }

    override fun toString(): String {
        val snapshotId = this.snapshotId
        val suffix = if (snapshotId != null && snapshotId.isNotEmpty()) ":$snapshotId" else ""
        return this.groupId + ":" + this.artifactId + ":" + this.version + suffix
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as Dependency
        return groupId == that.groupId &&
                artifactId == that.artifactId &&
                version == that.version &&
                sha256 == that.sha256
    }

    override fun hashCode(): Int {
        return Objects.hash(groupId, artifactId, version, sha256)
    }

    override fun compareTo(other: Dependency): Int {
        if (groupId != other.groupId)
            return groupId.compareTo(other.groupId)
        if (artifactId != other.artifactId)
            return artifactId.compareTo(other.artifactId)
        return version.compareTo(other.version)
    }
}