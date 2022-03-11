package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

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

    @Nullable
    Consumer<CommandEnvironment> readCommandBody()
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
}
