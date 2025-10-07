package me.marin.lockout.command.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import me.marin.lockout.server.LockoutServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerArgumentType implements ArgumentType<ServerPlayerEntity> {
    @Override
    public ServerPlayerEntity parse(StringReader reader) throws CommandSyntaxException {
        try {
            String string = reader.readString();
            PlayerManager playerManager = LockoutServer.server.getPlayerManager();

            if (playerManager.getPlayer(string) != null) {
                return playerManager.getPlayer(string);
            }

            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create("Player name " + string + " is invalid.");
        } catch (Exception e)
        {
            // Throw an exception if player name is not valid.
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException().create("Invalid player name.");
        }
    }
}
