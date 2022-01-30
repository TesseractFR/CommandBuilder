package onl.tesseract.commandBuilder;

import org.bukkit.command.CommandSender;

import java.util.HashMap;

/**
 * Execution environment of a CommandBuilder. Holds parsed arguments.
 */
public class CommandEnvironment {
    final CommandSender sender;
    HashMap<String, Object> args = new HashMap<>();

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
        return type.cast(args.get(argName));
    }

    public <T> T getOrDefault(String argName, Class<T> type, T def)
    {
        return type.cast(args.getOrDefault(argName, def));
    }

    public void set(String argName, Object value)
    {
        args.put(argName, value);
    }

    public CommandSender getSender()
    {
        return sender;
    }
}
