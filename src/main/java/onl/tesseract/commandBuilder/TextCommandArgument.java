package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.v2.ArgumentErrorHandlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TextCommandArgument extends CommandArgument<String> {
    /**
     * Create a new command argument, to be used with CommandBuilder
     *
     * @param name name of the argument.
     */
    public TextCommandArgument(String name)
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
        return null;
    }

    @Override
    protected void errors(final ArgumentErrorHandlers handlers)
    {
        // No errors to handle
    }
}
