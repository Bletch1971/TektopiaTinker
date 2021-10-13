package bletch.tektopiatinker.commands;

import java.util.List;

import bletch.tektopiatinker.core.ModCommands;
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

public class CommandSpawn extends CommandVillageBase {

	private static final String COMMAND_NAME = "spawn";
	
	public CommandSpawn() {
		super(COMMAND_NAME);
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length > 0) {
			throw new WrongUsageException(ModCommands.COMMAND_PREFIX + COMMAND_NAME + ".usage", new Object[0]);
		} 
		
		EntityPlayer entityPlayer = super.getCommandSenderAsPlayer(sender);
		World world = entityPlayer != null ? entityPlayer.getEntityWorld() : null;
		
		if (world == null || world.isRaining() || Village.isNightTime(world)) {
			notifyCommandListener(sender, this, "commands.tinker.spawn.badconditions", new Object[0]);
			LoggerUtils.debug(TextUtils.translate("commands.tinker.spawn.badconditions", new Object[0]), true);
			return;
		}
		
		VillageManager villageManager = world != null ? VillageManager.get(world) : null;
		Village village = villageManager != null && entityPlayer != null ? villageManager.getVillageAt(entityPlayer.getPosition()) : null;
		if (village == null) {
			notifyCommandListener(sender, this, "commands.tinker.spawn.novillage", new Object[0]);
			LoggerUtils.debug(TextUtils.translate("commands.tinker.spawn.novillage", new Object[0]), true);
			return;
		}

		BlockPos spawnPosition = village.getEdgeNode();
		if (spawnPosition == null) {
			notifyCommandListener(sender, this, "commands.tinker.spawn.noposition", new Object[0]);
			LoggerUtils.debug(TextUtils.translate("commands.tinker.spawn.noposition", new Object[0]), true);
			return;
		}

        List<EntityTinker> entityList = world.getEntitiesWithinAABB(EntityTinker.class, village.getAABB().grow(Village.VILLAGE_SIZE));
        if (entityList.size() > 0) {
			notifyCommandListener(sender, this, "commands.tinker.spawn.exists", new Object[0]);
			LoggerUtils.debug(TextUtils.translate("commands.tinker.spawn.exists", new Object[0]), true);
			return;
        }
        
		// attempt to spawn the tinker
		Boolean entitySpawned = TektopiaUtils.trySpawnEntity(world, spawnPosition, (World w) -> new EntityTinker(w));
		
		if (!entitySpawned) {
			notifyCommandListener(sender, this, "commands.tinker.spawn.failed", new Object[0]);
			LoggerUtils.debug(TextUtils.translate("commands.tinker.spawn.failed", new Object[0]), true);
			return;
		}
		
		notifyCommandListener(sender, this, "commands.tinker.spawn.success", new Object[] { TektopiaUtils.formatBlockPos(spawnPosition) });
		LoggerUtils.debug(TextUtils.translate("commands.tinker.spawn.success", new Object[] { TektopiaUtils.formatBlockPos(spawnPosition) }), true);
	}
    
}