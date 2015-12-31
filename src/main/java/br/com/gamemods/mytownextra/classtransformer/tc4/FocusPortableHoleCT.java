package br.com.gamemods.mytownextra.classtransformer.tc4;

import mytown.entities.Resident;
import mytown.entities.flag.FlagType;
import mytown.new_datasource.MyTownUniverse;
import mytown.protection.ProtectionManager;
import mytown.protection.segment.Segment;
import mytown.protection.segment.SegmentEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class FocusPortableHoleCT implements IClassTransformer
{
    public static EntityPlayer holeOwner;

    public static boolean hookRightClick(ItemStack stack, World world, EntityPlayer player, MovingObjectPosition mop)
    {
        holeOwner = player;
        return !canOpenHole(world, mop.blockX, mop.blockY, mop.blockZ);
    }

    public static boolean canOpenHole(World world, int x, int y, int z)
    {
        if(holeOwner == null)
            return false;
        Resident resident = MyTownUniverse.instance.getOrMakeResident(holeOwner);
        boolean perms =  ProtectionManager.hasPermission(resident, FlagType.ACTIVATE, world.provider.dimensionId, x, y, z)
            && ProtectionManager.hasPermission(resident, FlagType.ENTER, world.provider.dimensionId, x, y, z);

        if(!perms)
            return false;

        @SuppressWarnings("unchecked")
        List<Entity> entityList =
            world.getEntitiesWithinAABB(Entity.class, AxisAlignedBB.getBoundingBox(x-2,y-1,z-2,x+3,y+2,z+3));

        for(Entity entity: entityList)
        {
            for(SegmentEntity segment: ProtectionManager.segmentsEntity.get(entity.getClass()))
            {
                if(!segment.shouldInteract(entity, resident))
                {
                    resident.protectionDenial(FlagType.PVE);
                    return false;
                }
            }
        }

        //noinspection unchecked
        entityList =
                world.getEntitiesWithinAABB(EntityPlayer.class, AxisAlignedBB.getBoundingBox(x-1,y+1,z-1,x+2,y+2,z+2));

        for(Entity entity: entityList)
        {
            if(!entity.getPersistentID().equals(resident.getUUID()) && entity.posY >= y+1
                    && !ProtectionManager.hasPermission(resident, FlagType.PVP, entity.worldObj.provider.dimensionId, (int)entity.posX, (int)entity.posY, (int)entity.posZ))
                return false;
        }

        return true;
    }

    private class RightClickGenerator extends GeneratorAdapter
    {
        private boolean patched, waitingIF_ACMPNE;
        protected RightClickGenerator(MethodVisitor mv, int access, String name, String desc)
        {
            super(Opcodes.ASM4, mv, access, name, desc);
        }

        @Override
        public void visitFieldInsn(int opcode, String owner, String name, String desc)
        {
            super.visitFieldInsn(opcode, owner, name, desc);

            if(!patched && !waitingIF_ACMPNE && opcode == Opcodes.GETSTATIC && owner.equals("net/minecraft/util/MovingObjectPosition$MovingObjectType"))
            {
                waitingIF_ACMPNE = true;
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label)
        {
            super.visitJumpInsn(opcode, label);

            if(!patched && waitingIF_ACMPNE && opcode == Opcodes.IF_ACMPNE)
            {
                super.visitVarInsn(Opcodes.ALOAD, 1);
                super.visitVarInsn(Opcodes.ALOAD, 2);
                super.visitVarInsn(Opcodes.ALOAD, 3);
                super.visitVarInsn(Opcodes.ALOAD, 4);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, FocusPortableHoleCT.class.getName().replace('.','/'),
                        "hookRightClick", "(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/MovingObjectPosition;)Z"
                );
                super.visitJumpInsn(Opcodes.IFNE, label);
                patched = true;
            }
        }
    }

    private class OpenHoleGenerator extends GeneratorAdapter
    {
        private boolean patched, waitingIFEQ;
        protected OpenHoleGenerator(MethodVisitor mv, int access, String name, String desc)
        {
            super(Opcodes.ASM4, mv, access, name, desc);
        }

        @Override
        public void visitLdcInsn(Object cst)
        {
            super.visitLdcInsn(cst);
            if(!patched && !waitingIFEQ && cst instanceof Float && cst.equals(-1f))
            {
                waitingIFEQ = true;
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label)
        {
            super.visitJumpInsn(opcode, label);
            if(!patched && waitingIFEQ && opcode == Opcodes.IFEQ)
            {
                super.visitVarInsn(Opcodes.ALOAD, 0);
                super.visitVarInsn(Opcodes.ILOAD, 1);
                super.visitVarInsn(Opcodes.ILOAD, 2);
                super.visitVarInsn(Opcodes.ILOAD, 3);
                super.visitMethodInsn(Opcodes.INVOKESTATIC, FocusPortableHoleCT.class.getName().replace('.','/'),
                        "canOpenHole", "(Lnet/minecraft/world/World;III)Z"
                );
                super.visitJumpInsn(Opcodes.IFEQ, label);
                patched = true;
            }
        }
    }

    @Override
    public byte[] transform(String name, String srgName, byte[] bytes)
    {
        if("thaumcraft.common.items.wands.foci.ItemFocusPortableHole".equals(srgName))
        {
            ClassReader reader = new ClassReader(bytes);
            ClassWriter writer = new ClassWriter(reader, Opcodes.ASM4);

            ClassVisitor visitor = new ClassVisitor(Opcodes.ASM4, writer)
            {
                @Override
                public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions)
                {
                    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

                    if("onFocusRightClick".equals(name))
                        return new RightClickGenerator(mv, access, name, desc);
                    if("createHole".equals(name))
                        return new OpenHoleGenerator(mv, access, name, desc);

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
