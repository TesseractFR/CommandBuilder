package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        for (final Method method : clazz.getMethods())
        {
            CommandBody annotation = method.getAnnotation(CommandBody.class);
            if (annotation == null)
                continue;
            if (method.getParameters().length != 1 || !CommandEnvironment.class.isAssignableFrom(method.getParameters()[0].getType()))
                throw new CommandBuildException("CommandBody should have 1 parameter of type CommandEnvironment");
            return env -> {
                try
                {
                    method.invoke(env);
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
        Method[] methods = clazz.getMethods();
        for (final Method method : methods)
        {
            Command annotation = method.getAnnotation(Command.class);
            if (annotation == null)
                continue;
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
