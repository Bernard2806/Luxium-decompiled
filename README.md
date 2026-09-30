# Luxium

- Mod: `Luxium: Let there be light` (`luxium`), version `2.8.0-pre-alpha`.
- Input artifact: `Luxium Let there be light-2.8.0-pre-alpha.jar`.
- Decompiler: CFR 0.152.
- Loader metadata: Forge / JavaFML, requiring Forge 47.1.3 or newer. This is consistent with Forge for Minecraft 1.20.1; the JAR metadata itself does not name the Minecraft version explicitly.

## Contents

- `src/main/java/`: Java reconstructed from the JAR's `.class` files.
- `src/main/resources/`: non-class resources extracted from the JAR, including mod metadata, mixin configuration, shaders, and assets.

This is a decompilation, not the original source tree. CFR reports one class/method it could not fully decompile: `com.vinlanx.luxium.rtx.TorchRtxState.drainFrontierChecks(int)`; see `src/main/java/summary.txt`.

## Build

- Java 17 JDK
- Gradle wrapper is included; it downloads Gradle 8.1.1 on first use.

Run `./gradlew build` to compile and package the mod. The build remaps the decompiled SRG member names to Minecraft's official mappings and targets Minecraft 1.20.1, Forge 47.2.0, and Embeddium 0.3.31.

## Releases

Push a tag matching `v*` (for example, `v2.8.1`) to trigger the GitHub Actions release workflow. It builds the mod using the tag as the version, creates a GitHub Release, and attaches the JAR. Tags containing a hyphen are marked as prereleases.
