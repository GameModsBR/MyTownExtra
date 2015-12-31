package br.com.gamemods.mytownextra.classtransformer.tc4;

import mytown.protection.ProtectionHandlers;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.*;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.io.File;
import java.io.FileOutputStream;

public class ServerTickCT implements IClassTransformer
{
    public static PlayerInteractEvent hook(EntityPlayer player, PlayerInteractEvent.Action action, int x, int y, int z,
                                           int face, World world, int wandSlot, ItemStack target)
    {
        int playerSlot = player.inventory.currentItem;
        PlayerInteractEvent interactEvent;
        try
        {
            player.inventory.currentItem = wandSlot;
            interactEvent = ForgeEventFactory.onPlayerInteract(player, action, x, y, z, face, world);
            if (interactEvent.isCanceled())
                return interactEvent;
        }
        finally
        {
            player.inventory.currentItem = playerSlot;
        }

        Block current = world.getBlock(x, y, z);
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(x,y,z,world, current,world.getBlockMetadata(x,y,z),player);
        ProtectionHandlers.instance.onPlayerBreaksBlock(breakEvent);
        if(breakEvent.isCanceled())
        {
            interactEvent.setCanceled(true);
            return interactEvent;
        }

        ItemBlock item = (ItemBlock) target.getItem();
        BlockSnapshot snapshot = new BlockSnapshot(world, x, y, z, item.field_150939_a, item.getMetadata(target.getItemDamage()));
        BlockEvent.PlaceEvent placeEvent = new BlockEvent.PlaceEvent(snapshot, current, player);
        ProtectionHandlers.instance.onBlockPlacement(placeEvent);
        if(placeEvent.isCanceled())
            interactEvent.setCanceled(true);
        return interactEvent;
    }

    private class Generator extends GeneratorAdapter
    {
        boolean patched = false;
        protected Generator(MethodVisitor mv, int access, String name, String desc)
        {
            super(Opcodes.ASM4, mv, access, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf)
        {
            if(!patched && owner.equals("net/minecraftforge/event/ForgeEventFactory") && name.equals("onPlayerInteract"))
            {
                super.visitVarInsn(Opcodes.ALOAD, 5);
                super.visitFieldInsn(Opcodes.GETFIELD,
                        "thaumcraft/common/lib/events/ServerTickEventsFML$VirtualSwapper", "wand", "I");
                super.visitVarInsn(Opcodes.ALOAD, 3);
                super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/concurrent/LinkedBlockingQueue", "peek", "()Ljava/lang/Object;");
                Label elseLabel = new Label();
                super.visitInsn(Opcodes.DUP);
                super.visitJumpInsn(Opcodes.IFNULL, elseLabel);
                super.visitTypeInsn(Opcodes.CHECKCAST, "thaumcraft/common/lib/events/ServerTickEventsFML$VirtualSwapper");
                super.visitFieldInsn(Opcodes.GETFIELD, "thaumcraft/common/lib/events/ServerTickEventsFML$VirtualSwapper", "target", "Lnet/minecraft/item/ItemStack;");
                super.visitLabel(elseLabel);
                super.visitTypeInsn(Opcodes.CHECKCAST, "net/minecraft/item/ItemStack");
                super.visitMethodInsn(Opcodes.INVOKESTATIC,
                        ServerTickCT.class.getName().replace('.','/'),
                        "hook",
                        "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraftforge/event/entity/player/PlayerInteractEvent$Action;" +
                            "IIIILnet/minecraft/world/World;ILnet/minecraft/item/ItemStack;" +
                        ")Lnet/minecraftforge/event/entity/player/PlayerInteractEvent;"
                );

                patched = true;
                return;
            }

            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    @Override
    public byte[] transform(String name, String srgName, byte[] bytes)
    {
        if(srgName.equals("thaumcraft.common.lib.events.ServerTickEventsFML"))
        {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, Opcodes.ASM4);

            ClassVisitor visitor = new ClassVisitor(Opcodes.ASM4, writer)
            {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
                {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                    if("tickBlockSwap".equals(name))
                        return new Generator(mv, access, name, desc);

                    return mv;
                }
            };

            reader.accept(visitor, ClassReader.EXPAND_FRAMES);

            bytes = writer.toByteArray();
            try(FileOutputStream out = new FileOutputStream(new File(name+".class")))
            {
                out.write(bytes);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

        return bytes;
    }
}
