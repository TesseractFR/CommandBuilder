package onl.tesseract.commandBuilder;

import lombok.Getter;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Getter
public final class Permission {
    private static final Map<String, Permission> permissionPool = new HashMap<>();
    public static final Permission NONE = newPermission("", null);

    @NotNull
    private final String name;
    @Nullable
    private final Permission parent;

    private Permission(@NotNull final String name, @Nullable final Permission parent)
    {
        this.name = name;
        this.parent = parent;
    }

    public boolean hasPermission(@NotNull final Permissible permissible)
    {
        if (permissible.isOp() || this == NONE)
            return true;

        if (permissible.isPermissionSet(this.name))
            return permissible.hasPermission(name);

        if (this.parent == null)
            return false;
        String globPermission = this.parent.name + ".*";
        if (permissible.isPermissionSet(globPermission))
            return permissible.hasPermission(globPermission);
        return this.parent.hasPermission(permissible);
    }

    @NotNull
    public Permission getChild(@NotNull final String name)
    {
        return Permission.get(this.name + "." + name);
    }

    private static Permission newPermission(@NotNull final String name, @Nullable final Permission parent)
    {
        Permission permission = new Permission(name, parent);
        permissionPool.put(name, permission);
        return permission;
    }

    @NotNull
    public static Permission get(@NotNull final String name)
    {
        if (permissionPool.containsKey(name))
            return permissionPool.get(name);

        int separatorIndex = name.lastIndexOf('.');
        if (separatorIndex == -1)
            return newPermission(name, null);

        Permission parent = Permission.get(name.substring(0, separatorIndex));
        return newPermission(name, parent);
    }
}
