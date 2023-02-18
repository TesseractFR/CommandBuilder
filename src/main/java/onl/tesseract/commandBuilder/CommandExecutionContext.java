package onl.tesseract.commandBuilder;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
class CommandExecutionContext {

    private final CommandEnvironment environment;
    private final CommandDefinition rootCommand;
    private final String[] args;
    private int currentArgIndex;

    public CommandExecutionContext(final CommandEnvironment environment, final CommandDefinition rootCommand, final String[] args)
    {
        this.environment = environment;
        this.rootCommand = rootCommand;
        this.args = args;
    }

    @Nullable
    public String nextArg()
    {
        if (currentArgIndex == args.length)
            return null;
        return args[currentArgIndex++];
    }

    @Nullable
    public String peekNextArg()
    {
        if (currentArgIndex == args.length)
            return null;
        return args[currentArgIndex];
    }

    public boolean hasNextArg()
    {
        return currentArgIndex < args.length;
    }

    public int countRemainingArgs()
    {
        return args.length - currentArgIndex;
    }
}
