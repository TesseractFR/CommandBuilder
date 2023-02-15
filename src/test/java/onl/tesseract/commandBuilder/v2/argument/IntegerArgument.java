package onl.tesseract.commandBuilder.v2.argument;

import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandArgumentBuilderSteps;
import onl.tesseract.commandBuilder.definition.CommandArgumentDefinition;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class IntegerArgument extends CommandArgument<Integer> {

    public IntegerArgument(@NotNull final String name)
    {
        super(name);
    }

    @Override
    public void define(final CommandArgumentBuilderSteps.@NotNull Parser<Integer> builder)
    {
        builder.parser((input, env) -> {
                   int i = Integer.parseInt(input);
                   if (i == -1)
                       throw new IllegalStateException("Error testing behavior");
                   return i;
               })
               .tabCompleter((input, env) -> List.of("1", "2", "3"))
               .errorHandler(NumberFormatException.class, "Invalid number");
    }
}
