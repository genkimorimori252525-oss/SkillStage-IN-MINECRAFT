# P4 Forge 1.20.1 runtime-mod build contract

P4 is the first real Minecraft client integration. It remains a separate nested Gradle build so ForgeGradle/Minecraft resolution cannot infect the loader-neutral KVFX/compiler build.

## Exact runtime identity

| Component | Exact P4 pin | Build identity |
| --- | --- | --- |
| Minecraft | `1.20.1` | `net.minecraftforge:forge:1.20.1-47.1.3` supplies mappings/runtime |
| Forge | `47.1.3` | same artifact above |
| Java | `17` | Forge 1.20.1 runtime/toolchain |
| ForgeGradle | `[6.0,6.2)` | standard 1.20.1 ForgeGradle line |
| Photon | `1.1.17` | Curse project `871522`, file `7501597`, Maven `curse.maven:photon-871522:7501597` |
| LDLib | `1.0.42` | Curse project `626676`, file `6849454`, Maven `curse.maven:ldlib-626676:6849454` |
| Photon source evidence | commit `507499fbe5a3fc72bb90993e059848cbb31f13ca` | source/behavior pin used by P3/P4 |

Photon's exact 1.20.1 source declares Forge `47.1.3` and LDLib `1.0.42`. The public Photon 1.1.17 Forge binary is `photon-forge-1.20.1-1.1.17.jar`; the public LDLib binary is `ldlib-forge-1.20.1-1.0.42.jar`.

P4 declares Photon and LDLib separately because generic mod Maven endpoints do not promise transitive mod dependency metadata.

## Build layout

```text
SkillStage-IN-MINECRAFT/
├─ kvfx-core/                 # loader-neutral
├─ photon2-adapter/           # loader-neutral Photon format reader
├─ runtime-plan/              # loader-neutral runtime meaning
├─ forge1201-compiler/        # loader-neutral target compiler
└─ minecraft/
   └─ forge1201-runtime/      # isolated ForgeGradle build
```

The nested build must not be included by the root `settings.gradle.kts`; root unit tests stay fast and Minecraft-independent. P4 CI invokes the nested Forge build explicitly as a second job/gate.

## Runtime package trust boundary

The runtime mod does **not** deserialize Java objects produced by the compiler and does not trust the compiler's in-memory types. It independently validates JSON.

Only a package satisfying every condition below may reach Photon playback:

```text
format              = kneekura-forge1201-target
schemaVersion       = 1
status              = ready
defaultProfile      = photon1-default-v1
target.minecraft    = 1.20.1
target.loader       = forge
target.forge        = 47.1.3
target.runtimeSubstrate = photon
target.photon       = 1.1.17
target.photonCommit = 507499fbe5a3fc72bb90993e059848cbb31f13ca
runtimePlan.format  = kneekura-vfx-runtime-plan
runtimePlan.schemaVersion = 1
```

The validator additionally rejects:

- any `error` diagnostic even if `status` was forged to `ready`;
- missing or null runtime plans;
- unknown target/default profile identities;
- duplicate particle ids;
- invalid lifecycle/range/non-finite values;
- non-zero prewarm (Photon 1.1.17 cannot preserve it in this gate);
- target files larger than the bounded runtime read limit.

This is intentionally independent of P3 tests: the producer and consumer validate the contract from opposite sides.

## P4 default profile

`photon1-default-v1` is an **explicit approximation profile**, not a fidelity claim.

For modules that P3 does not yet reconstruct, the runtime adapter deliberately sets a known bounded default:

- emission rate: `1 particle/tick`;
- Photon 1 default shape;
- Photon 1 default texture material/render settings;
- Photon 1 default start size/rotation/color;
- all lifetime/physics/trail/sub-emitter modules remain at Photon 1 defaults.

Therefore a successfully played P4 effect remains `EMULATED`. The runtime cannot promote it to exact/equivalent visual fidelity.

## Playback boundary

For each validated particle plan P4 constructs Photon 1 `ParticleEmitter`, writes only the P3-supported scalar fields into `ParticleConfig`, sets the explicit default emission profile, adds the emitter to `FX.mainFX`, then starts a client `BlockEffect` near the player.

No Photon source is copied or modified.

## Commands

P4 provides client-only commands:

- `/skillstage_vfx_golden` — load an embedded deterministic READY package and play it near the player;
- `/skillstage_vfx_play` — load `config/skillstage-vfx/effect.kvfx-forge1201.json`, validate, then play;
- `/skillstage_vfx_validate` — validate the config package without playback.

Rejected packages produce an actionable client message and never reach Photon runtime construction.

## Acceptance boundary

GitHub CI can prove parser tests and that the Forge mod compiles against the pinned dependencies. It cannot prove the visual result. P4 stays "build verified / in-game acceptance pending" until an actual Forge 1.20.1 client run executes the golden command and records the result.