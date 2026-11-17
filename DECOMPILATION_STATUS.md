# 2.8.4-pre-alpha decompilation status

> Working notes for this branch, updated with every step.
> Last updated: 2026-11-17.

## Match against the original JAR

Measured by comparing the merged JAR built by `./gradlew mergeJars` with
`Luxium Let there be light-2.8.4-pre-alpha forge-fabric.jar`
(sha256 `e2edabf0ec2cf9b5a88242c08123c68753ff122db4b97d07e75877b37d5d89d3`).

| Copy | Byte-identical classes | Identical methods (code + line numbers) |
|---|---|---|
| `forge/` | 402 / 421 (95.49 %) | 3843 / 3843 (100 %) |
| `fabric/` | 409 / 425 (96.24 %) | 3845 / 3845 (100 %) |
| **Total** | **811 / 846 (95.86 %)** | **7688 / 7688 (100 %)** |

- Every method now has the original bytecode and line numbers in both copies.
- Source files whose classes are all identical: 217 / 234.
- The other 35 classes differ only outside the code: local variable order in the LocalVariableTable (28 classes), constant pool order (32) and class attributes such as `InnerClasses`/`NestMembers` order (5).
- All 1086 entries of the original JAR are present, with the same names.
- 57 resources differ only because of the packaging (see below).

2026-11-16: local variables renamed to their original names, redundant `else` blocks removed where the original LVT order shows it (greedy search, `lvtopt.py`), field order fixed in `Grass` and `NeoShadowsEngine`, and `ModelBlockRendererMixin` split per loader (the fabric copy targets the vanilla `tesselateWithAO`/`tesselateWithoutAO` signatures). Still left: LVT order in 17 methods per copy, constant pool/InnerClasses order in a few classes, and the final repackaging (Forgix rewrites CRLF text resources and drops the 1980 timestamps; the sources already hold the original bytes).

Latest (2026-11-17): more redundant `else` blocks after `return`/`continue` removed where the original LVT order shows the variables were declared at the outer level (+23 classes).

History: 2026-11-03 → 7258 methods (94.41 %), 674 classes. 2026-11-16 → 7628 methods (99.22 %), 742 classes. 2026-11-16 (later) → 7688 methods (100 %), 750 classes, then 788. 2026-11-17 → 811 classes.

## How the original was built

- It is a **Forgix 1.2.9** merge of two JARs:
  - `forge/com/vinlanx/...`: a **ForgeGradle 6** build with SRG names. 256 classes are byte-identical to 2.8.0.
  - `fabric/com/vinlanx/...`: a **Fabric Loom 1.1.14** build with intermediary names, remapped with tiny-remapper 0.8.5.
- 395 classes come from the same source in both copies: same bytecode and same line numbers once the names are translated. That code lives in `common/`.
- Forge only: `forge/*`, the `Embeddium*` and `Forge*` mixins. Fabric only: `fabric/*`, the `Sodium*` and `Fabric*` mixins, and `SpriteContentsAccessor`.
- Same name but different code in each loader (one copy in `forge/` and one in `fabric/`): `ModCommands`, `RainPuddleCommands`, `DynamicShadowMeshCapture`, `LightTextureMixin`, `ItemBlockRenderTypesMixin`.
- The published JAR was repackaged **after** Forgix: entries with a fixed 1980-02-01 timestamp, a different order, and assets without Forgix's text rewriting (Forgix turns CRLF into LF and adds a trailing newline). That is why 57 resources, the manifest and the refmaps still differ.

## Project layout (MultiLoader, Mojang names)

- `common/src/main/java` and `common/src/main/resources`: added as a `srcDir` to both loaders.
- `forge/`: ForgeGradle `[6.0,6.2)`, Forge 47.4.10, Embeddium `maven.modrinth:sk9rgfiA:UTbfe5d1`, mixin AP 0.8.5.
- `fabric/`: Loom 1.1.14, `officialMojangMappings()`, fabric-loader 0.16.14, fabric-api 0.92.2+1.20.1, Sodium `mc1.20.1-0.5.13-fabric`, night-config 3.6.6 with `include`. The access widener in the sources uses the `named` namespace.
- Root: Forgix 1.2.9. `mergeJars` depends on `:forge:build` and `:fabric:build`.
- Gradle 8.8 and **JDK 17.0.13** (javac 17.0.14 changes string concatenation).

```
JAVA_HOME=/path/to/jdk-17.0.13 ./gradlew mergeJars   # -> build/libs/luxium-2.8.4-pre-alpha.jar
```

## How the sources were produced

1. Classes identical to 2.8.0 reuse the already corrected sources of the `2.8.0-pre-alpha` branch, translated to Mojang names with that branch's `remapSrgSources`.
2. The rest started as a **Vineflower 1.10.1** draft, decompiled from classes translated to Mojang names.
3. Automatic fixes on the drafts:
   - **Transplant:** when a method did not change since 2.8.0 (same bytecode, lambdas included), the 2.8.0 text is used (~300 methods).
   - **Early returns:** Vineflower's `if (c) { rest }` inversion is undone back to `if (!(c)) { return; } rest` (or `continue`) (251 cases).
   - **Realignment:** every statement is placed on its original line. An "exploded" copy of the file (each tree node on its own line) is compiled, its `LineNumberTable`s are matched with the original ones, and class members are reordered by those lines.
4. Fixes from 2026-11-16 (all checked against the original bytecode):
   - **`align.py` also translates method names** (SRG/intermediary → Mojang) when matching with the original. Before, it did not recognise overridden methods (`init`, `tick`, `render`, `vertex`...) and left them unaligned.
   - **Headers:** imports are joined on a single line so the class declaration, the implicit constructor and field initialisers land on their original lines.
   - **`static` blocks split** into several blocks interleaved with methods and nested classes (`Grass`), and initialisations moved back into the constructor as in the original (`NeoShadowsEngine`).
   - **Loops:** Vineflower `for` loops turned back into `while` (javac adds a line entry at a `while`'s closing brace) and the other way round; loop limits moved from the `for` header into an `if (...) break;`.
   - **Early exits:** `else` → `continue;`/`return;`, `return c ? a : b` → `if (c) return a; return b;`, inverted `if/else` with `return` (`if (!(c)) { exit } rest`), `!(a && b)` conditions split into several `if (...) continue;`.
   - **Other:** inverted ternaries, `x = x + y` → `x += y`, local variables Vineflower had removed (`h = ...; return h;`), pattern-matching `instanceof`, `case` order in a `switch`, method references (`this::addRenderableWidget`) instead of lambdas.
5. Manual fixes: `(Object)this` casts in mixins, `remap=false` in `GlStateManagerMixin`, imports of protected `RenderStateShard` classes, lambda capture in `NeoShadowsStaticPipeline`, and `@Mod.EventBusSubscriber` removed from `PostProcessStateGuard`.

The tools (Python + Java) live outside the repo, in `~/tools/luxium-decomp-tools/` on the working machine: `verify.py`, `align.py`, `transplant.py`, `flips.py`/`FlipIf.java`, `mappings.py`, `classfile.py`, `mdiff.py`, `todo.py`. The ones from the 2026-11-16 session are in `session-2026-11-16/` (`flipret.py`, `tern.py`, `unelse.py`, `tocont.py`, `splitand.py`, `retvar.py`, `ctx.py`, `hk.py`, and `audit.py`/`audit2.py` to check that no inversion changed the semantics, among others).

## What is left

- **LocalVariableTable:** local variable names and declaration order. The original names are in the original LVT (it was compiled with `-g`), so most of them can be recovered automatically by matching slot and start pc. Fixing names should also fix most constant pool differences, since names are constant pool entries.
- **Constant pool / class attributes:** order of constant pool entries, `InnerClasses`/`NestMembers`, and field order in `Grass` and `NeoShadowsEngine`.
- **Packaging:** reproduce the final repackaging (1980 timestamps, entry order, assets not rewritten).
- **Branch:** README, release workflow, `scripts/` and `.github/release/` indexes (as in 2.8.0).

### Last round of method fixes (2026-11-16)

The last 60 methods were fixed by restoring what Vineflower had changed: `try`/`finally` blocks that returned from inside (Vineflower stores the result in a `varN` temp and duplicates the `finally` body with a "semaphore" flag), try-with-resources returning from inside the block, `for` loops whose step javac placed after the body (they were `while` loops ending in `x++`) and vice versa (lambda capture order revealed `for` loops), pattern-matching `instanceof` in plain `else if` chains (slot allocation), constants such as `font.lineHeight` that Vineflower folded, locals it had inlined (`useEffectsTarget`, `waterPass`, `validFromStart`, `trackY`/`trackHeight`, `ov_left`, `files`, `planeGroups`), compound assignments (`|=`, `+=`), and inverted `if`/`else` and ternaries.

