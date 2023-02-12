package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.*;
import onl.tesseract.commandBuilder.definition.CommandArgumentDefinition;
import onl.tesseract.commandBuilder.exception.CommandExecutionException;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class CommandContextTest {

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
        CommandArgumentDefinition<?> argument = command.builder.arguments.get(0);
        assertEquals("argTest", argument.getName());
        assertEquals(StringCommandArgument.class, argument.getType());
    }

    @Test
    public void CommandNoContentNoValuesTest()
    {
        CommandContext command = new NoContentNoAnnotationValuesCommand();

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

        CommandBuilder builder = new CommandBuilderProvider().provideFor(command, innerCommand);
        assertEquals("inner", builder.getName());
    }

    @Test
    public void provideForMethodWithArgsTest() throws NoSuchMethodException
    {
        InnerMethodWithArgsTestCommand command = new InnerMethodWithArgsTestCommand();
        Method innerCommand = command.getClass().getMethod("innerCommand", StringCommandArgument.class);

        CommandBuilder builder = new CommandBuilderProvider().provideFor(command, innerCommand);
        assertEquals("inner", builder.getName());
        assertEquals(1, builder.arguments.size());
        assertEquals("arg", builder.arguments.get(0).getName());
        assertEquals(StringCommandArgument.class, builder.arguments.get(0).getType());
    }

    @Test
    public void CommandWithSubCommandTest()
    {
        CommandContext command = new CommandClassWithSubCommand();

        assertEquals("commandClassWithSub", command.builder.getName());
        assertNotNull(command.builder.getSubCommands().get("my"));
    }

    @Test
    public void CommandWithSubCommandClassTest()
    {
        CommandContext command = new CommandClassWithSubCommandAsClass();

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
        CommandContext commandA = new CommandA();
        assertNotNull(commandA.builder.getSubCommands().get("commandB"));
    }

    @Test
    public void InnerMethodBodyTest()
    {
        CommandContext commandA = new InnerMethodTestBody();

        CommandBuilder inner = commandA.builder.getSubCommands().get("inner");
        assertNotNull(inner);
        assertNotNull(inner.consumer);
    }

    @Test
    public void CallArgsOnSubCommandTest() throws CommandExecutionException
    {
        CommandContext commandA = new CallArgsOnSubCommand();

        CommandSender sender = Mockito.mock(CommandSender.class);
        commandA.builder.execute(sender, new String[] {"test", "Hello world!"});
        Mockito.verify(sender).sendMessage("Hello world!");
    }

    @Test
    public void CallArgsOnSubCommand_NoClassOnAnnotationTest() throws CommandExecutionException
    {
        CommandContext commandA = new CallArgsOnSubCommand();

        CommandSender sender = Mockito.mock(CommandSender.class);
        commandA.builder.execute(sender, new String[] {"test", "Hello world!"});
        Mockito.verify(sender).sendMessage("Hello world!");
    }

    @Test
    public void predicateTest() throws CommandExecutionException
    {
        CommandContext command = new CommandWithPredicates();
        CommandSender sender = Mockito.mock(CommandSender.class);
        Mockito.when(sender.getName()).thenReturn("foo");

        command.builder.execute(sender, new String[] {"test"});

        Mockito.verify(sender).sendMessage("test");
    }

    @Test
    public void predicateFailedTest() throws CommandExecutionException
    {
        CommandContext command = new CommandWithPredicates();
        CommandSender sender = Mockito.mock(CommandSender.class);
        Mockito.when(sender.getName()).thenReturn("bar");

        command.builder.execute(sender, new String[] {"test"});

        Mockito.verify(sender, Mockito.times(0)).sendMessage(Mockito.anyString());
    }

    @Test
    public void insertEnvTest() throws CommandExecutionException
    {
        CommandSender sender = Mockito.mock(CommandSender.class);
        CommandEnvironment env = new CommandEnvironment(sender);
        CommandInsertEnv commandInsertEnv = new CommandInsertEnv();
        commandInsertEnv.builder.execute(sender, env, new String[0]);

        Assertions.assertNotNull(env.get("bar"));
        Assertions.assertEquals(43, env.get("bar", Integer.class));
    }
}

@Command
class NoContentNoAnnotationValuesCommand extends CommandContext {

}

@Command(name = "test", permission = "test", playerOnly = true, description = "A test command")
class NoContentCommand extends CommandContext {

}

@Command(name = "test", permission = "test", playerOnly = true, description = "A test command",
        args = {
                @Argument(label = "argTest", clazz = StringCommandArgument.class)
        })
class NoContentArgsCommand extends CommandContext {

}

class InnerMethodTestCommand {
    @Command
    public void innerCommand()
    {}
}

class InnerMethodWithArgsTestCommand {
    @Command
    public void innerCommand(@Argument(label = "arg", clazz = StringCommandArgument.class) StringCommandArgument arg)
    {}
}

@Command
class CommandClassWithSubCommand extends CommandContext {

    @Command
    public void myCommand(@Argument(label = "test", clazz = StringCommandArgument.class) StringCommandArgument arg)
    {}
}

@Command
class CommandClassWithSubCommandAsClass extends CommandContext {

    @Command
    static class SubCommand {

    }
}

@Command
class CommandWithBody extends CommandContext {
    public int count = 0;

    @CommandBody
    public void command(CommandEnvironment environment)
    {
        count++;
    }
}

@Command(subCommands = CommandB.class)
class CommandA extends CommandContext {

}

@Command(name = "commandB")
class CommandB {

}

@Command
class InnerMethodTestBody extends CommandContext {
    @Command
    public void innerCommand()
    {}
}

@Command
class CallArgsOnSubCommand extends CommandContext {

    @Command
    public void testCommand(@Argument(label = "test", clazz = StringCommandArgument.class) StringCommandArgument testString,
                            CommandSender sender)
    {
        sender.sendMessage(testString.get());
    }
}

@Command
class CallArgsImplicitTypeOnSubCommand extends CommandContext {

    @Command
    public void testCommand(@Argument(label = "test") StringCommandArgument testString,
                            CommandSender sender)
    {
        sender.sendMessage(testString.get());
    }
}

@Command
class CommandWithPredicates extends CommandContext {

    boolean check(CommandSender sender)
    {
        return sender.getName().equals("foo");
    }

    @CommandPredicate("check")
    @Command
    void testCommand(CommandSender sender)
    {
        sender.sendMessage("test");
    }

    @CommandBody
    void main()
    {

    }
}

@Command
class CommandInsertEnv extends CommandContext {

    @EnvInsert("foo")
    int insertFoo() {
        return 42;
    }

    @CommandBody
    void command(CommandEnvironment env) {
        if (env.get("foo") != null)
            env.set("bar", env.get("foo", Integer.class) + 1);
    }
}