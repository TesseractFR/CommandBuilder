package onl.tesseract.commandBuilder.v2;

import onl.tesseract.commandBuilder.CommandEnvironment;
import onl.tesseract.commandBuilder.definition.CommandArgumentDefinition;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.Function;

public class CommandArgumentBuilder<T> {

    private final Class<? extends CommandArgument<T>> argumentClass;

    public CommandArgumentBuilder(final Class<? extends CommandArgument<T>> argumentClass)
    {
        this.argumentClass = argumentClass;
    }

    public CommandArgumentDefinition<T> build() throws ReflectiveOperationException
    {
        String name = "TODO";

        Constructor<? extends CommandArgument<T>> constructor = argumentClass.getDeclaredConstructor(String.class);
        CommandArgument<T> argumentInstance = constructor.newInstance(name);
        Function<CommandEnvironment, T> defSupplier = env -> null;

        return new CommandArgumentDefinition<>(name,
                argumentClass,
                argumentInstance::parser,
                argumentInstance::tabCompletion,
                defSupplier,
                Map.of());
    }
}
