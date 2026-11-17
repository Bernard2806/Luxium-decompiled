# Luxium 2.8.4-pre-alpha

> Unofficial decompilation of **Luxium: Let there be light**, originally created by **Vinlanx**.
> Original project: https://www.curseforge.com/minecraft/mc-mods/luxium
> All original code, shaders, assets, and branding are Copyright © Vinlanx and are distributed under the Luxium Custom License included in `common/src/main/resources/LICENSE.txt`. This repository is not affiliated with or endorsed by the original author. See `NOTICE.md`.

This is the `2.8.4-pre-alpha` branch. Each version of the mod is decompiled on its own branch; the [`main`](https://github.com/Bernard2806/Luxium-decompiled/tree/main) branch describes the repository layout and lists every version.

## Takedown and contact

This is an unofficial decompilation. If you are the original author or a rights holder and want this repository or its release artifacts (including the built JAR) removed, email **berramuspegonzalez@gmail.com** and the request will be honored promptly.

- Mod: `Luxium: Let there be light` (`luxium`), version `2.8.4-pre-alpha`.
- Input artifact: `Luxium Let there be light-2.8.4-pre-alpha forge-fabric.jar` (sha256 `e2edabf0ec2cf9b5a88242c08123c68753ff122db4b97d07e75877b37d5d89d3`).
- One JAR for both loaders: a [Forgix](https://github.com/PacifistMC/Forgix) 1.2.9 merge of a ForgeGradle 6 build (classes under `forge/`) and a Fabric Loom 1.1.14 build (classes under `fabric/`).
- Decompiler: Vineflower 1.10.1. Classes unchanged since 2.8.0 reuse the corrected sources of the `2.8.0-pre-alpha` branch.

## Contents

The project is laid out as a multi-loader build. All sources use Minecraft's official (Mojang) names.

- `common/src/main/java/`: code shared by both loaders, compiled into both copies.
- `common/src/main/resources/`: shaders, textures, language files, `pack.mcmeta`, `logo.png`, and `LICENSE.txt`.
- `forge/`: the Forge entry point and platform code, the Embeddium and Forge mixins, `META-INF/mods.toml`, and the Forge mixin configuration.
- `fabric/`: the Fabric entry point and platform code, the Sodium and Fabric mixins, `fabric.mod.json`, the access widener, and the Fabric mixin configuration.
- Root `build.gradle`: merges both loader JARs with Forgix, as the original release was built.

This is a decompilation, not the original source tree. The sources were corrected until a Gradle build reproduces every `.class` file of the original JAR byte for byte: 846 of 846 (421 in `forge/`, 425 in `fabric/`), including local variable tables and line numbers. Statements therefore sit on the line numbers of the original source, which leaves some blank lines and long single lines. Resources keep the original line endings (see `.gitattributes`). `DECOMPILATION_STATUS.md` describes how the original was built and how the sources were produced.

Only the packaging differs, and it cannot be fully reproduced from a Linux build: the manifest carries the original build time, the nested night-config JARs carry NTFS timestamps from the author's machine, the Forge mixin refmap lists one mixin in a different position, and the published JAR was repackaged after Forgix (fixed timestamps, its own entry order, and 52 text resources with CRLF line endings that Forgix would rewrite).

## Dependencies and requirements

Minecraft 1.20.1 and Java 17, on either loader. The same JAR goes in the `mods` folder for both.

**Forge** (from `META-INF/mods.toml`):

- Forge 47.1.3 or newer.
- Embeddium `0.3.31+mc1.20.1` or a later 0.3.x, mandatory. Embeddium 0.4.x is not compatible.

**Fabric** (from `fabric.mod.json`, client only):

- Fabric Loader 0.16.14 or newer.
- Fabric API `0.92.2+1.20.1` or newer.
- Sodium `0.5.13` (`mc1.20.1-0.5.13`), mandatory. The version is pinned exactly; other Sodium versions are rejected by the loader.
- night-config 3.6.6 (`core` and `toml`) is bundled inside the JAR.

Not compatible with shader loaders: the mod shows a blocking screen when Oculus (Forge) or Iris (Fabric) is installed. OptiFine is not supported either.

## Build

- Java 17 JDK. Use **17.0.13** or older to reproduce the original bytecode: javac 17.0.14 changed how string concatenation is compiled (JDK-8273914).
- Gradle wrapper is included; it downloads Gradle 8.8 on first use.

```sh
./gradlew mergeJars
```

This builds both loaders and writes the merged mod to `build/libs/luxium-2.8.4-pre-alpha.jar`. The single-loader JARs are left in `forge/build/libs/` and `fabric/build/libs/`. The build compiles against:

- Forge: Forge 47.4.10 and Embeddium 0.3.31 (from the Modrinth Maven), with the Mixin 0.8.5 annotation processor.
- Fabric: Fabric Loader 0.16.14, Fabric API 0.92.2+1.20.1, Sodium 0.5.13 (from the Modrinth Maven), and night-config 3.6.6.

Versions are set in `gradle.properties`.

To measure a build against the original JAR:

```sh
python3 scripts/calculate_decompilation_match.py --built-jar 'build/libs/luxium-*.jar'
```

It reports the share of top-level classes with complete sources, the share of methods with identical bytecode, and the share of `.class` files that are byte-identical, for both the `forge/` and `fabric/` copies. The original JAR is not committed; its classes and methods are stored as lists and hashes in `.github/release/`.

## Releases

This version has exactly one release tag, `v2.8.4-pre-alpha`, on this branch. Pushing it triggers the GitHub Actions release workflow, which builds the merged mod using the tag as the version, measures how closely the build matches the original JAR, creates a GitHub Release, and attaches the JAR and the decompilation map. Tags containing a hyphen are marked as prereleases. The workflow ignores every other tag, since other versions live on other branches. To publish a fix, move the tag to the new commit (`git tag -f`) and push it again.

## License

The original code, shaders, and assets are Copyright © Vinlanx under the Luxium Custom License (`common/src/main/resources/LICENSE.txt`). `scripts/decompilation_map.py` is GPL-2.0-only; see `NOTICE.md`.
