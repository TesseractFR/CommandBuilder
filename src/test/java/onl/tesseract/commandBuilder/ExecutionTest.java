package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import onl.tesseract.commandBuilder.annotation.Env;
import onl.tesseract.commandBuilder.exception.CommandExecutionException;
import onl.tesseract.commandBuilder.v2.argument.IntegerArgument;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

public class ExecutionTest {

    @Test
    public void CallArgsOnSubCommandTest()
    {
        CommandContext commandA = new CallArgsOnSubCommand();

        CommandSender sender = mock(CommandSender.class);
        commandA.builder.execute(sender, new String[] {"test", "Hello world!"});
        verify(sender).sendMessage("Hello world!");
    }

    @Test
    public void CallArgsOnSubCommand_NoClassOnAnnotationTest()
    {
        CommandContext commandA = new CallArgsImplicitTypeOnSubCommand();

        CommandSender sender = mock(CommandSender.class);
        commandA.builder.execute(sender, new String[] {"test", "Hello world!"});
        verify(sender).sendMessage("Hello world!");
    }

    @Test
    public void predicateTest()
    {
        CommandContext command = new CommandWithPredicates();
        CommandSender sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("foo");

        command.builder.execute(sender, new String[] {"test"});

        verify(sender).sendMessage("test");
    }

    @Test
    public void predicateFailedTest()
    {
        CommandContext command = new CommandWithPredicates();
        CommandSender sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("bar");

        command.builder.execute(sender, new String[] {"test"});

        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void insertEnvTest() throws CommandExecutionException
    {
        CommandSender sender = mock(CommandSender.class);
        CommandEnvironment env = new CommandEnvironment(sender);
        CommandInsertEnv commandInsertEnv = new CommandInsertEnv();
        commandInsertEnv.builder.execute(sender, env, new String[0]);

        Assertions.assertNotNull(env.get("bar"));
        Assertions.assertEquals(43, env.get("bar", Integer.class));
    }

    @Test
    public void exec_AccessArgumentViaEnv()
    {
        CommandSender sender = mock(CommandSender.class);

        new AccessArgumentViaEnv().builder.execute(sender, new String[]{"12"});

        verify(sender, times(1)).sendMessage("12");
    }

    @Test
    public void exec_AccessArgumentViaEnv_ImplicitGetValue()
    {
        CommandSender sender = mock(CommandSender.class);

        new AccessArgumentViaEnvImplicitGetValue().builder.execute(sender, new String[]{"12"});

        verify(sender, times(1)).sendMessage("12");
    }
}

@Command(args = @Argument(label = "arg", clazz = IntegerArgument.class))
class AccessArgumentViaEnv extends CommandContext
{
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
class AccessArgumentViaEnvImplicitGetValue extends CommandContext
{
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
