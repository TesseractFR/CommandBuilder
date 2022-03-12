package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import onl.tesseract.commandBuilder.annotation.Env;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class ClassAnnotationReader extends AnnotationReader {

    private final Class<?> clazz;

    ClassAnnotationReader(final Object instance)
    {
        super(instance, instance.getClass().getAnnotation(Command.class));
        this.clazz = instance.getClass();
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

    @Nullable
    @Override
    Consumer<CommandEnvironment> readCommandBody(Object instantiatedObject)
    {
        for (final Method method : clazz.getDeclaredMethods())
        {
            CommandBody annotation = method.getAnnotation(CommandBody.class);
            if (annotation == null)
                continue;
            method.setAccessible(true);
            return env -> {
                Parameter[] parameters = method.getParameters();
                Object[] objects = new Object[parameters.length];

                for (int i = 0; i < parameters.length; i++)
                {
                    Parameter parameter = parameters[i];
                    Env envAnnotation = parameter.getAnnotation(Env.class);
                    if (envAnnotation != null)
                    {
                        Object o = env.get(envAnnotation.key(), Object.class);
                        objects[i] = o;
                    }
                    else if (parameter.getType() == CommandEnvironment.class)
                        objects[i] = env;
                    else if (parameter.getType() == CommandSender.class)
                        objects[i] = env.getSender();
                    else if (parameter.getType() == Player.class)
                        objects[i] = env.getSenderAsPlayer();
                }

                try
                {
                    method.invoke(instantiatedObject, objects);
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                    e.printStackTrace();
                }
            };
        }
        return null;
    }

    List<CommandBuilder> readSubCommands()
    {
        List<CommandBuilder> res = new ArrayList<>();
        Method[] methods = clazz.getDeclaredMethods();
        for (final Method method : methods)
        {
            Command annotation = method.getAnnotation(Command.class);
            if (annotation == null)
                continue;
            method.setAccessible(true);
            res.add(new CommandBuilderProvider().provideFor(instance, method));
        }
        for (final Class<?> innerClass : clazz.getDeclaredClasses())
        {
            Command annotation = innerClass.getAnnotation(Command.class);
            if (annotation == null)
                continue;
            try
            {
                Object instance = innerClass.getDeclaredConstructor().newInstance();
                res.add(new CommandBuilderProvider().provideForClass(instance));
            }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
            {
                throw new CommandBuildException(e);
            }
        }
        // Read external classes
        for (final Class<?> outerClass : commandAnnotation.subCommands())
        {
            Command annotation = outerClass.getAnnotation(Command.class);
            if (annotation == null)
                continue;
            try
            {
                Object instance = outerClass.getDeclaredConstructor().newInstance();
                res.add(new CommandBuilderProvider().provideForClass(instance));
            }
            catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
            {
                throw new CommandBuildException(e);
            }
        }
        return res;
    }
}
