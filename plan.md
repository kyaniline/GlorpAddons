# GlorpAddons — Development Plan

## Project Overview
A Fabric mod for Minecraft 1.21.10, written in Kotlin, targeting Hypixel Skyblock.
Built incrementally — new features are added over time.

## Tech Stack
| Property | Value |
|---|---|
| Minecraft | 1.21.10 |
| Mod Loader | Fabric |
| Language | Kotlin + Java (mixins) |
| Yarn Mappings | 1.21.10+build.3 |
| Fabric Loader | 0.18.4 |
| Fabric API | 0.138.3+1.21.10 |
| Fabric Language Kotlin | 1.13.9+kotlin.2.3.10 |
| Fabric Loom | 1.15.4 |
| Java | 21 |

## Conventions
- Secrets go in `.env` (gitignored), never hardcoded
- Mod ID: `glorpaddons`
- Root package: `com.glorpaddons`
- Each feature lives in its own subpackage under `com.glorpaddons`
- Config files saved to `.minecraft/config/glorpaddons/`
- Entrypoint is `ClientModInitializer` (client-only mod)
- All rectangle rendering uses `DrawContext.fill()` (tessellator/RenderSystem APIs removed in 1.21.10)
- Mouse events use `net.minecraft.client.gui.Click` class (not `Double, Double, Int`)
- Custom font styling uses `StyleSpriteSource.Font(Identifier)` (not `Style.withFont(Identifier)`)
- HUD registration uses `HudElementRegistry.attachElementBefore()` from `net.fabricmc.fabric.api.client.rendering.v1.hud`

## Config Screen (`/ga`)
- 280×280 panel, rounded edges, semi-transparent dark navy background
- Left column (86px): section navigation buttons (highlighted when active)
- Right content: section-specific options
- Sections: `["⛏  Mining", "Inventory", "Mob ESP", "Misc", "Farming"]`

## Features

### Feature 1 — Mining Commission Tracker
**Status:** Complete ✓

**Description:**
A HUD overlay that tracks active mining commissions while the player is in the Dwarven Mines,
Glacite Tunnels, or Glacite Mineshaft on Hypixel Skyblock.

**Command:** `/ga` — opens the config screen. `/ga debug` — dumps tab list entries to chat. `/ga commissions` — dumps parsed commissions to chat.

**Mining section options:**
- Commission HUD toggle (ON/OFF pill switch)
- "Edit Position & Size" button → opens `MiningConfigScreen`

**MiningConfigScreen:**
- Full-screen overlay showing live HUD preview
- Drag HUD body to reposition; drag `↘` corner handle to resize (width + height)
- "◀ Back" button and "Reset Position" button (custom styled, no ButtonWidget)
- ESC / Back saves and returns to config screen

**HUD appearance:**
- Rounded rectangle, semi-transparent dark navy background (`0xC8101820`)
- Gold title: `⛏ Commissions`
- Per commission: name + progress % (right-aligned) + progress bar below
- Blue bar (`0xFF2196F3`) for in-progress, green bar (`0xFF00C853`) + green "DONE" for completed
- Auto-expands height to fit all commissions via `computeHeight(count)`
- Only visible in Dwarven Mines, Glacite Tunnels, or Glacite Mineshaft (tab list area detection)

**Data source:**
- Reads `client.networkHandler.playerList` (Hypixel tab list widget)
- Sorted by `scoreboardTeam?.name` then `profile.name` (case-insensitive)
- Area detected via `"Area: <name>"` line in tab list
- Commissions parsed from `"Commissions:"` header (startsWith, case-insensitive) → `"Name: XX%"` or `"Name: DONE"` lines → `"Info"` ends section
- Empty/spacer lines skipped (not treated as end of section)
- Formatting stripped with `§.` regex (catches all formatting codes including hex)
- Safety limit: parser stops after 10 lines past the header

**Files:**
```
src/main/kotlin/com/glorpaddons/
├── GlorpAddons.kt                        ← client entrypoint, registers events & commands
├── commissions/
│   ├── Commission.kt                     ← data model (name, current, total, progress, isDone)
│   ├── CommissionConfig.kt               ← HUD position/size/enabled config data class
│   ├── ConfigManager.kt                  ← load/save config to JSON via Gson
│   ├── CommissionTracker.kt              ← tab list parsing, area detection
│   ├── CommissionHud.kt                  ← HUD rendering via HudElementRegistry
│   ├── ConfigScreen.kt                   ← /ga main config GUI (contains Rect helper class)
│   └── MiningConfigScreen.kt             ← HUD position/size editor screen
└── util/
    └── RenderUtils.kt                    ← drawRoundedRect() using DrawContext.fill() scanlines
```

---

### Feature 2 — Storage Overlay
**Status:** Complete ✓

**Description:**
A full custom UI that replaces the vanilla chest/container screen when the player opens
their Hypixel Skyblock storage. Shows all storage pages as a scrollable grid of thumbnail
boxes, with cached item contents, a search bar, smooth scroll, and a player inventory panel.
Items can be moved between storage and inventory directly from the overlay.

**How it activates:**
- Detects `GenericContainerScreen` with title `"Storage"` → overview mode
- Detects other container screens whose titles match a known `StoragePageSlot` → sub-page mode
- Controlled by the `enabled` toggle in the Inventory section of `/ga`

**Overview panel:**
- Dark rounded panel, centered on screen, above the player inventory panel
- Shows all known storage pages as `3-column × N-row` grid of boxes
- Each box: page icon + name in a title bar, 5×9 item thumbnail grid (rows 1–5 of chest, skipping nav row 0)
- Active page box highlighted; hovering highlights in a lighter shade
- Click a box to navigate to that page (cursor position preserved via `MouseMixin`)
- Smooth scroll with framerate-independent lerp (`k=14`, exponential decay)
- Scrollbar on the right edge of the panel
- Search bar pinned to panel bottom: filters by item name; dims non-matching pages/items

**Sub-page panel:**
- Same layout as overview, with the current page's box highlighted as active
- Clicking items in the active box sends `clickSlot` packets to move items (left/right/shift)
- Clicking items in the player inventory panel also sends `clickSlot` packets

**Player inventory panel:**
- Rendered below the main panel with a small gap
- 3 rows of main inventory (slots 9–35) + divider + hotbar (slots 0–8)
- Interactive on sub-page screens: left/right/shift+click moves items

**Search:**
- Click the search bar or type when it's focused
- ESC clears text or unfocuses; Backspace deletes last char
- Character input via `GLFW.glfwGetKeyName` (no refMap required)
- Pages with zero matches are dimmed; items that don't match within a partially-matching page are dimmed

**Scroll speed setting:**
- Configurable 1–10 via ◀/▶ buttons in the Inventory section of `/ga`
- Persisted in `StorageConfig` alongside the enabled toggle

**Mixin notes:**
- `DrawBackgroundMixin` (`@Mixin(GenericContainerScreen.class)`) — cancels `drawBackground` to suppress the vanilla chest texture
- `GenericContainerScreenMixin` (`@Mixin(Screen.class)`) — cancels `applyBlur` and `renderBackground` (dark gradient overlay)
- `HandledScreenMixin` (`@Mixin(HandledScreen.class)`) — suppresses all vanilla rendering when the overlay is active:
  - At `renderBackground` HEAD: moves all handler slots to `(-10000, -10000)` via `SlotAccessor` so vanilla's `drawSlot` never renders items at visible positions. No `@Shadow` fields — handler accessed by casting `this` to `GenericContainerScreen` and calling `getScreenHandler()`. `@Shadow` was removed because: (a) no refMap is generated/embedded in the jar, (b) `width`/`height` are `Screen` fields (not `HandledScreen`) which Mixin can't resolve without a refMap.
  - Cancels `drawForeground` (Yarn name for the label-drawing method) so "Inventory" and container title text don't bleed through
  - Overrides `isClickOutsideBounds` to return `false` so the screen never closes when clicking inside our overlay panels
- `SlotAccessor` (`@Mixin(Slot.class)`, accessor interface) — generates `setX(int)` / `setY(int)` via `@Accessor` to write the otherwise-final `slot.x` and `slot.y` fields from within `HandledScreenMixin`
- `MouseMixin` — saves/restores GLFW cursor position around `lockCursor`/`unlockCursor` so the cursor doesn't snap to screen center when navigating between pages; also intercepts scroll events during peek chat

**Item interaction:**
- `StorageOverlay.handleClick` uses manual `clickSlot` packets instead of relying on vanilla's slot pipeline:
  - Chest item grid click: computes handler slot index as `9 + row*9 + col` (skipping nav row 0) and calls `interactionManager.clickSlot`
  - Inventory panel click: maps visual row/col to handler index `chestSlots + row*9 + col` (main inv) or `chestSlots + 27 + col` (hotbar)
  - Shift state detected via GLFW directly (`glfwGetKey` for left/right shift) → `SlotActionType.QUICK_MOVE`
  - `allowMouseClick` returns `false` for all interaction areas to consume the event after sending the packet

**Files:**
```
src/main/kotlin/com/glorpaddons/storage/
├── StorageOverlay.kt       ← all rendering, input handling, smooth scroll, search, clickSlot
├── StorageCache.kt         ← page registry, item cache, overview/sub-page state, NBT persistence
├── StoragePageSlot.kt      ← known page definitions, title matching, navigation
├── StoragePage.kt          ← page data model (pageSlot, icon, name)
├── StorageConfig.kt        ← enabled + scrollSpeed (1–10)
└── StorageConfigManager.kt ← load/save StorageConfig via Gson

src/main/java/com/glorpaddons/mixin/
├── DrawBackgroundMixin.java         ← cancels GenericContainerScreen.drawBackground
├── GenericContainerScreenMixin.java ← cancels applyBlur + renderBackground on Screen
├── HandledScreenMixin.java          ← moves all slots off-screen + cancels drawForeground
├── SlotAccessorMixin.java           ← @Accessor mixin giving setX/setY for final Slot fields
└── MouseMixin.java                  ← cursor-reset suppression during page navigation + peek chat scroll
```

---

### Feature 3 — Bat ESP
**Status:** Complete ✓

**Description:**
Renders green wireframe boxes around all bat entities (through walls) and draws a tracer
line from the camera to the nearest bat. Used for finding hidden bats in mining areas.

**Config:** Toggle in the "Mob ESP" section of `/ga`.

**Rendering:**
- Uses a custom `RenderPipeline` with `POSITION_COLOR_SNIPPET`, `NO_DEPTH_TEST`, and `DEBUG_LINES` draw mode
- Renders via `WorldRenderEvents.AFTER_ENTITIES`
- Camera-relative vertex coordinates with sub-tick interpolation via `MathHelper.lerp`
- Tracer starts 0.1 units in front of camera (avoids near-clip) using forward direction from yaw/pitch
- Box edges drawn as 12 individual line segments (4 bottom + 4 top + 4 vertical)

**Files:**
```
src/main/kotlin/com/glorpaddons/mobesp/
├── BatEsp.kt              ← rendering logic, RenderPipeline/RenderLayer setup
├── MobEspConfig.kt        ← batEspEnabled toggle
└── MobEspConfigManager.kt ← load/save via Gson
```

---

### Feature 4 — Item Rarity Backgrounds
**Status:** Complete ✓

**Description:**
Draws colored gradient backgrounds behind items in inventory slots and the hotbar,
matching the item's Skyblock rarity. Colors and sprite pattern match Skyblocker's implementation.

**Config:** Toggle "Rarity Backgrounds" in the "Inventory" section of `/ga`.

**Detection:**
- Reads the last non-empty line of item lore (via `DataComponentTypes.LORE`)
- Matches against rarity keywords (ADMIN, VERY SPECIAL, SPECIAL, DIVINE, MYTHIC, LEGENDARY, EPIC, RARE, UNCOMMON, COMMON, SUPREME, ULTIMATE)
- Uses `indexOf` + word-boundary check to handle recombobulated items (obfuscated prefix)

**Rendering:**
- 5-ring concentric gradient from border (alpha 189) to center (alpha 71), matching Skyblocker's `item_background_square` sprite
- Each pixel painted exactly once (no overdraw / alpha compounding)
- Disabled during storage overlay to prevent visual clutter

**Mixins:**
- `ItemRarityMixin` (`@Mixin(HandledScreen.class)`) — `@Inject` at `drawSlot` HEAD to draw behind container/inventory items
- `InGameHudMixin` — `@Inject` at `renderHotbarItem` HEAD to draw behind hotbar items

**Files:**
```
src/main/kotlin/com/glorpaddons/itemrarity/
├── ItemRarityBg.kt              ← rarity detection + gradient rendering
├── ItemRarityConfig.kt          ← enabled toggle
└── ItemRarityConfigManager.kt   ← load/save via Gson
```

---

### Feature 5 — Equipment Overlay
**Status:** Complete ✓

**Description:**
Shows the player's Hypixel Skyblock equipment (necklace, cloak, belt, gauntlet) in the
vanilla inventory screen. Equipment items are cached from the `/equipment` GUI.

**Config:** Toggle "Show Equipment" in the "Inventory" section of `/ga`.

**How it works:**
- `EquipmentTracker` watches for `GenericContainerScreen` with "equipment" in the title
- Reads items from column 1, rows 1–4 (handler slots 10, 19, 28, 37)
- Caches a snapshot of the 4 equipment items
- `GlorpInventoryScreen` extends `InventoryScreen` to render equipment slots
- `MinecraftClientMixin` uses `@WrapOperation` on `handleInputEvents` to replace the vanilla `InventoryScreen` constructor with `GlorpInventoryScreen` when enabled
- Equipment slots rendered at column x=77 relative to screen, starting at y=8, spaced 18px apart
- Offhand slot shifted 21px right to avoid overlap with 4th equipment row
- Clicking any equipment slot sends `/equipment` command
- Supports rarity backgrounds on equipment items when both features are enabled

**Files:**
```
src/main/kotlin/com/glorpaddons/equipment/
├── EquipmentTracker.kt        ← reads equipment from /equipment GUI, caches snapshot
├── EquipmentConfig.kt         ← showEquipmentInInventory toggle
└── EquipmentConfigManager.kt  ← load/save via Gson

src/main/java/com/glorpaddons/screen/
└── GlorpInventoryScreen.java  ← custom InventoryScreen with equipment column

src/main/java/com/glorpaddons/mixin/
└── MinecraftClientMixin.java  ← replaces InventoryScreen with GlorpInventoryScreen
```

---

### Feature 6 — Cancel Component Update Animations
**Status:** Complete ✓

**Description:**
Suppresses the hand-swap animation that plays when an item's components change but the
item type stays the same (e.g. Hypixel updating lore mid-game).

**Config:** Toggle "Cancel Anim. Updates" in the "Misc" section of `/ga`.

**Mixin:**
- `HeldItemRendererMixin` (`@Mixin(HeldItemRenderer.class)`) — `@Inject` at `shouldSkipHandAnimationOnSwap` RETURN
- When enabled and `from.getItem() == to.getItem()`, returns `true` to skip the animation

**Files:**
```
src/main/kotlin/com/glorpaddons/misc/
├── MiscConfig.kt            ← cancelComponentUpdateAnimations + peekChatEnabled
└── MiscConfigManager.kt     ← load/save via Gson

src/main/java/com/glorpaddons/mixin/
└── HeldItemRendererMixin.java
```

---

### Feature 7 — Peek Chat
**Status:** Complete ✓

**Description:**
Hold Z to temporarily show the chat at full size and full opacity (as if the chat screen
were open), without actually opening the chat screen. Scroll wheel redirects to chat
scrolling while peeking. Releasing Z resets chat scroll position.

**Config:** Toggle "Peek Chat (hold Z)" in the "Misc" section of `/ga`.

**How it works:**
- `PeekChat.tick()` checks GLFW key state for Z key each client tick
- Only activates when no screen is open (`client.currentScreen == null`)
- On release: resets chat scroll via `chatHud.resetScroll()`

**Mixins (3 hooks working together):**
1. `ChatHudMixin` — `@Inject` on `isChatFocused()` RETURN: returns `true` when peeking so `getWidth()`/`getHeight()`/`getVisibleLineCount()` use focused (full-size) dimensions
2. `ChatHudMixin` — `@ModifyVariable` on `render()` HEAD: changes the boolean parameter to `true` when peeking so `forEachVisibleLine` uses full opacity instead of age-based fade
3. `InGameHudMixin` — `@Redirect` on `renderChat()`'s `isChatFocused()` call: returns `false` when peeking to prevent `InGameHud` from skipping HUD chat rendering (vanilla skips when `isChatFocused()` is true, assuming `ChatScreen` handles it)

**Scroll interception:**
- `MouseMixin` — `@Inject` at `onMouseScroll` HEAD: when peeking, redirects scroll to `chatHud.scroll()` with configurable speed multiplier (5x) and cancels the event

**Files:**
```
src/main/kotlin/com/glorpaddons/misc/
├── PeekChat.kt              ← Z key detection, peeking state, scroll reset
├── MiscConfig.kt            ← peekChatEnabled toggle
└── MiscConfigManager.kt     ← load/save via Gson

src/main/java/com/glorpaddons/mixin/
├── ChatHudMixin.java        ← isChatFocused override + render boolean modification
├── InGameHudMixin.java      ← renderChat isChatFocused redirect
└── MouseMixin.java          ← scroll interception during peek
```

---

### Feature 8 — Pest Highlighter
**Status:** Complete ✓

**Description:**
Highlights pest mobs (armor stands with specific skull textures) with a dark red glow
outline, making them visible through blocks. Uses Hypixel Skyblock's pest head textures
sourced from Skyblocker.

**Config:** Toggle "Pest Highlight" in the "Farming" section of `/ga`.

**Detection:**
- Scans armor stands within 32-block range each tick
- Checks helmet item for `PLAYER_HEAD` with a `PROFILE` component
- Matches Base64-encoded texture values against a set of 16 known pest textures
  (Beetle, Cricket, Dragonfly, Earthworm head/tail, Field Mouse, Firefly/flash, Fly, Locust, Mite, Mosquito, Moth, Praying Mantis, Rat, Slug)
- Tracks matching entity IDs in a mutable set

**Rendering:**
- `EntityGlowMixin` — `@Inject` on `isGlowing()` RETURN: returns `true` for pest entities
- `EntityGlowMixin` — `@Inject` on `getTeamColorValue()` RETURN: sets glow color to `0xFFB62F00` (dark red)

**Files:**
```
src/main/kotlin/com/glorpaddons/farming/
├── PestHighlighter.kt        ← texture matching, entity tracking, glow color constant
├── FarmingConfig.kt           ← pestHighlightEnabled toggle
└── FarmingConfigManager.kt    ← load/save via Gson

src/main/java/com/glorpaddons/mixin/
└── EntityGlowMixin.java      ← isGlowing + getTeamColorValue overrides
```

---

## Full File Tree
```
src/main/kotlin/com/glorpaddons/
├── GlorpAddons.kt                          ← entrypoint, config loading, tick registration, commands
├── commissions/
│   ├── Commission.kt
│   ├── CommissionConfig.kt
│   ├── CommissionHud.kt
│   ├── CommissionTracker.kt
│   ├── ConfigManager.kt
│   ├── ConfigScreen.kt                     ← main /ga config GUI
│   └── MiningConfigScreen.kt
├── equipment/
│   ├── EquipmentConfig.kt
│   ├── EquipmentConfigManager.kt
│   └── EquipmentTracker.kt
├── farming/
│   ├── FarmingConfig.kt
│   ├── FarmingConfigManager.kt
│   └── PestHighlighter.kt
├── itemrarity/
│   ├── ItemRarityBg.kt
│   ├── ItemRarityConfig.kt
│   └── ItemRarityConfigManager.kt
├── misc/
│   ├── MiscConfig.kt
│   ├── MiscConfigManager.kt
│   └── PeekChat.kt
├── mobesp/
│   ├── BatEsp.kt
│   ├── MobEspConfig.kt
│   └── MobEspConfigManager.kt
├── storage/
│   ├── StorageCache.kt
│   ├── StorageConfig.kt
│   ├── StorageConfigManager.kt
│   ├── StorageOverlay.kt
│   ├── StoragePage.kt
│   └── StoragePageSlot.kt
└── util/
    └── RenderUtils.kt

src/main/java/com/glorpaddons/
├── mixin/
│   ├── ChatHudMixin.java
│   ├── DrawBackgroundMixin.java
│   ├── EntityGlowMixin.java
│   ├── GenericContainerScreenMixin.java
│   ├── HandledScreenMixin.java
│   ├── HeldItemRendererMixin.java
│   ├── InGameHudMixin.java
│   ├── ItemRarityMixin.java
│   ├── MinecraftClientMixin.java
│   ├── MouseMixin.java
│   └── SlotAccessorMixin.java
└── screen/
    └── GlorpInventoryScreen.java

src/main/resources/
├── glorpaddons.mixins.json
└── assets/glorpaddons/font/
    ├── inter.ttf
    └── inter.json
```

## Mixin Registry (`glorpaddons.mixins.json`)
```json
{
  "client": [
    "ChatHudMixin",
    "DrawBackgroundMixin",
    "EntityGlowMixin",
    "GenericContainerScreenMixin",
    "HandledScreenMixin",
    "HeldItemRendererMixin",
    "InGameHudMixin",
    "ItemRarityMixin",
    "MinecraftClientMixin",
    "MouseMixin",
    "SlotAccessorMixin"
  ]
}
```

## Planned Features
*(none yet — submit ideas!)*
