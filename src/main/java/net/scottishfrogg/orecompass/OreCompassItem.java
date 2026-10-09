package net.scottishfrogg.orecompass;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

public class OreCompassItem extends Item {
    public static final int DURATION_TICKS = 5 * 60 * 20; // 5 minutes
    private static final String TAG_ORE = "TrackedOre";
    private static final String TAG_EXPIRES = "ExpiresAt";
    private static final String TAG_TARGET = "Target";
    public OreCompassItem(Properties properties) {
        super(properties);
    }
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack compass, ItemStack other, Slot slot,
                                            ClickAction action, Player player, SlotAccess access) {
        if (other.isEmpty() || !slot.allowModification(player)) return false;
        if (!other.is(ModTags.TRACKABLE_ORES)) return false;
        tryPay(compass, other, player);
        return true;
    }
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack compass = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND
                ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack payment = player.getItemInHand(otherHand);
        if (payment.isEmpty() || !payment.is(ModTags.TRACKABLE_ORES)) {
            return InteractionResultHolder.pass(compass);
        }
        tryPay(compass, payment, player);
        return InteractionResultHolder.sidedSuccess(compass, level.isClientSide());
    }
    private void tryPay(ItemStack compass, ItemStack payment, Player player) {
        Level level = player.level();
        if (level.isClientSide()) return;
        OreSettings settings = OreDataManager.INSTANCE.getSettings(payment);
        if (settings == null) {
            player.displayClientMessage(Component.translatable("message.orecompass.unsupported"), true);
            return;
        }
        if (isActive(compass, level)) {
            player.displayClientMessage(Component.translatable("message.orecompass.already_active"), true);
            return;
        }
        int cost = settings.cost();
        if (payment.getCount() < cost) {
            player.displayClientMessage(Component.translatable("message.orecompass.not_enough",
                    cost, payment.getHoverName()), true);
            return;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(payment.getItem());
        Component name = payment.getHoverName();
        payment.shrink(cost);
        CompoundTag tag = compass.getOrCreateTag();
        tag.putString(TAG_ORE, id.toString());
        tag.putLong(TAG_EXPIRES, level.getGameTime() + DURATION_TICKS);
        updateTarget(compass, (ServerLevel) level, player);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.MASTER, 1.0F, 1.0F);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                SoundSource.MASTER, 1.0F, 1.2F);
        player.displayClientMessage(Component.translatable("message.orecompass.activated", name), true);
    }
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player)) return;
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ORE)) return;
        if (!isActive(stack, level)) {
            tag.remove(TAG_ORE);
            tag.remove(TAG_EXPIRES);
            tag.remove(TAG_TARGET);
            if (tag.isEmpty()) stack.setTag(null);
            player.displayClientMessage(Component.translatable("message.orecompass.expired"), true);
            return;
        }
        if (level.getGameTime() % 20 == 0) {
            updateTarget(stack, serverLevel, player);
        }
    }
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }
    private static void updateTarget(ItemStack compass, ServerLevel level, Player player) {
        Item tracked = getTrackedItem(compass);
        OreSettings settings = tracked == null ? null
                : OreDataManager.INSTANCE.getSettings(new ItemStack(tracked));
        BlockPos found = settings == null ? null
                : OreFinder.findNearest(level, player.blockPosition(), settings, Config.SEARCH_RADIUS.get());
        CompoundTag tag = compass.getOrCreateTag();
        if (found == null) {
            tag.remove(TAG_TARGET);
        } else if (!tag.contains(TAG_TARGET) || tag.getLong(TAG_TARGET) != found.asLong()) {
            tag.putLong(TAG_TARGET, found.asLong());
        }
    }
    public static boolean isActive(ItemStack stack, Level level) {
        return getRemainingTicks(stack, level) > 0;
    }
    public static long getRemainingTicks(ItemStack stack, Level level) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_EXPIRES)) return 0;
        return Math.max(0, tag.getLong(TAG_EXPIRES) - level.getGameTime());
    }
    @Nullable
    public static Item getTrackedItem(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_ORE)) return null;
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(tag.getString(TAG_ORE)));
    }
    @Nullable
    public static BlockPos getTarget(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_TARGET)) return null;
        return BlockPos.of(tag.getLong(TAG_TARGET));
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.orecompass.survival_1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.orecompass.survival_2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.orecompass.creative_1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.orecompass.creative_2").withStyle(ChatFormatting.GRAY));
    }
}