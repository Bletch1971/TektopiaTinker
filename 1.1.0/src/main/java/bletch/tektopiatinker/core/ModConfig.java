package bletch.tektopiatinker.core;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Type;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.ParametersAreNonnullByDefault;

@Config(modid = ModDetails.MOD_ID, category = "")
@ParametersAreNonnullByDefault
public class ModConfig {

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent event) {

        if (event.getModID().equals(ModDetails.MOD_ID)) {
            ConfigManager.sync(ModDetails.MOD_ID, Type.INSTANCE);
        }

    }

    @Config.LangKey("config.debug")
    public static final Debug debug = new Debug();

    @Config.LangKey("config.tinker")
    public static final Tinker tinker = new Tinker();

    public static class Debug {

        @Config.Comment("If true, debug information will be output to the console.")
        @Config.LangKey("config.debug.enableDebug")
        public boolean enableDebug = false;

    }

    public static class Tinker {

        @Config.Comment("The amount of emeralds required for each tinker. Set to 0 to prevent emerald charge. Default: 1")
        @Config.LangKey("config.tinker.emeraldsPerTinker")
        @Config.RangeInt(min = 0, max = 64)
        public int emeraldsPerTinker = 1;

        @Config.Comment("The number of tinkers that can be made for each item per day. Default: 5")
        @Config.LangKey("config.tinker.tinkersperday")
        @Config.RangeInt(min = 1, max = 99999)
        public int tinkersperday = 5;

        @Config.Comment("If enabled, when trying to spawn a tinker it will check the size of the village. The more villagers the less often the tinker will spawn. Default: True")
        @Config.LangKey("config.tinker.checksvillagesize")
        public Boolean checksVillageSize = true;
    }

}
