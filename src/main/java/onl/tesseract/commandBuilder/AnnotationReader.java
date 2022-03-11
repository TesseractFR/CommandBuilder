package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.exception.CommandBuildException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

abstract class AnnotationReader {
    protected final Command commandAnnotation;

    protected AnnotationReader(final Command commandAnnotation)
    {
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
        Map<CommandArgument, Boolean> res = new HashMap<>();

        for (Argument argAnnotation : args)
        {
            try
            {
                @SuppressWarnings("unchecked")
                CommandArgument commandArgument = instantiateArgument((Class<? extends CommandArgument>) argAnnotation.clazz(), argAnnotation.label());
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
}
