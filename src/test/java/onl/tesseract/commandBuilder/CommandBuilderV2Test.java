package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class CommandBuilderV2Test {

    @Test
    public void CommandOnCommandBuilderV2ClassTest()
    {
        NoContentCommand command = new NoContentCommand();

        assertEquals("test", command.builder.getName());
        assertEquals("test", command.builder.getPermission());
        assertEquals("A test command", command.builder.getDescription());
        assertTrue(command.builder.isPlayerOnly());
    }

    @Test
    public void CommandOnCommandBuilderV2ClassWithArgsTest()
    {
        NoContentArgsCommand command = new NoContentArgsCommand();

        assertEquals("test", command.builder.getName());
        assertEquals("test", command.builder.getPermission());
        assertEquals("A test command", command.builder.getDescription());
        assertTrue(command.builder.isPlayerOnly());

        assertEquals(1, command.builder.arguments.size());
        CommandArgument argument = command.builder.arguments.get(0);
        assertEquals("argTest", argument.getName());
        assertInstanceOf(StringCommandArgument.class, argument);
    }

    @Test
    public void CommandNoContentNoValuesTest()
    {
        CommandBuilderV2 command = new NoContentNoAnnotationValuesCommand();

        assertEquals("noContentNoAnnotationValues", command.builder.getName());
        assertEquals("", command.builder.getPermission());
        assertEquals("", command.builder.getDescription());
        assertFalse(command.builder.isPlayerOnly());

        assertEquals(0, command.builder.arguments.size());
    }

    @Test
    public void provideForMethodTest() throws NoSuchMethodException
    {
        InnerMethodTestCommand command = new InnerMethodTestCommand();
        Method innerCommand = command.getClass().getMethod("innerCommand");

        CommandBuilder builder = new CommandBuilderProvider().provideFor(null, innerCommand);
        assertEquals("inner", builder.getName());
    }

    @Test
    public void provideForMethodWithArgsTest() throws NoSuchMethodException
    {
        InnerMethodWithArgsTestCommand command = new InnerMethodWithArgsTestCommand();
        Method innerCommand = command.getClass().getMethod("innerCommand", String.class);

        CommandBuilder builder = new CommandBuilderProvider().provideFor(null, innerCommand);
        assertEquals("inner", builder.getName());
        assertEquals(1, builder.arguments.size());
        assertEquals("arg", builder.arguments.get(0).getName());
        assertInstanceOf(StringCommandArgument.class, builder.arguments.get(0));
    }

    @Test
    public void CommandWithSubCommandTest()
    {
        CommandBuilderV2 command = new CommandClassWithSubCommand();

        assertEquals("commandClassWithSub", command.builder.getName());
        assertNotNull(command.builder.getSubCommands().get("my"));
    }

    @Test
    public void CommandWithSubCommandClassTest()
    {
        CommandBuilderV2 command = new CommandClassWithSubCommandAsClass();

        assertNotNull(command.builder.getSubCommands().get("sub"));
    }

    @Test
    public void CommandBodyTest()
    {
        CommandWithBody commandWithBody = new CommandWithBody();
        assertNotNull(commandWithBody.builder.consumer);
    }

    @Test
    public void SubCommandOnExternalClassTest()
    {
        CommandBuilderV2 commandA = new CommandA();
        assertNotNull(commandA.builder.getSubCommands().get("commandB"));
    }

    @Test
    public void InnerMethodBodyTest()
    {
        CommandBuilderV2 commandA = new InnerMethodTestBody();

        CommandBuilder inner = commandA.builder.getSubCommands().get("inner");
        assertNotNull(inner);
        assertNotNull(inner.consumer);
    }

    @Test
    public void CallArgsOnSubCommandTest()
    {
        CommandBuilderV2 commandA = new CallArgsOnSubCommand();

        CommandSender sender = Mockito.mock(CommandSender.class);
        commandA.builder.execute(sender, new String[] {"test", "Hello world!"});
        Mockito.verify(sender).sendMessage("Hello world!");
    }
}

@Command
class NoContentNoAnnotationValuesCommand extends CommandBuilderV2 {

}

@Command(name = "test", permission = "test", playerOnly = true, description = "A test command")
class NoContentCommand extends CommandBuilderV2 {

}

@Command(name = "test", permission = "test", playerOnly = true, description = "A test command",
        args = {
                @Argument(label = "argTest", clazz = StringCommandArgument.class)
        })
class NoContentArgsCommand extends CommandBuilderV2 {

}

class InnerMethodTestCommand {
    @Command
    public void innerCommand()
    {}
}

class InnerMethodWithArgsTestCommand {
    @Command
    public void innerCommand(@Argument(label = "arg", clazz = StringCommandArgument.class) String arg)
    {}
}

@Command
class CommandClassWithSubCommand extends CommandBuilderV2 {

    @Command
    public void myCommand(@Argument(label = "test", clazz = StringCommandArgument.class) String arg)
    {}
}

@Command
class CommandClassWithSubCommandAsClass extends CommandBuilderV2 {

    @Command
    static class SubCommand {

    }
}

@Command
class CommandWithBody extends CommandBuilderV2 {
    public int count = 0;

    @CommandBody
    public void command(CommandEnvironment environment)
    {
        count++;
    }
}

@Command(subCommands = CommandB.class)
class CommandA extends CommandBuilderV2 {

}

@Command(name = "commandB")
class CommandB {

}

@Command
class InnerMethodTestBody extends CommandBuilderV2 {
    @Command
    public void innerCommand()
    {}
}

@Command
class CallArgsOnSubCommand extends CommandBuilderV2 {

    @Command
    public void testCommand(@Argument(label = "test", clazz = StringCommandArgument.class) String testString,
                            CommandSender sender)
    {
        sender.sendMessage(testString);
    }
}