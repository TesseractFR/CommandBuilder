package onl.tesseract.commandBuilder;

import java.util.HashMap;

public class CommandEnvironment {
    HashMap<String, Object> args = new HashMap<>();

    public <T> T get(String argName, Class<T> type)
    {
        return type.cast(args.get(argName));
    }
}
