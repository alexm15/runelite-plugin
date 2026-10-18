# runelite-plugin

A work-in-progress [RuneLite](https://runelite.net) plugin for Old School RuneScape, targeting the
[Plugin Hub](https://github.com/runelite/plugin-hub). The feature is not decided yet; see
[docs/ideas.md](docs/ideas.md).

- Dev setup, build, run, and Jagex account login: [CLAUDE.md](CLAUDE.md)
- What a plugin may and may not do: [docs/rules.md](docs/rules.md)
- Cloud environment for Claude Code sessions: [docs/cloud-environment.md](docs/cloud-environment.md)

Quick start (Windows, PowerShell):

```powershell
.\gradlew.bat build   # compile
.\gradlew.bat run     # launch RuneLite with this plugin loaded, in --developer-mode
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1   # rules checks + build, before committing
```

Licensed under the BSD 2-Clause license (see [LICENSE](LICENSE)).
