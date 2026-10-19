# Luxium 2.8.0-pre-alpha

> Unofficial decompilation of **Luxium: Let there be light**, originally created by **Vinlanx**.
> Original project: https://www.curseforge.com/minecraft/mc-mods/luxium
> All original code, shaders, assets, and branding are Copyright © Vinlanx and are distributed under the Luxium Custom License included in `src/main/resources/LICENSE.txt`. This repository is not affiliated with or endorsed by the original author. See `NOTICE.md`.

This is the `2.8.0-pre-alpha` branch. Each version of the mod is decompiled on its own branch; the [`main`](https://github.com/Bernard2806/Luxium-decompiled/tree/main) branch describes the repository layout and lists every version.

## Takedown and contact

This is an unofficial decompilation. If you are the original author or a rights holder and want this repository or its release artifacts (including the built JAR) removed, email **berramuspegonzalez@gmail.com** and the request will be honored promptly.

- Mod: `Luxium: Let there be light` (`luxium`), version `2.8.0-pre-alpha`.
- Input artifact: `Luxium Let there be light-2.8.0-pre-alpha.jar`.
- Decompiler: CFR 0.152.
- Loader metadata: Forge / JavaFML, requiring Forge 47.1.3 or newer. This is consistent with Forge for Minecraft 1.20.1; the JAR metadata itself does not name the Minecraft version explicitly.

## Contents

- `src/main/java/`: Java reconstructed from the JAR's `.class` files.
- `src/main/resources/`: non-class resources extracted from the JAR, including mod metadata, mixin configuration, shaders, and assets.

This is a decompilation, not the original source tree. CFR reports one class/method it could not fully decompile: `com.vinlanx.luxium.rtx.TorchRtxState.drainFrontierChecks(int)`; see `src/main/java/summary.txt`. That method was reconstructed with Vineflower 1.10.1 and compiles to the same bytecode as the original.

The sources were then corrected until a Gradle build reproduces every `.class` file of the original JAR byte for byte (395 of 395), including local variable tables and line numbers. Statements therefore sit on the line numbers of the original source, which leaves some blank lines and a few compact lines. Resources keep the original line endings (see `.gitattributes`). Only packaging differs: the manifest and `mods.toml` carry different build metadata and credits, and the generated mixin refmap lists the same entries in a different order.

## Dependencies and requirements

- Minecraft 1.20.1.
- Forge 47.1.3 or newer. The original project page reports it works with Forge 47.4.10 (and may also work with 47.4.20).
- Embeddium `0.3.31+mc1.20.1`, mandatory. Embeddium 0.4.x is not compatible.
- Java 17.
- Not compatible with Oculus/OptiFine. For low-memory crashes the original author recommends adding ImmediatelyFast and ModernFix.

## Build

- Java 17 JDK. Use 17.0.13 or older to reproduce the original bytecode: javac 17.0.14 changed how string concatenation is compiled (JDK-8273914).
- Gradle wrapper is included; it downloads Gradle 8.1.1 on first use.

Run `./gradlew build` to compile and package the mod. The build remaps the decompiled SRG member names to Minecraft's official mappings and targets Minecraft 1.20.1, Forge 47.4.10, and Embeddium 0.3.31.

## Releases

This version has exactly one release tag, `v2.8.0-pre-alpha`, on this branch. Pushing it triggers the GitHub Actions release workflow, which builds the mod using the tag as the version, measures how closely the build matches the original JAR, creates a GitHub Release, and attaches the JAR and the decompilation map. Tags containing a hyphen are marked as prereleases. The workflow ignores every other tag, since other versions live on other branches. To publish a fix, move the tag to the new commit (`git tag -f`) and push it again.

## License

The original code, shaders, and assets are Copyright © Vinlanx under the Luxium Custom License (`src/main/resources/LICENSE.txt`). `scripts/decompilation_map.py` is GPL-2.0-only; see `NOTICE.md`.
