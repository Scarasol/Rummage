package com.scarasol.rummage.configuration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Scarasol
 */
public class CommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.ConfigValue<Double> RUMMAGE_TIME;
    public static final ForgeConfigSpec.ConfigValue<Boolean> CHAIN_RUMMAGING;

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RARITY_BASED_RUMMAGE_TIMES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RARITY_BASED_RUMMAGE_SOUND;

    private static final Map<String, Double> RARITY_BASED_RUMMAGE_TIMES_MAP = new HashMap<>();
    private static final Map<String, SoundEvent> RARITY_BASED_RUMMAGE_SOUND_MAP = new HashMap<>();

    private static boolean loaded = false;

    static {
        RUMMAGE_TIME = BUILDER.comment("Time required to rummage for an item")
                .defineInRange("Rummage Time (Sec)", 2D, 0, 5000);
        CHAIN_RUMMAGING = BUILDER.comment("Whether rummaging an item will automatically rummage all other items with the same ID.")
                .define("Chain Rummaging", true);

        BUILDER.push("Compat - Item Rarity");

        // 默认时间配置
        List<String> defaultTimes = new ArrayList<>();
        defaultTimes.add("common, 2.5");
        defaultTimes.add("uncommon, 3");
        defaultTimes.add("rare, 3.5");
        defaultTimes.add("epic, 4");
        defaultTimes.add("legendary, 5");
        defaultTimes.add("artifact, 5");

        RARITY_BASED_RUMMAGE_TIMES = BUILDER
                .comment("Rummage durations for different item rarities when Item Rarity is installed.",
                        "Format: 'rarity_id, duration' (e.g., 'epic, 4' means epic items take 4 seconds to rummage).")
                .defineList("Rarity-Based Rummage Times", defaultTimes, obj -> obj instanceof String);

        // 默认声音配置
        List<String> defaultSounds = new ArrayList<>();
        defaultSounds.add("uncommon, rummage:uncommon_found");
        defaultSounds.add("rare, rummage:rare_found");
        defaultSounds.add("epic, rummage:epic_found");
        defaultSounds.add("legendary, rummage:legendary_found");
        defaultSounds.add("artifact, rummage:artifact_found");

        RARITY_BASED_RUMMAGE_SOUND = BUILDER
                .comment("Sound effects played upon completing a rummage for different item rarities when Item Rarity is installed.",
                        "Format: 'rarity_id, sound_location' (e.g., 'epic, minecraft:example_sound' means epic items play that sound when found).")
                .defineList("Rarity-Based Rummage Sound", defaultSounds, obj -> obj instanceof String);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void markDirty() {
        loaded = false;
    }

    private static void ensureLoaded() {
        if (!loaded) {
            init();
            loaded = true;
        }
    }

    private static void init() {
        RARITY_BASED_RUMMAGE_TIMES_MAP.clear();
        RARITY_BASED_RUMMAGE_SOUND_MAP.clear();

        // 解析时间
        for (String entry : RARITY_BASED_RUMMAGE_TIMES.get()) {
            String[] parts = entry.split(",");
            if (parts.length >= 2) {
                try {
                    RARITY_BASED_RUMMAGE_TIMES_MAP.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        // 解析声音
        for (String entry : RARITY_BASED_RUMMAGE_SOUND.get()) {
            String[] parts = entry.split(",");
            if (parts.length >= 2) {
                String rarity = parts[0].trim();
                ResourceLocation loc = ResourceLocation.tryParse(parts[1].trim());
                if (loc != null) {
                    SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(loc);
                    if (sound != null) {
                        RARITY_BASED_RUMMAGE_SOUND_MAP.put(rarity, sound);
                    }
                }
            }
        }
    }

    public static double getRummageTime(String rarityId) {
        ensureLoaded();
        return RARITY_BASED_RUMMAGE_TIMES_MAP.getOrDefault(rarityId, RUMMAGE_TIME.get());
    }

    @Nullable
    public static SoundEvent getRummageSound(String rarityId) {
        ensureLoaded();
        return RARITY_BASED_RUMMAGE_SOUND_MAP.get(rarityId);
    }
}