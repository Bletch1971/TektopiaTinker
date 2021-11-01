package bletch.tektopiatinker.schedulers;

import java.util.List;

import bletch.tektopiatinker.core.ModConfig;
import bletch.tektopiatinker.entities.EntityTinker;
import bletch.tektopiatinker.utils.TektopiaUtils;
import bletch.tektopiatinker.utils.LoggerUtils;
import bletch.tektopiatinker.utils.TextUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.tangotek.tektopia.Village;

public class TinkerScheduler implements IScheduler {

	protected Boolean checkedVillages = false;
	protected Boolean resetNight = false;

	@Override
	public void resetDay() {
	}

	@Override
	public void resetNight() {
		if (this.resetNight)
			return;
		
		LoggerUtils.info("TinkerScheduler - resetNight called", true);

		// if it is night time, then clear the village checks
		this.checkedVillages = false;
		this.resetNight = true;
	}

	@Override
	public void update(World world) {
		// do not process any further if we have already performed the check, it is raining or it is night
		if (this.checkedVillages || world == null || world.isRaining() || !EntityTinker.isWorkTime(world, 0))
			return;
		
		LoggerUtils.info("TinkerScheduler - update called", true);
		
		this.resetNight = false;
		this.checkedVillages = true;

		// get a list of the villages from the VillageManager 
		List<Village> villages = TektopiaUtils.getVillages(world);
		if (villages == null || villages.isEmpty())
			return;

		// cycle through each village
		villages.forEach((v) -> {

			List<EntityTinker> entityList = null;
			String villageName = v.getName();
			int villageLevel = TektopiaUtils.getVillageLevel(v);
			
			for (int tinkerType : EntityTinker.getTinkerTypes()) {

				// get the village level (1-5) and test to spawn - bigger villages will reduce the number of spawns of the Tinker.
				int villageCheck = ModConfig.tinker.checksVillageSize ? world.rand.nextInt(villageLevel) : 0;
				
				if (villageLevel > 0 && villageCheck == 0) {
					
					LoggerUtils.info(TextUtils.translate("message.tinker.villagechecksuccess", new Object[] { villageName, villageLevel, villageCheck }), true);
					
					// get a list of the Tinkers in the village
					if (entityList == null)
						entityList = world.getEntitiesWithinAABB(EntityTinker.class, v.getAABB().grow(Village.VILLAGE_SIZE));
					
					long tinkerTypeCount = entityList.stream().filter((r) -> r.getTinkerType() == tinkerType).count();
					
					if (tinkerTypeCount == 0) {
						
						BlockPos spawnPosition = TektopiaUtils.getVillageSpawnPoint(world, v);

						// attempt spawn
						if (TektopiaUtils.trySpawnEntity(world, spawnPosition, (World w) -> new EntityTinker(w, tinkerType))) {
							v.sendChatMessage(new TextComponentTranslation("message.tinker.spawned", new Object[0]));
							LoggerUtils.info(TextUtils.translate("message.tinker.spawned.village", new Object[] { villageName, TektopiaUtils.formatBlockPos(spawnPosition) }), true);
						} else {
							LoggerUtils.info(TextUtils.translate("message.tinker.noposition.village", new Object[] { villageName }), true);
						}
						
					} else {
						LoggerUtils.info(TextUtils.translate("message.tinker.exists", new Object[] { villageName }), true);
					}
					
				} else {
					LoggerUtils.info(TextUtils.translate("message.tinker.villagecheckfailed", new Object[] { villageName, villageLevel, villageCheck }), true);
				}
			}
		});
	}
}
