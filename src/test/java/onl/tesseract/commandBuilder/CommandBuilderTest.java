package onl.tesseract.commandBuilder;

import onl.tesseract.Guild;
import onl.tesseract.Parcel;
import onl.tesseract.TPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class CommandBuilderTest {
    CommandSender sender;

    @BeforeEach
    void setUp()
    {
        Guild.guilds.clear();
        sender = mock(CommandSender.class);
    }

    @Test
    public void SimpleMoney()
    {
        // Given
        Player test = mock(Player.class);
        CommandBuilder moneyCommand = new CommandBuilder();
        moneyCommand.withArg(new CommandArgument("player", Player.class)
                             .supplier((input, env) -> test))
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
                             .supplier((input, env) -> player))
                    .withArg(new CommandArgument("quantity", Float.class)
                                     .supplier((input, env) -> Float.parseFloat(input))
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
                                     .supplier((input, env) -> player))
                    .withArg(new CommandArgument("quantity", Float.class)
                                     .supplier((input, env) -> Float.parseFloat(input))
                                     .error(NumberFormatException.class, "Nombre invalide"))
                    .command(env -> {
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity", Float.class));
                    });

        moneyCommand.execute(sender, List.of("GabRay", "invalid number"));
        verify(player, times(0)).giveMoney(anyFloat());
        verify(sender, times(1)).sendMessage(anyString());
    }

    @Test
    public void argWithDependency()
    {
        // GIVEN
        Guild guild = mock(Guild.class);
        when(guild.getName()).thenReturn("Phoenix");
        Parcel parcel = mock(Parcel.class);
        when(parcel.getName()).thenReturn("maison");
        when(guild.getParcels()).thenReturn(new HashSet<>(Set.of(parcel)));
        Guild.guilds.add(guild);

        CommandBuilder getParcelCommand = new CommandBuilder();
        getParcelCommand.withArg(new CommandArgument("guild", Guild.class)
                                         .supplier((input, env) -> {
                                             Guild g = Guild.forName(input);
                                             if (g == null)
                                                 throw new IllegalArgumentException();
                                             return g;
                                         }).error(IllegalArgumentException.class, "Ville introuvable"))
                        .withArg(new CommandArgument("parcel", Parcel.class)
                                        .supplier((input, env) -> {
                                            Parcel p = Parcel.forName(env.get("guild", Guild.class), input);
                                            if (p == null)
                                                throw new IllegalArgumentException();
                                            return p;
                                        })
                                 .error(IllegalArgumentException.class, "Parcelle introuvable"))
                        .command(env -> {
                            assertEquals("maison", env.get("parcel", Parcel.class).getName());
                        });

        getParcelCommand.execute(sender, List.of("Phoenix", "maison"));
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void argWithDependencyError()
    {
        // GIVEN
        Guild guild = mock(Guild.class);
        when(guild.getName()).thenReturn("Phoenix");
        Parcel parcel = mock(Parcel.class);
        when(parcel.getName()).thenReturn("maison");
        when(guild.getParcels()).thenReturn(new HashSet<>(Set.of(parcel)));
        Guild.guilds.add(guild);

        CommandBuilder getParcelCommand = new CommandBuilder();
        getParcelCommand.withArg(new CommandArgument("guild", Guild.class)
                                         .supplier((input, env) -> {
                                             Guild g = Guild.forName(input);
                                             if (g == null)
                                                 throw new IllegalArgumentException();
                                             return g;
                                         }).error(IllegalArgumentException.class, "Ville introuvable"))
                        .withArg(new CommandArgument("parcel", Parcel.class)
                                         .supplier((input, env) -> {
                                             Parcel p = Parcel.forName(env.get("guild", Guild.class), input);
                                             if (p == null)
                                                 throw new IllegalArgumentException();
                                             return p;
                                         })
                                         .error(IllegalArgumentException.class, "Parcelle introuvable"))
                        .command(env -> fail());

        getParcelCommand.execute(sender, List.of("inexistent guild", "maison"));
        verify(sender, times(1)).sendMessage(anyString());
    }

    /**
     * /money {player} {give} {amount}
     */

    @Test
    public void subCommand()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyGiveCommand = new CommandBuilder();
        moneyGiveCommand.withArg(new CommandArgument("quantity", Float.class)
                                         .supplier((input, env) -> Float.parseFloat(input))
                                         .error(NumberFormatException.class, "Nombre invalide"))
                        .command(env -> {
                            env.get("player", TPlayer.class).giveMoney(env.get("quantity", Float.class));
                        });
        CommandBuilder moneyCommand = new CommandBuilder();
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .subCommand("give", moneyGiveCommand)
                    .command(env -> fail());

        moneyCommand.execute(sender, List.of("GabRay", "give", "42"));
        verify(sender, times(0)).sendMessage(anyString());
        verify(player, times(1)).giveMoney(42);
    }

    @Test
    public void MoneyGivePlayerOptionalArgument()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder();
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .withOptionalArg(new OptionalCommandArgument("quantity", Float.class)
                                     .supplier((input, env) -> Float.parseFloat(input))
                                     .error(NumberFormatException.class, "Nombre invalide")
                                     .defaultValue(env -> 21))
                    .command(env -> {
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity", Float.class));
                    });

        moneyCommand.execute(sender, List.of("GabRay", "42"));
        verify(player, times(1)).giveMoney(42);
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void MoneyGivePlayerOptionalArgumentOpt()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder();
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .withOptionalArg(new OptionalCommandArgument("quantity", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 21f))
                    .command(env -> {
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity", Float.class));
                    });

        moneyCommand.execute(sender, List.of("GabRay"));
        verify(player, times(1)).giveMoney(21);
        verify(sender, times(0)).sendMessage(anyString());
    }
}