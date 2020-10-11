package tech.dttp.block.logger.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import tech.dttp.block.logger.save.sql.DbConn;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public final class Commands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                literal("bl") // bl
                        .requires(scs -> scs.hasPermissionLevel(3))
                        .then(literal("i") // bl i
                                .then(argument("pos", BlockPosArgumentType.blockPos()) // bl i <pos>
                                        .then(argument("dimension", DimensionArgumentType.dimension()) // bl i <pos> [dimension]
                                                .executes(scs -> getEventsAt(scs.getSource(), BlockPosArgumentType.getBlockPos(scs, "pos"), DimensionArgumentType.getDimensionArgument(scs, "dimension")))
                                        )
                                        .executes(scs -> getEventsAt(scs.getSource(), BlockPosArgumentType.getBlockPos(scs, "pos"), null))
                                )
                        )
                        .then(literal("tool") // bl tool
                                .executes(scs -> giveTool(scs.getSource()))
                        )
        );
    }

    private static int getEventsAt(ServerCommandSource scs, BlockPos pos, ServerWorld world) throws CommandSyntaxException {
        if (world == null) {
            // if run without a dimension argument, get the world the player is in
            // the game will handle the case that the runner isn't a player for us
            // by throwing a REQUIRES_PLAYER_EXCEPTION
            world = scs.getPlayer().getServerWorld();
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        RegistryKey<World> key = world.getRegistryKey();
        // Query the database for all the events at this x, y, z and dimension
        DbConn.readEvents(x, y, z, key.getValue().getNamespace() + ":" + key.getValue().getPath(), null);
        return 1;
    }

    private static int giveTool(ServerCommandSource scs) {
        scs.sendFeedback(new LiteralText("Not yet implemented"), false);
        return 1;
    }
}
