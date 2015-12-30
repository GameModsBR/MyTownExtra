package br.com.gamemods.mytownextra.classtransformer.tc4;

import mytown.entities.Resident;
import mytown.entities.flag.FlagType;
import mytown.new_datasource.MyTownUniverse;
import mytown.protection.ProtectionManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.world.World;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.io.File;
import java.io.FileOutputStream;

public class FocusWardingCT implements IClassTransformer
{
    public static boolean hook(EntityPlayer player, World world, int x, int y, int z)
    {
        Resident res = MyTownUniverse.instance.getOrMakeResident(player);
        return !ProtectionManager.hasPermission(res, FlagType.MODIFY, world.provider.dimensionId, x, y, z);
    }

    private class Generator extends GeneratorAdapter
    {
        private boolean waitingSecond, waitingIFNE, waitingGoto, waitingALOAD2, patched;
        private Label modificationBlock, skipLabel;
        protected Generator(MethodVisitor mv, int access, String name, String desc)
        {
            super(Opcodes.ASM4, mv, access, name, desc);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc)
        {
            super.visitMethodInsn(opcode, owner, name, desc);
            if(patched) return;
            if(opcode == Opcodes.INVOKEVIRTUAL && ("isBlockNormalCubeDefault".equals(name)||"func_147445_c".equals(name)))
            {
                if(!waitingSecond) waitingSecond = true;
                else waitingIFNE = true;
            }
        }

        @Override
        public void visitJumpInsn(int opcode, Label label)
        {
            super.visitJumpInsn(opcode, label);
            if(patched) return;
            if(waitingIFNE && opcode == Opcodes.IFNE)
            {
                modificationBlock = label;
                waitingGoto = true;
                waitingIFNE = false;
            }
            else if(waitingGoto && opcode == Opcodes.GOTO)
            {
                skipLabel = label;
                waitingALOAD2 = true;
            }
        }

        @Override
        public void visitVarInsn(int opcode, int var)
        {
            if(!patched && waitingALOAD2 && opcode == Opcodes.ALOAD && var == 2)
            {
                super.visitVarInsn(Opcodes.ALOAD, 3); // Player
                super.visitVarInsn(Opcodes.ALOAD, 2); // World
                super.visitVarInsn(Opcodes.ALOAD, 11);
                super.visitFieldInsn(Opcodes.GETFIELD, "thaumcraft/api/BlockCoordinates", "x", "I");
                super.visitVarInsn(Opcodes.ALOAD, 11);
                super.visitFieldInsn(Opcodes.GETFIELD, "thaumcraft/api/BlockCoordinates", "y", "I");
                super.visitVarInsn(Opcodes.ALOAD, 11);
                super.visitFieldInsn(Opcodes.GETFIELD, "thaumcraft/api/BlockCoordinates", "z", "I");
                super.visitMethodInsn(Opcodes.INVOKESTATIC, FocusWardingCT.class.getName().replace('.','/'),
                        "hook", "(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/world/World;III)Z");
                super.visitJumpInsn(Opcodes.IFNE, skipLabel);
                patched = true;
            }

            super.visitVarInsn(opcode, var);
        }
    }

    @Override
    public byte[] transform(String name, String srgName, byte[] bytes)
    {
        if("thaumcraft.common.items.wands.foci.ItemFocusWarding".equals(srgName))
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
            try(FileOutputStream out = new FileOutputStream(new File(name)))
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
