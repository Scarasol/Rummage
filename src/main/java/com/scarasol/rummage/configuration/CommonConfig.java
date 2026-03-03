package com.scarasol.rummage.configuration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author Scarasol
 */
public class CommonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 基础配置项
    public static final ForgeConfigSpec.ConfigValue<Double> RUMMAGE_TIME;
    public static final ForgeConfigSpec.ConfigValue<String> RUMMAGE_SOUND;
    public static final ForgeConfigSpec.ConfigValue<Boolean> CHAIN_RUMMAGING;
    public static final ForgeConfigSpec.ConfigValue<Double> DESTROY_CHANCE;

    // 黑名单配置项
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RUMMAGE_BLACKLIST;

    // 兼容配置项 (Item Rarity)
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RARITY_BASED_RUMMAGE_TIMES;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RARITY_BASED_RUMMAGE_SOUND;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RARITY_BASED_DESTROY_CHANCE;

    // 运行时缓存 Map/Set
    private static final Map<String, Double> RARITY_BASED_RUMMAGE_TIMES_MAP = new HashMap<>();
    private static final Map<String, SoundEvent> RARITY_BASED_RUMMAGE_SOUND_MAP = new HashMap<>();
    private static final Map<String, Double> RARITY_BASED_DESTROY_CHANCE_MAP = new HashMap<>();

    private static final Set<ResourceLocation> RUMMAGE_BLACKLIST_SET = new HashSet<>();

    private static SoundEvent RUMMAGE_SOUND_EVENT = null;

    // 懒加载与重载标记
    private static boolean loaded = false;

    static {
        // --- 通用设置 ---
        RUMMAGE_TIME = BUILDER.comment("Time required to rummage for an item")
                .defineInRange("Rummage Time (Sec)", 0.8D, 0, 5000);
        RUMMAGE_SOUND = BUILDER.comment("The default sound played when an item is rummaged successfully.")
                .define("Rummage Sound", "rummage:normal_found");
        CHAIN_RUMMAGING = BUILDER.comment("Whether rummaging a stackable item will automatically rummage all other items with the same ID.",
                "Recommended to use with Tag Editor. Items tagged with rummage:chain_blacklist will not be chain-rummaged, whereas items tagged with rummage:chain_whitelist will be.")
                .define("Chain Rummaging", true);
        DESTROY_CHANCE = BUILDER.comment("The probability of each item being destroyed if the container is broken before rummaging is complete.")
                .defineInRange("Destroy Chance", 0.2D, 0, 1);

        // --- 黑名单设置 ---
        List<String> defaultBlacklist = new ArrayList<>();
         defaultBlacklist.add("minecraft:ender_chest");
        RUMMAGE_BLACKLIST = BUILDER
                .comment("A list of container block/entity IDs that should bypass the rummaging mechanic.",
                        "Format: 'modid:container_id' (e.g., 'minecraft:chest').")
                .defineList("Rummage Blacklist", defaultBlacklist, obj -> obj instanceof String);

        // --- 兼容设置: Item Rarity ---
        BUILDER.push("Compat - Item Rarity");

        // 1. 默认时间配置
        List<String> defaultTimes = new ArrayList<>();
        defaultTimes.add("common, 1");
        defaultTimes.add("uncommon, 1.5");
        defaultTimes.add("rare, 2");
        defaultTimes.add("epic, 3.5");
        defaultTimes.add("legendary, 5.0");
        defaultTimes.add("artifact, 5.0");

        RARITY_BASED_RUMMAGE_TIMES = BUILDER
                .comment("Rummage durations for different item rarities when Item Rarity is installed.",
                        "Format: 'rarity_id, duration' (e.g., 'epic, 4.0' means epic items take 4 seconds to rummage).")
                .defineList("Rarity-Based Rummage Times", defaultTimes, obj -> obj instanceof String);

        // 2. 默认声音配置
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

        // 3. 默认损毁概率配置
        List<String> defaultChances = new ArrayList<>();
        defaultChances.add("uncommon, 0.4");
        defaultChances.add("rare, 0.6");
        defaultChances.add("epic, 0.7");
        defaultChances.add("legendary, 0.9");
        defaultChances.add("artifact, 0.9");

        RARITY_BASED_DESTROY_CHANCE = BUILDER
                .comment("Destruction probabilities for different item rarities when Item Rarity is installed.",
                        "Format: 'rarity_id, chance' (e.g., 'epic, 0.2' means epic items have a 20% chance to be destroyed).")
                .defineList("Rarity-Based Destroy Chance", defaultChances, obj -> obj instanceof String);

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
        RARITY_BASED_DESTROY_CHANCE_MAP.clear();
        RUMMAGE_BLACKLIST_SET.clear();

        ResourceLocation defaultSoundLoc = ResourceLocation.tryParse(RUMMAGE_SOUND.get());
        if (defaultSoundLoc != null) {
            RUMMAGE_SOUND_EVENT = ForgeRegistries.SOUND_EVENTS.getValue(defaultSoundLoc);
        } else {
            RUMMAGE_SOUND_EVENT = null;
        }

        // 解析黑名单
        for (String entry : RUMMAGE_BLACKLIST.get()) {
            ResourceLocation loc = ResourceLocation.tryParse(entry.trim());
            if (loc != null) {
                RUMMAGE_BLACKLIST_SET.add(loc);
            }
        }

        // 解析翻找时间
        for (String entry : RARITY_BASED_RUMMAGE_TIMES.get()) {
            String[] parts = entry.split(",");
            if (parts.length >= 2) {
                try {
                    RARITY_BASED_RUMMAGE_TIMES_MAP.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));
                } catch (NumberFormatException ignored) {}
            }
        }

        // 解析翻找音效
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

        // 解析损毁概率
        for (String entry : RARITY_BASED_DESTROY_CHANCE.get()) {
            String[] parts = entry.split(",");
            if (parts.length >= 2) {
                try {
                    RARITY_BASED_DESTROY_CHANCE_MAP.put(parts[0].trim(), Double.parseDouble(parts[1].trim()));
                } catch (NumberFormatException ignored) {}
            }
        }
    }

    // --- 数据获取接口 ---

    /**
     * 判断指定 ID 是否在黑名单中
     */
    public static boolean isBlacklisted(@Nullable ResourceLocation id) {
        if (id == null) return false;
        ensureLoaded();
        return RUMMAGE_BLACKLIST_SET.contains(id);
    }

    public static double getRummageTime(String rarityId) {
        ensureLoaded();
        return RARITY_BASED_RUMMAGE_TIMES_MAP.getOrDefault(rarityId, RUMMAGE_TIME.get());
    }

    @Nullable
    public static SoundEvent getRummageSound(String rarityId) {
        ensureLoaded();
        return RARITY_BASED_RUMMAGE_SOUND_MAP.getOrDefault(rarityId, RUMMAGE_SOUND_EVENT);
    }

    public static double getDestroyChance(String rarityId) {
        ensureLoaded();
        return RARITY_BASED_DESTROY_CHANCE_MAP.getOrDefault(rarityId, DESTROY_CHANCE.get());
    }

    @Nullable
    public static SoundEvent getBaseRummageSound() {
        ensureLoaded();
        return RUMMAGE_SOUND_EVENT;
    }
}