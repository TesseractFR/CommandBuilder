package onl.tesseract.commandBuilder.v2.argument;

import onl.tesseract.commandBuilder.CommandEnvironment;
import onl.tesseract.commandBuilder.v2.CommandArgument;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IntegerArgument extends CommandArgument<Integer> {

    public IntegerArgument(@NotNull final String name)
    {
        super(name);
    }

    @Override
    protected @NotNull Integer parser(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return Integer.parseInt(input);
    }

    @Override
    protected @NotNull List<String> tabCompletion(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return List.of("1", "2", "3");
    }
}
