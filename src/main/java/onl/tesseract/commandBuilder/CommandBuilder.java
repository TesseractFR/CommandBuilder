package onl.tesseract.commandBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CommandBuilder {
    ArrayList<CommandArgument> arguments = new ArrayList<>();
    Consumer<CommandEnvironment> consumer;

    public CommandBuilder withArg(CommandArgument argument)
    {
        arguments.add(argument);
        return this;
    }

    public CommandBuilder command(Consumer<CommandEnvironment> consumer)
    {
        this.consumer = consumer;
        return this;
    }

    public void execute(List<String> args)
    {
        CommandEnvironment env = new CommandEnvironment();
        for (int i = 0; i < args.size(); i++)
        {
            arguments.get(i).
        }
    }
}
