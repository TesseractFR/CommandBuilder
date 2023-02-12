package onl.tesseract.commandBuilder.sample;

import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandEnvironment;
import onl.tesseract.commandBuilder.v2.ArgumentErrorHandlers;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
    protected @NotNull Player parser(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return player;
    }

    @Override
    protected @Nullable List<String> tabCompletion(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return null;
    }

    @Override
    protected void errors(final ArgumentErrorHandlers handlers)
    {

    }
}
