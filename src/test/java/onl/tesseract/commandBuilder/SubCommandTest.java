package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

        Map<String, CommandBuilder> subCommands = simpleCommand.builder.getSubCommands();
        assertEquals(1, subCommands.size());
        assertTrue(subCommands.containsKey("help"));
    }

    @Test
    public void defaultHelpSubCommandTest()
    {
        SubCommand subCommand = new SubCommand();

        Map<String, CommandBuilder> sub = subCommand.builder.getSubCommands().get("sub").getSubCommands();
        assertEquals(1, sub.size());
        assertTrue(sub.containsKey("help"));
    }

    @Test
    public void subCommandOptionArg_NoArg_ShouldExecWithNullArg()
    {
        SubCommandWithOptionalArg subCommandWithOptionalArg = new SubCommandWithOptionalArg();

        boolean res = subCommandWithOptionalArg.builder.execute(sender, new String[] {"sub"});

        assertTrue(res);
        verify(sender).sendMessage("null arg");
    }

    @Test
    public void subCommandOptionArg_Arg_ShouldExec()
    {
        SubCommandWithOptionalArg subCommandWithOptionalArg = new SubCommandWithOptionalArg();

        boolean res = subCommandWithOptionalArg.builder.execute(sender, new String[] {"sub", "true"});

        assertTrue(res);
        verify(sender, times(1)).setOp(true);
    }

    @Test
    public void subCommandOptionArg_ArgHelp_ShouldCallHelp()
    {
        SubCommandWithOptionalArg subCommandWithOptionalArg = new SubCommandWithOptionalArg();

        boolean res = subCommandWithOptionalArg.builder.execute(sender, new String[] {"sub", "help"});

        assertTrue(res);
        verify(sender).sendMessage(any(String[].class));
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

@Command
class SubCommandWithOptionalArg extends CommandContext {
    @Command
    public void sub(CommandSender sender, @Argument(label = "flag", optional = true) @Nullable BooleanArgument boolArg)
    {
        if (boolArg == null)
        {
            sender.sendMessage("null arg");
            return;
        }
        sender.setOp(boolArg.get());
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
