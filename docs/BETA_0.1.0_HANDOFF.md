# AlgoCraft 0.1.0 Beta verification

The `main` branch targets NeoForge 1.21.1 / Java 21; `1.20.1` targets Forge
1.20.1 / Java 17. Both builds are self-contained. Building requires no internal
repositories, authentication tokens or private tool installations.
See [developing](DEVELOPING.md) for the bundled build and test entrypoints.

The latest regression covers the official-format help link, draft preservation,
pixel computer favicon and branding, placeable engraved trophies and the
command-based bank guide. Tests are hidden, muted and input/clipboard isolated.
Both native clients completed ten passes with zero isolation or cleanup failures,
and both GameTest servers passed all 24 cases. Headless Web testing passed 61
checks and 1,000 English/Chinese statement renders, including the new icons.

The baseline suites ran 1,000 / 1,001 checks. They found obsolete CI assertions
and a Java 17 default-encoding issue in the packaged bank. The CI tests now match
the read-only manifest validation workflow; packaging explicitly uses UTF-8.
Every failure was repaired and retested in the final 56 / 58 targeted checks.
Both versions also passed all 67 bank contracts and six quality checks.
Failed attempts and final results are preserved in the private evidence archive.

The bank and judging algorithms are unchanged from the exhaustive acceptance
of 2,171 reference solutions through two paths (4,342 checks). Its dedicated
multiplayer evidence is reused. Every file in the final bundled bank was checked
against the source, and both final release JARs passed 63 representative compiler
cases with ECJ absent from the parent classpath.

| Minecraft | Final JAR SHA-256 | Native checks / passes |
| --- | --- | --- |
| 1.21.1 | `187b48cfa1efd7a9e1e215f3cd2df699d30eb6a2507144c664731f855b4f2eaf` | 6510 / 10 |
| 1.20.1 | `dd41689de7d89de0cd8f42faefde38fee8ba37b2e4026f914e6896e26e3e3144` | 6505 / 10 |

The [portable verification summary](verification/0.1.0-beta.json) records the
final artifact identities. Machine-specific logs, images and test worlds stay
outside the source repository. The mod remains a Beta: longer player sessions,
reward balance and learning enjoyment should continue to inform later releases.

An earlier accepted NeoForge build was verified to start and log in in the
local production modpack. Final release artifacts are separate from that running
human play session and should be installed together on clients and server.
