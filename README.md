# Luxium-decompiled

> Unofficial decompilation of **Luxium: Let there be light**, originally created by **Vinlanx**.
> Original project: https://www.curseforge.com/minecraft/mc-mods/luxium
> All original code, shaders, assets, and branding are Copyright © Vinlanx and are distributed under the Luxium Custom License (`LICENSE.txt`). This repository is not affiliated with or endorsed by the original author. See `NOTICE.md`.

Luxium is a performance-focused graphics mod for Minecraft 1.20.1: dynamic shadows, god rays, reflections, clouds, post-processing, and more. This repository turns each published Luxium JAR back into Java sources and resources that build into the same mod.

This `main` branch holds no code. It describes how the repository is organized and lists every decompiled version. **To get the sources of a version, switch to its branch.**

## Versions

| Version | Branch | Loaders | Status | Release |
| --- | --- | --- | --- | --- |
| 2.8.4-pre-alpha | [`2.8.4-pre-alpha`](https://github.com/Bernard2806/Luxium-decompiled/tree/2.8.4-pre-alpha) | Forge + Fabric (one JAR) | Not started; the branch is empty. | — |
| 2.8.0-pre-alpha | [`2.8.0-pre-alpha`](https://github.com/Bernard2806/Luxium-decompiled/tree/2.8.0-pre-alpha) | Forge | Complete: the build reproduces all 395 `.class` files of the original JAR byte for byte. | [`v2.8.0-pre-alpha`](https://github.com/Bernard2806/Luxium-decompiled/releases/tag/v2.8.0-pre-alpha) |

### 2.8.4-pre-alpha

Input artifact: `Luxium Let there be light-2.8.4-pre-alpha forge-fabric.jar`. The original author's changelog:

- Port, one JAR for Fabric and Forge.
- Fixed shadows on entities.
- Added a button to toggle shadows on entities.
- Added HDR and a button to enable HDR.
- Changed dependencies: Forge uses Embeddium, Fabric uses Sodium.
- Added a fullscreen resolution option to the Fabric version, since Sodium for 1.20.1 does not have this slider.
- Detector and blocking screen for Iris and Oculus.
- Fixed/improved the cloud rendering order.

### 2.8.0-pre-alpha

Input artifact: `Luxium Let there be light-2.8.0-pre-alpha.jar`. Forge only, for Minecraft 1.20.1 with Forge 47.1.3 or newer and Embeddium `0.3.31+mc1.20.1`. Decompiled with CFR 0.152; the one method CFR could not decompile was reconstructed with Vineflower 1.10.1. See the branch README for details.

## Repository architecture

### One branch per version

Every Luxium version is an independent branch named after the version, without a `v` prefix. Version branches do not share history: each one starts from its own JAR, so a version can be decompiled, corrected, and released without touching the others.

```
main               documentation only (this README, NOTICE.md, LICENSE.txt)
2.8.0-pre-alpha    sources of Luxium Let there be light-2.8.0-pre-alpha.jar
2.8.4-pre-alpha    sources of Luxium Let there be light-2.8.4-pre-alpha forge-fabric.jar
```

Each version has exactly one release tag, named `v` + version (for example, `v2.8.0-pre-alpha`), on its branch. There are no extra tags for fixes or rebuilds: to publish a fix, the version's tag is moved to the new commit and pushed again, which rebuilds that version's release. Branches and tags never share a name, so git never has to guess which one a name refers to.

### Layout of a version branch

```
.
├── src/main/java/                     decompiled Java sources (package com.vinlanx.luxium)
├── src/main/resources/                everything in the JAR that is not a .class file:
│   ├── META-INF/mods.toml             loader metadata
│   ├── luxium.mixins.json             mixin configuration
│   ├── assets/luxium/                 shaders, textures, images, and language files
│   ├── assets/minecraft/              overrides of vanilla shaders
│   └── LICENSE.txt                    the Luxium Custom License shipped in the JAR
├── build.gradle, settings.gradle      Gradle build (ForgeGradle 6, Mixin)
├── gradlew, gradlew.bat, gradle/      Gradle wrapper
├── scripts/
│   ├── calculate_decompilation_match.py   compares the built JAR with the original, method by method
│   └── decompilation_map.py               draws the decompilation map (GPL-2.0-only)
├── .github/
│   ├── workflows/release.yml          release workflow
│   └── release/                       class and method index of the original JAR
├── .gitattributes                     line endings that reproduce the original resources
├── NOTICE.md
└── README.md                          version-specific details
```

The original JAR is never committed. Its contents are kept only as `.github/release/original-class-index.txt` (the list of classes) and `.github/release/original-method-index.txt` (a normalized bytecode hash of every method), which is enough to measure how closely a build matches it.

The layout above is the one of `2.8.0-pre-alpha`. Later versions keep it where they can, but a Forge + Fabric JAR may need a different build (for example, a multi-loader project). Each branch's README is the reference for that version.

### Build

Each version branch builds with its own Gradle wrapper:

```sh
git switch 2.8.0-pre-alpha
./gradlew build
```

The decompiled sources use SRG member names; the build remaps them to Minecraft's official mappings before compiling. The JAR is written to `build/libs/`. Java and loader requirements are listed in each branch's README; `2.8.0-pre-alpha` needs Java 17 (17.0.13 or older to reproduce the original bytecode).

### Releases

Pushing a version's tag runs that branch's `.github/workflows/release.yml`, which triggers on that one tag only:

1. Builds the mod with the tag as its version.
2. Runs `scripts/calculate_decompilation_match.py`, which reports the share of classes with complete sources and the share of methods whose bytecode is identical to the original, and draws the decompilation map.
3. Creates a GitHub Release with the JAR and the map attached. Tags containing a hyphen are marked as prereleases.

## License

This repository contains no original work of its own beyond documentation and build tooling, and it does not relicense anything:

- **Luxium code, shaders, assets, and branding:** Copyright © Vinlanx, under the Luxium Custom License. A copy is in `LICENSE.txt`, and each version branch ships the license from its JAR in `src/main/resources/LICENSE.txt`. Note that the license does not allow redistributing Luxium or derivative versions without written permission from Vinlanx; see the takedown section below.
- **`scripts/decompilation_map.py`:** derived from [AnyPS5](https://github.com/boykopovar/AnyPS5), GPL-2.0-only. It applies to that file only, a build tool that is not part of the mod JAR.

See `NOTICE.md` for the full notice.

## Takedown and contact

This is an unofficial decompilation. If you are the original author or a rights holder and want this repository or its release artifacts (including the built JARs) removed, email **berramuspegonzalez@gmail.com** and the request will be honored promptly.
