package com.ihanuat.mod.modules;

import com.ihanuat.mod.MacroConfig;
import com.ihanuat.mod.MacroWorkerThread;
import com.ihanuat.mod.util.ClientUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class WardrobeManager {
    public static volatile boolean isSwappingWardrobe = false;
    public static volatile long wardrobeInteractionTime = 0;
    public static volatile int wardrobeInteractionStage = 0;
    public static volatile int wardrobeCleanupTicks = 0;
    public static volatile int trackedWardrobeSlot = -1;
    public static volatile int targetWardrobeSlot = -1;
    public static volatile boolean shouldRestartFarmingAfterSwap = false;
    public static volatile long wardrobeOpenPendingTime = 0;
    public static volatile boolean wardrobeGuiDetected = false;
    public static volatile boolean wardrobeDataLoaded = false;

    public static void resetState() {
        isSwappingWardrobe = false;
        shouldRestartFarmingAfterSwap = false;
        wardrobeCleanupTicks = 0;
        trackedWardrobeSlot = -1;
        targetWardrobeSlot = -1;
        wardrobeInteractionTime = 0;
        wardrobeInteractionStage = 0;
        wardrobeGuiDetected = false;
        wardrobeDataLoaded = false;
        wardrobeOpenPendingTime = 0;
    }

    public static void triggerWardrobeSwap(Minecraft client, int slot) {
        if (trackedWardrobeSlot == slot) {
            ClientUtils.sendDebugMessage(client, "Stopping script: Wardrobe already on target slot, restarting");
            com.ihanuat.mod.util.CommandUtils.stopScript(client, 0);
            MacroWorkerThread.getInstance().submit("Wardrobe-AlreadyOnSlot-FastResume", () -> {
                if (MacroWorkerThread.shouldAbortTask(client, com.ihanuat.mod.MacroState.State.FARMING))
                    return;
                MacroWorkerThread.sleep(400);
                if (MacroWorkerThread.shouldAbortTask(client, com.ihanuat.mod.MacroState.State.FARMING))
                    return;
                client.execute(() -> GearManager.swapToFarmingTool(client));
                MacroWorkerThread.sleep(250);
                if (MacroWorkerThread.shouldAbortTask(client, com.ihanuat.mod.MacroState.State.FARMING))
                    return;
                ClientUtils.sendDebugMessage(client,
                        "Starting farming script after wardrobe swap: " + MacroConfig.getFullRestartCommand());
                com.ihanuat.mod.util.CommandUtils.startScript(client, MacroConfig.getFullRestartCommand(), 0);
            });
            return;
        }

        targetWardrobeSlot = slot;
        isSwappingWardrobe = true;
        if (EquipmentManager.isSwappingEquipment) {
            EquipmentManager.isSwappingEquipment = false;
            ClientUtils.sendDebugMessage(client, "§eInterrupted equipment swap for wardrobe priority.");
        }
        wardrobeGuiDetected = false;
        wardrobeDataLoaded = false;
        wardrobeInteractionTime = 0;
        wardrobeInteractionStage = 0;
        shouldRestartFarmingAfterSwap = true;
        ClientUtils.sendDebugMessage(client, "Stopping script: Triggering wardrobe swap to slot " + slot);
        com.ihanuat.mod.util.CommandUtils.stopScript(client, 0);
        MacroWorkerThread.getInstance().submit("Wardrobe-OpenGui", () -> {
            if (MacroWorkerThread.shouldAbortTask(client))
                return;
            MacroWorkerThread.sleep(375);
            if (MacroWorkerThread.shouldAbortTask(client))
                return;
            client.execute(() -> ClientUtils.sendCommand(client, "/wardrobe"));
            ClientUtils.waitForWardrobeGui(client);
        });
    }

    public static void ensureWardrobeSlot(Minecraft client, int slot) {
        if (trackedWardrobeSlot == slot)
            return;
        targetWardrobeSlot = slot;
        isSwappingWardrobe = true;
        if (EquipmentManager.isSwappingEquipment) {
            EquipmentManager.isSwappingEquipment = false;
            ClientUtils.sendDebugMessage(client, "§eInterrupted equipment swap for wardrobe priority.");
        }
        wardrobeGuiDetected = false;
        wardrobeDataLoaded = false;
        wardrobeInteractionTime = 0;
        wardrobeInteractionStage = 0;
        ClientUtils.sendCommand(client, "/wardrobe");
    }

    public static void handleWardrobeMenu(Minecraft client, AbstractContainerScreen<?> screen) {
        if (!isSwappingWardrobe || targetWardrobeSlot == -1)
            return;

        String title = screen.getTitle().getString().toLowerCase();
        if (!title.contains("wardrobe"))
            return;

        // Detect GUI immediately by title (works for high ping users)
        if (!wardrobeGuiDetected) {
            wardrobeGuiDetected = true;
            ClientUtils.sendDebugMessage(client, "§6[Wardrobe Debug] GUI detected by title. Waiting for slot data to load...");
        }

        long now = System.currentTimeMillis();

        int slotIdx = 35 + targetWardrobeSlot;
        if (slotIdx >= screen.getMenu().slots.size()) {
            return;
        }

        Slot slot = screen.getMenu().slots.get(slotIdx);
        ItemStack stack = slot.getItem();

        // Wait for slot data to load (high ping tolerance)
        if (stack.isEmpty() || stack.getItem().toString().toLowerCase().contains("air")
                || stack.getItem().toString().toLowerCase().contains("gray_dye")
                || stack.getHoverName().getString().toLowerCase().contains("gray dye")) {
            return; // Silently wait for data to load
        }

        // Data loaded! Mark it and start interaction timer
        if (!wardrobeDataLoaded) {
            wardrobeDataLoaded = true;
            wardrobeInteractionTime = now;
            ClientUtils.sendDebugMessage(client, "§6[Wardrobe Debug] Slot data LOADED. Preparing to interact with slot " + targetWardrobeSlot);
        }

        // Respect GUI click delay before any interaction
        if (now - wardrobeInteractionTime < MacroConfig.getRandomizedDelay(MacroConfig.guiClickDelay))
            return;

        if (wardrobeInteractionStage == 0) {
            String itemName = stack.getItem().toString().toLowerCase();
            String hoverName = stack.getHoverName().getString().toLowerCase();

            if (itemName.contains("green_dye") || hoverName.contains("green dye") || itemName.contains("lime_dye")
                    || hoverName.contains("lime dye")) {
                client.player.displayClientMessage(
                        Component.literal("§aWardrobe Slot " + targetWardrobeSlot + " is already active."), true);
                trackedWardrobeSlot = targetWardrobeSlot;
                isSwappingWardrobe = false;
                client.player.closeContainer();
                ClientUtils.sendDebugMessage(client, "§6[Wardrobe Debug] Slot " + targetWardrobeSlot + " was already active. Skipping swap.");
                handleWardrobeCompletion(client);
                return;
            }

            ClientUtils.sendDebugMessage(client, "§6[Wardrobe Debug] Clicking slot " + targetWardrobeSlot + " (item: " + stack.getHoverName().getString() + ")");
            client.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slot.index, 0, ClickType.PICKUP,
                    client.player);
            wardrobeInteractionTime = now;
            wardrobeInteractionStage = 1;
        } else if (wardrobeInteractionStage == 1) {
            long lastClickElapsed = now - wardrobeInteractionTime;
            if (lastClickElapsed < 150)
                return;

            int confirmSlotIdx = 35 + targetWardrobeSlot;
            if (confirmSlotIdx >= screen.getMenu().slots.size())
                return;

            ItemStack confirmStack = screen.getMenu().slots.get(confirmSlotIdx).getItem();
            if (confirmStack.isEmpty())
                return;

            String itemName = confirmStack.getItem().toString().toLowerCase();
            String hoverName = confirmStack.getHoverName().getString().toLowerCase();

            if (itemName.contains("green_dye") || hoverName.contains("green dye") || itemName.contains("lime_dye")
                    || hoverName.contains("lime dye")) {
                ClientUtils.sendDebugMessage(client, "§aWardrobe swap detected (green dye visible). Target slot: "
                        + targetWardrobeSlot + ", Confirmed active slot: " + targetWardrobeSlot);
                trackedWardrobeSlot = targetWardrobeSlot;
                isSwappingWardrobe = false;
                client.player.closeContainer();
                // Add small delay to ensure server receives the close packet and swap confirmation
                wardrobeInteractionTime = now;
                wardrobeInteractionStage = 2;
            }
        } else if (wardrobeInteractionStage == 2) {
            long lastClickElapsed = now - wardrobeInteractionTime;
            if (lastClickElapsed < 250)
                return;
            // Final validation: confirm swap completed and trigger farming restart
            ClientUtils.sendDebugMessage(client, "§aWARDROBE SWAP COMPLETE: Active slot is now " + trackedWardrobeSlot
                    + " (target was " + targetWardrobeSlot + ")");
            handleWardrobeCompletion(client);
            wardrobeInteractionStage = 0;
        }
    }

    private static void handleWardrobeCompletion(Minecraft client) {
        if (shouldRestartFarmingAfterSwap) {
            shouldRestartFarmingAfterSwap = false;

            if (PestManager.isCleaningInProgress) {
                client.player.displayClientMessage(
                        Component.literal("§aWardrobe swap finished. Cleaning in progress, skipping restart."), true);
                return;
            }

            client.player.displayClientMessage(Component.literal("§aWardrobe swap finished. Restarting farming..."),
                    true);
            client.execute(() -> GearManager.swapToFarmingTool(client));
            MacroWorkerThread.getInstance().submit("WardrobeCompletion-Resume", () -> {
                if (MacroWorkerThread.shouldAbortTask(client, com.ihanuat.mod.MacroState.State.FARMING))
                    return;
                if (PestManager.isCleaningInProgress)
                    return;

                GearManager.finalResume(client);
            });
        }
    }

    /**
     * Force-completes the wardrobe swap when detection fails. Used as a failsafe
     * when the detection system doesn't properly verify swap completion.
     */
    public static void forceWardrobeCompletionFailsafe(Minecraft client) {
        if (isSwappingWardrobe && shouldRestartFarmingAfterSwap) {
            ClientUtils.sendDebugMessage(client,
                    "§eWardrobe swap failsafe triggered: forcing completion and resuming farming");
            trackedWardrobeSlot = targetWardrobeSlot;
            isSwappingWardrobe = false;
            wardrobeGuiDetected = false;
            wardrobeDataLoaded = false;
            handleWardrobeCompletion(client);
        }
    }
}

