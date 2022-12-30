package bletch.tektopiatinker.core;

import bletch.common.core.CommonEntities;
import bletch.tektopiatinker.entities.EntityTinker;
import bletch.tektopiatinker.entities.render.RenderTinker;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.registries.IForgeRegistry;

public class ModEntities extends CommonEntities {

    public static void register(IForgeRegistry<EntityEntry> registry) {
        int id = 1;

        registry.register(EntityEntryBuilder.create()
                .entity(EntityTinker.class)
                .id(new ResourceLocation(ModDetails.MOD_ID, EntityTinker.RESOURCE_PATH), id++)
                .name(EntityTinker.ENTITY_NAME)
                .egg(2697513, 7494986)
                .tracker(128, 1, true)
                .build()
        );
    }

    @SideOnly(Side.CLIENT)
    public static void registerModels() {
        RenderingRegistry.registerEntityRenderingHandler(EntityTinker.class, RenderTinker.FACTORY);
    }

}
