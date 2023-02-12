package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Env;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class MethodInvoker {

    private final Method methodToInvoke;
    private final Object instance;

    public MethodInvoker(final Method methodToInvoke, final Object instance)
    {
        this.methodToInvoke = methodToInvoke;
        this.instance = instance;
    }

    @Nullable
    public Object invoke(CommandEnvironment env)
    {
        methodToInvoke.setAccessible(true);
        Parameter[] parameters = methodToInvoke.getParameters();
        Object[] objects = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++)
        {
            Parameter parameter = parameters[i];
            Argument annotation = parameter.getAnnotation(Argument.class);
            Env envAnnotation = parameter.getAnnotation(Env.class);
            if (annotation != null)
            {
                Object o = env.get(annotation.label(), parameter.getType());
                objects[i] = o;
            }
            else if (envAnnotation != null)
            {
                Object o = env.get(envAnnotation.key(), parameter.getType());
                objects[i] = o;
            }
            else if (parameter.getType() == CommandEnvironment.class)
                objects[i] = env;
            else if (parameter.getType() == CommandSender.class)
                objects[i] = env.getSender();
            else if (parameter.getType() == Player.class)
                objects[i] = env.getSenderAsPlayer();
        }

        try
        {
            return methodToInvoke.invoke(instance, objects);
        }
        catch (IllegalAccessException | InvocationTargetException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
