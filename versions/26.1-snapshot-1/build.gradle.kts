import io.papermc.sculptor.shared.util.MinecraftJarType
import io.papermc.sculptor.version.tasks.DecompileJar

plugins {
    id("io.papermc.sculptor.version") version "2.0.0-SNAPSHOT"
}

val generateReportsProperty = providers.gradleProperty("generateReports")
mache {
    minecraftVersion = "26.1-snapshot-1"
    minecraftJarType = MinecraftJarType.SERVER

    val args = mutableListOf(
        "--temp-dir={tempDir}",
        "--unpick-file={constantsFile}",
        "--output={output}",
        "--input={input}",
        "--input-classpath={inputClasspath}",
        "--hypo-parallelism=1",
    )
    if (generateReportsProperty.getOrElse("false").toBooleanStrict()) {
        args.addAll(listOf(
            "--reports-dir={reportsDir}",
            "--all-reports",
        ))
    }

    codebookArgs = args
    decompilerArgs.convention(listOf(
        "--only=com/hypixel/hytale",
    ))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:-deprecation", "-Xlint:-removal",
        "-Xlint:-options", "-nowarn", "-Xmaxerrs", "500"))
}

repositories {
    mavenLocal()
}

tasks.withType<DecompileJar> {
    inputJar.set(file("${project.rootDir}/HytaleServer.jar"))
}

val extractResources by tasks.registering(Copy::class) {
    val jarFile = file("${project.rootDir}/HytaleServer.jar")

    from(zipTree(jarFile)) {
        include("manifests.json")
        include("migration/**")
    }

    into("src/main/resources")

    doFirst {
        println("Extracting resources to src/main/resources")
    }
}

dependencies {
    codebook("2.0.0-SNAPSHOT")
    decompiler(vineflower("1.11.2"))
    constants("io.papermc.parchment.data:parchment:1.21.11+build.3")
}

dependencies {
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    //Hytale Libs
    implementation("org.checkerframework:checker-compat-qual:2.5.5")
    implementation("io.netty:netty-all:4.2.9.Final")
    implementation("org.bouncycastle:bcpkix-jdk18on:1.83")
    implementation("com.hypixel:ConcurrentFastUtil:1.2-SNAPSHOT")
    implementation("it.unimi.dsi:fastutil:8.5.12")
}

val repackageJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Repackages the Hytale Server jar with modified classes."

    val inputJarFile = tasks.withType<DecompileJar>().first().inputJar.get().asFile
    from(zipTree(inputJarFile)) {
        exclude("com/hypixel/hytale/**/*.class")
        exclude("META-INF/MANIFEST.MF")
    }

    manifest.from(zipTree(inputJarFile).matching {
        include("META-INF/MANIFEST.MF")
    }.singleFile)

    val compileJava = tasks.named<JavaCompile>("compileJava")
    from(compileJava.map { it.destinationDirectory }) {
        include("com/hypixel/hytale/**")
    }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    archiveFileName.set("HytaleServer-repackaged.jar")
}
