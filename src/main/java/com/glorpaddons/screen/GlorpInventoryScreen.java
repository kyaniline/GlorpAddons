package com.glorpaddons.screen;

import com.glorpaddons.equipment.EquipmentTracker;
import com.glorpaddons.itemrarity.ItemRarityBg;
import com.glorpaddons.itemrarity.ItemRarityConfigManager;
import com.glorpaddons.mixin.SlotAccessorMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

/**
 * Extends the vanilla inventory screen to display the player's Hypixel equipment
 * (necklace, cloak, belt, gauntlet) in a column at the left of the screen.
 *
 * Equipment items are read from {@link EquipmentTracker}, which watches the
 * Hypixel /equipment container screen and caches the four items.  The offhand
 * slot is shifted 21 px right so the 4th equipment row doesn't overlap it.
 *
 * Clicking any equipment slot sends /equipment.
 */
public class GlorpInventoryScreen extends InventoryScreen {

    private static final Identifier SLOT_TEXTURE = Identifier.ofVanilla("container/slot");

    /** Relative to this.x / this.y (i.e. leftPos / topPos). */
    private static final int EQUIP_X       = 77;
    private static final int EQUIP_START_Y = 8;
    private static final int EQUIP_SPACING = 18;
    private static final int EQUIP_COUNT   = 4;

    /** Index of the offhand slot inside PlayerScreenHandler.slots. */
    private static final int OFFHAND_SLOT  = 45;
    private static final int OFFHAND_SHIFT = 21;

    private final SimpleInventory equipInventory;
    private boolean offhandMoved = false;

    public GlorpInventoryScreen(PlayerEntity player) {
        super(player);
        ItemStack[] snap = EquipmentTracker.INSTANCE.snapshot();
        this.equipInventory = new SimpleInventory(snap[0], snap[1], snap[2], snap[3]);
    }

    // ── Lifecycle ──────────────────────────────────────────────────────────

    @Override
    protected void init() {
        super.init();
        // Move the offhand slot right to make room for the 4th equipment row.
        // Guard against being called multiple times on window resize.
        if (!offhandMoved) {
            Slot offhand = handler.slots.get(OFFHAND_SLOT);
            ((SlotAccessorMixin) offhand).setX(offhand.x + OFFHAND_SHIFT);
            offhandMoved = true;
        }
    }

    @Override
    public void removed() {
        super.removed();
        Slot offhand = handler.slots.get(OFFHAND_SLOT);
        ((SlotAccessorMixin) offhand).setX(offhand.x - OFFHAND_SHIFT);
    }

    // ── Rendering ──────────────────────────────────────────────────────────

    /** Draw the 18×18 slot background sprites behind each equipment cell. */
    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        super.drawBackground(context, delta, mouseX, mouseY);
        for (int i = 0; i < EQUIP_COUNT; i++) {
            context.drawGuiTexture(
                    RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE,
                    x + EQUIP_X - 1, y + EQUIP_START_Y + i * EQUIP_SPACING - 1,
                    18, 18);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        boolean rarityEnabled = ItemRarityConfigManager.INSTANCE.getConfig().getEnabled();

        for (int i = 0; i < EQUIP_COUNT; i++) {
            int sx = x + EQUIP_X;
            int sy = y + EQUIP_START_Y + i * EQUIP_SPACING;
            boolean hovered = mouseX >= sx && mouseX < sx + 16
                           && mouseY >= sy && mouseY < sy + 16;

            ItemStack stack = equipInventory.getStack(i);

            if (rarityEnabled && !stack.isEmpty()) {
                Integer color = ItemRarityBg.INSTANCE.getRarityColor(stack);
                if (color != null) {
                    ItemRarityBg.INSTANCE.drawBackground(context, sx, sy, color);
                }
            }

            if (hovered) {
                context.fill(sx - 1, sy - 1, sx + 17, sy + 17, 0x80FFFFFF);
            }

            if (!stack.isEmpty()) {
                context.drawItem(stack, sx, sy);
                context.drawStackOverlay(textRenderer, stack, sx, sy);
            }
        }
    }

    /** Append tooltip for hovered equipment slots after vanilla tooltip logic. */
    @Override
    protected void drawMouseoverTooltip(DrawContext context, int mouseX, int mouseY) {
        super.drawMouseoverTooltip(context, mouseX, mouseY);
        if (!handler.getCursorStack().isEmpty()) return;

        for (int i = 0; i < EQUIP_COUNT; i++) {
            int sx = x + EQUIP_X;
            int sy = y + EQUIP_START_Y + i * EQUIP_SPACING;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
                ItemStack stack = equipInventory.getStack(i);
                if (!stack.isEmpty()) {
                    context.drawItemTooltip(textRenderer, stack, mouseX, mouseY);
                }
                return;
            }
        }
    }

    // ── Input ──────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int mx = (int) click.x();
        int my = (int) click.y();
        for (int i = 0; i < EQUIP_COUNT; i++) {
            int sx = x + EQUIP_X;
            int sy = y + EQUIP_START_Y + i * EQUIP_SPACING;
            if (mx >= sx && mx < sx + 16 && my >= sy && my < sy + 16) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    client.player.networkHandler.sendChatCommand("equipment");
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }
}
