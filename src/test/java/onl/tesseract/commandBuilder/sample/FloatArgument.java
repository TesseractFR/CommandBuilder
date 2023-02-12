package onl.tesseract.commandBuilder.sample;

import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandEnvironment;
import onl.tesseract.commandBuilder.v2.ArgumentErrorHandlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FloatArgument extends CommandArgument<Float> {
    public FloatArgument(@NotNull final String name)
    {
        super(name);
    }

    @Override
    protected @NotNull Float parser(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return Float.parseFloat(input);
    }

    @Override
    protected @Nullable List<String> tabCompletion(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return null;
    }

    @Override
    protected void errors(final ArgumentErrorHandlers handlers)
    {
        handlers.on(NumberFormatException.class, "Invalid number");
    }
}
