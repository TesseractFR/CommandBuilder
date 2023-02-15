package onl.tesseract.commandBuilder.definition;

import lombok.Getter;
import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandEnvironment;
import onl.tesseract.commandBuilder.exception.ArgumentParsingException;
import onl.tesseract.commandBuilder.v2.ArgumentErrorHandlers;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.BiFunction;

/**
 * Definition of a type of argument, how to parse it, how to auto-complete it, and how to handle errors
 */
public class CommandArgumentDefinition<T> {
    @Getter
    private final String name;
    @Getter
    private final Class<? extends CommandArgument<T>> type;
    private final BiFunction<String, CommandEnvironment, T> parser;
    private final BiFunction<String, CommandEnvironment, List<String>> tabCompleter;
    @Nullable
    private final String defaultInput;
    private final ArgumentErrorHandlers errorHandlers;
    private final boolean optional;

    public CommandArgumentDefinition(final String name,
                                     final Class<? extends CommandArgument<T>> type,
                                     final BiFunction<String, CommandEnvironment, T> parser,
                                     final BiFunction<String, CommandEnvironment, List<String>> tabCompleter,
                                     final String defaultInput,
                                     final ArgumentErrorHandlers errorHandlers,
                                     final boolean optional)
    {
        this.name = name;
        this.type = type;
        this.parser = parser;
        this.tabCompleter = tabCompleter;
        this.defaultInput = defaultInput;
        this.errorHandlers = errorHandlers;
        this.optional = optional;
    }

    /**
     * Get a new instance of this argument by parsing the input.
     *
     * @return The value resulting of the parsing of input, or null if a handled parsing error occurred
     *
     * @throws ArgumentParsingException If an exception occurred during parsing and was not handled
     */
    @Nullable
    public <U extends CommandArgument<T>> U newInstance(@NotNull String input,
                                                        @NotNull CommandEnvironment environment) throws ArgumentParsingException
    {
        try
        {
            T value = parser.apply(input, environment);
            return wrapValue(value);
        }
        catch (Exception e)
        {
            if (errorHandlers.hasHandlerFor(e.getClass()))
                environment.getSender().sendMessage(ChatColor.RED + errorHandlers.getMessageFor(e));
            else
                throw new ArgumentParsingException("Received exception during parsing", e);
        }
        return null;
    }

    @NotNull
    private <U extends CommandArgument<T>> U wrapValue(@NotNull T value) throws ArgumentParsingException
    {
        try
        {
            Constructor<? extends CommandArgument<T>> constructor = this.type.getDeclaredConstructor(String.class);
            return (U) constructor.newInstance(this.name).setValue(value);
        }
        catch (SecurityException | ReflectiveOperationException e)
        {
            throw new ArgumentParsingException("Failed to construct a new argument instance via reflective access", e);
        }
    }

    @Nullable
    public List<String> tabComplete(String input, CommandEnvironment env)
    {
        return tabCompleter == null ? null : tabCompleter.apply(input, env);
    }

    public boolean hasDefault()
    {
        return defaultInput != null && !defaultInput.isEmpty();
    }

    public @NotNull CommandArgument<T> getDefault(CommandEnvironment env) throws IllegalStateException, ArgumentParsingException
    {
        if (defaultInput == null)
            throw new IllegalStateException("Not default supplier provided");
        T value = parser.apply(defaultInput, env);
        return wrapValue(value);
    }

    public boolean isOptional()
    {
        return optional;
    }
}
