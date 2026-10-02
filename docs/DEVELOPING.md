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
requires a working OpenGL driver. GitHub-hosted Windows CI installs the public
MSYS2 Mesa package and uses LLVMpipe in its disposable JDK; it does not modify
the operating system or the local developer JDK. Driver support is checked by
the actual hidden launch. OpenAL uses its null backend on CI, while the existing
listener-gain gate still requires a real zero-gain audio context. Missing graphics support is a failed lane, never permission to show a
window. Keep the desktop-isolation and clean-exit receipts.

The two branches share the official JSON/PNG bank and component formats.
See [custom problem format](PROBLEM_FORMAT.md).

Before release, run `python scripts/check_public_tree.py`. The build wrapper and
CI run this check too. Keep internal reports and generated test output outside
the publishable tree; use ignored `build/` or a separate private evidence folder.
