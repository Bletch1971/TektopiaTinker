package bletch.tektopiatinker.entities.ai;

import java.util.function.Function;
import java.util.function.Predicate;

import bletch.tektopiatinker.utils.LoggerUtils;
import net.minecraft.util.math.BlockPos;
import net.tangotek.tektopia.entities.EntityVillagerTek;
import net.tangotek.tektopia.entities.EntityVillagerTek.MovementMode;

public class EntityAIVisitVillage extends EntityAIMoveToBlock {
	protected final Function<EntityVillagerTek, BlockPos> whereFunc;
	protected final Predicate<EntityVillagerTek> shouldPred;
	protected final Runnable resetRunner;
	protected final Runnable startRunner;
	protected final EntityVillagerTek entity;
	protected final MovementMode moveMode;

	public EntityAIVisitVillage(EntityVillagerTek entity, Predicate<EntityVillagerTek> shouldPred, Function<EntityVillagerTek, BlockPos> whereFunc, MovementMode moveMode, Runnable startRunner, Runnable resetRunner) {
		super(entity);
		this.entity = entity;
		this.shouldPred = shouldPred;
		this.whereFunc = whereFunc;
		this.resetRunner = resetRunner;
		this.startRunner = startRunner;
		this.moveMode = moveMode;
	}

	public boolean shouldExecute() {
		if (this.entity.isAITick() && this.entity.hasVillage() && this.shouldPred.test(this.entity))
			return super.shouldExecute();
		return false;
	}

	public void startExecuting() {
		LoggerUtils.info("EntityAILeaveVillage - startExecuting called", true);
		
		if (this.startRunner != null) {
			this.startRunner.run();
		}

		super.startExecuting();
	}

	public void resetTask() {
		LoggerUtils.info("EntityAILeaveVillage - resetTask called", true);
		
		if (this.resetRunner != null) {
			this.resetRunner.run();
		}

		super.resetTask();
	}

	protected BlockPos getDestinationBlock() {
		return (BlockPos)this.whereFunc.apply(this.entity);
	}

	protected boolean isNearWalkPos() {
		return this.entity.getPosition().distanceSq(this.destinationPos) < 4.0D;
	}

	void updateMovementMode() {
		LoggerUtils.info("EntityAILeaveVillage - updateMovementMode called with mode " + this.moveMode.name(), true);
		
		this.entity.setMovementMode(this.moveMode);
	}
}