package onl.tesseract.commandBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Represents an instance of a command argument
 */
public abstract class CommandArgument<T> {

    @NotNull
    private final String name;
    @Nullable
    private T value;

    public CommandArgument(@NotNull String name)
    {
        this.name = name;
    }

    public abstract void define(CommandArgumentBuilderSteps.Parser<T> builder);

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
