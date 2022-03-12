package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Env;
import onl.tesseract.commandBuilder.annotation.ErrorHandler;
import onl.tesseract.commandBuilder.annotation.Parser;
import onl.tesseract.commandBuilder.annotation.TabCompleter;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class CommandArgumentAnnotationTest {

    @Test
    public void parseFunctionTest()
    {
        CommandArgument test = new IntCommandArg("test");

        Assertions.assertEquals(42, test.supplier.apply("42", null));
    }

    @Test
    public void parseFunctionWithEnvTest()
    {
        CommandArgument test = new IntWithDependenciesCommandArg("test");
        CommandSender sender = Mockito.mock(CommandSender.class);
        CommandEnvironment env = new CommandEnvironment(sender);
        env.set("foo", "Hello world!");

        Assertions.assertEquals(42, test.supplier.apply("42", env));
        Mockito.verify(sender).sendMessage("Hello world!");
    }

    @Test
    public void parseFunctionWithErrorTest()
    {
        CommandArgument test = new CommandArgWithError("foo");

        Assertions.assertTrue(test.hasError(NumberFormatException.class));
    }

    @Test
    public void tabCompleteTest()
    {
        CommandArgument test = new CommandWithCompletion("foo");

        Player sender = Mockito.mock(Player.class);
        Mockito.when(sender.getName()).thenReturn("Hello world!");
        CommandEnvironment env = new CommandEnvironment(sender);
        env.set("test", "foo");
        List<String> res = test.tabComplete(sender, env);

        Assertions.assertNotNull(res);
        Assertions.assertEquals(2, res.size());
        Assertions.assertEquals("foo", res.get(0));
        Assertions.assertEquals("Hello world!", res.get(1));
    }
}

class IntCommandArg extends CommandArgument
{
    protected IntCommandArg(final String name)
    {
        super(name);
    }

    @Parser
    Integer parse(String input)
    {
        return Integer.parseInt(input);
    }
}

class IntWithDependenciesCommandArg extends CommandArgument
{
    protected IntWithDependenciesCommandArg(final String name)
    {
        super(name);
    }

    @Parser
    Integer parse(String input, CommandSender sender, @Env(key = "foo") String message)
    {
        sender.sendMessage(message);
        return Integer.parseInt(input);
    }
}

class CommandArgWithError extends CommandArgument
{
    protected CommandArgWithError(final String name)
    {
        super(name);
    }

    @Parser
    Integer parse(String input)
    {
        return Integer.parseInt(input);
    }

    @ErrorHandler(NumberFormatException.class)
    String onError(String input)
    {
        return "invalid number";
    }
}

class CommandWithCompletion extends CommandArgument
{
    protected CommandWithCompletion(final String name)
    {
        super(name);
    }

    @Parser
    Integer parse(String input)
    {
        return Integer.parseInt(input);
    }

    @TabCompleter
    List<String> tabComplete(@Env(key = "test") String testMessage, Player player)
    {
        return List.of(testMessage, player.getName());
    }
}