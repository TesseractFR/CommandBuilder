package onl.tesseract.commandBuilder;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Consumer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.logging.Level;

public class CommandBuilder {
    ArrayList<CommandArgument> arguments = new ArrayList<>();
    ArrayList<OptionalCommandArgument> optionalArguments = new ArrayList<>();
    Consumer<CommandEnvironment> consumer;
    private final HashMap<String, CommandBuilder> subCommands = new HashMap<>();
    private BiFunction<CommandSender, CommandEnvironment, String[]> help;
    private String description;
    private final String name;

    public CommandBuilder(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public CommandBuilder withArg(CommandArgument argument)
    {
        arguments.add(argument);
        return this;
    }

    public CommandBuilder withOptionalArg(OptionalCommandArgument optArg)
    {
        optionalArguments.add(optArg);
        return this;
    }

    public CommandBuilder command(Consumer<CommandEnvironment> consumer)
    {
        this.consumer = consumer;
        return this;
    }

    public CommandBuilder help(BiFunction<CommandSender, CommandEnvironment, String[]> help)
    {
        this.help = help;
        return this;
    }

    public String[] help(CommandSender sender, CommandEnvironment env)
    {
        if (help != null)
            return help.apply(sender, env);
        else
        {
            String[] msg = new String[subCommands.size() + (description == null ? 0 : 1)];
            StringJoiner argListJoiner = new StringJoiner(" ", name, "");
            for (CommandArgument arg : arguments)
                argListJoiner.add("{" + arg.name + "}");
            for (CommandArgument arg : optionalArguments)
                argListJoiner.add("[" + arg.name + "]");
            String argList = argListJoiner.toString();
            int i = 0;
            if (description != null)
                msg[i++] = argList + " : " + description;
            for (CommandBuilder subCommand : subCommands.values())
                msg[i++] = argList + (subCommand.hasDescription() ? " : " + subCommand.getDescription() : "");
            return msg;
        }
    }

    public CommandBuilder description(final String description)
    {
        this.description = description;
        return this;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean hasDescription()
    {
        return description != null;
    }

    public void execute(CommandSender sender, List<String> args)
    {
        execute(sender, new CommandEnvironment(), args);
    }

    public void execute(CommandSender sender, CommandEnvironment env, List<String> args)
    {
        if (args.size() < arguments.size())
        {
            sender.sendMessage(help(sender, env));
            return;
        }
        if (!optionalArguments.isEmpty() && args.size() > arguments.size() + optionalArguments.size())
        {
            sender.sendMessage(help(sender, env));
            return;
        }

        // Parse mandatory arguments
        int i;
        for (i = 0; i < arguments.size() ; i++)
        {
            if (!parseArgument(env, arguments.get(i), args.get(i), sender))
            {
                sender.sendMessage(help(sender, env));
                return;
            }
        }

        // Parse optional arguments
        if (!optionalArguments.isEmpty())
        {
            for (i = 0; i + arguments.size() < args.size() && i < optionalArguments.size(); i++)
            {
                if (!parseArgument(env, optionalArguments.get(i), args.get(i + arguments.size()), sender))
                {
                    sender.sendMessage(help(sender, env));
                    return;
                }
            }
            // Default values
            for (; i < optionalArguments.size(); i++)
                env.set(optionalArguments.get(i).name, optionalArguments.get(i).getDefault(env));
        }
        else if (i < args.size() && subCommands.containsKey(args.get(i)))
        {
            subCommands.get(args.get(i)).execute(sender, env, args.subList(i + 1, args.size()));
            return;
        }

        consumer.accept(env);
    }

    private boolean parseArgument(CommandEnvironment env, CommandArgument argument, String input, CommandSender sender)
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
            }
            return false;
        }
        env.set(argument.name, parsed);
        return true;
    }

    public CommandBuilder subCommand(CommandBuilder subCommand)
    {
        subCommands.put(subCommand.name, subCommand);
        return this;
    }
}
