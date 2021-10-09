package bletch.tektopiatinker.schedulers;

import net.minecraft.world.World;

public interface IScheduler {

	void resetDay();
	
	void resetNight();
	
	void update(World world);
	
}
