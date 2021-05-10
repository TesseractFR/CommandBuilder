package onl.tesseract.commandBuilder;

import java.util.function.Function;

public class CommandArgument {
    public final String name;
    public final Class<?> clazz;
    public Function<String, Object> supplier;

    public CommandArgument(String name, Class<?> clazz)
    {
        this.name = name;
        this.clazz = clazz;
    }

    public CommandArgument supplier(Function<String, Object> supplier)
    {
        this.supplier = supplier;
        return this;
    }
}
