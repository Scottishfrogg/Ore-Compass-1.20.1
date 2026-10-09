package net.scottishfrogg.orecompass;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import javax.annotation.Nullable;

public class OreFinder {
    @Nullable
    public static BlockPos findNearest(ServerLevel level, BlockPos origin, OreSettings settings, int radius) {
        int minCX = (origin.getX() - radius) >> 4;
        int maxCX = (origin.getX() + radius) >> 4;
        int minCZ = (origin.getZ() - radius) >> 4;
        int maxCZ = (origin.getZ() + radius) >> 4;
        double maxSq = (double) radius * radius;
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        for (int cx = minCX; cx <= maxCX; cx++) {
            for (int cz = minCZ; cz <= maxCZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                LevelChunkSection[] sections = chunk.getSections();
                for (int i = 0; i < sections.length; i++) {
                    LevelChunkSection section = sections[i];
                    int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
                    if (baseY + 15 < origin.getY() - radius || baseY > origin.getY() + radius) continue;
                    if (section.hasOnlyAir() || !section.maybeHas(settings::matches)) continue;
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                if (!settings.matches(section.getBlockState(x, y, z))) continue;
                                int wx = (cx << 4) + x;
                                int wy = baseY + y;
                                int wz = (cz << 4) + z;
                                double dx = wx - origin.getX();
                                double dy = wy - origin.getY();
                                double dz = wz - origin.getZ();
                                double sq = dx * dx + dy * dy + dz * dz;
                                if (sq <= maxSq && sq < bestSq) {
                                    bestSq = sq;
                                    best = new BlockPos(wx, wy, wz);
                                }
                            }
                        }
                    }
                }
            }
        }
        return best;
    }
}