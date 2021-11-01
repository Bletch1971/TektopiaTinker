package bletch.tektopiatinker.commands;

import java.util.List;

import bletch.tektopiatinker.entities.EntityTinker;
import bletch.tektopiatinker.utils.LoggerUtils;
import bletch.tektopiatinker.utils.TektopiaUtils;
import bletch.tektopiatinker.utils.TextUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.VillageManager;

public class CommandTinkerSpawn extends CommandTinkerBase {

	private static final String COMMAND_NAME = "spawn";
	
	public CommandTinkerSpawn() {
		super(COMMAND_NAME);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length < 1 || args.length > 2) {
			throw new WrongUsageException(TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".usage", new Object[0]);
		} 
		
		Boolean spawnNearMe = false;
		if (args.length > 1) {
			if (!args[1].equalsIgnoreCase("me")) {
				throw new WrongUsageException(TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".usage", new Object[0]);
			}
			
			spawnNearMe = true;
		}
		
		int argValue = 0;
		try {
			argValue = Integer.parseInt(args[0]);
			
			if (!EntityTinker.isTinkerTypeValid(argValue)) {
				throw new WrongUsageException(TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".usage", new Object[0]);
			}
		}
		catch (Exception ex) {
			throw new WrongUsageException(TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".usage", new Object[0]);
		}
		
        int tinkerType = argValue;
		
		EntityPlayer entityPlayer = super.getCommandSenderAsPlayer(sender);
		World world = entityPlayer != null ? entityPlayer.getEntityWorld() : null;
		
		if (world == null || world.isRaining() || Village.isNightTime(world)) {
			notifyCommandListener(sender, this, TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".badconditions", new Object[0]);
			LoggerUtils.info(TextUtils.translate(TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".badconditions", new Object[0]), true);
			return;
		}
		
		VillageManager villageManager = world != null ? VillageManager.get(world) : null;
		Village village = villageManager != null && entityPlayer != null ? villageManager.getVillageAt(entityPlayer.getPosition()) : null;
		
		if (village == null) {
			notifyCommandListener(sender, this, TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".novillage", new Object[0]);
			LoggerUtils.info(TextUtils.translate(TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".novillage", new Object[0]), true);
			return;
		}

		BlockPos spawnPosition = spawnNearMe ? entityPlayer.getPosition() : TektopiaUtils.getVillageSpawnPoint(world, village);
		
		if (spawnPosition == null) {
			notifyCommandListener(sender, this, TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".noposition", new Object[0]);
			LoggerUtils.info(TextUtils.translate(TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".noposition", new Object[0]), true);
			return;
		}

        List<EntityTinker> entityList = world.getEntitiesWithinAABB(EntityTinker.class, village.getAABB().grow(Village.VILLAGE_SIZE));
		long tinkerTypeCount = entityList.stream().filter((r) -> r.getTinkerType() == tinkerType).count();
        
        if (tinkerTypeCount > 0) {
			notifyCommandListener(sender, this, TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".exists", new Object[0]);
			LoggerUtils.info(TextUtils.translate(TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".exists", new Object[0]), true);
			return;
        }
        
		// attempt to spawn the tinker
		if (!TektopiaUtils.trySpawnEntity(world, spawnPosition, (World w) -> new EntityTinker(w, tinkerType))) {
			notifyCommandListener(sender, this, TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".failed", new Object[0]);
			LoggerUtils.info(TextUtils.translate(TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".failed", new Object[0]), true);
			return;
		}
		
		notifyCommandListener(sender, this, TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".success", new Object[] { TektopiaUtils.formatBlockPos(spawnPosition) });
		LoggerUtils.info(TextUtils.translate(TinkerCommands.COMMAND_PREFIX + COMMAND_NAME + ".success", new Object[] { TektopiaUtils.formatBlockPos(spawnPosition) }), true);
	}
    
}
