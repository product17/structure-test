package net.fabricmc.example.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import net.fabricmc.loader.api.FabricLoader;

public class LoadConfig {
    private final File configFile;
    public static MainConfig config;
    public static Map<String, ZoneConfig> zones = new HashMap<>();
    public static Map<String, StructurePoolConfig> structurePools = new HashMap<>();

    public LoadConfig(String configName) {
        this.configFile = new File(FabricLoader.getInstance().getConfigDir().toFile(), configName + ".json");
    }

    public void readConfigFromFile() {
        try (FileInputStream stream = new FileInputStream(configFile)) {
            byte[] bytes = new byte[stream.available()];
            Gson gson = new Gson();
            stream.read(bytes);
            String file = new String(bytes);
            config = gson.fromJson(file, MainConfig.class);
        } catch (FileNotFoundException e) {
            saveConfigToFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void addZoneConfig(ZoneConfig labConfig) {
        zones.put(labConfig.name, labConfig);
    }

    public static void addPoolConfig(StructurePoolConfig poolConfig) {
        structurePools.put(poolConfig.name, poolConfig);
    }

    public static ZoneConfig getZoneConfig(String name) {
        return LoadConfig.zones.get(name);
    }

    public void saveConfigToFile() {
        JsonObject object = new JsonObject();

        try (FileOutputStream stream = new FileOutputStream(configFile)) {
            Gson gson = new Gson();
            stream.write(gson.toJson(object).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
