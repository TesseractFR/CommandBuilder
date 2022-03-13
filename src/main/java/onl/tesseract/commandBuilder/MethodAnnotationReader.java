package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandPredicate;
import onl.tesseract.commandBuilder.exception.CommandBuildException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
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
        CommandPredicate[] annotations = method.getAnnotationsByType(CommandPredicate.class);
        List<Predicate<CommandEnvironment>> res = new ArrayList<>();

        Map<String, Method> methods = getNamedMethods(instance.getClass());
        for (final CommandPredicate annotation : annotations)
        {
            String predicateName = annotation.value();
            Method method = methods.get(predicateName);
            if (method == null)
                throw new CommandBuildException("No predicate method found with name " + predicateName);
            if (method.getReturnType() != boolean.class)
                throw new CommandBuildException("Predicate " + predicateName + " should have boolean return type");
            res.add(env -> {
                Object invoke = new MethodInvoker(method, instance).invoke(env);
                return (boolean) invoke;
            });
        }
        return res;
    }

    private Map<String, Method> getNamedMethods(Class<?> clazz)
    {
        Map<String, Method> map = new HashMap<>();
        Method[] methods = clazz.getDeclaredMethods();
        for (final Method method : methods)
        {
            map.put(method.getName(), method);
        }
        return map;
    }
}
