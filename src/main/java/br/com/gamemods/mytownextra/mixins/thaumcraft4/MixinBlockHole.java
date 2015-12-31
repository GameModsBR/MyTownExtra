package br.com.gamemods.mytownextra.mixins.thaumcraft4;

import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;
import org.spongepowered.asm.mixin.Mixin;
import thaumcraft.common.blocks.BlockHole;

@Mixin(BlockHole.class)
public abstract class MixinBlockHole extends BlockContainer
{
    protected MixinBlockHole(Material p_i45386_1_)
    {
        super(p_i45386_1_);
    }

    @Override
    public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plantable)
    {
        return true;
    }

    @Override
    public boolean canSustainLeaves(IBlockAccess world, int x, int y, int z)
    {
        return true;
    }
}
