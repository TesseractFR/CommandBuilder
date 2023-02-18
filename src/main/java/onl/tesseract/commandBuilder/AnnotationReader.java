package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandPredicate;
import onl.tesseract.commandBuilder.annotation.Perm;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import onl.tesseract.commandBuilder.exception.CommandExecutionException;
import onl.tesseract.commandBuilder.exception.InvalidArgumentTypeException;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

abstract class AnnotationReader {
    protected final Command commandAnnotation;
    protected final Object instance;
    protected final CommandInstanceFactory instanceFactory;

    protected AnnotationReader(Object instance, final Command commandAnnotation,
                               final CommandInstanceFactory instanceFactory)
    {
        this.instance = instance;
        this.commandAnnotation = commandAnnotation;
        this.instanceFactory = instanceFactory;
    }

    abstract String readName();

    String[] readAliases()
    {
        return commandAnnotation.alias();
    }

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
        // FIXME
//        return commandAnnotation.permission();
        return "";
    }

    boolean readPlayerOnly()
    {
        return commandAnnotation.playerOnly();
    }

    List<CommandArgumentDefinition<?>> readArguments() throws CommandBuildException
    {
        Argument[] args = commandAnnotation.args();
        List<CommandArgumentDefinition<?>> res = new ArrayList<>();

        for (Argument argAnnotation : args)
        {
            try
            {
                CommandArgumentBuilder<?> argBuilder = CommandArgumentBuilder.getBuilderNsm(argAnnotation.clazz(), argAnnotation.label());
                argBuilder.setOptional(argAnnotation.optional());
                argBuilder.setDefaultInput(argAnnotation.def());
                res.add(argBuilder.build());
            }
            catch (Exception e)
            {
                throw new CommandBuildException(e);
            }
        }
        return res;
    }

    @Nullable
    abstract Consumer<CommandEnvironment> readCommandBody();

    abstract List<Predicate<CommandEnvironment>> readPredicates();

    abstract List<CommandArgumentDefinition<?>> readBodyArguments();

    protected List<Predicate<CommandEnvironment>> readPredicates(CommandPredicate[] annotations)
    {
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
                Object invoke = new MethodInvoker(method, instanceFactory.getClassInstance(method.getDeclaringClass())).invoke(env);
                if (!(invoke instanceof Boolean))
                    throw new CommandExecutionException("Predicate function does not return a boolean: " + method.getName());
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
        Class<?> enclosingClass = clazz.getEnclosingClass();
        if (enclosingClass != null)
            map.putAll(getNamedMethods(enclosingClass));
        return map;
    }

    List<Pair<String, Function<CommandEnvironment, Object>>> readEnvInserters()
    {
        return List.of();
    }

    /**
     * Returns the list of arguments that appear in the method parameters
     */
    protected List<CommandArgumentDefinition<?>> readMethodPassedArguments(final Method method)
    {
        List<CommandArgumentDefinition<?>> args = new ArrayList<>();
        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters)
        {
            Argument argAnnotation = parameter.getAnnotation(Argument.class);
            if (argAnnotation == null)
                continue;
            Class<? extends CommandArgument<?>> type = argAnnotation.clazz();
            if (type == Argument.None.class)
            {
                if (!CommandArgument.class.isAssignableFrom(parameter.getType()))
                    throw new InvalidArgumentTypeException(parameter.getType().getSimpleName() + " is not a valid argument type");
                type = (Class<? extends CommandArgument<?>>) parameter.getType();
            }

            try
            {
                CommandArgumentBuilder<?> builder = CommandArgumentBuilder.getBuilderNsm(type, argAnnotation.label());
                builder.setOptional(argAnnotation.optional());
                builder.setDefaultInput(argAnnotation.def());
                args.add(builder.build());
            }
            catch (Exception e)
            {
                throw new CommandBuildException(e);
            }
        }
        return args;
    }

    public Object getInstance()
    {
        return instance;
    }
}
