# Ideas and decisions log

Plugin ideas, their rules check against [rules.md](rules.md), and decisions. Newest entries at the
top. Nothing gets built until an idea is marked **Chosen**.

Entry template:

```markdown
## <Idea name> (status: Proposed | Shortlisted | Chosen | Rejected)

- **Date:** YYYY-MM-DD
- **Problem / gap:** what is annoying or missing today, and for whom
- **Idea:** what the plugin would do
- **Rules check:** OK / Grey area / Not allowed, plus which rules.md sections and why
- **Stable data sources:** events, varbits, item containers (avoid widget IDs / chat parsing)
- **Existing plugins:** similar Plugin Hub plugins, and how this differs
- **Decision / notes:**
```

---

<!-- Entries go below this line. -->

## Shopping list with GE cost (status: Proposed)

- **Date:** 2026-10-18
- **Problem / gap:** When preparing for something (a quest, a diary, a skilling session, a boss
  trip's supplies), you want to know what you still lack and what it costs to buy. Neither Quest
  Helper nor Inventory Setups adds up a GE price (checked in both repos, 2026-10-18).
- **Idea:** Your own lists of items and quantities, built in a side panel. For each list, show:
  - what you have across bank, inventory and equipment;
  - what is missing;
  - the total GE cost of the missing items;
  - optionally, a GE search shortcut that shows only the missing items, the same trick Quest
    Helper uses (type a keyword in the GE search box and it calls `client.setGeSearchResultIds`).
- **Rules check:** OK. It only shows your own items and the GE price that RuneLite already shows.
  `ItemManager.getItemPrice` comes from RuneLite's own price data, so there's no third-party
  server and no IP warning. The GE search shortcut is a display change, not automation, and Quest
  Helper ships it on the hub. Nothing combat-related; a supply list for a boss trip is fine
  because it doesn't advise on the fight itself.
- **Stable data sources:**
  - `ItemContainerChanged` for inventory, equipment and bank. The bank is only known after you open
    it once per session, so cache the last-seen contents in config, like Quest Helper's
    `QuestBankManager` does.
  - `ItemManager.getItemPrice` and `ItemComposition.isTradeable`.
  - Lists stored in RuneLite config as JSON through the injected `Gson`.
  - `GrandExchangeSearched` event plus `client.setGeSearchResultIds` for the optional GE shortcut.
- **Existing plugins:**
  - **Inventory Setups** (hub): saves gear/inventory layouts and highlights differences, but has
    no prices or totals.
  - Core **Bank Tags**: groups items into tabs, with no "missing" view or prices.
  - **Quest Helper**: only covers quest/diary requirements, and has no total.
- **Decision / notes:** The strongest standalone candidate so far. It's low-maintenance: no widget
  IDs, no chat parsing, no game-content data to keep updated. Things to pin down: how items are
  added (a search box, or "add from current inventory"), how charged/degraded variants count, and
  whether noted items count as owned. It could also absorb option 3 of **Skill goals**, as a list
  attached to a goal.

## Quest Helper: total GE cost of missing items (status: Proposed)

- **Date:** 2026-10-18
- **Problem / gap:** Quest Helper lists the items a quest needs and colours them by whether you
  have them. It can also show those items in the GE search. It never adds up what the missing items
  would cost to buy.
- **Idea:** Add a line under Quest Helper's item requirements, such as "Missing items: ~1.2M GP",
  plus each item's price in its tooltip.
- **Rules check:** OK. It shows GE prices RuneLite already has, for items the game already lists.
- **Stable data sources (in Quest Helper's code, checked 2026-10-18):**
  - `ItemRequirement` has the quantity, the alternate item IDs, and
    `checkTotalMatchesInContainers(...)` for how many you have.
  - `panel/QuestRequirementsPanel.setRequirements` renders the list.
  - `ItemManager` is already injected.
- **Existing plugins:** This is a change to Quest Helper itself (Zoinkwiz/quest-helper, BSD-2).
  **It can't be a separate plugin.** A hub plugin can't call another hub plugin's classes, and
  Quest Helper doesn't publish its requirement list. Its `PluginMessage` events only go to Shortest
  Path and Not Enough Runes.
- **Decision / notes:** Best done as a pull request to Quest Helper. Its README links a
  contribution guide and a Discord, so ask there first whether they'd accept it. Edge cases to
  decide:
  - which alternate to price (the cheapest?);
  - skipping untradeables, `canBeObtainedDuringQuest` items, and items marked recommended rather
    than required;
  - pricing only what's missing, not the full list.

  This gives Java/RuneLite practice in a real codebase, but isn't a plugin of our own.

## Skill goals (status: Proposed)

- **Date:** 2026-10-18
- **Problem / gap:** The OSRS mobile client lets you set a goal per skill (a target XP or a target
  level) and shows progress towards it. The user hasn't found the same thing in RuneLite.
- **Idea:** Set a goal per skill (target level or target XP) and see progress towards it: XP left,
  percent done, and a progress bar.
- **Rules check:** OK. It only shows the player's own XP, which the game already shows. There's no
  combat, PvP, menus, input or network. Nothing in rules.md applies.
- **Stable data sources:**
  - `StatChanged` for XP; `Experience.getXpForLevel` for level-to-XP conversion.
  - The game stores its own goals in varps `VarPlayerID.XPDROPS_<SKILL>_START` / `_END` (set in the
    XP drops "Setup" interface, and probably by mobile too). Read them with `VarbitChanged`.
  - Plugin-owned goals would live in RuneLite config (`ConfigManager`), not in the game.
- **Existing plugins:** The game already has skill goals (XP drops setup, since 2015). RuneLite's core
  **XP Tracker** already reads those varps (`XpTrackerPlugin.startGoalVarpForSkill`). With a goal
  set, its panel and "Add to canvas" progress bars run to the goal instead of the next level.
  This overlap needs checking in-game before building anything.
- **Decision / notes:** Open. The direction depends on what the core XP Tracker already covers:
  1. Nothing to build: set goals in-game and use the XP Tracker.
  2. A plugin that shows the in-game goals more clearly, such as an always-on goals overlay. This
     has a lot of overlap with the core plugin.
  3. A plugin with its own goals, stored in RuneLite config and set from a side panel. It could
     hold several goals per skill, or a list of goals. This is the clearest gap.
