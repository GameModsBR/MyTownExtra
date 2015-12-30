package br.com.gamemods.mytownextra.mixins.thaumcraft4;

import mytown.entities.flag.FlagType;
import mytown.protection.ProtectionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import thaumcraft.common.items.equipment.ItemVoidPickaxe;

@Mixin(ItemVoidPickaxe.class)
public abstract class MixinVoidPickaxe extends ItemPickaxe
{
    protected MixinVoidPickaxe(ToolMaterial p_i45347_1_)
    {
        super(p_i45347_1_);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity)
    {
        if(!player.worldObj.isRemote && entity instanceof EntityLivingBase &&
                (!(entity instanceof EntityPlayer) || MinecraftServer.getServer().isPVPEnabled()))
        {
            int x = (int) Math.floor(entity.posX);
            int y = (int) Math.floor(entity.posY);
            int z = (int) Math.floor(entity.posZ);
            Boolean flag = ProtectionManager.getFlagValueAtLocation(entity instanceof EntityPlayer? FlagType.PVP : FlagType.PVE, entity.dimension, x, y, z);
            if(flag == Boolean.FALSE)
                return false;

            ((EntityLivingBase)entity).addPotionEffect(new PotionEffect(Potion.weakness.getId(), 80));
        }

        return false;
    }
}
