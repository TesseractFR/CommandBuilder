package onl.tesseract.commandBuilder;

import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.function.BiFunction;

public class TextCommandArgument extends CommandArgument{
    /**
     * Create a new command argument, to be used with CommandBuilder
     *
     * @param name name of the argument.
     */
    public TextCommandArgument(String name)
    {
        super(name, String.class);
        supplier((input, env) -> input);
    }

    @Override
    public CommandArgument supplier(BiFunction<String, CommandEnvironment, Object> supplier)
    {
        super.supplier(supplier);
        return this;
    }

    @Override
    public CommandArgument error(Class<? extends Throwable> throwable, String message)
    {
        super.error(throwable, message);
        return this;
    }

    @Override
    public CommandArgument tabCompletion(BiFunction<CommandSender, CommandEnvironment, List<String>> tabCompletion)
    {
        super.tabCompletion(tabCompletion);
        return this;
    }

    @Override
    public TextCommandArgument name(String name)
    {
        super.name(name);
        return this;
    }
}
