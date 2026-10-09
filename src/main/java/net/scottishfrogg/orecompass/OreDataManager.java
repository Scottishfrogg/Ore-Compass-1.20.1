package net.scottishfrogg.orecompass;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OreDataManager extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    public static final int DEFAULT_COST = 5;
    public static final OreDataManager INSTANCE = new OreDataManager();
    private Map<Item, OreSettings> settings = Map.of();
    private OreDataManager() {
        super(GSON, "compass_ores");
    }
    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<Item, OreSettings> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), "ore file");
                ResourceLocation itemId = new ResourceLocation(GsonHelper.getAsString(json, "item"));
                if (!ForgeRegistries.ITEMS.containsKey(itemId)) {
                    CompassMod.LOGGER.warn("Ore file {} references unknown item {}", entry.getKey(), itemId);
                    continue;
                }
                Item item = ForgeRegistries.ITEMS.getValue(itemId);
                int cost = Math.max(1, GsonHelper.getAsInt(json, "cost", DEFAULT_COST));
                List<Block> blocks = new ArrayList<>();
                List<TagKey<Block>> tags = new ArrayList<>();
                for (JsonElement el : GsonHelper.getAsJsonArray(json, "blocks")) {
                    String raw = GsonHelper.convertToString(el, "block");
                    if (raw.startsWith("#")) {
                        tags.add(TagKey.create(Registries.BLOCK, new ResourceLocation(raw.substring(1))));
                    } else {
                        ResourceLocation blockId = new ResourceLocation(raw);
                        if (ForgeRegistries.BLOCKS.containsKey(blockId)) {
                            blocks.add(ForgeRegistries.BLOCKS.getValue(blockId));
                        } else {
                            CompassMod.LOGGER.warn("Ore file {} references unknown block {}", entry.getKey(), blockId);
                        }
                    }
                }
                OreSettings parsed = new OreSettings(cost, blocks, tags);
                if (parsed.isEmpty()) {
                    CompassMod.LOGGER.warn("Ore file {} has no usable blocks, skipping", entry.getKey());
                    continue;
                }
                loaded.put(item, parsed);
            } catch (Exception e) {
                CompassMod.LOGGER.error("Failed to load ore file {}", entry.getKey(), e);
            }
        }
        settings = loaded;
        CompassMod.LOGGER.info("Ore Compass loaded {} ores", loaded.size());
    }
    @Nullable
    public OreSettings getSettings(ItemStack stack) {
        if (!stack.is(ModTags.TRACKABLE_ORES)) return null;
        return settings.get(stack.getItem());
    }
}