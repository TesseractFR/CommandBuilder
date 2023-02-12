package onl.tesseract.commandBuilder.v2;

import onl.tesseract.commandBuilder.CommandEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Represents an instance of a command argument
 */
public abstract class CommandArgument<T> {

    @NotNull
    private final String name;
    @Nullable
    private T value;

    protected CommandArgument(@NotNull String name)
    {
        this.name = name;
    }

    @NotNull
    protected abstract T parser(@NotNull String input, @NotNull CommandEnvironment environment);

    @NotNull
    protected abstract List<String> tabCompletion(@NotNull String input, @NotNull CommandEnvironment environment);

    // TODO: errors

    @NotNull
    public final T get()
    {
        return Objects.requireNonNull(value);
    }

    public CommandArgument<T> setValue(@NotNull final T value)
    {
        this.value = value;
        return this;
    }

    @NotNull
    public final String getName()
    {
        return name;
    }
}
