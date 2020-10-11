package tech.dttp.block.logger.util;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

public class PlayerUtils {

    /**
     * Gets the namespaced ID of the dimension the player is in.
     *
     * @param player the player
     * @return namespaced dimension id, eg: minecraft:overworld
     */
    public static String getPlayerDimension(PlayerEntity player) {
        RegistryKey<World> key = player.getEntityWorld().getRegistryKey();
        return key.getValue().getNamespace() + ":" + key.getValue().getPath();
    }
}
