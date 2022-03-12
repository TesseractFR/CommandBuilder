package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class CommandTest {

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
        Command command = new NoContentNoAnnotationValuesCommand();

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
        Command command = new CommandClassWithSubCommand();

        assertEquals("commandClassWithSub", command.builder.getName());
        assertNotNull(command.builder.getSubCommands().get("my"));
    }

    @Test
    public void CommandWithSubCommandClassTest()
    {
        Command command = new CommandClassWithSubCommandAsClass();

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
        Command commandA = new CommandA();
        assertNotNull(commandA.builder.getSubCommands().get("commandB"));
    }

    @Test
    public void InnerMethodBodyTest()
    {
        Command commandA = new InnerMethodTestBody();

        CommandBuilder inner = commandA.builder.getSubCommands().get("inner");
        assertNotNull(inner);
        assertNotNull(inner.consumer);
    }

    @Test
    public void CallArgsOnSubCommandTest()
    {
        Command commandA = new CallArgsOnSubCommand();

        CommandSender sender = Mockito.mock(CommandSender.class);
        commandA.builder.execute(sender, new String[] {"test", "Hello world!"});
        Mockito.verify(sender).sendMessage("Hello world!");
    }
}

@onl.tesseract.commandBuilder.annotation.Command
class NoContentNoAnnotationValuesCommand extends Command {

}

@onl.tesseract.commandBuilder.annotation.Command(name = "test", permission = "test", playerOnly = true, description = "A test command")
class NoContentCommand extends Command {

}

@onl.tesseract.commandBuilder.annotation.Command(name = "test", permission = "test", playerOnly = true, description = "A test command",
        args = {
                @Argument(label = "argTest", clazz = StringCommandArgument.class)
        })
class NoContentArgsCommand extends Command {

}

class InnerMethodTestCommand {
    @onl.tesseract.commandBuilder.annotation.Command
    public void innerCommand()
    {}
}

class InnerMethodWithArgsTestCommand {
    @onl.tesseract.commandBuilder.annotation.Command
    public void innerCommand(@Argument(label = "arg", clazz = StringCommandArgument.class) String arg)
    {}
}

@onl.tesseract.commandBuilder.annotation.Command
class CommandClassWithSubCommand extends Command {

    @onl.tesseract.commandBuilder.annotation.Command
    public void myCommand(@Argument(label = "test", clazz = StringCommandArgument.class) String arg)
    {}
}

@onl.tesseract.commandBuilder.annotation.Command
class CommandClassWithSubCommandAsClass extends Command {

    @onl.tesseract.commandBuilder.annotation.Command
    static class SubCommand {

    }
}

@onl.tesseract.commandBuilder.annotation.Command
class CommandWithBody extends Command {
    public int count = 0;

    @CommandBody
    public void command(CommandEnvironment environment)
    {
        count++;
    }
}

@onl.tesseract.commandBuilder.annotation.Command(subCommands = CommandB.class)
class CommandA extends Command {

}

@onl.tesseract.commandBuilder.annotation.Command(name = "commandB")
class CommandB {

}

@onl.tesseract.commandBuilder.annotation.Command
class InnerMethodTestBody extends Command {
    @onl.tesseract.commandBuilder.annotation.Command
    public void innerCommand()
    {}
}

@onl.tesseract.commandBuilder.annotation.Command
class CallArgsOnSubCommand extends Command {

    @onl.tesseract.commandBuilder.annotation.Command
    public void testCommand(@Argument(label = "test", clazz = StringCommandArgument.class) String testString,
                            CommandSender sender)
    {
        sender.sendMessage(testString);
    }
}