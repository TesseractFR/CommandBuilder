package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    final CommandBuilder builder;

    public CommandContext()
    {
        builder = new CommandBuilderProvider().provideForClass(this);
    }

    @Override
    public final boolean onCommand(@NotNull final CommandSender sender, @NotNull final org.bukkit.command.Command command,
                                   @NotNull final String label,
                                   @NotNull final String[] args)
    {
        builder.execute(sender, args);
        return true;
    }

    @Nullable
    @Override
    public final List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final org.bukkit.command.Command command,
                                            @NotNull final String alias,
                                            @NotNull final String[] args)
    {
        return builder.tabComplete(sender, args);
    }
}

final class CommandBuilderProvider implements CommandInstanceFactory {

    private static final Map<Class<?>, Object> classToInstance = new HashMap<>();

    CommandBuilder provideForClass(final Object commandBuilder)
    {
        ClassAnnotationReader reader = new ClassAnnotationReader(commandBuilder, this);
        CommandBuilder command = provide(reader);
        for (final CommandBuilder subCommand : reader.readSubCommands())
        {
            command.subCommand(subCommand);
        }
        return command;
    }

    CommandBuilder provideFor(final Object instance, final Method method)
    {
        return provide(new MethodAnnotationReader(instance, method, this));
    }

    CommandBuilder provide(AnnotationReader reader)
    {
        CommandBuilder res = new CommandBuilder(reader.readName())
                .permission(reader.readPermission())
                .description(reader.readDescription())
                .playerOnly(reader.readPlayerOnly())
                .command(reader.readCommandBody());

        Map<CommandArgument, Boolean> arguments = reader.readArguments();
        arguments.forEach((arg, optional) -> {
            if (optional)
                res.withOptionalArg(arg);
            else
                res.withArg(arg);
        });
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
