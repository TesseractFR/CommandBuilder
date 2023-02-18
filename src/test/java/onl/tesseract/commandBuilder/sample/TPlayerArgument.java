package onl.tesseract.commandBuilder.sample;

import onl.tesseract.TPlayer;
import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandArgumentBuilderSteps;
import org.jetbrains.annotations.NotNull;

public class TPlayerArgument extends CommandArgument<TPlayer> {
    private TPlayer player;

    protected TPlayerArgument(@NotNull final String name)
    {
        super(name);
    }

    public TPlayerArgument(@NotNull final String name, final TPlayer player)
    {
        super(name);
        this.player = player;
    }

    @Override
    public void define(final CommandArgumentBuilderSteps.@NotNull Parser<TPlayer> builder)
    {
        builder.parser((input, env) -> player)
               .tabCompleter((input, env) -> null);
    }
}
