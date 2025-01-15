<h1 align="center">Libby Jar</h1>
<h3 align="center">Runtime Dependency Management</h3>
  <div align="center">
    <a href="https://github.com/CrazyDev05/libbyjar">
        <img src="https://img.shields.io/github/license/CrazyDev05/libbyjar">
    </a>
    <a href="https://github.com/CrazyDev05/libbyjar/actions/workflows/gradle.yml">
        <img src="https://github.com/CrazyDev05/libbyjar/actions/workflows/gradle.yml/badge.svg">
    </a>
    <a href="https://plugins.gradle.org/plugin/de.crazydev22.libbyjar">
        <img src="https://img.shields.io/gradle-plugin-portal/v/de.crazydev22.libbyjar">
    </a>
  </div>

<hr>

<h4>What is Libbyjar?</h4>

LibbyJar allows you to easily include your gradle dependencies in your plugin with the help of <a href="https://github.com/AlessioDP/libby">libby</a>

<h2 align="center">Usage Example</h2>
<h4 align="center">Note: Use the shadowJar task to compile your project</h4>
<br><br>


```java
// this needs to be ran before you reference your dependencies
Libraries.load(new PaperLibraryManager(plugin));
```
(NOTE: If you have specified relocations and are running in a IDE or any environment that does not use the shadowjar-ed build file, use the `ignoreRelocation` flag while running by using `-DignoreRelocation` in your runner arguments)
*build.gradle.kts*
```kotlin
plugins { 
    id("com.gradleup.shadow") version "8.3.5"
    id("de.crazydev22.libbyjar") version "1.0.0"
}
dependencies { 
    libby("group.id:artifact.id:version")
}

libbyJar { 
    relocate("a.b.c", "m.n.o")
    packageName = "de.crazydev22.libbyjar"
    className = "Libraries"
    
    type = "paper"
    version = "1.3.1"
}
```
