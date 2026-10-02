package com.crabmods.algocraft;

import com.crabmods.algocraft.item.TrophyItem;
import com.crabmods.algocraft.logic.ModTrophyBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TrophyBlockEntity extends BlockEntity {
    private ItemStack trophy = ItemStack.EMPTY;
    public TrophyBlockEntity(BlockPos pos, BlockState state) { super(ModTrophyBlocks.TROPHY_ENTITY.get(), pos, state); }
    private ItemStack validated(ItemStack stack) {
        return stack.getItem() instanceof TrophyItem item && item.getBlock() == getBlockState().getBlock()
                ? stack.copyWithCount(1) : ItemStack.EMPTY;
    }
    public ItemStack getTrophy() { return trophy.isEmpty() ? new ItemStack(getBlockState().getBlock().asItem()) : trophy.copy(); }
    public void setTrophy(ItemStack stack) {
        trophy = validated(stack);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }
    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!trophy.isEmpty()) tag.put("Trophy", trophy.save(new CompoundTag()));
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        trophy = validated(ItemStack.of(tag.getCompound("Trophy")));
    }
    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
}
