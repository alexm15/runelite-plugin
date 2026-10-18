@AGENTS.md

# Project context

A RuneLite plugin for Old School RuneScape, to be submitted to the
[Plugin Hub](https://github.com/runelite/plugin-hub).

- **Status: no feature chosen yet.** The code is the template's hello-world under placeholder names.
- Ideas and decisions are logged in [docs/ideas.md](docs/ideas.md).
- **Do not write feature code until the user has picked an idea there.**

`AGENTS.md` (imported above) is RuneLite's own agent guide, copied as-is from the
[example-plugin](https://github.com/runelite/example-plugin) template. Keep it unmodified so it can
be re-synced. Its rules on logging, threading, APIs and testing apply here. In particular, **never
interact with RuneScape yourself**: only the user tests in-game.

## About the user

- An experienced C# developer who is new to Java. When explaining Java, Gradle, Guice or Lombok,
  compare to C#/.NET where it helps.
- Works on Windows, in VS Code (not IntelliJ), terminal-first, and doesn't use Docker. Give
  PowerShell commands (`.\gradlew.bat ...`).
- Wants a **low-maintenance** plugin:
  - Prefer stable APIs and events: `@Subscribe` events, varbits/varps, and item containers.
  - Avoid fragile things: hardcoded widget IDs, chat-text parsing, and scene scanning.
  - If a widget is unavoidable, use the `net.runelite.api.gameval.InterfaceID` constants, never raw
    numbers.

## Tech stack (with .NET analogies)

| Piece | What it is | .NET analogy |
| --- | --- | --- |
| **Java 11** | Required by the Plugin Hub (`options.release.set(11)` in `build.gradle`). No records, no switch expressions, no text blocks. `var` works for locals. | Like targeting an older C# language version: newer syntax isn't available. |
| **Gradle** + wrapper | Build tool. `build.gradle` holds dependencies and tasks. `gradlew.bat` downloads the pinned Gradle version (8.10) on first use, so Gradle itself is never installed. | `.csproj` + NuGet + MSBuild targets. The wrapper is like `global.json` pinning an SDK that downloads itself. |
| `compileOnly` dependency | `net.runelite:client` is supplied by the running client at runtime, so it is not shipped in the jar. | A reference provided by the host app (`<Private>false</Private>` / `ExcludeAssets="runtime"`). |
| **Guice** | Dependency injection. `@Inject` fields are filled in when the plugin is created. `@Provides` methods are factory registrations. Each plugin gets a child injector. | `Microsoft.Extensions.DependencyInjection`. `@Inject` ≈ constructor/property injection, `@Provides` ≈ `services.AddSingleton(sp => ...)`, `@Singleton` ≈ singleton lifetime. |
| **Lombok** | An annotation processor that generates boilerplate at compile time. `@Slf4j` adds a `log` field; `@Getter`/`@Setter` generate accessors; `@Value` makes immutable data classes. VS Code's Java extension supports it out of the box. | Source generators. `@Getter` ≈ auto-properties, `@Value` ≈ `record`, `@Slf4j` ≈ an injected `ILogger`. |
| **RuneLite client API** | `net.runelite.api.Client` holds live game state. Events go over the `EventBus`. Game state must be touched on the **client thread**. | `Client` ≈ a read-mostly service over game state. `EventBus` ≈ C# events / MediatR notifications. Client thread ≈ the WPF/WinForms UI thread (`clientThread.invoke(...)` ≈ `Dispatcher.Invoke`). |

## Setup (one time, Windows)

From an **elevated** PowerShell in the repo root:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\setup-dev.ps1
```

What the script does (safe to re-run; it skips anything already done):

- Installs these via winget:
  - `EclipseAdoptium.Temurin.11.JDK`
  - `Git.Git`
  - `Microsoft.VisualStudioCode`
  - `GitHub.cli`
- Installs the VS Code extensions `vscjava.vscode-java-pack` and `vscjava.vscode-gradle`.
- Sets machine-level `JAVA_HOME` to the newest `C:\Program Files\Eclipse Adoptium\jdk-11*` folder.
  It warns if none is found.
- Does **not** install Gradle; the wrapper handles that.

Then open a **new** terminal and check:

```powershell
java -version            # expect 11.x Temurin
.\gradlew.bat --version  # first run downloads Gradle 8.10
```

## Build and run

### Terminal

```powershell
.\gradlew.bat build      # compile
.\gradlew.bat run        # launch the dev client with this plugin loaded
.\gradlew.bat shadowJar  # optional: a standalone runnable jar in build\libs\
```

The `run` task starts `StarterPluginTest.main`, which loads the plugin as a built-in and starts
RuneLite with `--developer-mode --debug` and the JVM flag `-ea`.

### Checks before committing

`scripts/check.ps1` (Windows) and `scripts/check.sh` (Linux, macOS, cloud) do the same two things:

- Run `scripts/Check.java`. These are static checks for the Plugin Hub requirements and for the
  rules below that can be found by reading files:
  - `runelite-plugin.properties` keys and values;
  - template leftovers and build artifacts;
  - icon and PNG limits;
  - forbidden APIs (reflection, subprocesses, `new Gson()`, `Thread.sleep`, raw widget IDs and so on).
- Run `gradlew build`.

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1            # static checks + build
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -NoBuild   # static checks only
powershell -ExecutionPolicy Bypass -File .\scripts\check.ps1 -Submit    # placeholder names become errors
```

It is also the VS Code task **check**.

- **Errors** fail the run.
- **Warnings** are things a reviewer will look at closely (file access, menus, chat parsing,
  scene scanning). Read each one.
- A clean run is not proof the plugin is allowed; it only means no known red flag was found.

**Claude:** run `scripts/check.sh` before every commit, and `--submit` before a Plugin Hub PR.

### VS Code

- **Run and Debug** (Ctrl+Shift+D), then pick **"RuneLite (developer mode)"** from
  `.vscode/launch.json`. This runs the same main class with the same arguments, under the debugger,
  so breakpoints work.
- **Terminal > Run Task** offers `gradle: build` (also Ctrl+Shift+B) and
  `gradle: run RuneLite (developer mode)`.
- The Gradle side panel (elephant icon) lists every task. `run` is under *other*.

On first open, VS Code imports the Gradle project. If code shows as red, run
**"Java: Clean Java Language Server Workspace"** from the command palette.

### Developer mode

`--developer-mode` only takes effect when the client is not started by the launcher, which is true
for `gradlew run` and the VS Code launch. It enables the **Developer Tools** plugin; its sidebar
panel is the wrench icon. Useful tools there:

- **Var Inspector:** watch varbits and varps change live. This is the main way to find stable state
  to subscribe to.
- **Widget Inspector:** find interface and component IDs, then look up their names in `InterfaceID`.
- **Shell:** jshell inside the client (the `jshell` test dependency).
- Overlays that show IDs for NPCs, objects, items, projectiles and more.

`--debug` turns on debug logging. Logs also go to `%USERPROFILE%\.runelite\logs\client.log`.

## Logging into the dev client with a Jagex account

Source: [RuneLite wiki, "Using Jagex Accounts"](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts),
checked 2026-10-18.

1. Make sure the RuneLite **launcher** is version 2.6.3 or newer.
2. Run **"RuneLite (configure)"** from the Windows Start menu.
3. In **Client arguments**, add `--insecure-write-credentials`, then click **Save**.
4. Launch RuneLite once **via the Jagex Launcher**. This needs RuneLite selected as the game client
   in the Jagex Launcher.
   - RuneLite writes your launcher credentials to `%USERPROFILE%\.runelite\credentials.properties`.
5. Close that client. Start the dev client (`.\gradlew.bat run` or F5 in VS Code). It reads the saved
   credentials and logs in without a password.

**Security:**

- `credentials.properties` lets anyone log into the account, bypassing the password.
  - Never share it or copy it into the repo (`.gitignore` blocks the name as a safety net).
- When you are done developing:
  - Delete the file.
  - Remove the flag from "RuneLite (configure)".
- If the file may have leaked, use **"End sessions"** under account settings on runescape.com. That
  invalidates the credentials.
- Legacy (non-Jagex) accounts don't need any of this; they type their username and password into the
  dev client.

## Core plugin patterns

```java
@Slf4j                                    // Lombok: generates `log`
@PluginDescriptor(
	name = "Starter (WIP)",                // shown in the plugin list
	description = "...",
	tags = {"search", "keywords"}
)
public class StarterPlugin extends Plugin // lifecycle: startUp()/shutDown() on enable/disable
{
	@Inject private Client client;
	@Inject private ClientThread clientThread;
	@Inject private OverlayManager overlayManager;
	@Inject private StarterOverlay overlay;  // Guice builds it; it can @Inject too
	@Inject private StarterConfig config;

	@Override protected void startUp()  { overlayManager.add(overlay); }
	@Override protected void shutDown() { overlayManager.remove(overlay); } // undo everything here

	@Provides                              // registers the config proxy with Guice
	StarterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(StarterConfig.class);
	}
}
```

- **Events.** `@Subscribe` on a `public void onX(X event)` method. The plugin is registered with
  the EventBus automatically. Common events, in `net.runelite.api.events` unless noted:
  - `GameTick`: every 0.6 s server tick. Keep the work cheap.
  - `GameStateChanged`: login, logout, loading.
  - `VarbitChanged`: covers both varbits and varps. Compare against `VarbitID` / `VarPlayerID`
    constants. **This is the preferred, stable source of game state.**
  - `ItemContainerChanged`: inventory, equipment and bank. Use `InventoryID` / `gameval` constants.
  - `StatChanged`: XP and levels.
  - `NpcSpawned`/`NpcDespawned` and `GameObjectSpawned`/`GameObjectDespawned`: keep your own
    collection instead of scanning the scene.
  - `ChatMessage`: game and chat messages. Matching on text is fragile; prefer varbits when one
    exists.
  - `MenuEntryAdded`: menu changes. This is rules-sensitive; see [docs/rules.md](docs/rules.md).
  - `ConfigChanged` (in `net.runelite.client.events`): filter on `event.getGroup()`.
- **Threading.** Event handlers run on the client thread, so they may read `client`.
  - From any other thread, such as an OkHttp callback or Swing code, call
    `clientThread.invoke(() -> ...)` before touching `client`.
  - Swing UI updates go through `SwingUtilities.invokeLater`.
- **Config.** Config is an interface extending `Config` with `@ConfigGroup("alexm15starter")`.
  - Each `default` method with `@ConfigItem(keyName, name, description)` becomes a setting. RuneLite
    builds the UI.
  - Extras: `@ConfigSection` groups settings, `@Range` and `@Units` constrain numbers, and `@Alpha`
    allows transparency on colours.
  - Never rename a key or group without a migration.
- **Overlays** draw on the game canvas each frame.
  - Extend `Overlay`, or `OverlayPanel` for a ready-made `panelComponent` box.
  - In the constructor, set `setPosition(OverlayPosition.TOP_LEFT)` and similar.
  - Override `Dimension render(Graphics2D g)`.
  - Register with `overlayManager.add` / `remove`.
  - `render` runs every frame, so do no heavy work there; compute in event handlers and only draw
    here.
  - Related types: `InfoBox` (small icons with a counter) and `OverlayUtil` helpers for tile and
    actor highlights.
- **Side panels.**
  - Extend `PluginPanel`, which is Swing (≈ WinForms).
  - Wrap it in a `NavigationButton` built with `.tooltip()`, `.icon(ImageUtil.loadImageResource(getClass(), "icon.png"))`,
    `.priority()` and `.panel()`.
  - Add it with `clientToolbar.addNavigation(button)` in `startUp`, and remove it in `shutDown`.
- **Resources** (icons, data files) go in `src/main/resources/com/alexm15/starter/`.

## Renaming once an idea is picked

`Starter` and `com.alexm15.starter` are placeholders. When renaming, update all of these in one
commit:

- the package folder and `package` lines;
- the class names;
- `@PluginDescriptor(name)`;
- the config group, while it has no users yet;
- `pluginMainClass` in `build.gradle`;
- the launch.json `mainClass`;
- `runelite-plugin.properties` (`displayName`, `description`, `plugins`, `tags`);
- the `logback-test.xml` logger, if the package root changes.

`settings.gradle` / the repo name can stay as they are or change too.

## Plugin Hub submission

- **The plugin repo:**
  - It must be **public on GitHub**, open source, with a `LICENSE` file (BSD-2-Clause here).
  - Every line is **manually reviewed** for security and rule compliance. Reviewers don't check
    functionality.
  - Simple updates may be approved by RuneLite's review bot.
- **`runelite-plugin.properties`:**
  - `displayName`, `author`, `description` and `plugins` are required, and must not be the
    template defaults.
  - `tags` and `version` are optional.
  - Keep `build=standard`. It means no third-party dependencies, and the hub uses its own copy of the
    build files. `build=gradle` is only for vetted dependencies with checksum metadata.
  - Any other key fails the build.
- **Optional `icon.png`** at the repo root: about 48×72 px recommended, at most 5000 px² and
  256 KiB, and a real PNG.
- **Submitting:**
  1. Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub).
  2. Add a file `plugins/<internal-name>`. The name is lowercase `a-z0-9-` and becomes the plugin's
     internal name. Contents:
     ```
     repository=https://github.com/alexm15/runelite-plugin.git
     commit=<full 40-char commit SHA>
     ```
     Optional keys:
     - `warning=` is required in practice if anything talks to a third-party server.
     - `authors=` lists co-maintainers.
  3. Open a PR. Fix any CI or "Plugin Hub Checks" failures by pushing a new commit to the plugin repo
     and updating `commit=` in the same PR.
  4. Put the attribution footer from AGENTS.md (`Generated-by: Claude Code`) in the PR description.
- **Updates:** a new PR that changes only `commit=`, from a branch synced with upstream master.

## Rules: summary (full version in [docs/rules.md](docs/rules.md))

- **No automation:** no injected input, camera movement, autotyping, or several actions from one
  click. No `menuAction`, and no new server-action menu entries.
- **No boss or combat solving anywhere:** nothing that works out what the player should do. That
  means no attack prediction or counters, prayer switch advice, automatic safe tiles, future hazard
  warnings, flinch helpers or NPC-target indicators.
  - This covers all bosses, sub-bosses, Slayer bosses, demi-bosses and wave minigames, not only
    Inferno, raids and PvP.
  - RuneLite accepts **no new high-end PvM boss plugins**.
- **No revealing hidden information** or predicting what the game doesn't show. Also no unhiding
  hidden interface parts.
- **Menus:** only minor swaps. NPC left-click and shift-click swaps are fine. No conditional
  removal, no spell-target simplification, and nothing that gives a combat advantage.
- **No PvP advantages:** freeze timers, level-range highlights, scouting, clan identification and
  loot previews are all out.
- **No changes to click zones** of the inventory, equipment, prayer, spellbook or 3D objects. No
  inventory click-through, no detached-camera interaction, and no changing RuneLite's own UI or
  other plugins' configs.
- **Code:**
  - Java 11 only.
  - No dynamic code loading, reflection, JNI or subprocesses.
  - File access only through `Filepath`.
  - Only the injected OkHttp and Gson.
- **Network:** external HTTP needs clear justification. Third-party-server features must be opt-in
  with the standard IP warning. Never expose or crowdsource player data, and never store credentials.
- **The key distinction:** user-placed markers and clearer display of information the game already
  shows are generally fine. Answers to mechanics computed by the plugin are not. Grey areas are
  judged case by case, and hard-to-verify plugins are rejected.

### Rule for Claude: flag rule risks

**You must flag any feature idea, or part of one, that might break these rules.** Do this before
designing or coding it, even when the user didn't ask, and even when it is a small sub-feature.

- Say which rule it risks, and how to reshape the idea to stay inside the rules. Common reshapes:
  user-placed markers instead of computed ones, showing only current state, and staying out of
  combat and PvP.
- Record the verdict in `docs/ideas.md`.
- If the risk is still unclear, recommend checking the RuneLite wiki and `wontfix` issues, or asking
  RuneLite reviewers, before building.
