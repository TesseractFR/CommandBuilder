package onl.tesseract.commandBuilder.sample;

import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandArgumentBuilderSteps;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlayerArgument extends CommandArgument<Player> {
    private Player player;

    protected PlayerArgument(@NotNull final String name)
    {
        super(name);
    }

    public PlayerArgument(@NotNull final String name, final Player player)
    {
        super(name);
        this.player = player;
    }

    @Override
    public void define(final CommandArgumentBuilderSteps.@NotNull Parser<Player> builder)
    {
        builder.parser((input, env) -> player)
               .tabCompleter((input, env) -> null);
    }
}
