package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import onl.tesseract.commandBuilder.annotation.Perm;
import onl.tesseract.commandBuilder.exception.CommandExecutionException;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import java.util.Map;

import static org.mockito.Mockito.*;

public class ExecutionTest {

    @Test
    public void CallArgsOnSubCommandTest()
    {
        CommandContext commandA = new CallArgsOnSubCommand();

        CommandSender sender = mock(CommandSender.class);
        commandA.command.execute(sender, new String[] {"test", "Hello world!"});
        verify(sender).sendMessage("Hello world!");
    }


    @Test
    public void predicateTest()
    {
        CommandContext command = new CommandWithPredicates();
        CommandSender sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("foo");

        command.command.execute(sender, new String[] {"test"});

        verify(sender).sendMessage("test");
    }

    @Test
    public void predicateFailedTest()
    {
        CommandContext command = new CommandWithPredicates();
        CommandSender sender = mock(CommandSender.class);
        when(sender.getName()).thenReturn("bar");

        command.command.execute(sender, new String[] {"test"});

        verify(sender, times(0)).sendMessage(anyString());
    }

    @Test
    public void insertEnvTest() throws CommandExecutionException
    {
        CommandSender sender = mock(CommandSender.class);
        CommandEnvironment env = new CommandEnvironment(sender);
        CommandInsertEnv commandInsertEnv = new CommandInsertEnv();
        commandInsertEnv.command.execute(sender, new CommandExecutionContext(env, commandInsertEnv.command, new String[0]));

        Assertions.assertNotNull(env.get("bar"));
        Assertions.assertEquals(43, env.get("bar", Integer.class));
    }

    @Test
    public void permission_commandBody_HasPerm() {
        CommandSender sender = PermissionTest.senderWithPermission(Map.of("a", true));

        boolean res = new PermissionsTest().command.execute(sender, new String[] {});

        Assertions.assertTrue(res);
    }

    @Test
    public void permission_commandBody_HasNotPerm() {
        CommandSender sender = PermissionTest.senderWithPermission(Map.of("a", false));

        boolean res = new PermissionsTest().command.execute(sender, new String[] {});

        Assertions.assertFalse(res);
    }

    @Test
    public void permission_commandBody_Sub_HasParentPerm() {
        CommandSender sender = PermissionTest.senderWithPermission(Map.of("a", true));

        boolean res = new PermissionsTest().command.execute(sender, new String[] {"b"});

        Assertions.assertTrue(res);
    }

    @Test
    public void permission_commandBody_Sub_HasNotParentPerm_HasSubPerm() {
        CommandSender sender = PermissionTest.senderWithPermission(Map.of("a", false, "a.b", true));

        boolean res = new PermissionsTest().command.execute(sender, new String[] {"b"});

        Assertions.assertTrue(res);
    }

    @Test
    public void permission_commandBody_SubSub_HasOnlySubSubPerm() {
        CommandSender sender = PermissionTest.senderWithPermission(Map.of("a", false, "a.b", false, "a.b.c", true));

        boolean res = new PermissionsTest().command.execute(sender, new String[] {"b", "c"});

        Assertions.assertTrue(res);
    }
}

@Command(name = "a", permission = @Perm(mode = Perm.Mode.AUTO))
class PermissionsTest extends CommandContext {

    @CommandBody
    public void command()
    {
    }

    @Command
    public static class B {
        @CommandBody
        public void command()
        {
        }

        @Command
        public void c()
        {

        }
    }
}