package com.dantaeusb.zettergallery.tileentity;

import com.dantaeusb.zettergallery.menu.PaintingMerchantMenu;
import com.dantaeusb.zettergallery.core.ZetterGalleryBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.UUID;

public class PaintingMerchantTileEntity extends BlockEntity implements MenuProvider {
    private final UUID entityId = UUID.randomUUID();

    public PaintingMerchantTileEntity(BlockPos pos, BlockState state) {
        super(ZetterGalleryBlockEntities.TRADER_TILE_ENTITY, pos, state);
    }

    public UUID getUniqueId() {
        return this.entityId;
    }

    // render

    @Override
    public AABB getRenderBoundingBox()
    {
        return new AABB(this.getBlockPos(), this.getBlockPos().offset(1, 1, 1));
    }

    // NBT stack

    @Override
    public CompoundTag save(CompoundTag parentNBTTagCompound)
    {
        super.save(parentNBTTagCompound); // The super call is required to save and load the tileEntity's location

        return parentNBTTagCompound;
    }

    @Override
    public void load(CompoundTag parentNBTTagCompound)
    {
        super.load(parentNBTTagCompound);
    }

    // network stack

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
        this.load(packet.getTag());
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag nbtTagCompound = new CompoundTag();
        this.load(nbtTagCompound);
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        this.load(tag);
    }

    @Override
    public Component getDisplayName() {
        return new TranslatableComponent("container.zetter.easel");
    }

    /**
     * The name is misleading; createMenu has nothing to do with creating a Screen, it is used to create the Container on the server only
     * @param windowID
     * @param playerInventory
     * @param playerEntity
     * @return
     */
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowID, Inventory playerInventory, Player player) {
        return PaintingMerchantMenu.createContainerServerSide(windowID, playerInventory, this, ContainerLevelAccess.create(this.level, this.worldPosition));
    }
}
