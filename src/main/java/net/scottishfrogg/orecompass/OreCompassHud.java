package net.scottishfrogg.orecompass;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class OreCompassHud {
    private static final int MARGIN = 6;
    private static final int GLYPH_SIZE = 12;
    private static final int GLYPH_ADVANCE = 12;
    private static final int ROW_GAP = 2;
    private static final int TEXT_GAP = 4;
    private static final int BOX_TEXTURE = 48;
    private static final int BOX_OFFSET = 11;
    private static final int BOX_VISIBLE = 26;
    private static final ResourceLocation BOX =
            new ResourceLocation(CompassMod.MOD_ID, "textures/item/ore_box.png");
    private static final Map<Character, ResourceLocation> GLYPHS = new HashMap<>();
    public static final IGuiOverlay OVERLAY = (gui, g, partialTick, screenWidth, screenHeight) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.level == null) return;
        ItemStack compass = player.getMainHandItem();
        if (!(compass.getItem() instanceof OreCompassItem)) compass = player.getOffhandItem();
        if (!(compass.getItem() instanceof OreCompassItem)) return;
        long remaining = OreCompassItem.getRemainingTicks(compass, mc.level);
        boolean active = remaining > 0;
        Item tracked = OreCompassItem.getTrackedItem(compass);
        int x = MARGIN;
        int y = MARGIN;
        RenderSystem.enableBlend();
        g.blit(BOX, x - BOX_OFFSET, y - BOX_OFFSET, 0, 0, BOX_TEXTURE, BOX_TEXTURE, BOX_TEXTURE, BOX_TEXTURE);
        if (active && tracked != null && tracked != Items.AIR) {
            int iconOffset = (BOX_VISIBLE - 16) / 2;
            g.renderItem(new ItemStack(tracked), x + iconOffset, y + iconOffset);
            BlockPos target = OreCompassItem.getTarget(compass);
            if (target != null) {
                int textX = x + BOX_VISIBLE + TEXT_GAP;
                int textY = y + (BOX_VISIBLE - GLYPH_SIZE) / 2;
                drawText(g, "y:" + target.getY(), textX, textY);
            }
        }
        long seconds = active ? (remaining + 19) / 20 : 0;
        drawText(g, String.format("%d:%02d", seconds / 60, seconds % 60), x, y + BOX_VISIBLE + ROW_GAP);

        RenderSystem.disableBlend();
    };
    private static void drawText(GuiGraphics g, String text, int x, int y) {
        for (char c : text.toCharArray()) {
            ResourceLocation texture = glyph(c);
            if (texture != null) {
                g.blit(texture, x, y, GLYPH_SIZE, GLYPH_SIZE, 0, 0, 16, 16, 16, 16);
            }
            x += GLYPH_ADVANCE;
        }
    }
    @Nullable
    private static ResourceLocation glyph(char c) {
        final String name = glyphName(c);
        if (name == null) return null;
        return GLYPHS.computeIfAbsent(c,
                k -> new ResourceLocation(CompassMod.MOD_ID, "textures/item/ore_" + name + ".png"));
    }
    @Nullable
    private static String glyphName(char c) {
        if (c >= '0' && c <= '9') return String.valueOf(c);
        return switch (c) {
            case 'y' -> "y";
            case ':' -> "colon";
            case '-' -> "minus";
            default -> null;
        };
    }
}