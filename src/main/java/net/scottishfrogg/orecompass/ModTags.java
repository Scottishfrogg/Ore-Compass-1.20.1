package net.scottishfrogg.orecompass;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    public static final TagKey<Item> TRACKABLE_ORES =
            ItemTags.create(new ResourceLocation(CompassMod.MOD_ID, "trackable_ores"));
}