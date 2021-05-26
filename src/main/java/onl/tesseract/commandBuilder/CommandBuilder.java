package onl.tesseract.commandBuilder;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class CommandBuilder {
    ArrayList<CommandArgument> arguments = new ArrayList<>();
    Consumer<CommandEnvironment> consumer;
    private final HashMap<String, CommandBuilder> subCommands = new HashMap<>();

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

    public void execute(CommandSender sender, List<String> args)
    {
        execute(sender, new CommandEnvironment(), args);
    }

    public void execute(CommandSender sender, CommandEnvironment env, List<String> args)
    {
        boolean subCommandExecuted = false;
        for (int i = 0; i < args.size(); i++)
        {
            if (i < arguments.size())
                parseArgument(env, arguments.get(i), args.get(i), sender);
            else if (subCommands.containsKey(args.get(i)))
            {
                subCommands.get(args.get(i)).execute(sender, env, args.subList(i + 1, args.size()));
                subCommandExecuted = true;
            }
        }

        if (!subCommandExecuted)
            consumer.accept(env);
    }

    private void parseArgument(CommandEnvironment env, CommandArgument argument, String input, CommandSender sender)
    {
        Object parsed;
        try
        {
            parsed = argument.supplier.apply(input, env);
        }
        catch (Exception e)
        {
            if (argument.hasError(e.getClass()))
                sender.sendMessage(ChatColor.RED + argument.onError(e.getClass()));
            else
            {
                if (argument.hasError(e.getClass()))
                    sender.sendMessage(ChatColor.RED + argument.onError(e.getClass()));
                else
                {
                    sender.sendMessage(ChatColor.RED + "Une erreur inattendue est survenue pendant l'execution de cette commande. Contactez un administrateur pour obtenir de l'aide.");
                    System.err.println("Error while executing command");
                    e.printStackTrace();
                }
                return;
            }
            return;
        }
        env.set(argument.name, parsed);
    }

    public CommandBuilder subCommand(String name, CommandBuilder subCommand)
    {
        subCommands.put(name, subCommand);
        return this;
    }
}
