package bletch.tektopiatinker.core;

import java.io.File;

import javax.annotation.ParametersAreNonnullByDefault;

import com.leviathanstudio.craftstudio.client.registry.CSRegistryHelper;
import com.leviathanstudio.craftstudio.client.util.EnumRenderType;
import com.leviathanstudio.craftstudio.client.util.EnumResourceType;

import bletch.tektopiatinker.utils.LoggerUtils;
import bletch.tektopiatinker.entities.EntityTinker;
import net.minecraft.client.Minecraft;

@ParametersAreNonnullByDefault
public class ModClientProxy extends ModCommonProxy {

	protected CSRegistryHelper registry = new CSRegistryHelper(ModDetails.MOD_ID);

	@Override
	public boolean isRemote() {
		return true;
	}

	@Override
	public File getMinecraftDirectory() {
		return Minecraft.getMinecraft().mcDataDir;
	}
	   
	@Override
	public void registerCraftStudioAnimations() {
		super.registerCraftStudioAnimations();
		
		LoggerUtils.info("Starting registerCraftStudioAnimations...");
		
		this.registry.register(EnumResourceType.ANIM, EnumRenderType.ENTITY, ModEntities.ANIMATION_VILLAGER_EAT);
		this.registry.register(EnumResourceType.ANIM, EnumRenderType.ENTITY, ModEntities.ANIMATION_VILLAGER_READ);
		this.registry.register(EnumResourceType.ANIM, EnumRenderType.ENTITY, ModEntities.ANIMATION_VILLAGER_RUN);
		this.registry.register(EnumResourceType.ANIM, EnumRenderType.ENTITY, ModEntities.ANIMATION_VILLAGER_SIT);
		this.registry.register(EnumResourceType.ANIM, EnumRenderType.ENTITY, ModEntities.ANIMATION_VILLAGER_SITCHEER);
		this.registry.register(EnumResourceType.ANIM, EnumRenderType.ENTITY, ModEntities.ANIMATION_VILLAGER_SLEEP);
		this.registry.register(EnumResourceType.ANIM, EnumRenderType.ENTITY, ModEntities.ANIMATION_VILLAGER_WALK);
		this.registry.register(EnumResourceType.ANIM, EnumRenderType.ENTITY, ModEntities.ANIMATION_VILLAGER_WALKSAD);
		
		LoggerUtils.info("Finished registerCraftStudioAnimations...");
	}	
	
	@Override
	public void registerCraftStudioModels() {
		super.registerCraftStudioModels();
		
		LoggerUtils.info("Starting registerCraftStudioModels...");
		
		registry.register(EnumResourceType.MODEL, EnumRenderType.ENTITY, EntityTinker.ANIMATION_MODEL_NAME);
		
		LoggerUtils.info("Finished registerCraftStudioModels...");
	}
		
}
