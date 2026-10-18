# Rules for this plugin

What a RuneLite Plugin Hub plugin may and may not do. If a feature breaks any of these, Jagex can ban
the accounts that use it, and RuneLite will reject the plugin or pull it from the hub.

**Last checked: 2026-10-18.** Sources, in order of authority:

1. Jagex: [Third Party Client Guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1)
   ([wiki copy](https://oldschool.runescape.wiki/w/Update:Third_Party_Client_Guidelines)), plus the
   [2024-04-17 update](https://secure.runescape.com/m=news/a=13/undead-pirates-tweaks-varlamore-cas--more?oldschool=1)
   and the [mouse keys clarification](https://services.runescape.com/m=news/mouse-keys---changes--clarification?oldschool=1).
2. RuneLite: [Rejected or Rolled-Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
   (last edited 2026-10-17), [Plugin Hub Review](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review),
   [plugin-hub README](https://github.com/runelite/plugin-hub), and closed issues/PRs labelled
   [`wontfix`](https://github.com/runelite/runelite/issues?q=label%3Awontfix).
3. The template's [AGENTS.md](../AGENTS.md), RuneLite's own condensed list.

> **Caveat:** the Jagex page could not be opened directly while this file was written (network policy).
> The Jagex items below come from search extracts and the wiki copy, so they are paraphrases. The
> RuneLite items were read word for word. Skim the Jagex page yourself once. Jagex's last guideline
> change found was 2024-04-17. RuneLite's list changes often (several edits in October 2026), so
> re-check it before submitting and before each big feature.

---

## 0. The core principle

> **User-placed markers, and making information the game already shows clearer, are generally fine.
> Answers to game mechanics that the plugin works out for you are not.**

- Jagex says the "where to stand" ban covers *only automatic indicators, and not tiles which have been
  manually marked*. It also allows extra indicators of a boss mechanic only when they come from
  *a manually triggered external helper*.
- Jagex's list is "not exhaustive". A feature that behaves like a banned one counts as banned.
- RuneLite reviewers judge grey areas case by case. *"If it is difficult for us to ensure the plugin
  isn't against the rules we will not merge it."*
- Clicks and effort also count. A 2019 Jagex post flags features that change how you pick actions
  or click menus, players or NPCs, especially where that means fewer clicks than the official client.

## 1. No automation, no injected input

- No injected mouse or keyboard events, and no camera movement done for you.
- One click must never produce several game actions.
- No adding menu entries that send actions to the server. The only exceptions are the Max cape and
  achievement diary cape teleports.
- No `Client.menuAction`. The single allowed use is fetching collection log data.
- No `Client.runScript` that sends server actions (it throws by default).
- Never call `ScriptEventBuilder.setCanSendPackets`.
- No autotyping. A plugin must not put text into the chatbox input in any way, including pasting and
  shorthand expansion. *"The only way text can get into the chatbox is if the user is pressing keys
  themselves."*
- No changing an outgoing chat message after the user sends it.
- No controlling the client window: no minimizing and no changing which window has focus.
- No touchscreen or controller input plugins, because they would likely trip Jagex's macro detection.
- For mouse keys, only your operating system's own default mouse keys program is allowed.
- No camera yaw, pitch or position presets.
- No features that can be abused for AFK training, such as AFK agility.
- `Client.hopToWorld` is restricted and needs manual review. Avoid it.

## 2. No boss or combat "solving"

The Jagex rules apply to all bosses, raid sub-bosses, Slayer bosses, demi-bosses and wave-based
minigames such as Fight Caves and Inferno. Raids and PvP are not the only places they apply.

**Prohibited:**

- **Attack prediction:**
  - the timing or style of the next attack;
  - attack counters;
  - "flinch" timing helpers.
- **Projectiles:** where a projectile is aimed or where it will land.
- **Prayer advice:**
  - prayer switching indicators;
  - combat prayer recommendations;
  - prayer order tools, as in the removed Cerberus helper.
- **Standing advice:** automatic "stand here / don't stand there" indicators. Tiles you mark
  yourself are fine.
- **Future hazards:** warnings of hazards before they happen. Highlighting a hazard that is active
  right now is fine.
- **NPC targeting:** showing which player an NPC is focused on.
- **Added cues:** any extra visual or audio indicator of a boss mechanic, unless it is a manually
  triggered external helper.
- **Simulation:** any simulation of game content, for any purpose. Jagex had Quest Helper's
  Leviathan simulation removed.
- **Examples removed in the past:**
  - the AoE warning plugin;
  - Zulrah, Jad/Fight Caves, Cerberus and Demonic Gorilla helpers;
  - the Volcanic Mine helper;
  - Barbarian Assault left-click calling.

**RuneLite policy:** *"Any new high-end PvM boss plugins"* are not being considered at all. Treat
boss content as off-limits for this project.

## 3. No revealing hidden information or predicting what the game doesn't show

- Don't show anything the game deliberately hides, and don't predict outcomes the game doesn't reveal.
- **Banned examples:**
  - revealing the Sotetseg maze in Theatre of Blood;
  - predicting Hallowed Sepulchre trap activations (removed in October 2026 at Jagex's request);
  - dynamic Puro-Puro spawns;
  - opponent freeze timers;
  - PvP loot previews.
- No unhiding interface components the game hides, such as the special attack bar or the minimap
  where they are disabled (Barrows, for example).

## 4. Menu entries: only minor swaps

- Left-click and shift-click swaps for NPCs have been allowed since June 2022.
- No conditional menu entry removal, for example hiding Attack based on friend status or NPC type.
  RuneLite calls this "overpowered in some cases".
- No menu changes that give a combat advantage.
- In PvP, no removing, hiding or deprioritising Attack or Cast options on players.
- No spell-targeting simplification, such as removing menu options to make targets easier to click.
- As in section 1, no new menu entries that send server actions.
- **Unverified:** older guidelines also restricted reordering player options like "Trade with",
  changing menu options on worn or equipped items, and Blackjacking swaps. Construction menu changes
  have been allowed since April 2024. If an idea touches these, check the current Jagex page first.

## 5. No PvP advantages

- No opponent freeze timers.
- No PK or skull warnings.
- No highlighting players by combat level, for example who you can attack or who is within X levels.
- No identifying opposing clan members.
- No PvP loot previews.
- No showing who your opponent's opponent is.
- No scouting information about other players.
- No group summaries such as how many players are attackable, or what prayers players are using.
- No Attack or Cast menu changes in PvP (section 4).

## 6. Interface, click zones and camera

- No moving or resizing the click zones of 3D objects in the game world.
- No moving or resizing the click zones of these interfaces:
  - combat options;
  - inventory;
  - worn equipment;
  - spellbook;
  - prayer.
- Spellbook components may be hidden, unhidden or moved (allowed since April 2024), but never resized.
- No removing the inventory background or making it click-through.
- No world interaction from a detached camera.
- No changing RuneLite's own UI:
  - other overlays or info boxes;
  - overlay or interface anchors;
  - the root frame or the sidebar.
- No changing other plugins' configs.
- No significant changes to in-game content or assets, such as model or texture replacement.

## 7. Network, data and privacy

- **No dynamic code:**
  - no downloading or vendoring code at runtime;
  - no class loading;
  - no runtime code generation;
  - no Java (de)serialization.
- **External HTTP calls need clear justification:**
  - Use only the injected `OkHttpClient`, called asynchronously, never on the client thread.
  - Network IO gets extra review every time it changes.
  - A feature that talks to a third-party server must be **off by default (opt-in)**.
  - It must carry this config warning: *"This feature submits your IP address to a 3rd-party server
    not controlled or verified by RuneLite developers"*.
  - The plugin-hub manifest usually gets a matching `warning=` line too.
- **Player data:**
  - No exposing player information over HTTP.
  - No crowdsourcing data about *other* players, such as locations, gear or names.
  - Sending even your own data needs a strong reason. Default to keeping everything local.
- **Credentials:** no plugin that stores account credentials itself. Only a vetted store such as
  Bitwarden is acceptable.
- **Player highlighting:** no highlighting offline friends, and no generic player highlighting.
- **Paid services:** since 2026-10-14, new plugins may not lock significant features behind a paid
  external service. Common services like Spotify are exceptions.

## 8. Code restrictions (RuneLite review)

- **Language:**
  - Java only, no Kotlin or Scala.
  - The code must compile for Java 11.
  - Don't use `net.runelite` as the package name.
- **Forbidden techniques:**
  - reflection;
  - JNI or JNA;
  - `Unsafe` or LWJGL native memory access;
  - running external processes (`Process`, `ProcessBuilder`, `Runtime`).
- **Forbidden calls:**
  - `Thread.sleep` and `Thread.interrupt`;
  - `java.awt.KeyboardFocusManager`;
  - `Desktop.browse`, `Desktop.open` and `LinkBrowser.open`. Use `LinkBrowser.browse` instead.
- **Use these instead:**
  - `Filepath` for all file I/O, not `java.io` file APIs. This also needs `internalName` set on
    `@PluginDescriptor`.
  - The injected `Gson`. Never `new Gson()`; use `.newBuilder()` if you need to customize it.
  - The injected `OkHttpClient`. Never `new OkHttpClient()`.
  - `net.runelite.api.gameval` constants (`ItemID`, `InterfaceID`, `VarbitID`, ...) instead of magic
    numbers.
  - `WidgetInfo` is rejected by the packager.
- **Third-party dependencies:**
  - Each one needs Gradle checksum verification metadata and manual verification by a maintainer.
  - Avoid them. They also force `build=gradle` and a slower review.
  - The jar is limited to 10 MiB by default.
- **Rule of thumb:** *"If we cannot review every single line of source code that your plugin will
  execute, we will not accept it."* Some older plugins are grandfathered in, so an existing hub
  plugin doing something does not prove it is allowed.

## 9. Content and other policies

- No adult or overtly sexual content.
- No plugins driven entirely by IDs the player enters. A fixed, built-in set of IDs is fine.
- No Twitch, BTTV, FFZ or 7TV emotes.
- No personal-best or other "hiscores" for data that isn't on the official hiscores (it can be spoofed).
- No party auto-rejoin.

---

## How this differs from the original draft list

The draft's seven points all still hold. Checking the official sources showed these changes:

| Draft point | What the official sources add or change |
| --- | --- |
| No automation | The concrete bans are wider: autotyping and pasting into chat, editing sent messages, `menuAction`/`runScript`, adding server-action menu entries, window/focus control, controller/touch input, camera presets, inventory click-through, and AFK-agility aids. |
| No boss solving | Covers **all** bosses, sub-bosses, Slayer bosses, demi-bosses and wave minigames, not only Inferno, raids and PvP. Also bans attack counters, projectile targets, prayer-order tools, flinch helpers, NPC-focus indicators, *future* hazards, added mechanic cues, and simulations. RuneLite accepts **no new high-end PvM boss plugins at all**. |
| No hidden info | Concrete cases: the Sotetseg maze, Sepulchre traps (removed Oct 2026), Puro-Puro spawns, freeze timers, loot previews, and unhiding hidden interface parts. |
| Minor menu swaps only | NPC left/shift-click swaps are explicitly allowed. Conditional removal is banned. Spell-targeting simplification and PvP Attack/Cast changes are banned. |
| No PvP advantages | Concrete list: freeze timers, skull warnings, level-range highlighting, clan identification, loot previews, opponent's opponent, scouting, and group summaries. |
| No dynamic code; justify HTTP | Also: third-party-server features must be opt-in with the standard IP warning, no exposing player data over HTTP, no crowdsourcing other players' data, no credential storage, and no paid-service paywalls. |
| User markers OK, computed answers not | Confirmed in Jagex's own wording ("manually marked", "manually triggered"). |
| *(new)* | Interface and click-zone rules, the RuneLite UI and other-plugin config bans, the asset-alteration ban, the code restrictions in section 8, and the content policies in section 9. |

## Checking a grey-area idea

1. Does it compute an answer, a prediction or a recommendation, or does it only show what the player
   already sees, or something the player set up?
2. Does it reduce clicks or effort compared with the official client?
3. Does it apply anywhere near bosses, raids, wave minigames or PvP? If so, assume no.
4. Search the [Rejected or Rolled-Back Features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features)
   page and the [`wontfix` issues](https://github.com/runelite/runelite/issues?q=label%3Awontfix).
5. Still unsure? Ask in the RuneLite Discord before building. Record the answer in [ideas.md](ideas.md).
