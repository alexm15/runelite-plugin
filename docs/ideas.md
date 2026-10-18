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
