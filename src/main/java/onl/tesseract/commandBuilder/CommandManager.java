package onl.tesseract.commandBuilder;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to register commands for a plugin
 */
public final class CommandManager {

    private final JavaPlugin plugin;
    private final Map<Class<? extends CommandContext>, CommandContext> commands = new HashMap<>();

    public CommandManager(final JavaPlugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * Register a new command
     */
    public void register(@NotNull final CommandContext command, @NotNull final String commandName)
    {
        command.register(plugin, commandName);
        commands.put(command.getClass(), command);
    }

    /**
     * Get the instance of a command type
     */
    public <T extends CommandContext> T get(@NotNull final Class<T> type)
    {
        return type.cast(commands.get(type));
    }

    public Collection<CommandContext> getRegisteredCommands()
    {
        return Collections.unmodifiableCollection(commands.values());
    }
}
