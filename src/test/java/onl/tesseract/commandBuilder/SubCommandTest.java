package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import onl.tesseract.commandBuilder.annotation.Perm;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SubCommandTest {

    private CommandSender sender;

    @Before
    public void setup()
    {
        sender = mock(CommandSender.class);
    }

    @Test
    public void defaultHelpCommandTest()
    {
        SimpleCommand simpleCommand = new SimpleCommand();

        Map<String, CommandDefinition> subCommands = simpleCommand.command.getSubCommands();
        assertEquals(1, subCommands.size());
        assertTrue(subCommands.containsKey("help"));
    }

    @Test
    public void defaultHelpSubCommandTest()
    {
        SubCommand subCommand = new SubCommand();

        Map<String, CommandDefinition> sub = subCommand.command.getSubCommands().get("sub").getSubCommands();
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey("help"));
    }

    @Test
    public void subCommandOptionArg_NoArg_ShouldExecWithNullArg()
    {
        SubCommandWithOptionalArg subCommandWithOptionalArg = new SubCommandWithOptionalArg();

        boolean res = subCommandWithOptionalArg.command.execute(sender, new String[] {"sub"});

        assertTrue(res);
        verify(sender).sendMessage("null arg");
    }

    @Test
    public void subCommandOptionArg_Arg_ShouldExec()
    {
        SubCommandWithOptionalArg subCommandWithOptionalArg = new SubCommandWithOptionalArg();

        boolean res = subCommandWithOptionalArg.command.execute(sender, new String[] {"sub", "true"});

        assertTrue(res);
        verify(sender, times(1)).setOp(true);
    }

    @Test
    public void subCommandOptionArg_ArgHelp_ShouldCallHelp()
    {
        SubCommandWithOptionalArg subCommandWithOptionalArg = new SubCommandWithOptionalArg();

        boolean res = subCommandWithOptionalArg.command.execute(sender, new String[] {"sub", "help"});

        assertTrue(res);
        verify(sender).sendMessage(any(String[].class));
    }

    @Test
    public void permissionPresenceCheck_NoPerm_Root()
    {
        CommandWithNoPermission command = new CommandWithNoPermission();

        assertSame(Permission.NONE, command.command.getPermission());
    }

    @Test
    public void permissionPresenceCheck_NoPerm_SubWithPerm()
    {
        CommandWithNoPermission command = new CommandWithNoPermission();

        assertEquals("test", command.command.getSubCommands().get("sub2").getPermission().getName());
    }

    @Test
    public void permissionPresenceCheck_NoPerm_SubNoPerm()
    {
        CommandWithNoPermission command = new CommandWithNoPermission();

        assertSame(Permission.NONE, command.command.getSubCommands().get("sub").getPermission());
    }

    @Test
    public void permissionPresenceCheck_Perm_Root()
    {
        CommandWithPermission command = new CommandWithPermission();

        assertEquals("command", command.command.getPermission().getName());
    }

    @Test
    public void permissionPresenceCheck_Perm_SubRelativePerm()
    {
        CommandWithPermission command = new CommandWithPermission();

        assertEquals("command.test", command.command.getSubCommands().get("sub2").getPermission().getName());
    }

    @Test
    public void permissionPresenceCheck_Perm_SubAbsolutePerm()
    {
        CommandWithPermission command = new CommandWithPermission();

        assertEquals("test", command.command.getSubCommands().get("sub3").getPermission().getName());
    }

    @Test
    public void permissionPresenceCheck_Perm_SubNoPerm()
    {
        CommandWithPermission command = new CommandWithPermission();

        assertSame(Permission.NONE, command.command.getSubCommands().get("sub").getPermission());
    }

    @Test
    public void permissionPresenceCheck_AutoPerm_Root()
    {
        CommandWithAutoPermission command = new CommandWithAutoPermission();

        assertEquals("command", command.command.getPermission().getName());
    }

    @Test
    public void permissionPresenceCheck_AutoPerm_Sub()
    {
        CommandWithAutoPermission command = new CommandWithAutoPermission();

        assertEquals("command.sub", command.command.getSubCommands().get("sub").getPermission().getName());
    }

    @Test
    public void permissionPresenceCheck_AutoPerm_SubManualMode()
    {
        CommandWithAutoPermission command = new CommandWithAutoPermission();

        assertEquals("command.test", command.command.getSubCommands().get("sub2").getPermission().getName());
    }
}

@Command
class SimpleCommand extends CommandContext {
    @CommandBody
    public void body()
    {

    }
}

@Command
class SubCommand extends CommandContext {
    @Command
    public void sub(CommandSender sender)
    {
        sender.sendMessage("Sub command");
    }
}

@Command(name = "command")
class SubCommandWithOptionalArg extends CommandContext {
    @Command
    public void sub(CommandSender sender, @Argument(value = "flag", optional = true) @Nullable BooleanArgument boolArg)
    {
        if (boolArg == null)
        {
            sender.sendMessage("null arg");
            return;
        }
        sender.setOp(boolArg.get());
    }
}

@Command(name = "command", permission = @Perm("command"))
class CommandWithPermission extends CommandContext {
    @CommandBody
    public void cmd(CommandSender sender)
    {
        sender.setOp(true);
    }

    @Command
    public void sub(CommandSender sender)
    {
        sender.setOp(false);
    }

    @Command(permission = @Perm("test"))
    public void sub2(CommandSender sender)
    {
        sender.setOp(false);
    }

    @Command(permission = @Perm(value = "test", absolute = true))
    public void sub3(CommandSender sender)
    {
        sender.setOp(false);
    }
}

@Command(name = "command", permission = @Perm(""))
class CommandWithNoPermission extends CommandContext {
    @CommandBody
    public void cmd(CommandSender sender)
    {
        sender.setOp(true);
    }

    @Command
    public void sub(CommandSender sender)
    {
        sender.setOp(false);
    }

    @Command(permission = @Perm("test"))
    public void sub2(CommandSender sender)
    {
        sender.setOp(false);
    }
}

@Command(name = "command", permission = @Perm(mode = Perm.Mode.AUTO))
class CommandWithAutoPermission extends CommandContext {
    @Command
    public void sub(CommandSender sender)
    {
        sender.setOp(false);
    }

    @Command(permission = @Perm(mode = Perm.Mode.MANUAL, value = "test"))
    public void sub2(CommandSender sender)
    {
        sender.setOp(false);
    }
}

class BooleanArgument extends CommandArgument<Boolean> {
    public BooleanArgument(@NotNull final String name)
    {
        super(name);
    }

    @Override
    public void define(final CommandArgumentBuilderSteps.Parser<Boolean> builder)
    {
        builder.parser((input, env) -> Boolean.parseBoolean(input))
               .tabCompleter((input, env) -> List.of("true", "false"))
               .errorHandler(IllegalArgumentException.class, "Flag invalid");
    }
}
