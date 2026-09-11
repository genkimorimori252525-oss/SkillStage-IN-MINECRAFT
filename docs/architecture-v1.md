# KNEEKURA VFX Studio v1 — frozen bootstrap boundary

## Direction

```text
External MOD / runtime observation
            |
            v
      .kvfxproj package
            |
      +-----+------+----------------+
      |            |                |
 semantic/     authoring/        opaque/
 editable IR   native payload    unknown/lossless
      |            |                |
 evidence/       assets/          targets/
      |                             |
      +---------- adapter ports -----+
                 /          \
       Photon 2 authoring    Forge 1.20.1 runtime
```

## Hard rules

1. Photon source/API types never appear in `kvfx-core` or `integration-api`.
2. Photon native files are preserved under `authoring/` even when a semantic conversion exists.
3. Unknown provider data belongs under `opaque/` and must survive read/write unchanged.
4. Static/runtime evidence is additive; reconstruction never overwrites the original evidence.
5. Representation, editability, replay fidelity, and target support are independent status dimensions.
6. A runtime adapter must never report `EXACT` merely because compilation succeeded.
7. Capability identifiers are open strings, not a closed enum, so newer Photon/provider features can be represented before the core understands them.
8. Forge 1.20.1 and Photon 2 implementations are adapter modules to be added later. The bootstrap does not port or vendor Photon.

## First real acceptance after bootstrap

The next implementation slice should replace the synthetic authoring fixture with a Photon 2.2.x adapter that imports one minimal particle effect while preserving the original `.fxproj`; then a Forge 1.20.1 target adapter should replay the same semantic effect and report any emulation explicitly.