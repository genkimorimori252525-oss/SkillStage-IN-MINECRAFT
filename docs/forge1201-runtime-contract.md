# Forge 1.20.1 / Photon 1.1.17 runtime contract used by P3

P3 targets a private Forge 1.20.1 playback environment while keeping the compiler and runtime-plan model independent of Minecraft classes. This document records the exact upstream behavior the later runtime adapter may rely on.

## Exact upstream pin

- Repository: `Low-Drag-MC/Photon`
- Branch inspected: `1.20.1`
- Commit: `507499fbe5a3fc72bb90993e059848cbb31f13ca`
- Declared Photon version: `1.1.17`
- Minecraft: `1.20.1`
- Forge: `47.1.3`
- LDLib: `1.0.42`

The 1.20.1 build is Architectury-based with Forge and Fabric subprojects. P3 does not copy these sources and does not make the loader-neutral runtime-plan module depend on Photon, LDLib, Minecraft, Architectury, or Forge.

## Photon 1 exported FX format

Photon 1's editor project version is `1`. Exported `.fx` is gzip-compressed NBT written by `NbtIo.writeCompressed`:

```text
root
├─ _version = 1
└─ fx
   ├─ mainFX
   │  └─ fxObjects = list<compound>
   └─ subFXs = compound<string, FXData>
```

The editor `.fxproj` itself is a different uncompressed NBT document containing `resources`, `fx`, and `_version`.

Runtime resource loading is performed by `FXHelper.getFX(ResourceLocation)`. A logical id `namespace:path` resolves to:

```text
assets/<namespace>/fx/<path>.fx
```

`FXHelper` reads the compressed NBT, reads `_version`, creates `FX`, and deserializes the `fx` compound. Older pre-v1 emitter layouts have a compatibility path.

Sources at the exact pin:

- `common/src/main/java/com/lowdragmc/photon/gui/editor/FXProject.java`
- `common/src/main/java/com/lowdragmc/photon/client/fx/FXHelper.java`
- `common/src/main/java/com/lowdragmc/photon/client/fx/FX.java`
- `common/src/main/java/com/lowdragmc/photon/client/fx/FXData.java`

## Object dispatch is different from Photon 2

Photon 1 `FXData` stores each object as the object's own serialized compound. `IFXObject.deserializeWrapper` dispatches by the `_type` string through `PhotonLDLibPlugin.REGISTER_FX_OBJECTS`.

Photon 1's particle emitter registration is:

```text
_type = "particle"
```

and `ParticleEmitter.VERSION = 2`, stored as `_version` on the particle object.

This differs materially from Photon 2, where the P2 adapter observes a codec wrapper using the `particle_emitter` registry discriminator plus a nested `data` payload/version. Therefore a Photon-2 object wrapper must never be copied directly into a Photon-1 `.fx`.

Sources:

- `common/src/main/java/com/lowdragmc/photon/client/gameobject/IFXObject.java`
- `common/src/main/java/com/lowdragmc/photon/client/gameobject/emitter/particle/ParticleEmitter.java`
- `common/src/main/java/com/lowdragmc/photon/integration/PhotonLDLibPlugin.java`

## Runtime entry path

Photon 1 `FX` owns `mainFX` plus named `subFXs`. `FX.createRuntime()` creates an `FXRuntime`, which copies or reuses the FX objects, builds the scene hierarchy, then `FXRuntime.emmit(IEffect)` resets/emits the objects.

A concrete effect such as `BlockEffect.start()` performs the high-level sequence:

```text
FX
→ createRuntime()
→ set root transform
→ runtime.emmit(effect)
→ IFXObject.emmit(...)
→ Minecraft particle engine / DummyWorld particle manager
```

The later Minecraft runtime adapter may use this public behavior where suitable, but P3's compiler only produces a loader-neutral execution package. Actual game attachment is a separate gate.

Sources:

- `common/src/main/java/com/lowdragmc/photon/client/fx/FXRuntime.java`
- `common/src/main/java/com/lowdragmc/photon/client/fx/BlockEffect.java`
- `common/src/main/java/com/lowdragmc/photon/client/gameobject/IFXObject.java`

## Bounded particle-field compatibility

Fields already extracted by P2 and present in Photon 1.1.17 `ParticleConfig`:

| Semantic field | Photon 1.1.17 | P3 policy |
| --- | --- | --- |
| `duration` | yes | representable |
| `looping` | yes | representable |
| `startDelay` constant | yes | representable, integral/non-negative |
| `startLifetime` constant | yes | representable, integral/non-negative |
| `startSpeed` constant | yes | representable, finite |
| `maxParticles` | yes | representable, 0..100000 |
| `parallelUpdate` | yes | representable |
| `prewarm` | **no** | only `0` is accepted; non-zero blocks P3 |

Photon 1 also has `parallelRendering`, which Photon 2's current bounded semantic slice does not map. P3 does not invent a value as a fidelity claim; the runtime package records that unmapped modules use an explicit target-default policy.

Photon 1 simulation space is only `Local` / `World`; Photon 2 additionally has `Custom`. P2 does not yet extract simulation space, so P3 cannot claim support for a non-default/custom source space.

Source: `common/src/main/java/com/lowdragmc/photon/client/gameobject/emitter/particle/ParticleConfig.java`.

## Major unsupported Photon 2 semantics at this gate

Photon 1.1.17 has no Photon-2 Timeline system. A source with `timelinePresent=true` is therefore blocked by P3 rather than flattened silently.

P2 has not yet semantically mapped emission, shape, material/renderer, start size/rotation/color, physics, lifetime modules, noise, UV, trails, sub-emitters, GPU custom data, meshes, shaders, post FX, or audio. P3 may produce a runnable **emulated/defaulted** first-particle plan only when there are no opaque whole FX objects; it must report the unmapped module/default policy and must not label the result fully supported/equivalent.

## Target compiler rules

1. Whole unknown/opaque FX objects block compilation.
2. Timeline presence blocks compilation.
3. Non-zero `prewarm` blocks compilation for the Photon-1 target.
4. Required scalar values that are unresolved/null block compilation instead of guessing whether they came from a curve/random function.
5. Accepted values are range/finite validated.
6. Successful P3 output is initially `EMULATED`, not `SUPPORTED`, because many particle modules are deliberately not mapped yet.
7. Compiler output is a loader-neutral target package. It is not yet a Photon-1 `.fx` and is not claimed to have been rendered in Minecraft.
8. The subsequent runtime-mod gate is responsible for turning the target package into actual client-side playback and proving visual behavior in Forge 1.20.1.