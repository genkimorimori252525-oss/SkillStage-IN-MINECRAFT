# Upstream pins — VFX Studio v1 bootstrap

These pins are evidence for the initial architecture boundary. They are **not vendored code**.

| Upstream | Branch | Exact SHA | Observed boundary |
|---|---|---|---|
| Low-Drag-MC/Photon | `1.21` | `609a975cea104fd4e067a94f222a7dd1d619d263` | Minecraft 1.21.1, Photon `2.2.6.a`, LDLib2 `2.2.39`, KilaGraph `21.1.0.14`; `FXProject.VERSION = 5`; native project data is NBT and has a Photon-owned data fixer. |
| Low-Drag-MC/Photon | `1.20.1` | `507499fbe5a3fc72bb90993e059848cbb31f13ca` | Photon `1.1.17`, Forge/Fabric legacy runtime line. |
| Low-Drag-MC/LDLib2 | `1.21` | `d30e86b56b2a31982063ef907a335d36371b0bc4` | LDLib2 `2.2.39.a`, Minecraft 1.21.1 / NeoForge. |

## Dependency rule

KNEEKURA/SkillStage code must not make Photon-native serialization the canonical project model. Photon data belongs under `authoring/photon2/` (or a future versioned provider path), and Photon integration is performed through adapters.

## License boundary

At the pinned Photon 2 SHA, `gradle.properties` declares `mod_license=GPL-3.0 license`, while the repository `LICENSE` file contains a CC BY-NC-SA 4.0 grant plus explicit Photon 2 port clauses. Because those upstream metadata sources disagree, this repository does not copy or vendor Photon source in the bootstrap. Photon is treated as an external dependency and native Photon files are user/project data.