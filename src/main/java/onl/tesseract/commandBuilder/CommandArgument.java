package onl.tesseract.commandBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class CommandArgument {
    public final String name;
    public final Class<?> clazz;
    public BiFunction<String, CommandEnvironment, Object> supplier;
    private final Map<Class<? extends Throwable>, String> errors = new HashMap<>();

    public CommandArgument(String name, Class<?> clazz)
    {
        this.name = name;
        this.clazz = clazz;
    }

    public CommandArgument supplier(BiFunction<String, CommandEnvironment, Object> supplier)
    {
        this.supplier = supplier;
        return this;
    }

    public CommandArgument error(Class<? extends Throwable> throwable, String message)
    {
        errors.put(throwable, message);
        return this;
    }

    public String onError(Class<? extends Throwable> throwable)
    {
        return errors.get(throwable);
    }

    public boolean hasError(Class<? extends Throwable> throwable)
    {
        return errors.containsKey(throwable);
    }
}
