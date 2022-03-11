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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
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
        return provide(new ClassAnnotationReader(commandBuilder.getClass()));
    }

    CommandBuilder provideFor(final Method method)
    {
        return provide(new MethodAnnotationReader(method));
    }

    CommandBuilder provide(AnnotationReader reader)
    {
        CommandBuilder res = new CommandBuilder(reader.readName())
                .permission(reader.readPermission())
                .description(reader.readDescription())
                .playerOnly(reader.readPlayerOnly());

        try
        {
            List<CommandArgument> arguments = reader.readArguments();
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

abstract class AnnotationReader {
    protected final Command commandAnnotation;

    protected AnnotationReader(final Command commandAnnotation)
    {
        this.commandAnnotation = commandAnnotation;
    }

    abstract String readName();

    String readName(String originalName)
    {
        if (originalName.endsWith("Command"))
            originalName = originalName.substring(0, originalName.lastIndexOf("Command"));

        originalName = originalName.substring(0, 1).toLowerCase() + originalName.substring(1);
        return originalName;
    }

    String readDescription()
    {
        return commandAnnotation.description();
    }

    String readPermission()
    {
        return commandAnnotation.permission();
    }

    boolean readPlayerOnly()
    {
        return commandAnnotation.playerOnly();
    }

    List<CommandArgument> readArguments() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        Argument[] args = commandAnnotation.args();
        List<CommandArgument> res = new ArrayList<>();

        for (Argument argAnnotation : args)
        {
            @SuppressWarnings("unchecked")
            CommandArgument commandArgument = instantiateArgument((Class<? extends CommandArgument>) argAnnotation.clazz(), argAnnotation.label());
            res.add(commandArgument);
        }
        return res;
    }

    protected CommandArgument instantiateArgument(Class<? extends CommandArgument> clazz, String name) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        Constructor<? extends CommandArgument> declaredConstructor = clazz.getDeclaredConstructor(String.class);
        return declaredConstructor.newInstance(name);
    }
}

final class MethodAnnotationReader extends AnnotationReader {
    private final Method method;

    public MethodAnnotationReader(final Method method)
    {
        super(method.getAnnotation(Command.class));
        this.method = method;
        if (commandAnnotation == null)
            throw new IllegalStateException(method.getName() + " method should be annotated with @Command");
    }

    @Override
    String readName()
    {
        return readName(method.getName());
    }

    @Override
    List<CommandArgument> readArguments() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        List<CommandArgument> args = super.readArguments();

        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters)
        {
            Argument argAnnotation = parameter.getAnnotation(Argument.class);
            Class<?> clazz = argAnnotation.clazz() == void.class
                             ? parameter.getType()
                             : argAnnotation.clazz();
            String name = argAnnotation.label();

            //noinspection unchecked
            args.add(instantiateArgument((Class<? extends CommandArgument>) clazz, name));
        }
        return args;
    }
}

final class ClassAnnotationReader extends AnnotationReader {

    private final Class<? extends CommandBuilderV2> clazz;

    ClassAnnotationReader(final Class<? extends CommandBuilderV2> clazz)
    {
        super(clazz.getAnnotation(Command.class));
        this.clazz = clazz;
        if (commandAnnotation == null)
            throw new IllegalStateException(clazz.getName() + " should be annotated with @Command");
    }

    String readName()
    {
        String name = commandAnnotation.name();
        return name.isEmpty()
               ? readName(clazz.getSimpleName())
               : name;
    }
}
