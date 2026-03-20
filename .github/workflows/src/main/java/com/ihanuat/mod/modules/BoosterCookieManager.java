package com.ihanuat.mod.modules;

import com.ihanuat.mod.IhanuatClient;
import com.ihanuat.mod.MacroConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BoosterCookieManager {
    public static volatile long interactionTime = 0;
    public static volatile int interactionStage = 0;

    /**
     * Tracks whether we have already armed the stash for this autosell session.
     * Prevents arming multiple times if the menu stays open with no items.
     */
    private static volatile boolean stashArmedThisCycle = false;

    public static void reset() {
        interactionTime = 0;
        interactionStage = 0;
        stashArmedThisCycle = false;
    }

    public static void handleBoosterCookieMenu(Minecraft client, AbstractContainerScreen<?> screen) {
        if (!MacroConfig.autoBoosterCookie || screen == null || client.player == null)
            return;

        long now = System.currentTimeMillis();
        if (now - interactionTime < MacroConfig.getRandomizedDelay(MacroConfig.autosellClickDelay))
            return;

        String title = screen.getTitle().getString();
        String lowerTitle = title.toLowerCase();

        if (!lowerTitle.equals("booster cookie"))
            return;

        // Stage 0: Search inventory and click matching items
        if (interactionStage == 0) {
            int foundSlotIdx = -1;

            int totalSlots = screen.getMenu().slots.size();
            int inventoryStart = totalSlots - 36;

            for (int i = inventoryStart; i < totalSlots; i++) {
                Slot slot = screen.getMenu().slots.get(i);
                if (!slot.hasItem())
                    continue;

                ItemStack stack = slot.getItem();
                String name = stack.getHoverName().getString().replaceAll("(?i)§.", "").toLowerCase();

                for (String target : MacroConfig.boosterCookieItems) {
                    if (target.isBlank()) continue;
                    if (name.contains(target.toLowerCase())) {
                        foundSlotIdx = i;
                        break;
                    }
                }
                if (foundSlotIdx != -1)
                    break;
            }

            if (foundSlotIdx != -1) {
                // Found a sellable item — sell it
                client.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, foundSlotIdx, 0,
                        ClickType.QUICK_MOVE, client.player);
                interactionTime = now;
                stashArmedThisCycle = false; // still selling, reset arm guard
            } else {
                // No matching items remain — autosell is complete for this cycle.
                // Arm stash pickup once per autosell session.
                if (!stashArmedThisCycle) {
                    stashArmedThisCycle = true;
                    IhanuatClient.armStashPickupAfterAutosell();
                }
            }
        }
    }

    /** Called when the booster cookie menu is closed so the next open resets the arm guard. */
    public static void onMenuClosed() {
        stashArmedThisCycle = false;
    }
}
