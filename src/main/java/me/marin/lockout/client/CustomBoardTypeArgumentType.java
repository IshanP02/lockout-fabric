package me.marin.lockout.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.marin.lockout.Lockout;
import me.marin.lockout.client.gui.BoardTypeIO;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Argument type for custom BoardType names with auto-completion.
 */
public class CustomBoardTypeArgumentType implements ArgumentType<String> {

    public static CustomBoardTypeArgumentType newInstance() {
        return new CustomBoardTypeArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String s = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        try {
            List<String> boardTypeNames = BoardTypeIO.INSTANCE.getSavedBoardTypes();
            if (!boardTypeNames.contains(s)) {
                throw new SimpleCommandExceptionType(Text.of("Invalid board type name.")).createWithContext(reader);
            }
        } catch (IOException e) {
            Lockout.error(e);
        }
        return s;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        try {
            List<String> boardTypeNames = BoardTypeIO.INSTANCE.getSavedBoardTypes();
            return CommandSource.suggestMatching(boardTypeNames, builder);
        } catch (IOException e) {
            Lockout.error(e);
            return Suggestions.empty();
        }
    }

}
