package br.com.gamemods.mytownextra;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@SuppressWarnings("unused")
@IFMLLoadingPlugin.Name("MyTownExtraCore")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.SortingIndex(1001)
public class MyTownExtraCore implements IFMLLoadingPlugin
{
    @Override
    public String[] getASMTransformerClass()
    {
        return new String[]{
            "br.com.gamemods.mytownextra.classtransformer.ae2.ToolNetworkToolCT"
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
