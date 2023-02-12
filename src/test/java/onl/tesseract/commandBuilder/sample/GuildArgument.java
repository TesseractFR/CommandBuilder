package onl.tesseract.commandBuilder.sample;

import onl.tesseract.Guild;
import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandEnvironment;
import onl.tesseract.commandBuilder.v2.ArgumentErrorHandlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GuildArgument extends CommandArgument<Guild> {
    public GuildArgument(@NotNull final String name)
    {
        super(name);
    }

    @Override
    protected @NotNull Guild parser(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        Guild guild = Guild.forName(input);
        if (guild == null)
            throw new IllegalArgumentException();
        return guild;
    }

    @Override
    protected @Nullable List<String> tabCompletion(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return null;
    }

    @Override
    protected void errors(final ArgumentErrorHandlers handlers)
    {
        handlers.on(IllegalArgumentException.class, "invalid guild");
    }
}
