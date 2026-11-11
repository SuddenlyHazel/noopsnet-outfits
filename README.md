# NoopsNet Outfits

Free pride pins and patches for Minecraft, with Fabric and NeoForge implementations.

Official NoopsNet source: https://noops.world/

## Build

Use JDK 21. The Gradle wrapper downloads pinned Minecraft/loader development dependencies.

```sh
./fabric/gradlew -p fabric build --console=plain
./neoforge/gradlew -p neoforge build --console=plain
```

The NeoForge build shares `Design.java`, `Outfit.java`, translations and textures from `fabric/`. Both directories must remain together.

## Licensing and provenance

Original code is MPL-2.0; see [LICENSE](LICENSE). Artwork and third-party components are covered separately in [NOTICE.md](NOTICE.md). [Published artifact provenance](PROVENANCE.json) identifies the original pack binaries covered by our permission grant. Historical binaries may retain an older All Rights Reserved metadata label; the explicit grant in NOTICE.md also applies to those identified versions.

This repository starts with a reviewed source snapshot. It intentionally contains no private server history, player data, accounts or deployment configuration. The source snapshot differs from the original binary build in license metadata and standalone build wiring; its build is not claimed byte-identical.
