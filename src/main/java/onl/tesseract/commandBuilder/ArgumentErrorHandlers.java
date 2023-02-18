package onl.tesseract.commandBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

public class ArgumentErrorHandlers {

    private final Map<Class<? extends Throwable>, UnaryOperator<String>> handlers = new HashMap<>();

    public ArgumentErrorHandlers on(Class<? extends Throwable> type, UnaryOperator<String> messageSupplier)
    {
        handlers.put(type, messageSupplier);
        return this;
    }

    public ArgumentErrorHandlers on(Class<? extends Throwable> type, String message)
    {
        handlers.put(type, originalMessage -> message);
        return this;
    }

    public boolean hasHandlerFor(Class<? extends Throwable> type)
    {
        return handlers.containsKey(type);
    }

    @NotNull
    public String getMessageFor(@NotNull Throwable error)
    {
        if (!handlers.containsKey(error.getClass()))
            throw new IllegalArgumentException("Error type not supported");
        return handlers.get(error.getClass()).apply(error.getMessage());
    }
}
