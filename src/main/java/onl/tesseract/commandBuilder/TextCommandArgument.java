package onl.tesseract.commandBuilder;

import org.jetbrains.annotations.NotNull;

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
    public void define(final CommandArgumentBuilderSteps.@NotNull Parser<String> builder)
    {
        builder.parser((input, env) -> input)
               .tabCompleter((input, env) -> null);
    }
}
