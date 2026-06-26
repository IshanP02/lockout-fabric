package me.marin.lockout.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.marin.lockout.Lockout;
import me.marin.lockout.client.gui.BoardBuilderIO;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CustomBoardFileArgumentType implements ArgumentType<String> {

    public CustomBoardFileArgumentType() {}

    public static CustomBoardFileArgumentType newInstance() {
        return new CustomBoardFileArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String s = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        try {
            List<String> boardNames = BoardBuilderIO.INSTANCE.getSavedBoards();
            if (!boardNames.contains(s)) {
                throw new SimpleCommandExceptionType(Component.literal("Invalid board name.")).createWithContext(reader);
            }
        } catch (IOException e) {
            Lockout.error(e);
        }
        return s;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        try {
            List<String> boardNames = BoardBuilderIO.INSTANCE.getSavedBoards();
            return SharedSuggestionProvider.suggest(boardNames, builder);
        } catch (IOException e) {
            Lockout.error(e);
            return Suggestions.empty();
        }
    }

}
