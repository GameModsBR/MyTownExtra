package br.com.gamemods.mytownextra.mixins.thaumcraft4;

import mytown.entities.flag.FlagType;
import mytown.protection.ProtectionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import thaumcraft.common.items.equipment.ItemElementalPickaxe;

@Mixin(ItemElementalPickaxe.class)
public abstract class MixinElementalPickaxe extends ItemPickaxe
{
    protected MixinElementalPickaxe(ToolMaterial p_i45347_1_)
    {
        super(p_i45347_1_);
    }

    /*
    @Inject(method = "onLeftClickEntity", at=@At(value = "INVOKE", target = "setFire", shift = At.Shift.BEFORE), cancellable = true)
    private void onFire(ItemStack stack, EntityPlayer player, Entity entity, CallbackInfoReturnable<Boolean> ci)
    {
        if(entity instanceof EntityLiving)
        {
            LivingAttackEvent event = new LivingAttackEvent((EntityLiving)entity, new EntityDamageSource("fire", player), 2);
            ProtectionHandlers.instance.onLivingAttack(event);
            if(event.isCanceled())
            {
                ci.setReturnValue(false);
                ci.cancel();
            }
        }
    }*/

    /*
    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity)
    {
        return super.onLeftClickEntity(stack, player, entity);
    }
    */

    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity)
    {
        if(!player.worldObj.isRemote && (
                !(entity instanceof EntityPlayer) || MinecraftServer.getServer().isPVPEnabled()
            )
        ){
            if(entity instanceof EntityLivingBase)
            {
                int x = (int) Math.floor(entity.posX);
                int y = (int) Math.floor(entity.posY);
                int z = (int) Math.floor(entity.posZ);
                Boolean flag = ProtectionManager.getFlagValueAtLocation(entity instanceof EntityPlayer? FlagType.PVP : FlagType.PVE, entity.dimension, x, y, z);
                if(flag == Boolean.FALSE)
                    return false;
            }

            entity.setFire(2);
        }

        return false;
    }
}
