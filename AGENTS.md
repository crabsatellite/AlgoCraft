# AlgoCraft repository instructions

Use `./mod-build.ps1 build` for the full build and `./mod-build.ps1 gradle <tasks>`
for targeted checks. The project is self-contained; no internal tools,
private repositories or publishing credentials are needed to build it.

All automated tests must run silently in the background without visible
windows, focus changes, hardware mouse capture/warping or clipboard writes.
Use the built-in hidden, input-isolated, muted client adapters. Disable early
splash windows before launch. Never fall back to a visible test.

Native tests acquire a lock on the declared `algocraftTestRuntimeDirectory`
and inspect live processes before launch. Preserve and restore pre-existing
test configuration. Bound only the test processes. Require the desktop-isolation
and clean-exit receipts; compilation never substitutes for gameplay evidence.

Never add `Co-Authored-By` lines to commits.
