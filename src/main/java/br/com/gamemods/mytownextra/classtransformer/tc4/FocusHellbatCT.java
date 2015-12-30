package br.com.gamemods.mytownextra.classtransformer.tc4;

import mytown.entities.Resident;
import mytown.entities.flag.FlagType;
import mytown.new_datasource.MyTownUniverse;
import mytown.protection.ProtectionManager;
import mytown.protection.segment.SegmentEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.io.File;
import java.io.FileOutputStream;

public class FocusHellbatCT implements IClassTransformer
{
    public static boolean hook(EntityPlayer player, Entity target)
    {
        Resident res = MyTownUniverse.instance.getOrMakeResident(player);
        if(target instanceof EntityPlayer)
        {
            return !ProtectionManager.hasPermission(res, FlagType.PVP, target.worldObj.provider.dimensionId, (int)target.posX, (int)target.posY, (int)target.posZ);
        }
        else
        {
            for(SegmentEntity segment : ProtectionManager.segmentsEntity.get(target.getClass()))
                if(!segment.shouldInteract(target, res))
                    return true;

            return false;
        }
    }

    private class Generator extends GeneratorAdapter
    {
        private boolean patched;
        protected Generator(MethodVisitor mv, int access, String name, String desc)
        {
            super(Opcodes.ASM4, mv, access, name, desc);
        }

        @Override
        public void visitTypeInsn(int opcode, String type)
        {
            if(!patched && opcode == Opcodes.NEW && type.equals("thaumcraft/common/entities/monster/EntityFireBat"))
            {
                super.visitVarInsn(Opcodes.ALOAD, 3); // player
                super.visitVarInsn(Opcodes.ALOAD, 6); // pointedEntity
                super.visitMethodInsn(Opcodes.INVOKESTATIC, FocusHellbatCT.class.getName().replace('.','/'),
                        "hook", "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/entity/Entity;)Z");

                Label elseLabel = new Label();
                super.visitJumpInsn(Opcodes.IFEQ, elseLabel);
                super.visitVarInsn(Opcodes.ALOAD, 1);
                super.visitInsn(Opcodes.ARETURN);
                super.visitLabel(elseLabel);
                patched = true;
            }

            super.visitTypeInsn(opcode, type);
        }
    }

    @Override
    public byte[] transform(String name, String srgName, byte[] bytes)
    {
        if("thaumcraft.common.items.wands.foci.ItemFocusHellbat".equals(srgName))
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
