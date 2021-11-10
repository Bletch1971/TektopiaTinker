package bletch.tektopiatinker.entities.render;

import bletch.tektopiatinker.entities.EntityTinker;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraftforge.fml.client.registry.IRenderFactory;

public class RenderTinker<T extends EntityTinker> extends RenderVillager<T> {
    public static final RenderTinker.Factory<EntityTinker> FACTORY;

    public RenderTinker(RenderManager manager) {
        super(manager, EntityTinker.MODEL_NAME, false, 64, 64, EntityTinker.MODEL_NAME);
    }

    public static class Factory<T extends EntityTinker> implements IRenderFactory<T> {
        public Render<? super T> createRenderFor(RenderManager manager) {
            return new RenderTinker<EntityTinker>(manager);
        }
    }

    static {
        FACTORY = new RenderTinker.Factory<>();
    }

}
