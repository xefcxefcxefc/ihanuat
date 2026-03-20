package com.ihanuat.mod;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RestStateManager {
    private static final Path STATE_FILE = FabricLoader.getInstance()
            .getGameDir().resolve("ihanuat_rest_state.json");
    private static RestState cachedState = RestState.empty();
    private static boolean cacheLoaded = false;

    public static final class RestState {
        private final long reconnectAt;
        private final boolean shouldResume;

        private RestState(long reconnectAt, boolean shouldResume) {
            this.reconnectAt = reconnectAt;
            this.shouldResume = shouldResume;
        }

        public static RestState empty() {
            return new RestState(0L, false);
        }

        public long reconnectAt() {
            return reconnectAt;
        }

        public boolean shouldResume() {
            return shouldResume;
        }
    }

    public static synchronized void saveReconnectTime(long epochSeconds, boolean shouldResume) {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("reconnectAt", epochSeconds);
            obj.addProperty("shouldResume", shouldResume);
            Files.writeString(STATE_FILE, new Gson().toJson(obj));
            cachedState = new RestState(epochSeconds, shouldResume);
            cacheLoaded = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static synchronized RestState getState() {
        if (cacheLoaded) {
            return cachedState;
        }
        if (!Files.exists(STATE_FILE)) {
            cachedState = RestState.empty();
            cacheLoaded = true;
            return cachedState;
        }
        try {
            JsonObject obj = JsonParser.parseString(
                    Files.readString(STATE_FILE)).getAsJsonObject();
            long reconnectAt = obj.has("reconnectAt") ? obj.get("reconnectAt").getAsLong() : 0L;
            boolean shouldResume = obj.has("shouldResume") && obj.get("shouldResume").getAsBoolean();
            cachedState = new RestState(reconnectAt, shouldResume);
        } catch (Exception e) {
            cachedState = RestState.empty();
        }
        cacheLoaded = true;
        return cachedState;
    }

    public static long loadReconnectTime() {
        return getState().reconnectAt();
    }

    public static boolean shouldResume() {
        return getState().shouldResume();
    }

    public static synchronized void clearState() {
        try {
            Files.deleteIfExists(STATE_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        cachedState = RestState.empty();
        cacheLoaded = true;
    }
}
