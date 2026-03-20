package com.ihanuat.mod.modules;

import com.ihanuat.mod.MacroConfig;
import com.ihanuat.mod.MacroWorkerThread;
import com.ihanuat.mod.util.ClientUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class PestBonusManager {
    public static volatile boolean isBonusInactive = false;
    public static volatile boolean isReactivatingBonus = false;

    public static void resetState() {
        isBonusInactive = false;
        isReactivatingBonus = false;
    }

    public static void handlePhillipMessage(Minecraft client, String text, String currentInfestedPlot) {
        if (!isReactivatingBonus || client.player == null)
            return;

        String plain = text.replaceAll("(?i)§[0-9a-fk-or]", "").trim();
        if (plain.toLowerCase().contains("pesthunter phillip") && plain.toLowerCase().contains("thanks for the")) {
            client.player.displayClientMessage(Component.literal(
                    "§aPhillip message detected! Returning to plot §e" + currentInfestedPlot + "..."), true);
            MacroWorkerThread.getInstance().submit("PhillipReactivation", () -> {
                try {
                    if (MacroWorkerThread.shouldAbortTask(client))
                        return;
                    ClientUtils.sendDebugMessage(client,
                            "Stopping script: Phillip message detected, reactivating bonus");
                    com.ihanuat.mod.util.CommandUtils.stopScript(client, MacroConfig.getRandomizedDelay(250));
                    if (MacroWorkerThread.shouldAbortTask(client))
                        return;
                    ClientUtils.sendDebugMessage(client, "Teleporting back to plot " + currentInfestedPlot);
                    com.ihanuat.mod.util.CommandUtils.plotTp(client, currentInfestedPlot);
                    if (MacroWorkerThread.shouldAbortTask(client))
                        return;
                    ClientUtils.sendDebugMessage(client, "Starting pest cleaner script after Phillip message");
                    com.ihanuat.mod.util.CommandUtils.startScript(client, ".ez-startscript misc:pestCleaner", 250);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    isReactivatingBonus = false;
                }
            });
        }
    }
}
