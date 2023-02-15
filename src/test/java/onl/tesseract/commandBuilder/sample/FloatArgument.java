package onl.tesseract.commandBuilder.sample;

import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandArgumentBuilderSteps;
import onl.tesseract.commandBuilder.definition.CommandArgumentDefinition;
import org.jetbrains.annotations.NotNull;

public class FloatArgument extends CommandArgument<Float> {
    public FloatArgument(@NotNull final String name)
    {
        super(name);
    }

    @Override
    public void define(final CommandArgumentBuilderSteps.@NotNull Parser<Float> builder)
    {
        builder.parser((input, env) -> Float.parseFloat(input))
               .tabCompleter((input, env) -> null)
               .errorHandler(NumberFormatException.class, "Invalid number");
    }
}
