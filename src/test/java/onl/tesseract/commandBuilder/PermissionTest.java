package onl.tesseract.commandBuilder;

import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class PermissionTest {

    public static CommandSender senderWithPermission(Map<String, Boolean> perms)
    {
        CommandSender mock = mock(CommandSender.class);
        when(mock.isPermissionSet(anyString())).thenReturn(false);

        perms.forEach((name, flag) -> {
            when(mock.isPermissionSet(name)).thenReturn(true);
            when(mock.hasPermission(name)).thenReturn(flag);
        });
        return mock;
    }

    @Test
    void simpleHasPermissionTest()
    {
        Permission a = Permission.get("a");
        Permissible sender = senderWithPermission(Map.of("a", true));

        assertTrue(a.hasPermission(sender));
    }

    @Test
    void simpleHasPermission_NotSet()
    {
        Permission a = Permission.get("a");
        Permissible sender = senderWithPermission(Map.of());

        assertFalse(a.hasPermission(sender));
    }

    @Test
    void subPermission_SubSet()
    {
        Permission a = Permission.get("a.b");
        Permissible sender = senderWithPermission(Map.of("a.b", true));

        assertTrue(a.hasPermission(sender));
    }

    @Test
    void subPermission_ParentSet()
    {
        Permission a = Permission.get("a.b");
        Permissible sender = senderWithPermission(Map.of("a", true));

        assertTrue(a.hasPermission(sender));
    }

    @Test
    void subPermission_GlobSet()
    {
        Permission a = Permission.get("a.b");
        Permissible sender = senderWithPermission(Map.of("a.*", true));

        assertTrue(a.hasPermission(sender));
    }

    @Test
    void subPermission_ParentFalse_SubTrue()
    {
        Permission a = Permission.get("a.b");
        Permissible sender = senderWithPermission(Map.of("a", false, "a.b", true));

        assertTrue(a.hasPermission(sender));
    }

    @Test
    void subPermission_ParentTrue_SubFalse()
    {
        Permission a = Permission.get("a.b");
        Permissible sender = senderWithPermission(Map.of("a", true, "a.b", false));

        assertFalse(a.hasPermission(sender));
    }

    @Test
    void subPermission_ParentTrue_SubTrue_GlobFalse()
    {
        Permissible sender = senderWithPermission(Map.of("a", true, "a.*", false, "a.b", true));

        assertTrue(Permission.get("a.b").hasPermission(sender));
        assertFalse(Permission.get("a.c").hasPermission(sender));
        assertTrue(Permission.get("a").hasPermission(sender));
    }
}
