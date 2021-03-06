package bletch.tektopiatinker;

import bletch.common.schedulers.ScheduleManager;
import bletch.tektopiatinker.commands.TinkerCommands;
import bletch.tektopiatinker.core.ModCommonProxy;
import bletch.tektopiatinker.core.ModDetails;
import bletch.tektopiatinker.core.ModEntities;
import bletch.tektopiatinker.schedulers.TinkerScheduler;
import bletch.tektopiatinker.utils.LoggerUtils;
import com.leviathanstudio.craftstudio.client.registry.CraftStudioLoader;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.ParametersAreNonnullByDefault;

@Mod(modid = ModDetails.MOD_ID, name = ModDetails.MOD_NAME, version = ModDetails.MOD_VERSION, dependencies = ModDetails.MOD_DEPENDENCIES, acceptableRemoteVersions = "*", acceptedMinecraftVersions = "[1.12.2]", updateJSON = ModDetails.MOD_UPDATE_URL)
@ParametersAreNonnullByDefault
public class TektopiaTinker {

    @Instance(ModDetails.MOD_ID)
    public static TektopiaTinker instance;

    @SidedProxy(clientSide = ModDetails.MOD_CLIENT_PROXY_CLASS, serverSide = ModDetails.MOD_SERVER_PROXY_CLASS)
    public static ModCommonProxy proxy;

    public static ScheduleManager scheduleManager;

    @Mod.EventHandler
    public void preInitialize(FMLPreInitializationEvent e) {
        instance = this;

        proxy.resetDebug();
        proxy.preInitialize(e);
    }

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent e) {
        proxy.initialize(e);
    }

    @Mod.EventHandler
    public void postInitialize(FMLPostInitializationEvent e) {
        proxy.postInitialize(e);
    }

    @Mod.EventHandler
    public void onServerStarting(final FMLServerStartingEvent e) {

        LoggerUtils.instance.info("Starting command registrations...");

        LoggerUtils.instance.info("Registering tinker commands");
        TinkerCommands commands = new TinkerCommands();
        e.registerServerCommand(commands);
        commands.registerNodes();

        LoggerUtils.instance.info("Finished command registrations");

        World world = e.getServer().getEntityWorld();

        LoggerUtils.instance.info("Starting ScheduleManager setup...");

        // create the schedule manager
        scheduleManager = new ScheduleManager(world);
        scheduleManager.addScheduler(new TinkerScheduler());

        LoggerUtils.instance.info("Finished ScheduleManager setup");
    }

    @EventBusSubscriber
    public static class RegistrationHandler {

        @SubscribeEvent
        public static void worldTick(WorldTickEvent e) {
            if (e.phase == Phase.START) {
                scheduleManager.onWorldTick(e);
            }
        }

        @CraftStudioLoader
        public static void registerCraftStudio() {
            proxy.registerCraftStudioModels();
            proxy.registerCraftStudioAnimations();
        }

        @SubscribeEvent
        public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
            ModEntities.register(event.getRegistry());
        }

        @SubscribeEvent
        @SideOnly(Side.CLIENT)
        public static void registerModels(ModelRegistryEvent event) {
            ModEntities.registerModels();
        }

    }

}
