package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import onl.tesseract.commandBuilder.annotation.CommandPredicate;
import onl.tesseract.commandBuilder.annotation.EnvInsert;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class ClassAnnotationReader extends AnnotationReader {

    private final Class<?> clazz;

    ClassAnnotationReader(final Object instance, CommandInstanceFactory instanceFactory)
    {
        super(instance, instance.getClass().getAnnotation(Command.class), instanceFactory);
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
    Consumer<CommandEnvironment> readCommandBody()
    {
        for (final Method method : clazz.getDeclaredMethods())
        {
            CommandBody annotation = method.getAnnotation(CommandBody.class);
            if (annotation == null)
                continue;
            return env -> {
                new MethodInvoker(method, instanceFactory.getClassInstance(method.getDeclaringClass())).invoke(env);
            };
        }
        return null;
    }

    @Override
    List<CommandArgumentDefinition<?>> readBodyArguments()
    {
        for (final Method method : clazz.getDeclaredMethods())
        {
            CommandBody annotation = method.getAnnotation(CommandBody.class);
            if (annotation == null)
                continue;
            return readMethodPassedArguments(method);
        }
        return new ArrayList<>();
    }

    List<CommandBuilder> readSubCommands()
    {
        List<Pair<CommandBuilder, Integer>> res = new ArrayList<>();
        Method[] methods = clazz.getDeclaredMethods();
        for (final Method method : methods)
        {
            Command annotation = method.getAnnotation(Command.class);
            if (annotation == null)
                continue;
            method.setAccessible(true);
            res.add(new Pair<>(new CommandBuilderProvider().provideFor(instance, method), annotation.helpPriority()));
        }
        res.addAll(readClassCommands(clazz.getDeclaredClasses()));
        // Read external classes
        res.addAll(readClassCommands(commandAnnotation.subCommands()));
        res.sort(Comparator.comparingInt(Pair::getRight));
        return res.stream()
                .map(Pair::getLeft)
                .collect(Collectors.toList());
    }

    private List<Pair<CommandBuilder, Integer>> readClassCommands(Class<?>[] classes) throws CommandBuildException
    {
        return Arrays.stream(classes)
                     .map(this::readClassCommand)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toList());
    }

    @Nullable
    private Pair<CommandBuilder, Integer> readClassCommand(final Class<?> clazz) throws CommandBuildException
    {
        Command annotation = clazz.getAnnotation(Command.class);
        if (annotation == null)
            return null;
        try
        {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            return new Pair<>(new CommandBuilderProvider().provideForClass(instance), annotation.helpPriority());
        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
        {
            throw new CommandBuildException(e);
        }
    }

    @Override
    List<Predicate<CommandEnvironment>> readPredicates()
    {
        return readPredicates(clazz.getAnnotationsByType(CommandPredicate.class));
    }

    @Override
    List<Pair<String, Function<CommandEnvironment, Object>>> readEnvInserters()
    {
        List<Pair<String, Function<CommandEnvironment, Object>>> res = new ArrayList<>();
        Method[] methods = clazz.getDeclaredMethods();
        for (final Method method : methods)
        {
            EnvInsert annotation = method.getAnnotation(EnvInsert.class);
            if (annotation == null)
                continue;
            String name = annotation.value();
            res.add(new Pair<>(name, env -> {
                return new MethodInvoker(method, instanceFactory.getClassInstance(method.getDeclaringClass()))
                        .invoke(env);
            }));
        }
        return res;
    }
}
