package onl.tesseract.commandBuilder;

import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CommandArgument {
    private String name;
    public final Class<?> clazz;
    public BiFunction<String, CommandEnvironment, Object> supplier;
    private final Map<Class<? extends Throwable>, Function<String, String>> errors;
    private BiFunction<CommandSender, CommandEnvironment, List<String>> tabCompletion;
    private Function<CommandEnvironment, Object> def;

    /**
     * Create a new command argument, to be used with CommandBuilder
     *
     * @param name name of the argument.
     * @param clazz Type
     */
    public CommandArgument(String name, Class<?> clazz)
    {
        this.name = name;
        this.clazz = clazz;
        ArgumentAnnotationReader reader = new ArgumentAnnotationReader(this);
        supplier = reader.readParser();
        errors = reader.readErrorHandlers();
        error(CommandArgumentException.class, msg -> msg);
    }

    protected CommandArgument(String name)
    {
        this(name, void.class);
    }

    public CommandArgument name(String name)
    {
        this.name = name;
        return this;
    }

    public String getName()
    {
        return name;
    }

    /**
     * Set the parser function
     *
     * @param supplier Parser function, taking the raw input and environment, returning the parsed value
     *
     * @return this
     */
    public CommandArgument supplier(BiFunction<String, CommandEnvironment, Object> supplier)
    {
        this.supplier = supplier;
        return this;
    }

    /**
     * Add an error message to be sent to the CommandSender in case of exception during parsing.
     *
     * @param throwable Type of exception
     * @param message Message
     *
     * @return this
     */
    public CommandArgument error(Class<? extends Throwable> throwable, String message)
    {
        errors.put(throwable, any -> message);
        return this;
    }

    public CommandArgument error(Class<? extends Throwable> throwable, Function<String, String> message)
    {
        errors.put(throwable, message);
        return this;
    }

    public String onError(Throwable throwable)
    {
        return errors.get(throwable.getClass()).apply(throwable.getMessage());
    }

    public boolean hasError(Class<? extends Throwable> throwable)
    {
        return errors.containsKey(throwable);
    }

    /**
     * Define the tab completion for this command
     *
     * @param tabCompletion Function
     *
     * @return this
     */
    public CommandArgument tabCompletion(BiFunction<CommandSender, CommandEnvironment, List<String>> tabCompletion)
    {
        this.tabCompletion = tabCompletion;
        return this;
    }

    @Nullable
    public List<String> tabComplete(CommandSender sender, CommandEnvironment env)
    {
        return tabCompletion == null ? null : tabCompletion.apply(sender, env);
    }

    public boolean hasDefault()
    {
        return def != null;
    }

    public CommandArgument defaultValue(Function<CommandEnvironment, Object> def)
    {
        this.def = def;
        return this;
    }

    public @Nullable Object getDefault(CommandEnvironment env)
    {
        return def == null ? null : def.apply(env);
    }
}
