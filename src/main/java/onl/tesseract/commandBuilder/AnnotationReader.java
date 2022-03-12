package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

abstract class AnnotationReader {
    protected final Command commandAnnotation;
    protected final Object instance;

    protected AnnotationReader(Object instance, final Command commandAnnotation)
    {
        this.instance = instance;
        this.commandAnnotation = commandAnnotation;
    }

    abstract String readName();

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
        return commandAnnotation.permission();
    }

    boolean readPlayerOnly()
    {
        return commandAnnotation.playerOnly();
    }

    Map<CommandArgument, Boolean> readArguments() throws CommandBuildException
    {
        Argument[] args = commandAnnotation.args();
        // LinkedHashMap to keep insertion order
        Map<CommandArgument, Boolean> res = new LinkedHashMap<>();

        for (Argument argAnnotation : args)
        {
            try
            {
                @SuppressWarnings("unchecked")
                CommandArgument commandArgument = instantiateArgument(argAnnotation.clazz(), argAnnotation.label());
                if (argAnnotation.optional() && !argAnnotation.def().isEmpty())
                {
                    commandArgument.defaultValue(env -> commandArgument.supplier.apply(argAnnotation.def(), env));
                }
                res.put(commandArgument, argAnnotation.optional());
            }catch (Exception e)
            {
                throw new CommandBuildException(e);
            }
        }
        return res;
    }

    protected CommandArgument instantiateArgument(Class<? extends CommandArgument> clazz,
                                                  String name) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        Constructor<? extends CommandArgument> declaredConstructor = clazz.getDeclaredConstructor(String.class);
        return declaredConstructor.newInstance(name);
    }

    @Nullable
    abstract Consumer<CommandEnvironment> readCommandBody(Object instantiatedObject);

    public Object getInstance()
    {
        return instance;
    }
}
