package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.exception.CommandExecutionException;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

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
        commandInsertEnv.builder.execute(sender, new CommandExecutionContext(env, commandInsertEnv.builder, new String[0]));

        Assertions.assertNotNull(env.get("bar"));
        Assertions.assertEquals(43, env.get("bar", Integer.class));
    }


}