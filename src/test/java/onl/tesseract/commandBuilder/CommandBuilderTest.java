package onl.tesseract.commandBuilder;

import onl.tesseract.Guild;
import onl.tesseract.Parcel;
import onl.tesseract.TPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new CommandArgument("player", Player.class)
                                     .supplier((input, env) -> test))
                    .command((sender, env) -> env.get("player", Player.class).sendMessage("test"));

        moneyCommand.execute(sender, new String[] {"GabRay"});
        verify(test, times(1)).sendMessage("test");
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void MoneyGivePlayer()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .withArg(new CommandArgument("quantity", Float.class)
                                     .supplier((input, env) -> Float.parseFloat(input))
                                     .error(NumberFormatException.class, "Nombre invalide"))
                    .command((sender, env) -> env.get("player", TPlayer.class).giveMoney(env.get("quantity", Float.class)));

        moneyCommand.execute(sender, new String[] {"GabRay", "42"});
        verify(player, times(1)).giveMoney(42);
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void MoneyGivePlayerErrorInvalidNumber()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .withArg(new CommandArgument("quantity", Float.class)
                                     .supplier((input, env) -> Float.parseFloat(input))
                                     .error(NumberFormatException.class, "Nombre invalide"))
                    .command((sender, env) -> env.get("player", TPlayer.class).giveMoney(env.get("quantity", Float.class)));

        moneyCommand.execute(sender, new String[] {"GabRay", "invalid number"});
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

        CommandBuilder getParcelCommand = new CommandBuilder("getParcel");
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
                        .command((sender, env) -> assertEquals("maison", env.get("parcel", Parcel.class).getName()));

        getParcelCommand.execute(sender, new String[] {"Phoenix", "maison"});
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

        CommandBuilder getParcelCommand = new CommandBuilder("getParcel");
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
                        .command((sender, env) -> fail());

        getParcelCommand.execute(sender, new String[] {"inexistent guild", "maison"});
        verify(sender, times(1)).sendMessage(anyString());
    }

    /**
     * /money {player} {give} {amount}
     */

    @Test
    public void subCommand()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyGiveCommand = new CommandBuilder("give");
        moneyGiveCommand.withArg(new CommandArgument("quantity", Float.class)
                                         .supplier((input, env) -> Float.parseFloat(input))
                                         .error(NumberFormatException.class, "Nombre invalide"))
                        .command((sender, env) -> env.get("player", TPlayer.class).giveMoney(env.get("quantity", Float.class)));
        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .subCommand(moneyGiveCommand)
                    .command((sender, env) -> fail());

        moneyCommand.execute(sender, new String[] {"GabRay", "give", "42"});
        verify(sender, times(0)).sendMessage(anyString());
        verify(player, times(1)).giveMoney(42);
    }

    @Test
    public void MoneyGivePlayerOptionalArgument()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .withOptionalArg(new OptionalCommandArgument("quantity", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 21))
                    .command((sender, env) -> env.get("player", TPlayer.class).giveMoney(env.get("quantity", Float.class)));

        moneyCommand.execute(sender, new String[] {"GabRay", "42"});
        verify(player, times(1)).giveMoney(42);
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void MoneyGivePlayerOptionalArgumentOpt()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .withOptionalArg(new OptionalCommandArgument("quantity", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 21f))
                    .command((sender, env) -> env.get("player", TPlayer.class).giveMoney(env.get("quantity", Float.class)));

        moneyCommand.execute(sender, new String[] {"GabRay"});
        verify(player, times(1)).giveMoney(21);
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void optionalManyAllGiven()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .withOptionalArg(new OptionalCommandArgument("quantity1", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 1f))
                    .withOptionalArg(new OptionalCommandArgument("quantity2", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 2f))
                    .withOptionalArg(new OptionalCommandArgument("quantity3", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 3f))
                    .command((sender, env) -> {
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity1", Float.class));
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity2", Float.class));
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity3", Float.class));
                    });

        moneyCommand.execute(sender, new String[] {"GabRay", "4", "5", "6"});
        verify(player, times(1)).giveMoney(4);
        verify(player, times(1)).giveMoney(5);
        verify(player, times(1)).giveMoney(6);
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void optionalManyOneGiven()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .withOptionalArg(new OptionalCommandArgument("quantity1", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 1f))
                    .withOptionalArg(new OptionalCommandArgument("quantity2", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 2f))
                    .withOptionalArg(new OptionalCommandArgument("quantity3", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 3f))
                    .command((sender, env) -> {
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity1", Float.class));
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity2", Float.class));
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity3", Float.class));
                    });

        moneyCommand.execute(sender, new String[] {"GabRay", "4"});
        verify(player, times(1)).giveMoney(4);
        verify(player, times(1)).giveMoney(2);
        verify(player, times(1)).giveMoney(3);
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void optionalManyTwoGiven()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .withOptionalArg(new OptionalCommandArgument("quantity1", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 1f))
                    .withOptionalArg(new OptionalCommandArgument("quantity2", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 2f))
                    .withOptionalArg(new OptionalCommandArgument("quantity3", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 3f))
                    .command((sender, env) -> {
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity1", Float.class));
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity2", Float.class));
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity3", Float.class));
                    });

        moneyCommand.execute(sender, new String[] {"GabRay", "4", "5"});
        verify(player, times(1)).giveMoney(4);
        verify(player, times(1)).giveMoney(5);
        verify(player, times(1)).giveMoney(3);
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void optionalManyNoneGiven()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .withOptionalArg(new OptionalCommandArgument("quantity1", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 1f))
                    .withOptionalArg(new OptionalCommandArgument("quantity2", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 2f))
                    .withOptionalArg(new OptionalCommandArgument("quantity3", Float.class)
                                             .supplier((input, env) -> Float.parseFloat(input))
                                             .error(NumberFormatException.class, "Nombre invalide")
                                             .defaultValue(env -> 3f))
                    .command((sender, env) -> {
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity1", Float.class));
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity2", Float.class));
                        env.get("player", TPlayer.class).giveMoney(env.get("quantity3", Float.class));
                    });

        moneyCommand.execute(sender, new String[] {"GabRay"});
        verify(player, times(1)).giveMoney(1);
        verify(player, times(1)).giveMoney(2);
        verify(player, times(1)).giveMoney(3);
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void severalSubCommands()
    {
        TPlayer player = mock(TPlayer.class);

        CommandBuilder giveCommand = new CommandBuilder("give");
        giveCommand.withArg(new CommandArgument("amount", Float.class)
                                    .supplier((input, env) -> Float.parseFloat(input))
                                    .error(NumberFormatException.class, "Nombre invalide"))
                   .command(
                           (sender, env) -> env.get("player", TPlayer.class).giveMoney(env.get("amount", Float.class)));
        CommandBuilder takeCommand = new CommandBuilder("take");
        takeCommand.withArg(new CommandArgument("amount", Float.class)
                                    .supplier((input, env) -> Float.parseFloat(input))
                                    .error(NumberFormatException.class, "Nombre invalide"))
                   .command(
                           (sender, env) -> env.get("player", TPlayer.class).takeMoney(env.get("amount", Float.class)));
        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new CommandArgument("player", TPlayer.class)
                                     .supplier((input, env) -> player))
                    .subCommand(giveCommand)
                    .subCommand(takeCommand)
                    .command((sender, env) -> env.get("player", TPlayer.class).getMoney());

        moneyCommand.execute(sender, new String[] {"GabRay", "give", "42"});
        verify(player, times(1)).giveMoney(42);
        verify(player, times(0)).takeMoney(anyFloat());
        verify(player, times(0)).getMoney();
        verify(sender, times(0)).sendMessage(anyString());

        reset(player, sender);

        moneyCommand.execute(sender, new String[] {"GabRay", "take", "42"});
        verify(player, times(0)).giveMoney(42);
        verify(player, times(1)).takeMoney(anyFloat());
        verify(player, times(0)).getMoney();
        verify(sender, times(0)).sendMessage(anyString());

        reset(player, sender);

        moneyCommand.execute(sender, new String[] {"GabRay"});
        verify(player, times(0)).giveMoney(42);
        verify(player, times(0)).takeMoney(anyFloat());
        verify(player, times(1)).getMoney();
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void permissionAllow()
    {
        Player player = mock(Player.class);
        when(sender.hasPermission("perm")).thenReturn(true);
        CommandBuilder cmd = new CommandBuilder("cmd");
        CommandBuilder subCmd = new CommandBuilder("subCmd");
        subCmd.permission("perm")
              .command((sender, env) -> player.getName());
        cmd.subCommand(subCmd)
           .command((sender, env) -> fail());

        cmd.execute(sender, new String[] {"subCmd"});
        verify(player, times(1)).getName();
        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void permissionDeny()
    {
        Player player = mock(Player.class);
        when(sender.hasPermission("perm")).thenReturn(false);
        CommandBuilder cmd = new CommandBuilder("cmd");
        CommandBuilder subCmd = new CommandBuilder("subCmd");
        subCmd.permission("perm")
              .command((sender, env) -> player.getName());
        cmd.subCommand(subCmd)
           .command((sender, env) -> fail());

        cmd.execute(sender, new String[] {"subCmd"});
        verify(player, times(0)).getName();
        verify(sender, times(1)).sendMessage(anyString());
    }

    @Test
    public void tabCompleteArgs()
    {
        CommandBuilder cmd = new CommandBuilder("cmd");
        cmd.withArg(new CommandArgument("foo", String.class)
                            .supplier((input, env) -> input)
                            .tabCompletion((sender, env) -> List.of("foo", "fooo")))
           .withArg(new CommandArgument("bar", String.class)
                            .supplier((input, env) -> input)
                            .tabCompletion((sender, env) -> List.of("bar", "baz")))
           .command((sender, env) -> {
           });

        List<String> list = cmd.tabComplete(sender, new String[] {"fo"});
        assertEquals(2, list.size());
        assertEquals("foo", list.get(0));
        assertEquals("fooo", list.get(1));

        list = cmd.tabComplete(sender, new String[] {"foo", "ba"});
        assertEquals(2, list.size());
        assertEquals("bar", list.get(0));
        assertEquals("baz", list.get(1));
    }

    @Test
    public void tabCompleteArgsNull()
    {
        CommandBuilder cmd = new CommandBuilder("cmd");
        cmd.withArg(new CommandArgument("foo", String.class)
                            .supplier((input, env) -> input)
                            .tabCompletion((sender, env) -> List.of("foo", "fooo")))
           .withArg(new CommandArgument("bar", String.class)
                            .supplier((input, env) -> input)
                            .tabCompletion((sender, env) -> null))
           .command((sender, env) -> {
           });

        List<String> list = cmd.tabComplete(sender, new String[] {"fo"});
        assertEquals(2, list.size());
        assertEquals("foo", list.get(0));
        assertEquals("fooo", list.get(1));

        list = cmd.tabComplete(sender, new String[] {"foo", "ba"});
        assertNull(list);
    }

    @Test
    public void tabCompleteDependency()
    {
        CommandBuilder cmd = new CommandBuilder("cmd");
        cmd.withArg(new CommandArgument("foo", Integer.class)
                            .supplier((input, env) -> Integer.parseInt(input))
                            .tabCompletion((sender, env) -> null))
           .withArg(new CommandArgument("bar", Integer.class)
                            .supplier((input, env) -> Integer.parseInt(input))
                            .tabCompletion((sender, env) -> List.of("" + (env.get("foo", Integer.class) + 1))))
           .command((sender, env) -> {
           });

        List<String> list = cmd.tabComplete(sender, new String[] {"2", ""});
        assertEquals(1, list.size());
        assertEquals("3", list.get(0));
    }

    @Test
    public void tabCompleteSubCommands()
    {
        CommandBuilder subCmd1 = new CommandBuilder("subCmd1");
        CommandBuilder subCmd2 = new CommandBuilder("subCmd2");

        CommandBuilder cmd = new CommandBuilder("cmd");
        cmd.withArg(new CommandArgument("foo", String.class)
                            .supplier((input, env) -> input)
                            .tabCompletion((sender, env) -> List.of("foo", "fooo")))
           .subCommand(subCmd1)
           .subCommand(subCmd2)
           .command((sender, env) -> fail());

        List<String> list = cmd.tabComplete(sender, new String[] {"foo", "sub"});
        assertEquals(2, list.size());
        assertTrue(list.get(0).equals("subCmd1") || list.get(1).equals("subCmd1"));
        assertTrue(list.get(0).equals("subCmd2") || list.get(1).equals("subCmd2"));
    }

    @Test
    public void malformedMissingCommand()
    {
        CommandBuilder cmd = new CommandBuilder("name");
        assertThrows(IllegalStateException.class, () -> cmd.execute(sender, new String[0]));
    }

    @Test
    public void malformedMissingSupplier()
    {
        CommandBuilder cmd = new CommandBuilder("cmd");
        cmd.withArg(new CommandArgument("foo", String.class)
                            .tabCompletion((sender, env) -> List.of("foo", "fooo")))
           .command((sender, env) -> fail());

        assertThrows(IllegalStateException.class, () -> cmd.execute(sender, new String[] {"foo"}));
    }

    @Test
    public void commandPlayerOnly()
    {
        CommandSender senderPlayer = mock(Player.class);
        CommandBuilder cmd = new CommandBuilder("cmd");
        cmd.playerOnly(true)
           .command((sender, env) -> sender.getName());

        cmd.execute(senderPlayer, new String[0]);
        verify(senderPlayer, times(1)).getName();
        verify(senderPlayer, times(0)).sendMessage(anyString());
    }

    @Test
    public void commandPlayerOnlyFail()
    {
        CommandBuilder cmd = new CommandBuilder("cmd");
        cmd.playerOnly(true)
           .command((sender, env) -> sender.getName());

        cmd.execute(sender, new String[0]);
        verify(sender, times(0)).getName();
        verify(sender, times(1)).sendMessage(anyString());
    }

    @Test
    public void textArgument()
    {
        CommandBuilder cmd = new CommandBuilder("cmd")
                .withArg(new TextCommandArgument("text"))
                .command((sender, env) -> sender.sendMessage(env.get("text", String.class)));

        cmd.execute(sender, new String[] {"Hello", "world", "!"});
        verify(sender, times(1)).sendMessage("Hello world !");
    }

    @Test
    public void completionInSubCommand()
    {
        CommandBuilder cmd = new CommandBuilder("cmd");
        CommandBuilder subCmd = new CommandBuilder("subCmd");
        subCmd.withArg(new CommandArgument("arg", String.class)
                       .supplier((input, env) -> input)
                       .tabCompletion((sender, env) -> List.of("foo", "bar")));
        cmd.subCommand(subCmd)
           .command((sender, env) -> fail());

        var list = cmd.tabComplete(sender, new String[] {"subCmd", ""});
        assertEquals(2, list.size());
        assertTrue(list.get(0).equals("foo") || list.get(0).equals("bar"));
        assertTrue(list.get(1).equals("foo") || list.get(1).equals("bar"));
    }

    @Test
    public void optionalNotGivenNoDefault()
    {
        CommandBuilder builder = new CommandBuilder("cmd");
        builder.withOptionalArg(new OptionalCommandArgument("arg", String.class)
                                .supplier((input, env) -> input))
               .command((sender, env) -> assertNull(env.get("arg", String.class)));
        builder.execute(sender, new String[0]);
    }

    @Test
    public void help()
    {
        CommandBuilder builder = new CommandBuilder("cmd");
        builder.withOptionalArg(new OptionalCommandArgument("arg", String.class)
                                        .supplier((input, env) -> input))
               .command((sender, env) -> assertNull(env.get("arg", String.class)));
        builder.help(sender);
    }

    @Test
    public void tabCompleteArgInSuperCommand()
    {
        CommandBuilder subCmd = new CommandBuilder("subCmd")
                .withArg(new CommandArgument("subArg", String.class)
                         .supplier((input, env) -> input)
                         .tabCompletion((sender, env) -> {
                             assertNotNull(env.get("arg", String.class));
                             return null;
                         }))
                .command((sender, env) -> fail());

        CommandBuilder cmd = new CommandBuilder("cmd")
                .withArg(new CommandArgument("arg", String.class)
                         .supplier((input, env) -> input))
                .subCommand(subCmd)
                .command((sender, env) -> fail());

        cmd.tabComplete(sender, new String[]{"foo", "subCmd", ""});
    }
}