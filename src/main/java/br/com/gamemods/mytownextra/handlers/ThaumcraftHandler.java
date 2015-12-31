package br.com.gamemods.mytownextra.handlers;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.ExplosionEvent;
import thaumcraft.common.items.wands.ItemWandCasting;
import thaumcraft.common.tiles.TileInfusionMatrix;

import java.util.concurrent.TimeUnit;

public class ThaumcraftHandler
{
    private Cache<TileInfusionMatrix, EntityPlayer> activators = CacheBuilder.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES).build();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event)
    {
        TileEntity te = event.world.getTileEntity(event.x, event.y, event.z);
        if(!(te instanceof TileInfusionMatrix))
            return;
        ItemStack stack = event.entityPlayer.inventory.getCurrentItem();
        if(stack == null || !(stack.getItem() instanceof ItemWandCasting))
            return;

        activators.put((TileInfusionMatrix) te, event.entityPlayer);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onExplosion(ExplosionEvent.Start ev)
    {
        if(ev.explosion.exploder == null)
        {
            for(int x = -12; x <= 12; x++)
                for(int z = -12; z <= 12; z++)
                    for(int y = -5; y <= 10; y++)
                    {
                        TileEntity te = ev.world.getTileEntity((int)ev.explosion.explosionX+x, (int)ev.explosion.explosionY+y, (int)ev.explosion.explosionZ+z);
                        if(te instanceof TileInfusionMatrix)
                        {
                            TileInfusionMatrix matrix = (TileInfusionMatrix) te;
                            NBTTagCompound compound = new NBTTagCompound();
                            matrix.writeCustomNBT(compound);
                            String recipePlayer = compound.getString("recipePlayer");
                            if(!recipePlayer.isEmpty())
                            {
                                EntityPlayer activator = activators.getIfPresent(matrix);
                                if(activator != null)
                                {
                                    ev.explosion.exploder = activator;
                                    return;
                                }

                                EntityPlayer player = ev.world.getPlayerEntityByName(recipePlayer);
                                if(player != null)
                                {
                                    ev.explosion.exploder = player;
                                    activators.put(matrix, player);
                                    return;
                                }

                                for(WorldServer world: MinecraftServer.getServer().worldServers)
                                {
                                    player = world.getPlayerEntityByName(recipePlayer);
                                    if(player != null)
                                    {
                                        ev.explosion.exploder = player;
                                        activators.put(matrix, player);
                                        return;
                                    }
                                }
                            }
                        }
                    }

        }
    }
}
