package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class CommandBuilderV2 implements CommandExecutor, TabCompleter {

    final CommandBuilder builder;

    public CommandBuilderV2()
    {
        builder = new CommandBuilderProvider().provideFor(this);
    }

    @Override
    public final boolean onCommand(@NotNull final CommandSender sender, @NotNull final org.bukkit.command.Command command,
                                   @NotNull final String label,
                                   @NotNull final String[] args)
    {
        return false;
    }

    @Nullable
    @Override
    public final List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final org.bukkit.command.Command command,
                                            @NotNull final String alias,
                                            @NotNull final String[] args)
    {
        return null;
    }
}

final class CommandBuilderProvider {

    CommandBuilder provideFor(final CommandBuilderV2 commandBuilder)
    {
        ClassAnnotationReader reader = new ClassAnnotationReader(commandBuilder.getClass());
        CommandBuilder res = new CommandBuilder(reader.readName())
                .permission(reader.readPermission())
                .description(reader.readDescription())
                .playerOnly(reader.readPlayerOnly());

        try
        {
            CommandArgument[] arguments = reader.readArguments();
            for (final CommandArgument argument : arguments)
            {
                res.withArg(argument);
            }
            return res;
        }
        catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e)
        {
            throw new CommandBuildException(e);
        }
    }
}

final class ClassAnnotationReader {

    private final Class<? extends CommandBuilderV2> clazz;
    private final Command commandContext;

    ClassAnnotationReader(final Class<? extends CommandBuilderV2> clazz)
    {
        this.clazz = clazz;
        commandContext = clazz.getAnnotation(Command.class);
        if (commandContext == null)
            throw new IllegalStateException(clazz.getName() + " should be annotated with @CommandContext");
    }

    String readName()
    {
        String name = commandContext.name();
        return name.isEmpty()
               ? clazz.getSimpleName()
               : name;
    }

    String readDescription()
    {
        return commandContext.description();
    }

    String readPermission()
    {
        return commandContext.permission();
    }

    boolean readPlayerOnly()
    {
        return commandContext.playerOnly();
    }

    CommandArgument[] readArguments() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        Argument[] args = commandContext.args();
        CommandArgument[] res = new CommandArgument[args.length];

        for (int i = 0; i < args.length; i++)
        {
            Argument argAnnotation = args[i];
            @SuppressWarnings("unchecked")
            Constructor<? extends CommandArgument> declaredConstructor = (Constructor<? extends CommandArgument>) argAnnotation.clazz().getDeclaredConstructor(String.class);
            CommandArgument commandArgument = declaredConstructor.newInstance(argAnnotation.label());
            res[i] = commandArgument;
        }
        return res;
    }
}
