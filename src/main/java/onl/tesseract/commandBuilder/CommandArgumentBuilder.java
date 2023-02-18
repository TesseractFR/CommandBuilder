package onl.tesseract.commandBuilder;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.BiFunction;

class CommandArgumentBuilder<T> {

    private final Class<? extends CommandArgument<T>> argumentClass;
    @Setter
    private boolean optional = false;
    @Nullable
    private String defaultInput;
    @NotNull
    private final String name;
    @Getter
    private final ArgumentErrorHandlers errorHandlers = new ArgumentErrorHandlers();
    private BiFunction<String, CommandEnvironment, List<String>> tabCompleter;
    private BiFunction<String, CommandEnvironment, T> parser;

    public CommandArgumentBuilder(final Class<? extends CommandArgument<?>> argumentClass, @NotNull String name)
    {
        this.argumentClass = (Class<? extends CommandArgument<T>>) argumentClass;
        this.name = name;
    }

    public CommandArgumentDefinition<T> build()
    {
        if (parser == null || tabCompleter == null)
            throw new IllegalStateException("Missing parser and/or tabCompleter for argument " + argumentClass.getSimpleName());
        return new CommandArgumentDefinition<>(name,
                argumentClass,
                parser,
                tabCompleter,
                defaultInput,
                errorHandlers,
                optional);
    }

    public CommandArgumentBuilder<T> setTabCompleter(final BiFunction<String, CommandEnvironment, List<String>> tabCompleter)
    {
        this.tabCompleter = tabCompleter;
        return this;
    }

    public CommandArgumentBuilder<T> setParser(final BiFunction<String, CommandEnvironment, T> parser)
    {
        this.parser = parser;
        return this;
    }

    public static <T> CommandArgumentBuilder<T> getBuilder(final Class<? extends CommandArgument<T>> argumentClass, @NotNull String name) throws ReflectiveOperationException
    {
        CommandArgumentBuilder<T> builder = new CommandArgumentBuilder<>(argumentClass, name);
        builder.getErrorHandlers().on(CommandArgumentException.class, msg -> msg);
        Constructor<? extends CommandArgument<T>> constructor = argumentClass.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        CommandArgument<T> argumentInstance = constructor.newInstance(name);

        argumentInstance.define(new ArgumentBuilderStepsImpl.Parser<>(builder));

        return builder;
    }

    public static <T> CommandArgumentBuilder<?> getBuilderNsm(final Class<? extends CommandArgument<?>> clazz, final String label) throws ReflectiveOperationException
    {
        return getBuilder((Class<? extends CommandArgument<T>>)clazz, label);
    }

    public CommandArgumentBuilder<T> setDefaultInput(final String defaultInput)
    {
        this.defaultInput = defaultInput;
        return this;
    }
}
