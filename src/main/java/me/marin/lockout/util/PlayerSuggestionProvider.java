package me.marin.lockout.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class PlayerSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining();
        int lastSpace = remaining.lastIndexOf(' ');
        
        // Create a builder offset to start from after the last space
        SuggestionsBuilder newBuilder = builder.createOffset(builder.getStart() + lastSpace + 1);
        
        return CommandSource.suggestMatching(context.getSource().getPlayerNames(), newBuilder);
    }
}