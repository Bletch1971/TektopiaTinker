package bletch.tektopiatinker.commands;

import bletch.common.commands.CommonCommandBase;
import bletch.common.utils.TektopiaUtils;
import bletch.common.utils.TextUtils;
import bletch.tektopiatinker.core.ModDetails;
import bletch.tektopiatinker.entities.EntityTinker;
import bletch.tektopiatinker.utils.LoggerUtils;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.tangotek.tektopia.Village;
import net.tangotek.tektopia.VillageManager;

import java.util.List;

public class CommandTinkerSpawn extends CommonCommandBase {

    private static final String COMMAND_NAME = "spawn";

    public CommandTinkerSpawn() {
        super(ModDetails.MOD_ID, TinkerCommands.COMMAND_PREFIX, COMMAND_NAME);
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 1 || args.length > 2) {
            throw new WrongUsageException(this.prefix + COMMAND_NAME + ".usage");
        }

        boolean spawnNearMe = false;
        if (args.length > 1) {
            if (!args[1].equalsIgnoreCase("me")) {
                throw new WrongUsageException(this.prefix + COMMAND_NAME + ".usage");
            }

            spawnNearMe = true;
        }

        int argValue;
        try {
            argValue = Integer.parseInt(args[0]);

            if (!EntityTinker.isTinkerTypeValid(argValue)) {
                throw new WrongUsageException(this.prefix + COMMAND_NAME + ".usage");
            }
        } catch (Exception ex) {
            throw new WrongUsageException(this.prefix + COMMAND_NAME + ".usage");
        }

        int tinkerType = argValue;

        EntityPlayer entityPlayer = getCommandSenderAsPlayer(sender);
        World world = entityPlayer != null ? entityPlayer.getEntityWorld() : null;

        if (world == null || world.isRaining() || Village.isNightTime(world)) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".badconditions");
            LoggerUtils.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".badconditions"), true);
            return;
        }

        VillageManager villageManager = world != null ? VillageManager.get(world) : null;
        Village village = villageManager != null && entityPlayer != null ? villageManager.getVillageAt(entityPlayer.getPosition()) : null;

        if (village == null) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".novillage");
            LoggerUtils.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".novillage"), true);
            return;
        }

        BlockPos spawnPosition = spawnNearMe ? entityPlayer.getPosition() : TektopiaUtils.getVillageSpawnPoint(world, village);

        if (spawnPosition == null) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".noposition");
            LoggerUtils.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".noposition"), true);
            return;
        }

        List<EntityTinker> entityList = world.getEntitiesWithinAABB(EntityTinker.class, village.getAABB().grow(Village.VILLAGE_SIZE));
        long tinkerTypeCount = entityList.stream().filter((r) -> r.getTinkerType() == tinkerType).count();

        if (tinkerTypeCount > 0) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".exists");
            LoggerUtils.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".exists"), true);
            return;
        }

        // attempt to spawn the tinker
        if (!TektopiaUtils.trySpawnEntity(world, spawnPosition, (World w) -> new EntityTinker(w, tinkerType))) {
            notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".failed");
            LoggerUtils.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".failed"), true);
            return;
        }

        notifyCommandListener(sender, this, this.prefix + COMMAND_NAME + ".success", TektopiaUtils.formatBlockPos(spawnPosition));
        LoggerUtils.info(TextUtils.translate(this.prefix + COMMAND_NAME + ".success", TektopiaUtils.formatBlockPos(spawnPosition)), true);
    }

}
