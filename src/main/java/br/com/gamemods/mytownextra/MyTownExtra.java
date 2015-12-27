package br.com.gamemods.mytownextra;

import br.com.gamemods.mytownextra.handlers.ProtectMyPlaneHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = "MyTownExtra", name="MyTown Extra", version = "1.0-SNAPSHOT",
    dependencies = "after:ProtectMyPlane;required-after:MyTown2;after:universalcoins",
    acceptableRemoteVersions = "*"
)
public class MyTownExtra
{
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        if(Loader.isModLoaded("ProtectMyPlane"))
            FMLCommonHandler.instance().bus().register(new ProtectMyPlaneHandler());
    }
}
