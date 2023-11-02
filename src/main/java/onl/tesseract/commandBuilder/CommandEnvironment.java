package onl.tesseract.commandBuilder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Execution environment of a CommandBuilder. Holds parsed arguments.
 */
public class CommandEnvironment {
    private final CommandSender sender;
    private final HashMap<String, Object> values = new HashMap<>();
    private final Map<String, CommandArgument<?>> argumentMap = new HashMap<>();

    public CommandEnvironment(final CommandSender sender)
    {
        this.sender = sender;
    }

    /**
     * Get an argument
     *
     * @param argName name of the argument
     * @param type Type of the argument
     *
     * @return Typed argument, or null if the argument does not exist
     * @throws ClassCastException if the argument is not of type T
     */
    public <T> T get(String argName, Class<T> type)
    {
        if (values.containsKey(argName))
            return type.cast(values.get(argName));
        else if (argumentMap.containsKey(argName))
        {
            CommandArgument<?> argument = argumentMap.get(argName);
            if (type.isPrimitive())
                return (T) argument.get();
            if (type.isInstance(argument.get()))
                return type.cast(argument.get());
            else
                return type.cast(argument);
        }
        return null;
    }

    public Object get(String argName)
    {
        return values.getOrDefault(argName, argumentMap.get(argName));
    }

    public void set(String argName, Object value)
    {
        values.put(argName, value);
    }

    public void setArgument(String argName, CommandArgument<?> arg)
    {
        argumentMap.put(argName, arg);
    }

    public CommandSender getSender()
    {
        return sender;
    }

    public Player getSenderAsPlayer()
    {
        return (Player) sender;
    }
}
