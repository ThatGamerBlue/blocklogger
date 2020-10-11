package tech.dttp.block.logger;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;

import tech.dttp.block.logger.save.sql.DbConn;
import tech.dttp.block.logger.command.Commands;

public class BlockLogger implements ModInitializer {
    @Override
    public void onInitialize() {
        //Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            Commands.register(dispatcher);
        });
        // Register database connection
        ServerLifecycleEvents.SERVER_STARTED.register(DbConn::connect);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            //SQL
            //Write to database every time a block is broken
            DbConn.writeBreak(pos.getX(), pos.getY(), pos.getZ(), state, player);
        });
        //todo: Block placement pos via hitresult
    }
}

