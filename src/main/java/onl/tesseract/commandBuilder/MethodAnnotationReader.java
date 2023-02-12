package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandPredicate;
import onl.tesseract.commandBuilder.definition.CommandArgumentDefinition;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import onl.tesseract.commandBuilder.exception.InvalidArgumentTypeException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class MethodAnnotationReader extends AnnotationReader {
    private final Method method;

    MethodAnnotationReader(final Object instance, final Method method, CommandInstanceFactory instanceFactory)
    {
        super(instance, method.getAnnotation(Command.class), instanceFactory);
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
    List<CommandArgumentDefinition<?>> readArguments() throws CommandBuildException, InvalidArgumentTypeException
    {
        List<CommandArgumentDefinition<?>> args = super.readArguments();

        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters)
        {
            Argument argAnnotation = parameter.getAnnotation(Argument.class);
            if (argAnnotation == null)
                continue;
            Class<? extends CommandArgument<?>> type = argAnnotation.clazz();
            if (type == Argument.None.class)
            {
                try
                {
                    type = (Class<? extends CommandArgument<?>>) parameter.getType();
                }
                catch (ClassCastException e)
                {
                    throw new InvalidArgumentTypeException(parameter.getType().getSimpleName() + " is not a valid argument type", e);
                }
            }
            CommandArgumentBuilder<?> argumentBuilder = new CommandArgumentBuilder<>(type, argAnnotation.label());
            argumentBuilder.setOptional(argAnnotation.optional());
            argumentBuilder.setDefaultInput(argAnnotation.def());
            try
            {
                args.add(argumentBuilder.build());
            }
            catch (Exception e)
            {
                throw new CommandBuildException(e);
            }
        }
        return args;
    }

    @Override
    Consumer<CommandEnvironment> readCommandBody()
    {
        return env -> {
            new MethodInvoker(method, instanceFactory.getClassInstance(method.getDeclaringClass()))
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
