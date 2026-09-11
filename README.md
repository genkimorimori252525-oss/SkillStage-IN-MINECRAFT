# SkillStage-IN-MINECRAFT

Private Minecraft VFX authoring/reconstruction workspace.

The first implementation slice is **KNEEKURA VFX Studio v1**. It treats Photon as an external authoring/runtime dependency behind adapters and keeps the project interchange format (`.kvfxproj`) independent from Photon internals.

## Current modules

- `kvfx-core` — versioned, loss-preserving `.kvfxproj` package reader/writer.
- Future slices will add stable authoring/runtime adapter APIs, Photon 2 integration for Minecraft 1.21.1, Forge 1.20.1 targeting, effect discovery, runtime capture, and reconstruction.

## Build

```bash
gradle test
```

Java 17 is the shared-core baseline so the same core can be consumed by Forge 1.20.1 and newer Java 21 authoring modules.