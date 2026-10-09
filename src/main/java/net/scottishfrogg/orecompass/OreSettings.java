package net.scottishfrogg.orecompass;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record OreSettings(int cost, List<Block> blocks, List<TagKey<Block>> tags) {
    public boolean isEmpty() {
        return blocks.isEmpty() && tags.isEmpty();
    }
    public boolean matches(BlockState state) {
        for (Block block : blocks) {
            if (state.is(block)) return true;
        }
        for (TagKey<Block> tag : tags) {
            if (state.is(tag)) return true;
        }
        return false;
    }
}