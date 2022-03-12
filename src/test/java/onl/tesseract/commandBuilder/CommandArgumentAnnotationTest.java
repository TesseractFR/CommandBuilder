package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Env;
import onl.tesseract.commandBuilder.annotation.Parser;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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