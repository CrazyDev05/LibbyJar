package de.crazydev22.libbyjar.func

import de.crazydev22.libbyjar.LibbyJarExtension
import de.crazydev22.libbyjar.exceptions.ConfigurationNotFoundException
import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.kotlin.dsl.maven
import java.io.File
import java.security.MessageDigest

internal fun Project.addRepository(ext: LibbyJarExtension) {
    repositories.maven(ext.repository)
}

internal fun Project.addDependency(ext: LibbyJarExtension) {
    dependencies.add(ext.config.get(), "net.byteflux:libby-${ext.type.get()}:${ext.version.get()}")
}

internal fun File.checksum(algorithm: String = "SHA-256", bufferSize: Int = 4096): String {
    val md = MessageDigest.getInstance(algorithm)
    forEachBlock(bufferSize) { buffer, bytesRead -> md.update(buffer, 0, bytesRead) }
    return md.toHex()
}

internal fun MessageDigest.toHex(): String = digest().toHex()
internal fun ByteArray.toHex() = fold(StringBuilder()) { sb, it -> sb.append("%02x".format(it)) }.toString()

internal fun File.createDirectory(): File {
    if (!exists()) mkdirs()
    return this
}

internal fun Project.findTasks(vararg tasks: String): MutableList<String> {
    val list = mutableListOf<String>()
    for (task in tasks) {
        project.tasks.findByName(task)?.apply { list.add(task) }
    }
    return list
}

public inline fun Any.asGroovyClosure(default: String, crossinline func: (arg: String) -> String): (String) -> String =
    object : Closure<String>(this), (String) -> String, () -> String {
        fun doCall(arg: String) = func(arg)
        fun doCall() = doCall(default)
        override fun invoke(p1: String): String = doCall(p1)
        override fun invoke(): String = invoke(default)
    }

/**
 * Utility for creating a configuration that extends another
 */
public fun Project.createConfig(configName: String, vararg extends: String): Configuration {
    val compileOnlyConfig = extends.map {
        configurations.findByName(it)
            ?: throw ConfigurationNotFoundException("Could not find `$extends` configuration!")
    }

    val libbyConfig = configurations.create(configName)
    compileOnlyConfig.forEach { it.extendsFrom(libbyConfig) }
    libbyConfig.isTransitive = true

    return libbyConfig
}