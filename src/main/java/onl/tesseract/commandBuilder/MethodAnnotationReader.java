package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.exception.CommandBuildException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Map;
import java.util.function.Consumer;

final class MethodAnnotationReader extends AnnotationReader {
    private final Method method;

    MethodAnnotationReader(final Object instance, final Method method)
    {
        super(instance, method.getAnnotation(Command.class));
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
    Map<CommandArgument, Boolean> readArguments() throws CommandBuildException
    {
        Map<CommandArgument, Boolean> args = super.readArguments();

        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters)
        {
            Argument argAnnotation = parameter.getAnnotation(Argument.class);
            if (argAnnotation == null)
                continue;
            Class<?> clazz = argAnnotation.clazz() == void.class
                             ? parameter.getType()
                             : argAnnotation.clazz();
            String name = argAnnotation.label();

            try
            {
                //noinspection unchecked
                args.put(instantiateArgument((Class<? extends CommandArgument>) clazz, name), argAnnotation.optional());
            }
            catch (Exception e)
            {
                throw new CommandBuildException(e);
            }
        }
        return args;
    }

    @Override
    Consumer<CommandEnvironment> readCommandBody(Object instantiatedObject)
    {
        return env -> {
            new MethodInvoker(method, instantiatedObject)
                    .includeEnvArguments()
                    .invoke(env);
        };
    }
}
