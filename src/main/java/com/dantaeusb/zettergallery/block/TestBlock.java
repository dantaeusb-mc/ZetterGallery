package com.dantaeusb.zettergallery.block;

import com.dantaeusb.zettergallery.tileentity.PaintingMerchantTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public class TestBlock extends Block implements EntityBlock {
    public TestBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PaintingMerchantTileEntity(pos, state);
    }

    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {
        if (worldIn.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            this.interactWith(worldIn, pos, player);
            return InteractionResult.CONSUME;
        }
    }

    /**
     * Interface for handling interaction with blocks that impliment AbstractFurnaceBlock. Called in onBlockActivated
     * inside AbstractFurnaceBlock.
     */
    protected void interactWith(Level worldIn, BlockPos pos, Player player) {
        BlockEntity currentTileEntity = worldIn.getBlockEntity(pos);
        if (currentTileEntity instanceof PaintingMerchantTileEntity) {
            player.openMenu((MenuProvider) currentTileEntity);
        }
    }
}
