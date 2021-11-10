package bletch.tektopiatinker.commands;

import bletch.tektopiatinker.core.ModDetails;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.command.CommandTreeBase;
import net.minecraftforge.server.permission.DefaultPermissionLevel;
import net.minecraftforge.server.permission.PermissionAPI;

public class TinkerCommands extends CommandTreeBase {

    public static final String COMMAND_PREFIX = "commands.tinker.";
    public static final String COMMAND_PREFIX_WITH_MODID = ModDetails.MOD_ID + "." + COMMAND_PREFIX;

    public TinkerCommands() {
        super.addSubcommand(new CommandTinkerKill());
        super.addSubcommand(new CommandTinkerSpawn());
    }

    public void registerNodes() {
        this.getSubCommands().forEach((c) -> PermissionAPI.registerNode(COMMAND_PREFIX_WITH_MODID + c.getName(), DefaultPermissionLevel.OP, c.getName()));
    }

    @Override
    public String getName() {
        return "tinker";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return true;
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return COMMAND_PREFIX + "usage";
    }
}
