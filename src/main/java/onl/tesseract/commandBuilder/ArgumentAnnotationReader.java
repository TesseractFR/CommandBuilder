package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Env;
import onl.tesseract.commandBuilder.annotation.ErrorHandler;
import onl.tesseract.commandBuilder.annotation.Parser;
import onl.tesseract.commandBuilder.annotation.TabCompleter;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

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
                method.setAccessible(true);
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
                    catch (IllegalAccessException e)
                    {
                        throw new CommandBuildException(e);
                    }
                    catch (InvocationTargetException e)
                    {
                        if (e.getCause() instanceof RuntimeException)
                            throw (RuntimeException) e.getCause();
                        throw new CommandBuildException(e);
                    }
                };
            }
        }
        return null;
    }

    Map<Class<? extends Throwable>, Function<String, String>> readErrorHandlers()
    {
        Map<Class<? extends Throwable>, Function<String, String>> res = new HashMap<>();
        for (final Method method : this.clazz.getDeclaredMethods())
        {
            ErrorHandler[] annotations = method.getAnnotationsByType(ErrorHandler.class);
            if (annotations.length == 0)
                continue;

            method.setAccessible(true);
            Parameter[] parameters = method.getParameters();
            if (parameters.length > 1 || (parameters.length == 1 && parameters[0].getType() != String.class))
                throw new CommandBuildException("Expected one parameter of type String for error handler " + method.getName());
            Function<String, String> func = input -> {
                try
                {
                    if (parameters.length == 0)
                        return String.valueOf(method.invoke(argument));
                    else
                        return String.valueOf(method.invoke(argument, input));
                }
                catch (IllegalAccessException | InvocationTargetException e)
                {
                    throw new CommandBuildException(e);
                }
            };
            for (final ErrorHandler annotation : annotations)
            {
                res.put(annotation.value(), func);
            }
        }
        return res;
    }

    @Nullable
    BiFunction<CommandSender, CommandEnvironment, List<String>> readCompletion()
    {
        for (final Method method : this.clazz.getDeclaredMethods())
        {
            TabCompleter annotation = method.getAnnotation(TabCompleter.class);
            if (annotation != null)
            {
                return (sender, env) -> {
                    return (List<String>) new MethodInvoker(method, argument).invoke(env);
                };
            }
        }
        return null;
    }
}
