package br.com.gamemods.mytownextra;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.util.Map;

@SuppressWarnings("unused")
@IFMLLoadingPlugin.Name("MyTownExtraCore")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1001)
public class MyTownExtraCore implements IFMLLoadingPlugin
{
    public MyTownExtraCore()
    {
        MixinBootstrap.init();

        MixinEnvironment.getDefaultEnvironment()
                .addConfiguration("mixins.mytownextra.json");
    }

    @Override
    public String[] getASMTransformerClass()
    {
        return new String[]{
            "br.com.gamemods.mytownextra.classtransformer.tc4.FocusWardingCT",
            "br.com.gamemods.mytownextra.classtransformer.tc4.FocusHellbatCT",
            //"br.com.gamemods.mytownextra.classtransformer.ae2.ToolNetworkToolCT" -- Moved to MyTown2
        };
    }

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {

    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }
}
