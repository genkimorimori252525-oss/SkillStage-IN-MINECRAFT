# Forge 1.20.1 in-game acceptance handoff

This is the first real-client acceptance gate for the P4 runtime. Passing CI is necessary but is **not** visual acceptance.

## Exact environment

Use a dedicated Minecraft client profile with exactly:

- Minecraft `1.20.1`
- Forge `47.1.3`
- Photon `1.1.17` (`photon-forge-1.20.1-1.1.17.jar`)
- LDLib `1.0.42` (`ldlib-forge-1.20.1-1.0.42.jar`)
- the P5 Actions artifact JAR `skillstage-vfx-runtime-forge1201-0.1.0-SNAPSHOT.jar`

Do not substitute a newer Photon/Forge/LDLib version for this acceptance run.

## Verify the downloaded runtime JAR

The Actions artifact also contains a `.sha256` file generated from the exact CI-built JAR. On Windows PowerShell:

```powershell
Get-FileHash .\skillstage-vfx-runtime-forge1201-0.1.0-SNAPSHOT.jar -Algorithm SHA256
Get-Content .\skillstage-vfx-runtime-forge1201-0.1.0-SNAPSHOT.jar.sha256
```

The hash values must match before the run is treated as evidence.

## Golden acceptance run

1. Put the three runtime/mod JARs in the dedicated profile's `mods` directory: Photon 1.1.17, LDLib 1.0.42, and the KNEEKURA runtime JAR.
2. Start Forge 1.20.1 and confirm the title/mod list loads without a dependency error.
3. Open a disposable test world.
4. Run `/skillstage_vfx_golden`.
5. Record the chat result and the area roughly two blocks in front of the player and one block above that origin.

The command must be registered and the client must report that the embedded `golden-particle-v1` package was accepted and started. A visible Photon particle effect is expected near that origin. This effect is still the `photon1-default-v1` **EMULATED** profile; matching Photon 2 visually is not part of this gate.

## Evidence to keep

For a successful run, capture:

- a screenshot or short recording showing the command result and rendered effect;
- the exact runtime JAR SHA-256;
- the Minecraft/Forge/Photon/LDLib versions used.

For a failure, keep the first applicable evidence:

- command missing: screenshot plus `latest.log`;
- `rejected [REASON]`: copy the full reason/message;
- accepted but no visible effect: screenshot/recording plus `latest.log`;
- crash: `latest.log` and the generated crash report.

Do not upgrade dependencies or edit the golden package to make a failed acceptance pass. The failure should be diagnosed against the pinned environment first.

## Optional config-package checks

After the embedded golden gate is understood:

- `/skillstage_vfx_validate` validates `config/skillstage-vfx/effect.kvfx-forge1201.json` without playback.
- `/skillstage_vfx_play` validates the same file and only then starts Photon playback.

These commands are secondary to the embedded golden acceptance and should not be used to hide a golden failure.