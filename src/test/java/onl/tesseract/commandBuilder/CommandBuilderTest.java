package onl.tesseract.commandBuilder;

import onl.tesseract.Guild;
import onl.tesseract.Parcel;
import onl.tesseract.commandBuilder.sample.FloatArgument;
import onl.tesseract.commandBuilder.sample.GuildArgument;
import onl.tesseract.commandBuilder.sample.ParcelArgument;
import onl.tesseract.commandBuilder.sample.PlayerArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

class CommandBuilderTest {
    CommandSender sender;

    @BeforeEach
    void setUp()
    {
        Guild.guilds.clear();
        sender = mock(CommandSender.class);
    }

    @Test
    public void setFlySpeedPlayerErrorInvalidNumber()
    {
        Player player = mock(Player.class);

        CommandBuilder moneyCommand = new CommandBuilder("money");
        moneyCommand.withArg(new PlayerArgument("player", player))
                    .withArg(new FloatArgument("quantity"))
                    .command(env -> env.get("player", Player.class).setFlySpeed(env.get("quantity", Float.class)));

        moneyCommand.execute(sender, new String[] {"GabRay", "invalid number"});
        verify(player, times(0)).setFlySpeed(anyFloat());
        verify(sender, times(1)).sendMessage(anyString());
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
        getParcelCommand.withArg(new GuildArgument("guild"))
                        .withArg(new ParcelArgument("parcel"))
                        .command(env -> fail());

        getParcelCommand.execute(sender, new String[] {"inexistent guild", "maison"});
        verify(sender, times(1)).sendMessage(anyString());
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
}