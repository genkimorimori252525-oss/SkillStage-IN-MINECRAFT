# Photon 2 serialization contract used by P2

This document freezes only the upstream facts that the KNEEKURA Photon-2 adapter is allowed to depend on.
It is not a claim that Photon internals are a stable public API.

## Exact upstream pins

- Photon repository: `Low-Drag-MC/Photon`
- Photon branch inspected: `1.21`
- Photon commit: `609a975cea104fd4e067a94f222a7dd1d619d263`
- Photon declared version at that commit: `2.2.6.a`
- Minecraft: `1.21.1`
- LDLib2 repository: `Low-Drag-MC/LDLib2`
- LDLib2 commit inspected: `d30e86b56b2a31982063ef907a335d36371b0bc4`

## Project file envelope

`FXProject.TYPE` uses suffix `.fxproj` and currently reports project version `5.0` (`FXProject.VERSION = 5`).
LDLib2 `ProjectType.saveProjectToFile` writes the project with `NbtIo.write`, i.e. the normal uncompressed NBT file form used by Minecraft's NBT IO. The outer project compound is created by `IProject.serializeNBT`:

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

Relevant upstream source:

- `src/main/java/com/lowdragmc/photon/gui/editor/FXProject.java`
- `src/main/java/com/lowdragmc/photon/client/fx/FX.java`
- `src/main/java/com/lowdragmc/photon/client/fx/FXData.java`
- LDLib2 `src/main/java/com/lowdragmc/lowdraglib2/editor/project/IProject.java`
- LDLib2 `src/main/java/com/lowdragmc/lowdraglib2/editor/project/ProjectType.java`

## Plain `.fx`

Photon's editor export path writes `FX.serializeNBT(...)`, stamps integer `version = FXProject.VERSION`, then writes the result with `NbtIo.writeCompressed`. Therefore `.fx` and `.fxproj` are deliberately treated as two separate native artifact kinds by KNEEKURA.

## FX object dispatch

`FXData` serializes each object through `IFXObject.serializeWrapper()`. `IFXObject.CODEC` dispatches on the registered Photon FX-object type and stores the type-owned payload under `data`.
Each `FXObjectType.codec()` stamps an integer `version` into that type's payload and applies the type's `fixData(...)` before decode.

The P2 adapter therefore treats every object wrapper as:

```text
object-wrapper
├─ type = registry discriminator       # codec dispatch discriminator
└─ data = compound
   ├─ version = object-type version
   └─ ... type-owned persisted fields
```

Unknown object types, unknown fields, and newer object versions MUST be retained as native NBT and MUST NOT be silently discarded.

Relevant upstream source:

- `src/main/java/com/lowdragmc/photon/client/gameobject/IFXObject.java`
- `src/main/java/com/lowdragmc/photon/client/gameobject/FXObjectType.java`
- `src/main/java/com/lowdragmc/photon/PhotonRegistries.java`
- LDLib2 `src/main/java/com/lowdragmc/lowdraglib2/registry/LDLRegistry.java`
- LDLib2 `src/main/java/com/lowdragmc/lowdraglib2/utils/PersistedParser.java`

## Particle emitter boundary

`ParticleEmitter.TYPE` registers as `particle_emitter` in `photon:fx_object` and currently reports object version `2`.
Its persisted `config` is a nested `ParticleConfig`. `ParticleConfig` contains both scalar fields and nested modules such as emission, shape, renderer, physics, color/size/rotation over lifetime, trails, sub-emitters, and additional GPU data.

P2 intentionally extracts only a bounded scalar primitive subset first. Everything else remains native/opaque until a later capability slice proves a semantic mapping.

Relevant upstream source:

- `src/main/java/com/lowdragmc/photon/client/gameobject/emitter/particle/ParticleEmitter.java`
- `src/main/java/com/lowdragmc/photon/client/gameobject/emitter/particle/ParticleConfig.java`

## Timeline boundary

`FXData` omits `timeline` when empty. When present, `Timeline` stores `tracks` as `{type,data}` entries and `markers` as `{tick,name}` compounds. Unknown track types are skipped by Photon itself on load, so KNEEKURA must keep the original native bytes independently even when it cannot semantically decode a track.

Relevant upstream source:

- `src/main/java/com/lowdragmc/photon/client/fx/timeline/Timeline.java`

## Project-level data fixing

Photon project version is currently 5. `PhotonFXProjectDataFixer` defines schemas 1 through 5 and migrations including material-to-renderer-materials, UV-animation tile migration, and model-location-to-model-source migration.
KNEEKURA does not reimplement those migrations in P2. It records the source Photon project version and leaves migration authority with Photon. A future semantic reader may understand historical shapes, but native bytes remain authoritative evidence.

Relevant upstream source:

- `src/main/java/com/lowdragmc/photon/client/gameobject/emitter/data/fixer/PhotonFXProjectDataFixer.java`

## P2 non-negotiable rules

1. Never require Photon/NeoForge classes inside `kvfx-core` or `integration-api`.
2. Preserve the original native Photon artifact bytes exactly.
3. Parsing failure may reduce semantic editability, never native preservation.
4. Unsupported objects/modules stay opaque and are reported as such.
5. Re-export with no semantic mutation returns the exact original bytes.
6. Semantic mutation is allowed only for fields whose codec/edit mapping is explicitly implemented and tested.
7. Project/object version mismatches fail closed for semantic mutation; they do not delete native data.