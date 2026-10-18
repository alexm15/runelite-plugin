# Cloud environment (Claude Code on the web)

The settings for the cloud environment that Claude Code sessions on this repo run in. They are
entered in the environment dialog (the cloud environment menu in a session's title bar, then
**Edit**), not read from this file. This file is the copy to paste from and keep in sync.

Docs: <https://code.claude.com/docs/en/cloud-environments>

## Name

`RuneLite plugin (Java/Gradle)`

## Network access

**Custom**, with **Also include default list of common package managers** ticked. That list
already covers Gradle (`services.gradle.org`, `plugins.gradle.org`) and Maven Central.

Allowed domains:

```text
repo.runelite.net
```

`repo.runelite.net` is RuneLite's Maven repo, where `net.runelite:client` and `jshell` come from.
Without it, `gradlew build` fails with a 403.

## Environment variables

None needed. Never put credentials here, and especially not `credentials.properties`: anyone
using the environment can read its variables.

Optional: `BASH_DEFAULT_TIMEOUT_MS=300000` gives a cold Gradle build 5 minutes before Claude's
command times out (the default is 2 minutes).

## Setup script

The image already has OpenJDK 21 and Gradle. `build.gradle` compiles with `--release 11`, which
checks against the Java 11 API, so no JDK 11 is installed. The script only pre-warms Gradle. The
environment cache snapshots the disk after setup (it is rebuilt about weekly, or when the script
or allowed domains change), so later sessions start with the Gradle 8.10 distribution and the
RuneLite jars already in `~/.gradle`.

```bash
#!/bin/bash
# Pre-warm Gradle for alexm15/runelite-plugin. Must exit 0, and should finish in under ~5 minutes
# so the environment cache can build.
repo=/home/user/runelite-plugin
if [ -x "$repo/gradlew" ]; then
	cd "$repo" && timeout 240 ./gradlew --no-daemon --quiet compileJava compileTestJava \
		|| echo "Gradle pre-warm failed (non-fatal); the first build in the session will download instead."
else
	echo "Repo not found at $repo; skipping Gradle pre-warm."
fi
exit 0
```

If the repo isn't cloned yet when the script runs, it skips the pre-warm and the first build in
each session downloads everything (about a minute). Nothing breaks.

## What the cloud can't do

It can't run the RuneLite client (`gradlew run`). AGENTS.md forbids Claude from interacting with
the game anyway. In-game testing happens on Windows.
