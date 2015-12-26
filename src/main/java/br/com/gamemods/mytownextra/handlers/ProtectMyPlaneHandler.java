package br.com.gamemods.mytownextra.handlers;

import br.com.gamemods.protectmyplane.event.AircraftAttackEvent;
import br.com.gamemods.protectmyplane.event.AircraftDropEvent;
import br.com.gamemods.protectmyplane.event.PlayerPilotAircraftEvent;
import br.com.gamemods.protectmyplane.event.PlayerSpawnVehicleEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import myessentials.entities.BlockPos;
import mytown.entities.Resident;
import mytown.entities.flag.FlagType;
import mytown.new_datasource.MyTownUniverse;
import mytown.protection.ProtectionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

public class ProtectMyPlaneHandler
{
    @SubscribeEvent
    public void onAircraftAttack(AircraftAttackEvent ev) {
        if(ev.entity.worldObj.isRemote || ev.isCanceled()) {
            return;
        }

        if(ev.source.getEntity() != null) {
            if (ev.source.getEntity() instanceof EntityPlayer) {
                EntityPlayer entityPlayer = (EntityPlayer) ev.source.getEntity();
                if(entityPlayer.getPersistentID().equals(ev.ownerId)) {
                    // Bypass check for the owner
                    return;
                }
                // Player vs Plane
                Resident res = MyTownUniverse.instance.getOrMakeResident(ev.source.getEntity());
                ProtectionManager.checkInteraction(ev.entity, res, ev);

                if(!ev.isCanceled()) {
                    System.out.println("DamageType: "+ev.source.damageType+" ev.ownerId:"+ev.ownerId);
                    //if(ev.ownerId != null)
                    //System.out.println("N-equals:"+!ev.ownerId.equals(ev.source.getEntity().getPersistentID())+" " +
                    //        "Permission:"+!ProtectionManager.hasPermission(res, FlagType.MODIFY, ev.entity.worldObj.provider.dimensionId,
                    //        (int)ev.entity.posX, (int)ev.entity.posY, (int)ev.entity.posZ));
                    if(ev.ownerId != null && !ev.ownerId.equals(ev.source.getEntity().getPersistentID())) {
                        // Non-owner attack
                        if(ev.entity.riddenByEntity instanceof EntityPlayer) {
                            entityPlayer.addChatComponentMessage(new ChatComponentTranslation("vehicle.you.are.not.the.owner", ev.ownerName));
                            ev.setCanceled(true);
                            return;
                        }

                        if(ev.source.damageType.equals("arrow") || ev.source.damageType.equals("fireball")
                                || ev.source.damageType.equals("thrown") || ev.source.damageType.equals("player")) {
                            entityPlayer.addChatComponentMessage(new ChatComponentTranslation("vehicle.you.are.not.the.owner", ev.ownerName));
                            ev.setCanceled(true);
                            return;
                        }

                        /*if(ev.source.damageType.equals("player") &&
                                ProtectionManager.hasPermission(res, FlagType.MODIFY, ev.entity.worldObj.provider.dimensionId,
                                        (int)ev.entity.posX, (int)ev.entity.posY, (int)ev.entity.posZ)) {
                            //noinspection unchecked
                            for(EntityPlayer player: (List<EntityPlayer>) MinecraftServer.getServer().getConfigurationManager().playerEntityList){
                                if(player.getPersistentID().equals(ev.ownerId))
                                    return;
                            }

                            entityPlayer.addChatComponentMessage(new ChatComponentTranslation("vehicle.you.are.not.the.owner", ev.ownerName));
                            ev.setCanceled(true);
                            return;
                        }*/

                    }
                }
            } else {
                // Entity vs Living Entity
            }
        } else {
            // Non-Entity Damage
        }
    }

    @SubscribeEvent
    public void onPlayerPilotEvent(PlayerPilotAircraftEvent ev) {
        if(ev.entity.worldObj.isRemote || ev.isCanceled()
                || (ev.entityPlayer instanceof EntityPlayerMP
                && ((EntityPlayerMP) ev.entityPlayer).theItemInWorldManager.getGameType() == WorldSettings.GameType.CREATIVE)) {
            return;
        }

        Resident res = MyTownUniverse.instance.getOrMakeResident(ev.entityPlayer);
        ProtectionManager.checkInteraction(ev.entity, res, ev);

        if(!ev.isCanceled()) {
            if(ev.ownerId != null && !ev.ownerId.equals(ev.entityPlayer.getPersistentID())) {
                ev.setCanceled(true);
                ev.entityPlayer.addChatComponentMessage(new ChatComponentTranslation("vehicle.you.are.not.the.owner", ev.ownerName));
            }
        }
    }

    @SubscribeEvent
    public void onPlayerSpawnVehicle(PlayerSpawnVehicleEvent ev) {
        Resident res = MyTownUniverse.instance.getOrMakeResident(ev.entityPlayer);
        if(!ProtectionManager.hasPermission(res, FlagType.MODIFY, ev.entityPlayer.worldObj.provider.dimensionId,ev.x,ev.y,ev.z)) {
            int heightValue = 0;
            for(int x=-2; x<=2;x++)
                for(int z=-2; z<=2; z++)
                    for(int y = 255; y>=5;y--)
                        if(ev.entity.worldObj.getBlock(ev.x+x, y, ev.z+z) != Blocks.air) {
                            heightValue = y-1;
                            break;
                        };

            if(ev.y < heightValue) {
                ev.setCanceled(true);
                ev.entityPlayer.addChatComponentMessage(new ChatComponentTranslation("gamemods.you.can.only.place.on.open.sky"));
                return;
            }
        }

        ProtectionManager.checkUsage(ev.stack, res, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, new BlockPos(ev.x,ev.y,ev.z,ev.entityPlayer.worldObj.provider.dimensionId),0, ev);
    }

    @SubscribeEvent
    public void onAircraftDrop(AircraftDropEvent event) {
        if(event.ownerId == null)
            return;

        //noinspection unchecked
        for(EntityPlayer player: (List<EntityPlayer>) MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            if(player.getPersistentID().equals(event.ownerId)) {
                ItemStack stack = new ItemStack(event.item, event.amount, 0);
                if(!player.inventory.addItemStackToInventory(stack)) {
                    player.dropItem(event.item, event.amount);
                }
                player.inventoryContainer.detectAndSendChanges();

                player.addChatComponentMessage(new ChatComponentTranslation("gamemode.vehicle.restored"));
                event.setCanceled(true);
                return;
            }
        }
    }
}
