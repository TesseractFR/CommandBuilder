package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.*;
import org.bukkit.command.CommandSender;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class CommandContextTest {

    @Test
    public void commandAnnotationOnClass_CheckProperties()
    {
        NoContentCommand command = new NoContentCommand();

        assertEquals("test", command.builder.getName());
        assertEquals("test", command.builder.getPermission().getName());
        assertEquals("A test command", command.builder.getDescription());
        assertTrue(command.builder.isPlayerOnly());
    }

    @Test
    public void commandAnnotationOnClass_CheckArgument()
    {
        NoContentArgsCommand command = new NoContentArgsCommand();

        assertEquals(1, command.builder.arguments.size());
        CommandArgumentDefinition<?> argument = command.builder.arguments.get(0);
        assertEquals("argTest", argument.getName());
        assertEquals(StringCommandArgument.class, argument.getType());
    }

    @Test
    public void commandAnnotationOnClass_NoValues_CheckDefault()
    {
        CommandContext command = new NoContentNoAnnotationValuesCommand();

        assertEquals("noContentNoAnnotationValues", command.builder.getName());
        assertSame(Permission.NONE, command.builder.getPermission());
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