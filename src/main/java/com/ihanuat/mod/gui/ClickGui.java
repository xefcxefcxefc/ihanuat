package com.ihanuat.mod.gui;

import com.ihanuat.mod.MacroConfig;
import com.ihanuat.mod.MacroStateManager;
import com.ihanuat.mod.modules.ProfitManager;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ClickGui extends Screen {

    private static final int COL_PANEL_BG    = 0xF0101018;
    private static final int COL_HEADER      = 0xFF18182C;
    private static final int COL_HEADER_LINE = 0xFF4444AA;
    private static final int COL_TEXT        = 0xFFCCCCCC;
    private static final int COL_TEXT_DIM    = 0xFF666677;
    private static final int COL_ON          = 0xFF4444BB;
    private static final int COL_ON_BRIGHT   = 0xFF6666DD;
    private static final int COL_OFF         = 0xFF2A2A3A;
    private static final int COL_HOVER       = 0xFF1C1C2E;
    private static final int COL_SLIDER_BG   = 0xFF151524;
    private static final int COL_SLIDER_FILL = 0xFF3A3A99;
    private static final int COL_SLIDER_KNOB = 0xFF7777CC;
    private static final int COL_SUBPANEL_BG = 0xF0141428;
    private static final int COL_SUBPANEL_BD = 0xFF333366;
    private static final int COL_ACCENT      = 0xFF5050A0;

    private static final int PANEL_W      = 180;
    private static final int HEADER_H     = 18;
    private static final int ENTRY_H      = 18;
    private static final int ENTRY_PAD    = 3;
    private static final int PANEL_RADIUS = 4;

    private boolean shiftHeld = false; // tracked via key events for non-scroll shift detection

    private final List<Panel> panels = new ArrayList<>();
    private SubPanel activeSubPanel = null;
    private Panel draggingPanel = null;
    private int dragOffX, dragOffY;
    private SliderEntry draggingSlider = null;
    private Panel draggingSliderPanel = null;

    public ClickGui() {
        super(Component.literal("ihanuat"));
    }

    @Override
    protected void init() {
        panels.clear();
        buildPanels();

        ScreenMouseEvents.allowMouseClick(this).register((scr, event) -> {
            handleMouseClicked((int)event.x(), (int)event.y(), event.button());
            return true;
        });
        ScreenMouseEvents.allowMouseRelease(this).register((scr, event) -> {
            handleMouseReleased();
            return true;
        });
        ScreenMouseEvents.allowMouseDrag(this).register((scr, event, dx, dy) -> {
            handleMouseDragged((int)event.x(), (int)event.y());
            return true;
        });
        ScreenMouseEvents.allowMouseScroll(this).register((scr, mx, my, hScroll, vScroll) -> {
            boolean shift = shiftHeld;
            handleMouseScrolled((int)mx, (int)my, hScroll, vScroll, shift);
            return true;
        });
        ScreenKeyboardEvents.allowKeyPress(this).register((scr, event) -> {
            if ((event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0) shiftHeld = true;
            handleKeyPressed(event.key(), event.scancode(), event.modifiers());
            return true;
        });
        ScreenKeyboardEvents.allowKeyRelease(this).register((scr, event) -> {
            if (event.key() == 340 || event.key() == 344) shiftHeld = false;
            handleKeyReleased(event.key());
            return true;
        });
    }

    public boolean charTyped(char c, int mods) {
        handleCharTyped(c, mods);
        return true;
    }

    private void buildPanels() {
        int startX = 4, startY = 4;
        int[] idx = {0};
        Supplier<int[]> nextPos = () -> {
            int i = idx[0]++;
            return new int[]{startX, startY + i * (HEADER_H + 1)};
        };
        int[][] saved = MacroConfig.clickGuiPanelPositions;
        panels.add(generalPanel(pos(saved, 0, nextPos)));
        panels.add(delaysPanel(pos(saved, 1, nextPos)));
        panels.add(wardrobePanel(pos(saved, 2, nextPos)));
        panels.add(autoRodPanel(pos(saved, 3, nextPos)));
        panels.add(equipmentPanel(pos(saved, 4, nextPos)));
        panels.add(autoPestPanel(pos(saved, 5, nextPos)));
        panels.add(autoVisitorPanel(pos(saved, 6, nextPos)));
        panels.add(autoGeorgePanel(pos(saved, 7, nextPos)));
        panels.add(autoSellPanel(pos(saved, 8, nextPos)));
        panels.add(profitPanel(pos(saved, 9, nextPos)));
        panels.add(dynamicRestPanel(pos(saved, 10, nextPos)));
        panels.add(qolPanel(pos(saved, 11, nextPos)));
    }

    private static int[] pos(int[][] saved, int i, Supplier<int[]> fallback) {
        if (saved != null && i < saved.length && saved[i] != null && saved[i].length == 2
                && (saved[i][0] != 0 || saved[i][1] != 0)) return saved[i];
        return fallback.get();
    }

    // panel builders

    private Panel generalPanel(int[] pos) {
        Panel p = new Panel("General", pos[0], pos[1]);
        p.add(toggle("Show Macro HUD",           () -> MacroConfig.showHud,                        v -> { MacroConfig.showHud = v; save(); }));
        p.add(toggle("GUI Only in Garden",        () -> MacroConfig.guiOnlyInGarden,                v -> { MacroConfig.guiOnlyInGarden = v; save(); }));
        p.add(toggle("Enable PlotTP Rewarp",      () -> MacroConfig.enablePlotTpRewarp,             v -> { MacroConfig.enablePlotTpRewarp = v; save(); }));
        p.add(toggle("Hold W Until Wall",         () -> MacroConfig.holdWUntilWall,                 v -> { MacroConfig.holdWUntilWall = v; save(); }));
        p.add(cycleEnum("Unfly Mode", MacroConfig.UnflyMode.values(), () -> MacroConfig.unflyMode,  v -> { MacroConfig.unflyMode = v; save(); }));
        p.add(textSetting("Farm Script",          () -> MacroConfig.restartScript,                  v -> { MacroConfig.restartScript = v; save(); }));
        p.add(textSetting("PlotTP Number",        () -> MacroConfig.plotTpNumber,                   v -> { MacroConfig.plotTpNumber = v; save(); }));
        p.add(button("Capture Rewarp Pos", () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                MacroConfig.rewarpEndX = mc.player.getX();
                MacroConfig.rewarpEndY = mc.player.getY();
                MacroConfig.rewarpEndZ = mc.player.getZ();
                MacroConfig.rewarpEndPosSet = true;
                save();
                mc.player.displayClientMessage(Component.literal("Rewarp End Position captured!"), true);
            }
        }));
        p.add(toggle("Auto-Resume After Rest",    () -> MacroConfig.autoResumeAfterDynamicRest,     v -> { MacroConfig.autoResumeAfterDynamicRest = v; save(); }));
        p.add(toggle("Auto-Recover Disconnect",   () -> MacroConfig.autoRecoverUnexpectedDisconnect,v -> { MacroConfig.autoRecoverUnexpectedDisconnect = v; save(); }));
        p.add(toggle("Persist Session Timer",     () -> MacroConfig.persistSessionTimer,            v -> { MacroConfig.persistSessionTimer = v; save(); }));
        return p;
    }

    private Panel delaysPanel(int[] pos) {
        Panel p = new Panel("Delays", pos[0], pos[1]);
        p.add(slider("Additional Random Delay", 0,   1000, () -> MacroConfig.additionalRandomDelay, v -> { MacroConfig.additionalRandomDelay = v; save(); }, "ms"));
        p.add(slider("Rotation Time",           100, 3000, () -> MacroConfig.rotationTime,          v -> { MacroConfig.rotationTime = v; save(); }, "ms"));
        p.add(slider("GUI Click Delay",         100, 2000, () -> MacroConfig.guiClickDelay,         v -> { MacroConfig.guiClickDelay = v; save(); }, "ms"));
        p.add(slider("Equipment Swap Delay",    100, 300,  () -> MacroConfig.equipmentSwapDelay,    v -> { MacroConfig.equipmentSwapDelay = v; save(); }, "ms"));
        p.add(slider("Rod Swap Delay",          50,  1000, () -> MacroConfig.rodSwapDelay,          v -> { MacroConfig.rodSwapDelay = v; save(); }, "ms"));
        p.add(slider("Garden Warp Delay",       0,   3000, () -> MacroConfig.gardenWarpDelay,       v -> { MacroConfig.gardenWarpDelay = v; save(); }, "ms"));
        p.add(slider("Pest Chat Trigger Delay", 0,   3000, () -> MacroConfig.pestChatTriggerDelay,  v -> { MacroConfig.pestChatTriggerDelay = v; save(); }, "ms"));
        p.add(slider("Book Combine Delay",      100, 2000, () -> MacroConfig.bookCombineDelay,      v -> { MacroConfig.bookCombineDelay = v; save(); }, "ms"));
        p.add(slider("Junk Drop Delay",         0,   1000, () -> MacroConfig.junkItemDropDelay,     v -> { MacroConfig.junkItemDropDelay = v; save(); }, "ms"));
        return p;
    }

    private Panel wardrobePanel(int[] pos) {
        Panel p = new Panel("Wardrobe Swap", pos[0], pos[1]);
        p.add(toggle("Auto Wardrobe (Pest)",    () -> MacroConfig.autoWardrobePest,    v -> { MacroConfig.autoWardrobePest = v; save(); }));
        p.add(toggle("Auto Wardrobe (Visitor)", () -> MacroConfig.autoWardrobeVisitor, v -> { MacroConfig.autoWardrobeVisitor = v; save(); }));
        p.add(toggle("Armor Swap (Visitor)",    () -> MacroConfig.armorSwapVisitor,    v -> { MacroConfig.armorSwapVisitor = v; save(); }));
        p.add(slider("Slot: Farming", 1, 9, () -> MacroConfig.wardrobeSlotFarming, v -> { MacroConfig.wardrobeSlotFarming = v; save(); }, ""));
        p.add(slider("Slot: Pest",    1, 9, () -> MacroConfig.wardrobeSlotPest,    v -> { MacroConfig.wardrobeSlotPest = v; save(); }, ""));
        p.add(slider("Slot: Visitor", 1, 9, () -> MacroConfig.wardrobeSlotVisitor, v -> { MacroConfig.wardrobeSlotVisitor = v; save(); }, ""));
        return p;
    }

    private Panel autoRodPanel(int[] pos) {
        Panel p = new Panel("Auto Rod", pos[0], pos[1]);
        p.add(toggle("Rod on Pest CD",        () -> MacroConfig.autoRodPestCd,       v -> { MacroConfig.autoRodPestCd = v; save(); }));
        p.add(toggle("Rod on Pest Spawn",     () -> MacroConfig.autoRodPestSpawn,    v -> { MacroConfig.autoRodPestSpawn = v; save(); }));
        p.add(toggle("Rod on Return to Farm", () -> MacroConfig.autoRodReturnToFarm, v -> { MacroConfig.autoRodReturnToFarm = v; save(); }));
        return p;
    }

    private Panel equipmentPanel(int[] pos) {
        Panel p = new Panel("Equipment Swap", pos[0], pos[1]);
        p.add(toggle("Auto-Equipment",       () -> MacroConfig.autoEquipment,     v -> { MacroConfig.autoEquipment = v; save(); }));
        p.add(slider("Equipment Swap Delay", 100, 300, () -> MacroConfig.equipmentSwapDelay, v -> { MacroConfig.equipmentSwapDelay = v; save(); }, "ms"));
        return p;
    }

    private Panel autoPestPanel(int[] pos) {
        Panel p = new Panel("Auto Pest", pos[0], pos[1]);
        p.add(slider("Pest Threshold",           1,  8,  () -> MacroConfig.pestThreshold,              v -> { MacroConfig.pestThreshold = v; save(); }, ""));
        p.add(toggle("Trigger Pest on Chat",      () -> MacroConfig.triggerPestOnChat,                 v -> { MacroConfig.triggerPestOnChat = v; save(); }));
        p.add(toggle("Delay for Crop Fever",      () -> MacroConfig.delayPestForCropFever,             v -> { MacroConfig.delayPestForCropFever = v; save(); }));
        p.add(toggle("AOTV to Roof",              () -> MacroConfig.aotvToRoof,                        v -> { MacroConfig.aotvToRoof = v; save(); }));
        p.add(toggle("Break Blocks Before AOTV",  () -> MacroConfig.breakBlocksBeforeAotv,             v -> { MacroConfig.breakBlocksBeforeAotv = v; save(); }));
        p.add(slider("AOTV Roof Pitch",           45, 90, () -> MacroConfig.aotvRoofPitch,             v -> { MacroConfig.aotvRoofPitch = v; save(); }, ""));
        p.add(slider("AOTV Pitch Humanization",   0,  15, () -> MacroConfig.aotvRoofPitchHumanization, v -> { MacroConfig.aotvRoofPitchHumanization = v; save(); }, ""));
        return p;
    }

    private Panel autoVisitorPanel(int[] pos) {
        Panel p = new Panel("Auto Visitor", pos[0], pos[1]);
        p.add(toggle("Auto-Visitor",      () -> MacroConfig.autoVisitor,       v -> { MacroConfig.autoVisitor = v; save(); }));
        p.add(slider("Visitor Threshold", 1, 5, () -> MacroConfig.visitorThreshold, v -> { MacroConfig.visitorThreshold = v; save(); }, ""));
        return p;
    }

    private Panel autoGeorgePanel(int[] pos) {
        Panel p = new Panel("Auto George", pos[0], pos[1]);
        p.add(toggle("Auto George Sell",      () -> MacroConfig.autoGeorgeSell,       v -> { MacroConfig.autoGeorgeSell = v; save(); }));
        p.add(slider("George Sell Threshold", 1, 35, () -> MacroConfig.georgeSellThreshold, v -> { MacroConfig.georgeSellThreshold = v; save(); }, ""));
        return p;
    }

    private Panel autoSellPanel(int[] pos) {
        Panel p = new Panel("Auto Sell", pos[0], pos[1]);
        p.add(toggle("Custom Autosell", () -> MacroConfig.autoBoosterCookie, v -> { MacroConfig.autoBoosterCookie = v; save(); }));
        return p;
    }

    private Panel profitPanel(int[] pos) {
        Panel p = new Panel("Profit Calculator", pos[0], pos[1]);
        p.add(toggle("Show Session Profit HUD",   () -> MacroConfig.showSessionProfitHud,       v -> { MacroConfig.showSessionProfitHud = v; save(); }));
        p.add(toggle("Show Daily Profit HUD",     () -> MacroConfig.showDailyHud,               v -> { MacroConfig.showDailyHud = v; save(); }));
        p.add(toggle("Show Lifetime Profit HUD",  () -> MacroConfig.showLifetimeHud,            v -> { MacroConfig.showLifetimeHud = v; save(); }));
        p.add(toggle("Show HUD While Off",        () -> MacroConfig.showProfitHudWhileInactive,  v -> { MacroConfig.showProfitHudWhileInactive = v; save(); }));
        p.add(toggle("Compact Profit Calculator", () -> MacroConfig.compactProfitCalculator,    v -> { MacroConfig.compactProfitCalculator = v; save(); }));
        p.add(button("Reset Session",  () -> { MacroStateManager.resetSession(); notifyMsg("Session profit reset!"); }));
        p.add(button("Reset Daily",    () -> { ProfitManager.resetDaily();       notifyMsg("Daily profit reset!"); }));
        p.add(button("Reset Lifetime", () -> { ProfitManager.resetLifetime();    notifyMsg("Lifetime profit reset!"); }));
        return p;
    }

    private Panel dynamicRestPanel(int[] pos) {
        Panel p = new Panel("Dynamic Rest", pos[0], pos[1]);
        p.add(intField("Scripting Time",        () -> MacroConfig.restScriptingTime,       v -> { MacroConfig.restScriptingTime = v; save(); }, "min"));
        p.add(intField("Scripting Time Offset", () -> MacroConfig.restScriptingTimeOffset, v -> { MacroConfig.restScriptingTimeOffset = v; save(); }, "min"));
        p.add(intField("Break Time",            () -> MacroConfig.restBreakTime,           v -> { MacroConfig.restBreakTime = v; save(); }, "min"));
        p.add(intField("Break Time Offset",     () -> MacroConfig.restBreakTimeOffset,     v -> { MacroConfig.restBreakTimeOffset = v; save(); }, "min"));
        return p;
    }

    private Panel qolPanel(int[] pos) {
        Panel p = new Panel("QOL", pos[0], pos[1]);
        p.add(toggle("Auto-Book Combine",     () -> MacroConfig.autoBookCombine,    v -> { MacroConfig.autoBookCombine = v; save(); }));
        p.add(toggle("Always Active Combine", () -> MacroConfig.alwaysActiveCombine,v -> { MacroConfig.alwaysActiveCombine = v; save(); }));
        p.add(slider("Book Threshold",        1, 35, () -> MacroConfig.bookThreshold, v -> { MacroConfig.bookThreshold = v; save(); }, ""));
        p.add(toggle("Chat Cleanup",          () -> MacroConfig.hideFilteredChat,   v -> { MacroConfig.hideFilteredChat = v; save(); }));
        p.add(toggle("Auto-Drop Junk",        () -> MacroConfig.autoDropJunk,       v -> { MacroConfig.autoDropJunk = v; save(); }));
        p.add(slider("Junk Threshold",        1, 35, () -> MacroConfig.junkThreshold, v -> { MacroConfig.junkThreshold = v; save(); }, ""));
        p.add(textSetting("Drop Junk PlotTP", () -> MacroConfig.dropJunkPlotTp,    v -> { MacroConfig.dropJunkPlotTp = v; save(); }));
        p.add(toggle("Stash Manager",         () -> MacroConfig.autoStashManager,  v -> { MacroConfig.autoStashManager = v; save(); }));
        p.add(toggle("Send Discord Status",   () -> MacroConfig.sendDiscordStatus, v -> { MacroConfig.sendDiscordStatus = v; save(); }));
        p.add(textSetting("Discord Webhook",  () -> MacroConfig.discordWebhookUrl, v -> { MacroConfig.discordWebhookUrl = v; save(); }));
        p.add(intField("Discord Update Time", () -> MacroConfig.discordStatusUpdateTime, v -> { MacroConfig.discordStatusUpdateTime = v; save(); }, "min"));
        p.add(toggle("Show Debug Messages",   () -> MacroConfig.showDebug,         v -> { MacroConfig.showDebug = v; save(); }));
        p.add(toggle("Log Debug to File",     () -> MacroConfig.logDebugToFile,    v -> { MacroConfig.logDebugToFile = v; if (!v) com.ihanuat.mod.DebugLogger.getInstance().close(); save(); }));
        p.add(button("Open Log Folder", () -> {
            try {
                java.awt.Desktop.getDesktop().open(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().toFile());
            } catch (Exception e) {
                notifyMsg("Failed to open folder: " + e.getMessage());
            }
        }));
        return p;
    }

    // entry factories

    private static ToggleEntry toggle(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) { return new ToggleEntry(label, getter, setter); }
    private static SliderEntry slider(String label, int min, int max, Supplier<Integer> getter, Consumer<Integer> setter, String unit) { return new SliderEntry(label, min, max, getter, setter, unit); }
    private static <E extends Enum<E>> CycleEnumEntry<E> cycleEnum(String label, E[] values, Supplier<E> getter, Consumer<E> setter) { return new CycleEnumEntry<>(label, values, getter, setter); }
    private static TextSettingEntry textSetting(String label, Supplier<String> getter, Consumer<String> setter) { return new TextSettingEntry(label, getter, setter); }
    private static IntFieldEntry intField(String label, Supplier<Integer> getter, Consumer<Integer> setter, String unit) { return new IntFieldEntry(label, getter, setter, unit); }
    private static ButtonEntry button(String label, Runnable action) { return new ButtonEntry(label, action); }

    // render

    @Override
    public void render(@NotNull GuiGraphics g, int mx, int my, float delta) {
        g.fill(0, 0, width, height, 0x99000000);
        for (int i = panels.size() - 1; i >= 0; i--) panels.get(i).render(g, mx, my, font);
        if (activeSubPanel != null) activeSubPanel.render(g, mx, my, font);
        g.drawString(font, "ihanuat  shift+scroll=pan", 3, height - 9, 0xFF333355, false);
    }

    // input routed through InputCatcherWidget

    void handleMouseClicked(int x, int y, int btn) {
        if (activeSubPanel != null) {
            if (activeSubPanel.contains(x, y)) { activeSubPanel.mouseClicked(x, y, btn, font); return; }
            else { activeSubPanel.commit(); activeSubPanel = null; save(); return; }
        }
        for (Panel panel : new ArrayList<>(panels)) {
            if (panel.headerContains(x, y)) {
                if (btn == 0) { draggingPanel = panel; dragOffX = x - panel.x; dragOffY = y - panel.y; }
                else if (btn == 1) { panel.collapsed = !panel.collapsed; }
                panels.remove(panel); panels.add(0, panel);
                return;
            }
            if (panel.contains(x, y)) {
                panels.remove(panel); panels.add(0, panel);
                Entry hit = panel.entryAt(x, y);
                if (hit != null) {
                    if (btn == 0) {
                        if (hit instanceof SliderEntry se) { draggingSlider = se; draggingSliderPanel = panel; se.onDrag(x, panel.x + ENTRY_PAD, PANEL_W - ENTRY_PAD * 2); }
                        else hit.onClick(x, y);
                    } else if (btn == 1) { SubPanel sp = hit.openSubPanel(x, y, width, height); if (sp != null) activeSubPanel = sp; }
                }
                return;
            }
        }
    }

    void handleMouseReleased() {
        if (draggingPanel != null) savePanelPositions();
        draggingPanel = null; draggingSlider = null; draggingSliderPanel = null;
    }

    void handleMouseDragged(int x, int y) {
        if (draggingPanel != null) {
            draggingPanel.x = Math.max(0, Math.min(width - PANEL_W, x - dragOffX));
            draggingPanel.y = Math.max(0, Math.min(height - HEADER_H, y - dragOffY));
        } else if (draggingSlider != null && draggingSliderPanel != null) {
            draggingSlider.onDrag(x, draggingSliderPanel.x + ENTRY_PAD, PANEL_W - ENTRY_PAD * 2);
        }
    }

    void handleMouseScrolled(int x, int y, double hScroll, double vScroll, boolean shift) {
        if (hScroll != 0 || shift) {
            double delta = hScroll != 0 ? hScroll : vScroll;
            int pan = (int)(delta * 20);
            for (Panel p : panels) p.x += pan;
            return;
        }
        if (activeSubPanel != null && activeSubPanel.contains(x, y)) { activeSubPanel.scroll((int) -vScroll); return; }
        for (Panel panel : panels) { if (panel.contains(x, y)) { panel.scroll((int) -vScroll); return; } }
    }

    void handleKeyPressed(int key, int scan, int mods) {
        if (activeSubPanel != null) {
            if (activeSubPanel.keyPressed(key, scan, mods)) return;
            if (key == 256) { activeSubPanel.commit(); activeSubPanel = null; save(); return; }
        }
        if (key == 256) onClose();
    }

    void handleKeyReleased(int key) {
        if (key == 340 || key == 344) shiftHeld = false;
    }

    void handleCharTyped(char c, int mods) {
        if (activeSubPanel != null) activeSubPanel.charTyped(c, mods);
    }

    @Override
    public void onClose() { savePanelPositions(); save(); super.onClose(); }

    @Override
    public boolean isPauseScreen() { return false; }

    private static void save() { MacroConfig.save(); }

    private void notifyMsg(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(msg), true);
    }

    private void savePanelPositions() {
        String[] order = {"General","Delays","Wardrobe Swap","Auto Rod","Equipment Swap",
                "Auto Pest","Auto Visitor","Auto George","Auto Sell","Profit Calculator","Dynamic Rest","QOL"};
        int[][] positions = new int[order.length][2];
        for (int i = 0; i < order.length; i++)
            for (Panel p : panels)
                if (p.title.equals(order[i])) { positions[i] = new int[]{p.x, p.y}; break; }
        MacroConfig.clickGuiPanelPositions = positions;
        MacroConfig.save();
    }

    static void fillRoundRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        for (int row = 0; row < h; row++) {
            int indent = 0;
            if (row < r) { double d = r - row - 0.5; indent = (int) Math.ceil(r - 0.5 - Math.sqrt(r * r - d * d)); }
            else if (row >= h - r) { double d = row - (h - r) + 0.5; indent = (int) Math.ceil(r - 0.5 - Math.sqrt(r * r - d * d)); }
            g.fill(x + indent, y + row, x + w - indent, y + row + 1, color);
        }
    }

    // Panel

    static class Panel {
        String title; int x, y; boolean collapsed = false; int scrollOffset = 0;
        final List<Entry> entries = new ArrayList<>();
        Panel(String title, int x, int y) { this.title = title; this.x = x; this.y = y; this.collapsed = true; }
        void add(Entry e) { entries.add(e); }
        int contentHeight() { return entries.size() * (ENTRY_H + ENTRY_PAD) + ENTRY_PAD; }
        int visibleHeight()  { return 126; }
        int maxScroll()      { return Math.max(0, contentHeight() - visibleHeight()); }
        boolean headerContains(int mx, int my) { return mx >= x && mx <= x + PANEL_W && my >= y && my <= y + HEADER_H; }
        boolean contains(int mx, int my) {
            if (collapsed) return false;
            int h = Math.min(contentHeight(), visibleHeight());
            return mx >= x && mx <= x + PANEL_W && my >= y + HEADER_H && my <= y + HEADER_H + h;
        }
        Entry entryAt(int mx, int my) {
            if (collapsed) return null;
            int ey = y + HEADER_H + ENTRY_PAD - scrollOffset;
            for (Entry e : entries) { if (my >= ey && my < ey + ENTRY_H && mx >= x && mx <= x + PANEL_W) return e; ey += ENTRY_H + ENTRY_PAD; }
            return null;
        }
        void scroll(int dir) { scrollOffset = Math.max(0, Math.min(maxScroll(), scrollOffset + dir * 10)); }

        void render(GuiGraphics g, int mx, int my, net.minecraft.client.gui.Font font) {
            int totalH = collapsed ? HEADER_H : HEADER_H + Math.min(contentHeight(), visibleHeight());
            fillRoundRect(g, x, y, PANEL_W, totalH, PANEL_RADIUS, COL_PANEL_BG);
            fillRoundRect(g, x, y, PANEL_W, HEADER_H, PANEL_RADIUS, COL_HEADER);
            if (!collapsed) g.fill(x, y + HEADER_H - PANEL_RADIUS, x + PANEL_W, y + HEADER_H, COL_HEADER);
            g.fill(x + 2, y + HEADER_H - 1, x + PANEL_W - 2, y + HEADER_H, COL_HEADER_LINE);
            g.drawString(font, title, x + 5, y + HEADER_H / 2 - 4, COL_TEXT, false);
            g.drawString(font, collapsed ? ">" : "v", x + PANEL_W - 10, y + HEADER_H / 2 - 4, COL_TEXT_DIM, false);
            if (collapsed) return;

            int clipY = y + HEADER_H;
            int clipH = Math.min(contentHeight(), visibleHeight());
            g.enableScissor(x, clipY, x + PANEL_W, clipY + clipH);
            int ey = clipY + ENTRY_PAD - scrollOffset;
            for (Entry e : entries) {
                boolean hovered = mx >= x && mx <= x + PANEL_W && my >= ey && my < ey + ENTRY_H;
                if (hovered) g.fill(x + 1, ey, x + PANEL_W - 1, ey + ENTRY_H, COL_HOVER);
                e.render(g, x + ENTRY_PAD, ey, PANEL_W - ENTRY_PAD * 2, ENTRY_H, hovered, font);
                ey += ENTRY_H + ENTRY_PAD;
            }
            g.disableScissor();

            if (maxScroll() > 0) {
                float frac = (float) scrollOffset / maxScroll();
                int barH = Math.max(10, clipH * clipH / contentHeight());
                int barY = clipY + (int)((clipH - barH) * frac);
                g.fill(x + PANEL_W - 3, barY, x + PANEL_W - 1, barY + barH, COL_ACCENT);
            }
        }
    }

    interface Entry {
        void render(GuiGraphics g, int x, int y, int w, int h, boolean hovered, net.minecraft.client.gui.Font font);
        void onClick(int mx, int my);
        default SubPanel openSubPanel(int mx, int my, int screenW, int screenH) { return null; }
    }

    static class ToggleEntry implements Entry {
        final String label; final Supplier<Boolean> getter; final Consumer<Boolean> setter;
        ToggleEntry(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) { this.label = label; this.getter = getter; this.setter = setter; }
        @Override public void render(GuiGraphics g, int x, int y, int w, int h, boolean hov, net.minecraft.client.gui.Font font) {
            boolean on = getter.get();
            int mid = y + h / 2;
            int dotW = 12, dotH = 7;
            int dotX = x + w - dotW - 2, dotY = mid - dotH / 2;
            fillRoundRect(g, dotX, dotY, dotW, dotH, 3, on ? COL_ON : COL_OFF);
            if (on) fillRoundRect(g, dotX + 5, dotY + 1, 5, 5, 2, COL_ON_BRIGHT);
            g.drawString(font, label, x + 2, mid - 4, hov ? COL_TEXT : COL_TEXT_DIM, false);
        }
        @Override public void onClick(int mx, int my) { setter.accept(!getter.get()); }
    }

    static class SliderEntry implements Entry {
        final String label; final int min, max; final Supplier<Integer> getter; final Consumer<Integer> setter; final String unit;
        SliderEntry(String label, int min, int max, Supplier<Integer> getter, Consumer<Integer> setter, String unit) {
            this.label = label; this.min = min; this.max = max; this.getter = getter; this.setter = setter; this.unit = unit;
        }
        @Override public void render(GuiGraphics g, int x, int y, int w, int h, boolean hov, net.minecraft.client.gui.Font font) {
            int val = getter.get(); String valStr = val + unit; int valW = font.width(valStr);
            int textY = y + 2;
            g.drawString(font, label, x + 2, textY, hov ? COL_TEXT : COL_TEXT_DIM, false);
            g.drawString(font, valStr, x + w - valW - 2, textY, COL_ON_BRIGHT, false);
            int barY = y + h - 5, barH = 3;
            fillRoundRect(g, x + 2, barY, w - 4, barH, 1, COL_SLIDER_BG);
            float frac = (float)(val - min) / (max - min);
            int fillW = (int)(frac * (w - 4));
            if (fillW > 0) fillRoundRect(g, x + 2, barY, fillW, barH, 1, COL_SLIDER_FILL);
            int knobX = x + 2 + fillW - 2;
            g.fill(Math.max(x + 2, knobX), barY - 1, Math.max(x + 2, knobX) + 4, barY + barH + 1, COL_SLIDER_KNOB);
        }
        void onDrag(int mx, int barX, int barW) {
            float frac = Math.max(0f, Math.min(1f, (float)(mx - barX) / barW));
            setter.accept(min + Math.round(frac * (max - min)));
        }
        @Override public void onClick(int mx, int my) { }
        @Override public SubPanel openSubPanel(int mx, int my, int screenW, int screenH) {
            return new IntInputSubPanel(mx, my, screenW, screenH, label, getter.get(), min, max, v -> { setter.accept(v); save(); });
        }
    }

    static class CycleEnumEntry<E extends Enum<E>> implements Entry {
        final String label; final E[] values; final Supplier<E> getter; final Consumer<E> setter;
        CycleEnumEntry(String label, E[] values, Supplier<E> getter, Consumer<E> setter) { this.label = label; this.values = values; this.getter = getter; this.setter = setter; }
        @Override public void render(GuiGraphics g, int x, int y, int w, int h, boolean hov, net.minecraft.client.gui.Font font) {
            String val = getter.get().name(); int vw = font.width(val);
            int mid = y + h / 2 - 4;
            g.drawString(font, label, x + 2, mid, hov ? COL_TEXT : COL_TEXT_DIM, false);
            g.drawString(font, val, x + w - vw - 2, mid, COL_ON_BRIGHT, false);
        }
        @Override public void onClick(int mx, int my) { E cur = getter.get(); setter.accept(values[(cur.ordinal() + 1) % values.length]); }
    }

    static class TextSettingEntry implements Entry {
        final String label; final Supplier<String> getter; final Consumer<String> setter;
        TextSettingEntry(String label, Supplier<String> getter, Consumer<String> setter) { this.label = label; this.getter = getter; this.setter = setter; }
        @Override public void render(GuiGraphics g, int x, int y, int w, int h, boolean hov, net.minecraft.client.gui.Font font) {
            String val = getter.get(); if (val.length() > 11) val = val.substring(0, 9) + "..";
            int vw = font.width(val);
            int mid = y + h / 2 - 4;
            g.drawString(font, label, x + 2, mid, hov ? COL_TEXT : COL_TEXT_DIM, false);
            g.drawString(font, val, x + w - vw - 6, mid, COL_TEXT_DIM, false);
        }
        @Override public void onClick(int mx, int my) { }
        @Override public SubPanel openSubPanel(int mx, int my, int screenW, int screenH) {
            return new StringInputSubPanel(mx, my, screenW, screenH, label, getter.get(), setter);
        }
    }

    static class IntFieldEntry implements Entry {
        final String label; final Supplier<Integer> getter; final Consumer<Integer> setter; final String unit;
        IntFieldEntry(String label, Supplier<Integer> getter, Consumer<Integer> setter, String unit) { this.label = label; this.getter = getter; this.setter = setter; this.unit = unit; }
        @Override public void render(GuiGraphics g, int x, int y, int w, int h, boolean hov, net.minecraft.client.gui.Font font) {
            String val = getter.get() + " " + unit; int vw = font.width(val);
            int mid = y + h / 2 - 4;
            g.drawString(font, label, x + 2, mid, hov ? COL_TEXT : COL_TEXT_DIM, false);
            g.drawString(font, val, x + w - vw - 2, mid, COL_ON_BRIGHT, false);
        }
        @Override public void onClick(int mx, int my) { }
        @Override public SubPanel openSubPanel(int mx, int my, int screenW, int screenH) {
            return new IntInputSubPanel(mx, my, screenW, screenH, label, getter.get(), Integer.MIN_VALUE, Integer.MAX_VALUE, v -> { setter.accept(v); save(); });
        }
    }

    static class ButtonEntry implements Entry {
        final String label; final Runnable action;
        ButtonEntry(String label, Runnable action) { this.label = label; this.action = action; }
        @Override public void render(GuiGraphics g, int x, int y, int w, int h, boolean hov, net.minecraft.client.gui.Font font) {
            fillRoundRect(g, x + 2, y + 2, w - 4, h - 4, 3, hov ? COL_ON : COL_OFF);
            int tw = font.width(label);
            g.drawString(font, label, x + (w - tw) / 2, y + h / 2 - 4, COL_TEXT, false);
        }
        @Override public void onClick(int mx, int my) { action.run(); }
    }

    interface SubPanel {
        void render(GuiGraphics g, int mx, int my, net.minecraft.client.gui.Font font);
        boolean contains(int mx, int my);
        boolean mouseClicked(int mx, int my, int btn, net.minecraft.client.gui.Font font);
        boolean keyPressed(int key, int scan, int mods);
        boolean charTyped(char c, int mods);
        void scroll(int dir);
        void commit();
    }

    static class StringInputSubPanel implements SubPanel {
        final String label; String value; final Consumer<String> setter;
        final int x, y, w = 200, h = 40;
        boolean cursorVisible = true; long lastBlink = System.currentTimeMillis();

        StringInputSubPanel(int mx, int my, int screenW, int screenH, String label, String initial, Consumer<String> setter) {
            this.label = label; this.value = initial; this.setter = setter;
            this.x = Math.min(mx, screenW - w - 4); this.y = Math.min(my, screenH - h - 4);
        }
        @Override public void render(GuiGraphics g, int mx, int my, net.minecraft.client.gui.Font font) {
            fillRoundRect(g, x - 2, y - 2, w + 4, h + 4, 4, COL_SUBPANEL_BD);
            fillRoundRect(g, x, y, w, h, 3, COL_SUBPANEL_BG);
            g.drawString(font, label, x + 4, y + 4, COL_TEXT, false);
            g.fill(x + 4, y + 16, x + w - 4, y + 33, COL_SLIDER_BG);
            g.fill(x + 4, y + 33, x + w - 4, y + 34, COL_ACCENT);
            if (System.currentTimeMillis() - lastBlink > 500) { cursorVisible = !cursorVisible; lastBlink = System.currentTimeMillis(); }
            g.drawString(font, value + (cursorVisible ? "|" : ""), x + 6, y + 20, COL_TEXT, false);
        }
        @Override public boolean contains(int mx, int my) { return mx >= x - 2 && mx <= x + w + 2 && my >= y - 2 && my <= y + h + 2; }
        @Override public boolean mouseClicked(int mx, int my, int btn, net.minecraft.client.gui.Font font) { return true; }
        @Override public void scroll(int dir) { }
        @Override public boolean keyPressed(int key, int scan, int mods) {
            if (key == 259 && !value.isEmpty()) { value = value.substring(0, value.length() - 1); return true; }
            if (key == 257 || key == 335) { commit(); return true; }
            return false;
        }
        @Override public boolean charTyped(char c, int mods) { value += c; return true; }
        @Override public void commit() { setter.accept(value); }
    }

    static class IntInputSubPanel implements SubPanel {
        final String label; String rawValue; final int min, max; final Consumer<Integer> setter;
        final int x, y, w = 180, h = 40;
        boolean cursorVisible = true; long lastBlink = System.currentTimeMillis();

        IntInputSubPanel(int mx, int my, int screenW, int screenH, String label, int initial, int min, int max, Consumer<Integer> setter) {
            this.label = label; this.rawValue = String.valueOf(initial); this.min = min; this.max = max; this.setter = setter;
            this.x = Math.min(mx, screenW - w - 4); this.y = Math.min(my, screenH - h - 4);
        }
        @Override public void render(GuiGraphics g, int mx, int my, net.minecraft.client.gui.Font font) {
            fillRoundRect(g, x - 2, y - 2, w + 4, h + 4, 4, COL_SUBPANEL_BD);
            fillRoundRect(g, x, y, w, h, 3, COL_SUBPANEL_BG);
            g.drawString(font, label, x + 4, y + 4, COL_TEXT, false);
            String range = min == Integer.MIN_VALUE ? "" : "[" + min + "-" + max + "]";
            int rw = font.width(range);
            g.drawString(font, range, x + w - rw - 4, y + 4, COL_TEXT_DIM, false);
            g.fill(x + 4, y + 16, x + w - 4, y + 33, COL_SLIDER_BG);
            g.fill(x + 4, y + 33, x + w - 4, y + 34, COL_ACCENT);
            if (System.currentTimeMillis() - lastBlink > 500) { cursorVisible = !cursorVisible; lastBlink = System.currentTimeMillis(); }
            g.drawString(font, rawValue + (cursorVisible ? "|" : ""), x + 6, y + 20, COL_TEXT, false);
        }
        @Override public boolean contains(int mx, int my) { return mx >= x - 2 && mx <= x + w + 2 && my >= y - 2 && my <= y + h + 2; }
        @Override public boolean mouseClicked(int mx, int my, int btn, net.minecraft.client.gui.Font font) { return true; }
        @Override public void scroll(int dir) { }
        @Override public boolean keyPressed(int key, int scan, int mods) {
            if (key == 259 && !rawValue.isEmpty()) { rawValue = rawValue.substring(0, rawValue.length() - 1); return true; }
            if (key == 257 || key == 335) { commit(); return true; }
            return false;
        }
        @Override public boolean charTyped(char c, int mods) {
            if (Character.isDigit(c) || (c == '-' && rawValue.isEmpty())) { rawValue += c; return true; }
            return false;
        }
        @Override public void commit() {
            try {
                int v = Integer.parseInt(rawValue);
                setter.accept(min == Integer.MIN_VALUE ? v : Math.max(min, Math.min(max, v)));
            } catch (NumberFormatException ignored) { }
        }
    }
}