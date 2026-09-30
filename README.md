# Luxium — decompiled sources

- Mod: `Luxium: Let there be light` (`luxium`), version `2.8.0-pre-alpha`.
- Input artifact: `Luxium Let there be light-2.8.0-pre-alpha.jar`.
- Decompiler: CFR 0.152.
- Loader metadata: Forge / JavaFML, requiring Forge 47.1.3 or newer. This is consistent with Forge for Minecraft 1.20.1; the JAR metadata itself does not name the Minecraft version explicitly.

## Contents

- `src/main/java/`: Java reconstructed from the JAR's `.class` files.
- `src/main/resources/`: non-class resources extracted from the JAR, including mod metadata, mixin configuration, shaders, and assets.

This is a decompilation, not the original source tree. Some Minecraft/Forge references remain in SRG-style names, so the result is useful to inspect but is not yet a ready-to-build ForgeGradle project. CFR reports one class/method it could not fully decompile: `com.vinlanx.luxium.rtx.TorchRtxState.drainFrontierChecks(int)`; see `src/main/java/summary.txt`.
