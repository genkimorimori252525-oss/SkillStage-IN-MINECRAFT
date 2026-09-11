# Photon 2 serialization contract used by P2

This document freezes only the upstream facts that the KNEEKURA Photon-2 adapter is allowed to depend on. It is not a claim that Photon internals are a stable public API.

## Exact upstream pins

- Photon repository: `Low-Drag-MC/Photon`
- Photon branch inspected: `1.21`
- Photon commit: `609a975cea104fd4e067a94f222a7dd1d619d263`
- Photon declared version: `2.2.6.a`
- Minecraft: `1.21.1`
- LDLib2 repository: `Low-Drag-MC/LDLib2`
- LDLib2 commit inspected: `d30e86b56b2a31982063ef907a335d36371b0bc4`

## Project file envelope

`FXProject.TYPE` uses suffix `.fxproj` and currently reports project version `5.0` (`FXProject.VERSION = 5`). LDLib2 `ProjectType.saveProjectToFile` uses `NbtIo.write`, so `.fxproj` is the normal uncompressed NBT file form.

```text
root
├─ meta
│  ├─ version = "5.0"
│  ├─ version_num = 5
│  ├─ suffix = ".fxproj"
│  └─ name = "fx_project"
└─ data
   └─ fx
      └─ fxData
         ├─ fxObjects = list<compound>
         └─ timeline = compound        # omitted when empty
```

Sources: Photon `FXProject.java`, `FX.java`, `FXData.java`; LDLib2 `IProject.java`, `ProjectType.java`.

## Plain `.fx`

Photon editor export writes `FX.serializeNBT(...)`, stamps integer `version = FXProject.VERSION`, then uses `NbtIo.writeCompressed`. KNEEKURA therefore treats `.fx` and `.fxproj` as separate native artifact kinds.

## FX object dispatch

`FXData` serializes each object through `IFXObject.serializeWrapper()`. `IFXObject.CODEC` dispatches on the registered Photon FX-object type and stores the type-owned payload under `data`. `FXObjectType.codec()` stamps integer `version` into that payload and applies `fixData(...)` before decode. Mojang dispatch uses the wrapper discriminator key `type`.

```text
object-wrapper
├─ type = registry discriminator
└─ data = compound
   ├─ version = object-type version
   └─ ... persisted fields
```

Unknown object types, unknown fields, and newer object versions MUST stay native/opaque rather than being deleted.

Sources: Photon `IFXObject.java`, `FXObjectType.java`, `PhotonRegistries.java`; LDLib2 `LDLRegistry.java`, `PersistedParser.java`.

## Particle emitter boundary

`ParticleEmitter.TYPE` registers as `particle_emitter` in `photon:fx_object` and currently reports object version `2`. Its `config` is nested `ParticleConfig` data. P2 semantically extracts only when both project version and object version exactly match the pinned contract.

Initial extracted subset:

- object `name`
- `duration`
- `looping`
- `prewarm`
- `maxParticles`
- `parallelUpdate`
- `startDelay` when its NumberFunction type is `constant`
- `startLifetime` when constant
- `startSpeed` when constant

Curve/random/gradient/shape/renderer/physics/trail/sub-emitter/custom-GPU and all unknown fields remain native until individually mapped and tested.

Sources: Photon `ParticleEmitter.java`, `ParticleConfig.java`, `NumberFunction.java`, `Constant.java`.

## Timeline boundary

`FXData` omits `timeline` when empty. When present, `Timeline` stores `tracks` as `{type,data}` entries and `markers` as `{tick,name}` compounds. KNEEKURA P2 records timeline presence but does not semantically decode tracks yet.

Source: Photon `Timeline.java`.

## Project-level data fixing

Photon project version is currently 5. `PhotonFXProjectDataFixer` defines schemas 1 through 5 and migrations including material-to-renderer-materials, UV-animation tile migration, and model-location-to-model-source migration. KNEEKURA does not pretend to reimplement those migrations. Semantic extraction is restricted to exact known versions; other versions remain native/opaque.

## P2 non-negotiable rules

1. Never require Photon/NeoForge classes inside `kvfx-core` or `integration-api`.
2. Preserve the original native Photon artifact bytes exactly.
3. Structural parsing failure reduces semantic understanding to opaque; it does not discard the native artifact.
4. Unsupported objects/modules stay opaque and are reported as such.
5. Re-export with no semantic mutation returns the exact original bytes.
6. Semantic mutation is allowed only after a field has an explicit tested write mapping.
7. Project/object version mismatches disable semantic extraction rather than guessing.
8. NBT parsing is bounded by native/decompressed byte, depth, and collection limits.