package onl.tesseract.commandBuilder;

import java.util.function.BiFunction;
import java.util.function.Function;

public class OptionalCommandArgument extends CommandArgument{
    private Function<CommandEnvironment, Object> def;

    public OptionalCommandArgument(String name, Class<?> clazz)
    {
        super(name, clazz);
    }

    public boolean hasDefault()
    {
        return def != null;
    }

    public OptionalCommandArgument defaultValue(Function<CommandEnvironment, Object> def)
    {
        this.def = def;
        return this;
    }

    public Object getDefault(CommandEnvironment env)
    {
        return def.apply(env);
    }

    @Override
    public OptionalCommandArgument supplier(BiFunction<String, CommandEnvironment, Object> supplier)
    {
        super.supplier(supplier);
        return this;
    }

    @Override
    public OptionalCommandArgument error(Class<? extends Throwable> throwable, String message)
    {
        super.error(throwable, message);
        return this;
    }
}
