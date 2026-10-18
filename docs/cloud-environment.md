# Cloud environment (Claude Code on the web)

How to create the cloud environment that Claude Code sessions on this repo run in. Every value to
paste is in its own code block. The settings live in the environment dialog, not in this file, so
update this file whenever you change them there.

Docs: <https://code.claude.com/docs/en/cloud-environments>

## Create it

1. Go to <https://claude.ai/code> and open the new-session screen.
2. Under the message box, select the first control in the row. That's the **environment
   selector**.
3. At the bottom of the menu, select **New environment**.
4. Fill in the fields as described below.
5. Select **Add environment**.
6. Start a session in it, with the `alexm15/runelite-plugin` repo selected. Ask Claude to run
   `scripts/check.sh`. Both the static checks and `./gradlew build` should pass, apart from the
   expected placeholder-name warnings.

To change an existing environment, there are two routes:
- **Before a session starts:** in the environment selector, find its row under **Cloud** (or
  **More**), hover over the row, and select the settings icon.
- **In a running session:** select the caret after the session title, then **Edit cloud
  environment**.

A change to the setup script or the allowed domains rebuilds the environment cache. New sessions
pick it up; running ones don't.

## Fields

### Name

```text
RuneLite plugin (Java/Gradle)
```

### Network access

- Select **Custom**.
- Tick **Also include default list of common package managers**. That list already covers Gradle
  (`services.gradle.org`, `plugins.gradle.org`) and Maven Central.
- Paste this into **Allowed domains**:

```text
repo.runelite.net
```

`repo.runelite.net` is RuneLite's Maven repo, where `net.runelite:client` and `jshell` come from.
Without it, `gradlew build` fails with a 403. GitHub access goes through its own proxy, so it
doesn't need to be listed.

### Environment variables

Optional. Leave the field empty, or paste:

```text
BASH_DEFAULT_TIMEOUT_MS=300000
```

That gives a cold Gradle build 5 minutes before Claude's command times out (the default is 2
minutes).

Never put credentials here, and especially not `credentials.properties`: anyone using the
environment can read its variables.

### Setup script

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

Why it is this small:
- The image already has OpenJDK 21 and Gradle. `build.gradle` compiles with `--release 11`, which
  checks against the Java 11 API, so no JDK 11 is installed.
- The script only pre-warms Gradle. After setup, the environment cache snapshots the disk. It is
  rebuilt about weekly, or when the script or allowed domains change. Later sessions start with the
  Gradle 8.10 distribution and the RuneLite jars already in `~/.gradle`.
- If the repo isn't cloned yet when the script runs, it skips the pre-warm. The first build in each
  session then downloads everything (about a minute). Nothing breaks.

## What the cloud can't do

It can't run the RuneLite client (`gradlew run`). AGENTS.md forbids Claude from interacting with
the game anyway. In-game testing happens on Windows.
