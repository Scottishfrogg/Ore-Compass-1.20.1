package net.scottishfrogg.orecompass;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec.IntValue SEARCH_RADIUS = BUILDER
            .comment("How far (in blocks) the compass searches for ore")
            .defineInRange("searchRadius", 64, 16, 256);
    public static final ForgeConfigSpec SPEC = BUILDER.build();
}