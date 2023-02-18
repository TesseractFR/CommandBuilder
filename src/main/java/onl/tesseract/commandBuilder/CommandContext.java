package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Perm;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Class representation of a command that can be performed in-game.
 * Each implementing class should be annotated with {@link onl.tesseract.commandBuilder.annotation.Command}.
 * <br/>
 * Subcommands can be added by annotating methods and inner static classes with {@link onl.tesseract.commandBuilder.annotation.Command}.
 * <br/>
 * Use the annotation {@link onl.tesseract.commandBuilder.annotation.CommandBody} on a method to mark it as the method to be called when the command
 * is performed.
 */
public abstract class CommandContext implements CommandExecutor, TabCompleter {

    final CommandDefinition command;

    public CommandContext()
    {
        command = new CommandBuilderProvider().provideForClass(this).build(null);
    }

    public void register(final JavaPlugin plugin, final String commandName)
    {
        PluginCommand command = Objects.requireNonNull(plugin.getCommand(commandName));
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @Override
    public final boolean onCommand(@NotNull final CommandSender sender, @NotNull final org.bukkit.command.Command command,
                                   @NotNull final String label,
                                   @NotNull final String[] args)
    {
        this.command.execute(sender, args);
        return true;
    }

    @Nullable
    @Override
    public final List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final org.bukkit.command.Command command,
                                            @NotNull final String alias,
                                            @NotNull final String[] args)
    {
        return this.command.tabComplete(sender, args);
    }
}

final class CommandBuilderProvider implements CommandInstanceFactory {

    private static final Map<Class<?>, Object> classToInstance = new HashMap<>();

    CommandBuilder provideForClass(final Object commandBuilder) throws CommandBuildException
    {
        ClassAnnotationReader reader = new ClassAnnotationReader(commandBuilder, this);
        CommandBuilder command = provide(reader);
        for (final CommandBuilder subCommand : reader.readSubCommands())
        {
            command.subCommand(subCommand);
        }
        return command;
    }

    CommandBuilder provideFor(final Object instance, final Method method) throws CommandBuildException
    {
        return provide(new MethodAnnotationReader(instance, method, this));
    }

    CommandBuilder provide(AnnotationReader reader) throws CommandBuildException
    {
        Perm permission = reader.readPermission();
        CommandBuilder res = new CommandBuilder(reader.readName())
                .description(reader.readDescription())
                .playerOnly(reader.readPlayerOnly())
                .permission(permission.value())
                .setAbsolutePermission(permission.absolute())
                .setPermissionMode(permission.mode())
                .command(reader.readCommandBody());

        List<CommandArgumentDefinition<?>> arguments = reader.readArguments();
        arguments.forEach(arg -> {
            if (arg.isOptional())
                res.withOptionalArg(arg);
            else
                res.withArg(arg);
        });
        for (final CommandArgumentDefinition<?> arg : reader.readBodyArguments())
            res.withBodyArg(arg);
        reader.readPredicates().forEach(res::predicate);
        for (final String alias : reader.readAliases())
            res.alias(alias);
        reader.readEnvInserters().forEach(pair -> res.envInserter(pair.getLeft(), pair.getRight()));
        return res;
    }

    @Override
    public Object getClassInstance(final Class<?> clazz)
    {
        if (classToInstance.containsKey(clazz))
            return classToInstance.get(clazz);
        Object instance;
        try
        {
            instance = clazz.getDeclaredConstructor().newInstance();
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
        {
            throw new CommandBuildException(e);
        }
        classToInstance.put(clazz, instance);
        return instance;
    }
}
