package de.crazydev22.libbyjar.func

import de.crazydev22.libbyjar.LibbyJarPlugin.Companion.API_CONFIGURATION_NAME
import de.crazydev22.libbyjar.LibbyJarPlugin.Companion.CONFIGURATION_NAME
import org.gradle.api.Action
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.internal.Cast.uncheckedCast

/**
 * Adds `libby` configuration for Kotlin dsl with options
 */
public fun DependencyHandler.libby(
    dependencyNotation: String,
    dependencyOptions: Action<ExternalModuleDependency>
): ExternalModuleDependency? {
    return withOptions(CONFIGURATION_NAME, dependencyNotation, dependencyOptions)
}

/**
 * Adds `libbyApi` configuration for Kotlin dsl with options
 */
public fun DependencyHandler.libbyApi(
    dependencyNotation: String,
    dependencyOptions: Action<ExternalModuleDependency>
): ExternalModuleDependency? {
    return withOptions(API_CONFIGURATION_NAME, dependencyNotation, dependencyOptions)
}

/**
 * Alternative for adding `libby` configuration for Kotlin dsl but without options
 */
public fun DependencyHandler.libby(dependencyNotation: Any): Dependency? = add(CONFIGURATION_NAME, dependencyNotation)

/**
 * Alternative for adding `libbyApi` configuration for Kotlin dsl but without options
 */
public fun DependencyHandler.libbyApi(dependencyNotation: Any): Dependency? =
    add(API_CONFIGURATION_NAME, dependencyNotation)

/**
 * Creates a configuration with options
 */
private fun DependencyHandler.withOptions(
    configuration: String,
    dependencyNotation: String,
    dependencyConfiguration: Action<ExternalModuleDependency>
): ExternalModuleDependency? = run {
    uncheckedCast<ExternalModuleDependency>(create(dependencyNotation)).also { dependency ->
        if (dependency == null) return@run null
        dependencyConfiguration.execute(dependency)
        add(configuration, dependency)
    }
}