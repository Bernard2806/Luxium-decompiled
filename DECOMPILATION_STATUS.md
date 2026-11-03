# Estado de la decompilación 2.8.4-pre-alpha

> Archivo de memoria del trabajo en curso en esta rama. Se actualiza en cada avance.
> Última actualización: 2026-11-03.

## Porcentaje de coincidencia con el JAR original

Medido comparando el JAR fusionado que genera `./gradlew mergeJars` contra
`Luxium Let there be light-2.8.4-pre-alpha forge-fabric.jar`
(sha256 `e2edabf0ec2cf9b5a88242c08123c68753ff122db4b97d07e75877b37d5d89d3`).

| Copia | Clases idénticas byte a byte | Métodos idénticos (código + números de línea) |
|---|---|---|
| `forge/` | 336 / 421 (79,81 %) | 3633 / 3843 (94,54 %) |
| `fabric/` | 338 / 425 (79,53 %) | 3625 / 3845 (94,28 %) |
| **Total** | **674 / 846 (79,67 %)** | **7258 / 7688 (94,41 %)** |

- Archivos fuente con todas sus clases idénticas: 148 / 227.
- Las 1086 entradas del JAR original están presentes, con los mismos nombres.
- 57 recursos difieren solo por el empaquetado (ver abajo).

## Cómo está armado el original

- Es una fusión **Forgix 1.2.9** de dos JAR:
  - `forge/com/vinlanx/...`: build de **ForgeGradle 6**, nombres SRG. 256 clases son idénticas byte a byte a 2.8.0.
  - `fabric/com/vinlanx/...`: build de **Fabric Loom 1.1.14**, nombres intermediary, con tiny-remapper 0.8.5.
- 395 clases salen del mismo código fuente en las dos copias: mismo bytecode y mismas líneas una vez traducidos los nombres. Ese código vive en `common/`.
- Solo Forge: `forge/*`, mixins `Embeddium*` y `Forge*`. Solo Fabric: `fabric/*`, mixins `Sodium*` y `Fabric*`, y `SpriteContentsAccessor`.
- Mismo nombre pero distinto código en cada loader (hay una copia en `forge/` y otra en `fabric/`): `ModCommands`, `RainPuddleCommands`, `DynamicShadowMeshCapture`, `LightTextureMixin`, `ItemBlockRenderTypesMixin`.
- El JAR publicado se reempaquetó **después** de Forgix: entradas con fecha fija 1980-02-01, otro orden, y assets sin la reescritura de texto que hace Forgix (Forgix pasa CRLF a LF y agrega un salto de línea final). Por eso hoy difieren 57 recursos, el manifest y los refmaps.

## Estructura del proyecto (MultiLoader, nombres Mojang)

- `common/src/main/java` y `common/src/main/resources`: se agregan como `srcDir` a los dos loaders.
- `forge/`: ForgeGradle `[6.0,6.2)`, Forge 47.4.10, Embeddium `maven.modrinth:sk9rgfiA:UTbfe5d1`, mixin AP 0.8.5.
- `fabric/`: Loom 1.1.14, `officialMojangMappings()`, fabric-loader 0.16.14, fabric-api 0.92.2+1.20.1, Sodium `mc1.20.1-0.5.13-fabric`, night-config 3.6.6 con `include`. El access widener en fuentes va en el namespace `named`.
- Raíz: Forgix 1.2.9. `mergeJars` depende de `:forge:build` y `:fabric:build`.
- Gradle 8.8 y **JDK 17.0.13** (javac 17.0.14 cambia la concatenación de strings).

```
JAVA_HOME=/ruta/a/jdk-17.0.13 ./gradlew mergeJars   # -> build/libs/luxium-2.8.4-pre-alpha.jar
```

## Cómo se obtuvieron las fuentes

1. Las clases idénticas a 2.8.0 usan las fuentes ya corregidas de la rama `2.8.0-pre-alpha`, pasadas a nombres Mojang con el `remapSrgSources` de esa rama.
2. El resto empezó como borrador de **Vineflower 1.10.1**, descompilado de clases traducidas a nombres Mojang.
3. Correcciones automáticas sobre los borradores:
   - **Trasplante:** si un método no cambió respecto a 2.8.0 (mismo bytecode, incluidas sus lambdas), se usa el texto de 2.8.0 (~300 métodos).
   - **Early returns:** se deshace la inversión `if (c) { resto }` de Vineflower y se vuelve a `if (!(c)) { return; } resto` (o `continue`) (251 casos).
   - **Realineado:** cada sentencia se ubica en su número de línea original. Se compila una versión "explotada" del archivo (cada nodo del árbol en su propia línea), se cruzan las `LineNumberTable` con las del original, y se reordenan los miembros de clase según esas líneas.
4. Arreglos manuales: casts `(Object)this` en mixins, `remap=false` en `GlStateManagerMixin`, imports de clases protegidas de `RenderStateShard`, captura de lambda en `NeoShadowsStaticPipeline`, y se quitó `@Mod.EventBusSubscriber` de `PostProcessStateGuard`.

Las herramientas (Python + Java) están fuera del repo, en `~/tools/luxium-decomp-tools/` de la máquina de trabajo: `verify.py`, `align.py`, `transplant.py`, `flips.py`/`FlipIf.java`, `mappings.py`, `classfile.py`, `mdiff.py`, `todo.py`.

## Qué falta

- **Código distinto:** ~145 métodos por loader (variables declaradas en otro orden u otros slots, `return`/`goto` reestructurados, casos `176`↔`167`, etc.).
- **Líneas distintas:** ~70 métodos por loader, con conflictos de orden que el realineador no resuelve.
- **LocalVariableTable** (nombres como `var32`, orden de las variables): 14 clases por loader tienen todos los métodos exactos pero difieren solo en esto o en el orden del constant pool.
- **Empaquetado:** reproducir el reempaquetado final (fechas 1980, orden de entradas, assets sin reescribir).
- **Rama:** README, workflow de release, `scripts/` e índices de `.github/release/` (como en 2.8.0).

### Archivos fuente pendientes

Métodos que aún difieren (sumando las dos copias) y clases afectadas:

| Archivo (`com/vinlanx/luxium/...`) | Métodos | Clases |
|---|---|---|
| client/NeoShadowsStaticPipeline | 36 | 2 |
| client/ReflectionSystem | 26 | 10 |
| rtx/soasnottointerfere/ExposedFaceService | 24 | 4 |
| client/DynamicShadowMeshCapture | 22 | 6 |
| client/guiscreen/VideoSettingsHubScreen | 22 | 4 |
| client/ConfigScreen/LuxiumConfigScreen | 22 | 2 |
| client/neocpu/NeoCpuShadowShapeBaker | 20 | 4 |
| client/clouds/LuxiumCloudRenderer | 14 | 2 |
| Testing/Grass | 12 | 4 |
| client/NeoShadowsEngine | 12 | 2 |
| mixin/GameRendererMixin | 10 | 2 |
| client/reflections/RainPuddleManager | 10 | 2 |
| client/guiscreen/AlphaWarningScreen | 10 | 2 |
| mixin/LightTextureMixin | 9 | 2 |
| rtx/TorchRtxState | 8 | 2 |
| rtx/neogpuvanilla/NeoGpuVanilla | 8 | 2 |
| client/shadows/neoskycelestia/NeoSkyCelestia | 8 | 2 |
| client/shadows/GpuShadowCache | 8 | 2 |
| client/guiscreen/IncompatibleModScreen | 8 | 2 |
| platform/config/ConfigSpec | 6 | 8 |
| rtx/VanillaLavaLightEngine | 6 | 2 |
| platform/ClientHooks | 6 | 2 |
| client/tfrpluslsr/TemporalFrameSystem | 6 | 2 |
| client/posteffects/PostEffectPipeline | 6 | 2 |
| client/posteffects/Hdr | 6 | 2 |
| client/shadows/GpuBakedHardShadowVolume | 6 | 1 |
| client/shadows/embeddium/EmbeddiumLocalShadowBridge | 4 | 6 |
| rtx/neogpuvanilla/NeoGpuVanillaVolume | 4 | 2 |
| mixin/sky/ShaderInstanceSkyShadowMixin | 4 | 2 |
| client/ssr/ScreenSpaceReflectionSystem | 4 | 2 |
| client/shadows/neoskycelestia/lightmap/NeoSkyCelestiaLightLut | 4 | 2 |
| client/posteffects/skyvolumetrigodrays | 4 | 2 |
| client/NeoShadowsDynamicPipeline | 4 | 2 |
| client/neocpu/NeoCpuEntityShadows | 4 | 2 |
| mixin/sky/SodiumChunkShaderInterfaceMixin | 4 | 1 |
| mixin/sky/EmbeddiumChunkShaderInterfaceMixin | 3 | 1 |
| forge/ForgePlatform | 3 | 1 |
| Testing/ExplosionTesting/ExplosionRenderer | 2 | 2 |
| rtx/neogpuvanilla/NeoGpuVanillaGpuDebug | 2 | 2 |
| mixin/LevelRendererMixin | 2 | 2 |
| luxiumMod | 2 | 2 |
| Config | 2 | 2 |
| client/wet/WetSurfaceRenderer | 2 | 2 |
| client/water/WaterSurfaceState | 2 | 2 |
| client/tfrpluslsr/LsrSystem | 2 | 2 |
| client/shadows/neoskycelestia/NeoSkyEntityShadowMap | 2 | 2 |
| client/shadows/neoskycelestia/NeoSkyCelestiaLighting | 2 | 2 |
| client/shadows/GpuNeoShadows | 2 | 2 |
| client/rainpuddles/RainPuddleRenderer | 2 | 2 |
| client/postprocess/PostProcessSwapChain | 2 | 2 |
| client/posteffects/skygodrays | 2 | 2 |
| client/NeoShadowsRuntime | 2 | 2 |
| client/NeoShadowsRenderPipeline | 2 | 2 |
| client/neocpu/NeoCpuShadowsEngine | 2 | 2 |
| client/kawase/KawaseBloomRenderer | 2 | 2 |
| client/GodRaySystem | 2 | 2 |
| client/f3/LuxiumF3Info | 2 | 2 |
| client/f3/LuxiumDebugHud | 2 | 2 |
| client/ConfigScreen/ResolutionControls | 2 | 2 |
| client/BlockLightTest/BlockLightTest | 2 | 2 |
| fabric/FabricPlatform | 2 | 1 |
| Testing/ModCommands | 1 | 2 |
| client/rainpuddles/RainPuddleCommands | 1 | 2 |
| mixin/SodiumFluidRendererWaterMixin | 1 | 1 |
| mixin/sky/SodiumNeoGpuCutoutPrepassMixin | 1 | 1 |
| mixin/sky/SodiumCloudShadowCasterMixin | 1 | 1 |
| mixin/sky/EmbeddiumShaderLoaderMixin | 1 | 1 |
| mixin/EmbeddiumFluidRendererWaterMixin | 1 | 1 |
| forge/ForgeLuxium | 1 | 1 |
| forge/ForgeClientEvents | 1 | 1 |
| Testing/TestFlashLight | 0 | 2 |
| Testing/ExplosionTesting/ExplosionShader | 0 | 2 |
| client/ShaderManager | 0 | 2 |
| client/posteffects/fog | 0 | 2 |
| client/kawase/KawaseSourceRegistry | 0 | 2 |
| mixin/sky/SodiumShaderLoaderMixin | 0 | 1 |
| mixin/ModelBlockRendererMixin | 0 | 1 |
| mixin/EmbeddiumRenderSectionAccessor | 0 | 1 |
| fabric/FabricConfig | 0 | 1 |
