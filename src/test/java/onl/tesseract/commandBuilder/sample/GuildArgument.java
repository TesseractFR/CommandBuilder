package onl.tesseract.commandBuilder.sample;

import onl.tesseract.Guild;
import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandArgumentBuilderSteps;
import onl.tesseract.commandBuilder.definition.CommandArgumentDefinition;
import org.jetbrains.annotations.NotNull;

public class GuildArgument extends CommandArgument<Guild> {
    public GuildArgument(@NotNull final String name)
    {
        super(name);
    }

    @Override
    public void define(final CommandArgumentBuilderSteps.@NotNull Parser<Guild> builder)
    {
        builder.parser((input, env) -> {
                   Guild guild = Guild.forName(input);
                   if (guild == null)
                       throw new IllegalArgumentException();
                   return guild;
               })
               .tabCompleter((input, env) -> null)
               .errorHandler(IllegalArgumentException.class, "Invalid guild");
    }
}
