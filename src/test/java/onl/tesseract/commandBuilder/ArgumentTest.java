package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import onl.tesseract.commandBuilder.annotation.Env;
import onl.tesseract.commandBuilder.exception.InvalidArgumentTypeException;
import onl.tesseract.commandBuilder.sample.FloatArgument;
import onl.tesseract.commandBuilder.v2.argument.IntegerArgument;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

public class ArgumentTest {
    @Test
    public void CallArgsOnSubCommand_NoClassOnAnnotationTest()
    {
        CommandContext commandA = new CallArgsImplicitTypeOnSubCommand();

        CommandSender sender = mock(CommandSender.class);
        commandA.builder.execute(sender, new String[] {"test", "Hello world!"});
        verify(sender).sendMessage("Hello world!");
    }

    @Test
    public void exec_ArgumentOnClass_AccessArgumentViaEnv()
    {
        CommandSender sender = mock(CommandSender.class);

        new AccessArgumentViaEnv().builder.execute(sender, new String[] {"12"});

        verify(sender, times(1)).sendMessage("12");
    }

    @Test
    public void exec_ArgumentOnClass_AccessArgumentViaEnv_ImplicitGetValue()
    {
        CommandSender sender = mock(CommandSender.class);

        new AccessArgumentViaEnvImplicitGetValue().builder.execute(sender, new String[] {"12"});

        verify(sender, times(1)).sendMessage("12");
    }

    @Test
    public void exec_ArgumentOnMethod()
    {
        CommandSender sender = mock(CommandSender.class);

        new ArgumentOnSubCommandFunction().builder.execute(sender, new String[] {"test", "12"});

        verify(sender, times(1)).sendMessage("12");
    }

    @Test
    public void exec_ArgumentOnMethod_ClazzSpecifiedAndCastToValueType()
    {
        CommandSender sender = mock(CommandSender.class);

        new ArgumentOnSubCommandFunctionClazzSpecifiedAndCastToValueType().builder.execute(sender, new String[] {"test", "12"});

        verify(sender, times(1)).sendMessage("12");
    }

    @Test
    public void exec_ArgumentOnMethod_InvalidType()
    {
        Assertions.assertThrows(InvalidArgumentTypeException.class, ArgumentOnSubCommandFunctionInvalidType::new);
    }

    @Test
    public void exec_ArgumentOnMethod_InvalidClassType()
    {
        Assertions.assertThrows(InvalidArgumentTypeException.class, ArgumentOnSubCommandFunctionInvalidClassType::new);
    }

    @Test
    public void exec_ArgumentOnMethodWithDefaultValue_Provided()
    {
        CommandSender sender = mock(CommandSender.class);

        new ArgumentOnSubCommandFunctionDefaultValue().builder.execute(sender, new String[] {"test", "12"});

        verify(sender, times(1)).sendMessage("12");
    }

    @Test
    public void exec_ArgumentOnMethodWithDefaultValue_NotProvided()
    {
        CommandSender sender = mock(CommandSender.class);

        new ArgumentOnSubCommandFunctionDefaultValue().builder.execute(sender, new String[] {"test"});

        verify(sender, times(1)).sendMessage("42");
    }

    @Test
    public void exec_OptionalArgumentOnMethod_Provided()
    {
        CommandSender sender = mock(CommandSender.class);

        new OptionalArgumentOnSubCommandFunction().builder.execute(sender, new String[] {"test", "12"});

        verify(sender, times(1)).sendMessage("12");
    }

    @Test
    public void exec_OptionalArgumentOnMethod_NotProvided()
    {
        CommandSender sender = mock(CommandSender.class);

        new OptionalArgumentOnSubCommandFunction().builder.execute(sender, new String[] {"test"});

        verify(sender, times(1)).sendMessage("null");
    }
}

@Command(args = @Argument(label = "arg", clazz = IntegerArgument.class))
class AccessArgumentViaEnv extends CommandContext {
    @CommandBody
    void cmd(@Env(key = "arg") IntegerArgument arg, CommandEnvironment env)
    {
        env.getSender().sendMessage(arg.get() + "");
    }

    @Command
    void getCommand(@Env(key = "arg") IntegerArgument arg, CommandEnvironment env)
    {
        env.getSender().sendMessage(arg.get() + "");
    }
}

@Command(args = @Argument(label = "arg", clazz = IntegerArgument.class))
class AccessArgumentViaEnvImplicitGetValue extends CommandContext {
    @CommandBody
    void cmd(@Env(key = "arg") int arg, CommandEnvironment env)
    {
        env.getSender().sendMessage(arg + "");
    }

    @Command
    void getCommand(@Env(key = "arg") int arg, CommandEnvironment env)
    {
        env.getSender().sendMessage(arg + "");
    }
}

@Command
class ArgumentOnSubCommandFunction extends CommandContext {
    @Command
    public void testCommand(@Argument(label = "arg") IntegerArgument integerArgument, CommandSender sender)
    {
        sender.sendMessage(integerArgument.get() + "");
    }
}

@Command
class ArgumentOnSubCommandFunctionClazzSpecifiedAndCastToValueType extends CommandContext {
    @Command
    public void testCommand(@Argument(label = "arg", clazz = IntegerArgument.class) int integer, CommandSender sender)
    {
        sender.sendMessage(integer + "");
    }
}

@Command
class ArgumentOnSubCommandFunctionInvalidType extends CommandContext {
    @Command
    public void testCommand(@Argument(label = "arg") int integer, CommandSender sender)
    {
        sender.sendMessage(integer + "");
    }
}

@Command
class ArgumentOnSubCommandFunctionInvalidClassType extends CommandContext {
    @Command
    public void testCommand(@Argument(label = "arg") String integer, CommandSender sender)
    {
        sender.sendMessage(integer + "");
    }
}

@Command
class ArgumentOnSubCommandFunctionDefaultValue extends CommandContext {
    @Command
    public void testCommand(@Argument(label = "arg", optional = true, def = "42") IntegerArgument integerArgument, CommandSender sender)
    {
        sender.sendMessage(integerArgument.get() + "");
    }
}


@Command
class OptionalArgumentOnSubCommandFunction extends CommandContext {
    @Command
    public void testCommand(@Argument(label = "arg", optional = true) @Nullable IntegerArgument integerArgument, CommandSender sender)
    {
        if (integerArgument != null)
            sender.sendMessage(integerArgument.get() + "");
        else
            sender.sendMessage("null");
    }
}
