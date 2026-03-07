package com.ihanuat.mod.modules;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;

public class PestTabListParser {
    private static final Pattern PESTS_ALIVE_PATTERN = Pattern.compile("(?i)(?:Pests|Alive):?\\s*\\(?(\\d+)\\)?");
    private static final Pattern COOLDOWN_PATTERN = Pattern
            .compile("(?i)Cooldown:\\s*\\(?(READY|MAX\\s*PESTS?|(?:(\\d+)m)?\\s*(?:(\\d+)s)?)\\)?");

    public static class TabListData {
        public int aliveCount = -1;
        public int cooldownSeconds = -1;
        public boolean bonusFound = false;
        public Set<String> infestedPlots = new HashSet<>();
    }

    public static TabListData parseTabList(Minecraft client) {
        TabListData data = new TabListData();
        
        if (client.getConnection() == null || client.player == null)
            return data;

        Collection<PlayerInfo> players = client.getConnection().getListedOnlinePlayers();

        for (PlayerInfo info : players) {
            String name = "";
            if (info.getTabListDisplayName() != null) {
                name = info.getTabListDisplayName().getString();
            } else if (info.getProfile() != null) {
                name = String.valueOf(info.getProfile());
            }

            String clean = name.replaceAll("(?i)\u00A7[0-9a-fk-or]", "").trim();
            // Replace non-breaking spaces with normal spaces for easier matching
            String normalized = clean.replace('\u00A0', ' ');

            // Parse alive count
            Matcher aliveMatcher = PESTS_ALIVE_PATTERN.matcher(normalized);
            if (aliveMatcher.find()) {
                int found = Integer.parseInt(aliveMatcher.group(1));
                if (found > data.aliveCount)
                    data.aliveCount = found;
            }

            if (normalized.toUpperCase().contains("MAX PESTS")) {
                data.aliveCount = 99; // Explicitly high count to ensure threshold is met
            }

            // Parse cooldown
            Matcher cooldownMatcher = COOLDOWN_PATTERN.matcher(normalized);
            if (cooldownMatcher.find()) {
                String cdVal = cooldownMatcher.group(1).toUpperCase();

                if (cdVal.contains("MAX PEST")) {
                    data.aliveCount = 99; // Treat as max threshold met
                    data.cooldownSeconds = 999; // High cooldown value to avoid prep-swap during max state
                } else if (cdVal.equalsIgnoreCase("READY")) {
                    data.cooldownSeconds = 0;
                } else {
                    int m = 0;
                    int s = 0;
                    if (cooldownMatcher.group(2) != null)
                        m = Integer.parseInt(cooldownMatcher.group(2));
                    if (cooldownMatcher.group(3) != null)
                        s = Integer.parseInt(cooldownMatcher.group(3));

                    if (m > 0 || s > 0) {
                        data.cooldownSeconds = (m * 60) + s;
                    }
                }
            }

            // Parse infested plots
            if (normalized.contains("Plot")) {
                Matcher m = Pattern.compile("(\\d+)").matcher(normalized);
                while (m.find()) {
                    data.infestedPlots.add(m.group(1).trim());
                }
            }

            // Check bonus status
            if (normalized.toUpperCase().contains("BONUS: INACTIVE")) {
                data.bonusFound = true;
            }
        }

        return data;
    }
}
