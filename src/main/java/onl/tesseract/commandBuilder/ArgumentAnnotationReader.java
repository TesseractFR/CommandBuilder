package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Env;
import onl.tesseract.commandBuilder.annotation.Parser;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.BiFunction;

class ArgumentAnnotationReader {

    private final CommandArgument argument;
    private final Class<? extends CommandArgument> clazz;

    ArgumentAnnotationReader(final CommandArgument argument)
    {
        this.argument = argument;
        this.clazz = argument.getClass();
    }

    @Nullable BiFunction<String, CommandEnvironment, Object> readParser()
    {
        for (final Method method : this.clazz.getDeclaredMethods())
        {
            Parser annotation = method.getAnnotation(Parser.class);
            if (annotation != null)
            {
                return (input, env) -> {
                    Parameter[] parameters = method.getParameters();
                    Object[] objects = new Object[parameters.length];

                    for (int i = 0; i < parameters.length; i++)
                    {
                        Parameter parameter = parameters[i];
                        Env envAnnotation = parameter.getAnnotation(Env.class);
                        if (envAnnotation != null)
                        {
                            Object o = env.get(envAnnotation.key(), Object.class);
                            objects[i] = o;
                        }
                        else if (parameter.getType() == CommandEnvironment.class)
                            objects[i] = env;
                        else if (parameter.getType() == CommandSender.class)
                            objects[i] = env.getSender();
                        else if (parameter.getType() == Player.class)
                            objects[i] = env.getSenderAsPlayer();
                        else if (parameter.getType() == String.class)
                            objects[i] = input;
                    }
                    try
                    {
                        return method.invoke(argument, objects);
                    }
                    catch (IllegalAccessException | InvocationTargetException e)
                    {
                        throw new CommandBuildException(e);
                    }
                };
            }
        }
        return null;
    }
}
