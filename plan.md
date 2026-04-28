# GlorpAddons — Development Plan

## Project Overview
A Fabric mod for Minecraft 1.21.10, written in Kotlin, targeting Hypixel Skyblock.
Built incrementally — new features are added over time.

## Tech Stack
| Property | Value |
|---|---|
| Minecraft | 1.21.10 |
| Mod Loader | Fabric |
| Language | Kotlin |
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

## Features

### Feature 1 — Mining Commission Tracker
**Status:** Complete ✓

**Description:**
A HUD overlay that tracks active mining commissions while the player is in the Dwarven Mines,
Glacite Tunnels, or Glacite Mineshaft on Hypixel Skyblock.

**Command:** `/ga` — opens the config screen. `/ga debug` — dumps tab list entries to chat.

**Config screen (`/ga`):**
- Square 360×360 panel, rounded edges, semi-transparent dark navy background, Inter font
- Left column (112px): section navigation buttons (gold when active)
- Right content: section-specific options

**Mining section options:**
- Commission HUD toggle (ON/OFF pill switch)
- "Edit Position & Size" button → opens `MiningConfigScreen`

**MiningConfigScreen:**
- Full-screen overlay showing live HUD preview
- Drag HUD body to reposition; drag `↘` corner handle to resize
- "◀ Back" button and "Reset Position" button (custom styled, no ButtonWidget)
- ESC / Back saves and returns to config screen

**HUD appearance:**
- Rounded rectangle, semi-transparent dark navy background (`0xC8101820`)
- Gold title: `⛏ Commissions`
- Per commission: name + progress % (right-aligned) + progress bar below
- Blue bar (`0xFF2196F3`) for in-progress, green bar (`0xFF00C853`) + green "DONE" for completed
- Only visible in Dwarven Mines, Glacite Tunnels, or Glacite Mineshaft (tab list area detection)

**Data source:**
- Reads `client.networkHandler.playerList` (Hypixel tab list widget)
- Sorted by `scoreboardTeam?.name` then `profile.name` (case-insensitive)
- Area detected via `"Area: <name>"` line in tab list
- Commissions parsed from `"Commissions:"` header → `"Name: XX%"` lines → `"Info"` ends section

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

src/main/resources/assets/glorpaddons/font/
├── inter.ttf                             ← Inter Regular (open-source Arial equivalent)
└── inter.json                            ← font provider (TTF, size 10, oversample 4)
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
- `MouseMixin` — saves/restores GLFW cursor position around `lockCursor`/`unlockCursor` so the cursor doesn't snap to screen center when navigating between pages

**Item interaction:**
- `StorageOverlay.handleClick` uses manual `clickSlot` packets instead of relying on vanilla's slot pipeline:
  - Chest item grid click: computes handler slot index as `9 + row*9 + col` (skipping nav row 0) and calls `interactionManager.clickSlot`
  - Inventory panel click: maps visual row/col to handler index `chestSlots + row*9 + col` (main inv) or `chestSlots + 27 + col` (hotbar)
  - Shift state detected via GLFW directly (`glfwGetKey` for left/right shift) → `SlotActionType.QUICK_MOVE`
  - `allowMouseClick` returns `false` for all interaction areas to consume the event after sending the packet

**Files:**
```
src/main/kotlin/com/glorpaddons/
├── storage/
│   ├── StorageOverlay.kt       ← all rendering, input handling, smooth scroll, search, clickSlot
│   ├── StorageCache.kt         ← page registry, item cache, overview/sub-page state
│   ├── StoragePageSlot.kt      ← known page definitions, title matching, navigation
│   ├── StorageConfig.kt        ← enabled + scrollSpeed (1–10)
│   └── StorageConfigManager.kt ← load/save StorageConfig via Gson

src/main/java/com/glorpaddons/mixin/
├── DrawBackgroundMixin.java         ← cancels GenericContainerScreen.drawBackground
├── GenericContainerScreenMixin.java ← cancels applyBlur + renderBackground on Screen
├── HandledScreenMixin.java          ← moves all slots off-screen + cancels drawForeground
├── SlotAccessor.java                ← @Accessor mixin giving setX/setY for final Slot fields
└── MouseMixin.java                  ← cursor-reset suppression during page navigation
```

**Known limitations / future work:**
- Items dropped onto the overlay outside the active grid are not handled (no drag support)
- Double-click to collect all is not implemented
- The overview screen's thumbnail items cannot be interacted with (by design — navigate first)

---

## Planned Features
*(none yet — submit ideas!)*
