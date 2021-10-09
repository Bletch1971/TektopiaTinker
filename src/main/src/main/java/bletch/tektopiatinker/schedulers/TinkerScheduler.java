package bletch.tektopiatinker.schedulers;

import java.util.List;

import bletch.tektopiatinker.core.ModConfig;
import bletch.tektopiatinker.entities.EntityTinker;
import bletch.tektopiatinker.utils.TektopiaUtils;
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

		// if it is night time, then clear the village checks
		this.checkedVillages = false;
		this.resetNight = true;
	}

	@Override
	public void update(World world) {
		// do not process any further if we have already performed the check, it is raining or it is night
		if (this.checkedVillages || world == null || world.isRaining() || Village.isNightTime(world))
			return;
		
		this.resetNight = false;
		this.checkedVillages = true;

		// get a list of the villages from the VillageManager 
		List<Village> villages = TektopiaUtils.getVillages(world);
		if (villages == null || villages.isEmpty())
			return;

		// cycle through each village
		villages.forEach((v) -> {

			// get the village level (1-5) and test to spawn - bigger villages will reduce the number of spawns of the Tinker.
			int villageLevel = ModConfig.tinker.checksVillageSize ? TektopiaUtils.getVillageLevel(v) : 1;
			if (villageLevel > 0 && world.rand.nextInt(villageLevel) == 0) {
				
				// get a list of the Tinkers in the village
				List<EntityTinker> entityList = world.getEntitiesWithinAABB(EntityTinker.class, v.getAABB().grow(Village.VILLAGE_SIZE));
				if (entityList.size() == 0) {
					
					BlockPos spawnPosition = v.getEdgeNode();

					// attempt spawn
					if (TektopiaUtils.trySpawnEntity(world, spawnPosition, (World w) -> new EntityTinker(w))) {
						v.sendChatMessage(new TextComponentTranslation("message.tinker.spawned", new Object[] { TektopiaUtils.formatBlockPos(spawnPosition) }));
					} else {
						v.sendChatMessage(new TextComponentTranslation("message.tinker.noposition", new Object[0]));
					}
					
				} else {
					v.debugOut(new TextComponentTranslation("message.tinker.exists", new Object[0]).getFormattedText());
				}
			}
		});
	}
}
