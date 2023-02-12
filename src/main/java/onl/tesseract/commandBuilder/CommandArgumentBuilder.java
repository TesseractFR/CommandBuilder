package onl.tesseract.commandBuilder;

import lombok.Setter;
import onl.tesseract.commandBuilder.definition.CommandArgumentDefinition;
import onl.tesseract.commandBuilder.v2.ArgumentErrorHandlers;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.function.Function;

public class CommandArgumentBuilder<T> {

    private final Class<? extends CommandArgument<T>> argumentClass;
    @Setter
    private boolean optional = false;
    @Setter
    private String defaultInput;
    @NotNull
    private final String name;

    public CommandArgumentBuilder(final Class<? extends CommandArgument<?>> argumentClass, @NotNull String name)
    {
        this.argumentClass = (Class<? extends CommandArgument<T>>) argumentClass;
        this.name = name;
    }

    public CommandArgumentDefinition<T> build() throws ReflectiveOperationException
    {
        Constructor<? extends CommandArgument<T>> constructor = argumentClass.getDeclaredConstructor(String.class);
        constructor.setAccessible(true);
        CommandArgument<T> argumentInstance = constructor.newInstance(name);

        ArgumentErrorHandlers errorHandlers = new ArgumentErrorHandlers();
        argumentInstance.errors(errorHandlers);

        return new CommandArgumentDefinition<>(name,
                argumentClass,
                argumentInstance::parser,
                argumentInstance::tabCompletion,
                defaultInput,
                errorHandlers,
                optional);
    }
}
