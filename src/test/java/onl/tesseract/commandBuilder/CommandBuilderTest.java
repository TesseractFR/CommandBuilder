package onl.tesseract.commandBuilder;

import onl.tesseract.TPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class CommandBuilderTest {
    CommandSender sender;

    @BeforeEach
    void setUp()
    {
        sender = mock(CommandSender.class);
    }

    @Test
    public void SimpleMoney()
    {
        // Given
        Player test = mock(Player.class);
        CommandBuilder moneyCommand = new CommandBuilder();
        moneyCommand.withArg(new CommandArgument("player", Player.class)
                             .supplier(input -> test))
                    .command(env -> {
            env.get("player", Player.class).sendMessage("test");
        });

        moneyCommand.execute(sender, List.of("GabRay"));
        verify(test, times(1)).sendMessage("test");
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void MoneyGivePlayer()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder();
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                             .supplier(input -> player))
                    .withArg(new CommandArgument("quantity", Float.class)
                                     .supplier(Float::parseFloat)
                             .error(NumberFormatException.class, "Nombre invalide"))
                    .command(env -> {
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity", Float.class));
                    });

        moneyCommand.execute(sender, List.of("GabRay", "42"));
        verify(player, times(1)).giveMoney(42);
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void MoneyGivePlayerErrorInvalidNumber()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder();
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier(input -> player))
                    .withArg(new CommandArgument("quantity", Float.class)
                                     .supplier(Float::parseFloat)
                                     .error(NumberFormatException.class, "Nombre invalide"))
                    .command(env -> {
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity", Float.class));
                    });

        moneyCommand.execute(sender, List.of("GabRay", "invalid number"));
        verify(player, times(0)).giveMoney(anyFloat());
        verify(sender, times(1)).sendMessage(anyString());
    }
}