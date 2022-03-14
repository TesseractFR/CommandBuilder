package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandPredicate;
import onl.tesseract.commandBuilder.exception.CommandBuildException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
        String name = commandAnnotation.name();
        return name.isEmpty()
               ? readName(method.getName())
               : name;
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
            Class<?> clazz = argAnnotation.clazz();
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

    @Override
    List<Predicate<CommandEnvironment>> readPredicates()
    {
        return readPredicates(method.getAnnotationsByType(CommandPredicate.class));
    }
}
