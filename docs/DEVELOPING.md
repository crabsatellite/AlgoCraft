# Build AlgoCraft

This branch targets Forge 1.20.1 (1.20.1). Use Windows, PowerShell 7, Python 3.11+,
JDK 17 and a working OpenGL driver for native client tests. Set `JAVA_HOME`
to your JDK. The build uses only the repository and public Gradle/Minecraft
dependencies; GitHub sign-in, API tokens and internal tools are unnecessary.

Run `./mod-build.ps1 build`. It includes JVM, official-bank and GameTest gates.
The bundled runtime guard leases each test directory and rejects an existing
Minecraft process using it. The local preflight checks the unattended adapters
and loader configuration before a native test starts.

For real editor acceptance, run:

```powershell
./mod-build.ps1 gradle ideClientAcceptanceTest --no-daemon '-PalgocraftTestRuntimeDirectory=run-ide-stress'
```

All tests must be invisible, muted and input/clipboard isolated. The native lane
requires a working OpenGL driver; CI driver support is checked by the actual
launch. Missing graphics support is a failed lane, never permission to show a
window. Keep the desktop-isolation and clean-exit receipts.

The two branches share the official JSON/PNG bank and component formats.
See [custom problem format](PROBLEM_FORMAT.md) and [Beta verification](BETA_0.1.0_HANDOFF.md).
