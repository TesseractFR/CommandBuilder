package onl.tesseract.commandBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StringCommandArgument extends CommandArgument<String> {
    public StringCommandArgument(final String name)
    {
        super(name);
    }

    @Override
    public void define(final CommandArgumentBuilderSteps.@NotNull Parser<String> builder)
    {
        builder.parser((input, env) -> input)
               .tabCompleter((input, env) -> List.of());
    }
}
