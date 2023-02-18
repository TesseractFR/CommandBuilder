package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandPredicate;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import onl.tesseract.commandBuilder.exception.InvalidArgumentTypeException;

import java.lang.reflect.Method;
import java.util.ArrayList;
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

        args.addAll(readMethodPassedArguments(method));
        return args;
    }

    @Override
    Consumer<CommandEnvironment> readCommandBody()
    {
        return env -> {
            new MethodInvoker(method, instanceFactory.getClassInstance(method.getDeclaringClass()))
                    .invoke(env);
        };
    }

    @Override
    List<Predicate<CommandEnvironment>> readPredicates()
    {
        return readPredicates(method.getAnnotationsByType(CommandPredicate.class));
    }

    @Override
    List<CommandArgumentDefinition<?>> readBodyArguments()
    {
        return new ArrayList<>();
    }
}
