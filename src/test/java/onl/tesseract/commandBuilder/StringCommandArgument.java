package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.v2.ArgumentErrorHandlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StringCommandArgument extends CommandArgument<String> {
    public StringCommandArgument(final String name)
    {
        super(name);
    }

    @Override
    protected @NotNull String parser(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return input;
    }

    @Override
    protected @Nullable List<String> tabCompletion(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return List.of();
    }

    @Override
    protected void errors(final ArgumentErrorHandlers handlers)
    {

    }
}
