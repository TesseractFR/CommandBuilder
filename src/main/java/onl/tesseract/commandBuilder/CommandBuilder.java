package onl.tesseract.commandBuilder;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Consumer;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CommandBuilder {
    ArrayList<CommandArgument> arguments = new ArrayList<>();
    ArrayList<OptionalCommandArgument> optionalArguments = new ArrayList<>();
    BiConsumer<CommandSender, CommandEnvironment> consumer;
    private final HashMap<String, CommandBuilder> subCommands = new HashMap<>();
    private BiFunction<CommandSender, CommandEnvironment, String[]> help;
    private String description;
    private final String name;
    private String permission;
    private boolean playerOnly;

    public CommandBuilder(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String getPermission()
    {
        return permission;
    }

    public boolean hasPermission(CommandSender sender)
    {
        return permission == null || sender.hasPermission(permission);
    }

    public CommandBuilder permission(final String permission)
    {
        this.permission = permission;
        return this;
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

    public CommandBuilder command(BiConsumer<CommandSender, CommandEnvironment> consumer)
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
            List<CommandBuilder> validSubCommands = subCommands.values().stream()
                                                               .filter(cmd -> cmd.hasPermission(sender))
                                                               .collect(Collectors.toList());
            String[] msg = new String[validSubCommands.size() + (description == null ? 0 : 1)];
            StringJoiner argListJoiner = new StringJoiner(" ", name, "");
            for (CommandArgument arg : arguments)
                argListJoiner.add("{" + arg.name + "}");
            for (CommandArgument arg : optionalArguments)
                argListJoiner.add("[" + arg.name + "]");
            String argList = argListJoiner.toString();
            int i = 0;
            if (description != null)
                msg[i++] = argList + " : " + description;
            for (CommandBuilder subCommand : validSubCommands)
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
        if (consumer == null)
            throw new IllegalStateException("Missing command function.");
        if (playerOnly && !(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être faite en jeu.");
            return;
        }
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
        for (i = 0; i < arguments.size(); i++)
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
            CommandBuilder subCommand = subCommands.get(args.get(i));
            if (subCommand.hasPermission(sender))
                subCommand.execute(sender, env, args.subList(i + 1, args.size()));
            else
                sender.sendMessage(ChatColor.RED + "Vous n'avez pas la permission de faire cela.");
            return;
        }

        consumer.accept(sender, env);
    }

    private boolean parseArgument(CommandEnvironment env, CommandArgument argument, String input, CommandSender sender)
    {
        if (argument.supplier == null)
            throw new IllegalStateException("Missing argument parser (supplier).");
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

    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        CommandEnvironment env = new CommandEnvironment();
        int i = 0;
        for (; i < args.length - 1; i++)
        {
            Optional<CommandArgument> arg = getAnyArgumentAt(i);

            if (arg.isPresent())
            {
                try
                {
                    env.set(arg.get().name, arg.get().supplier.apply(args[i], env));
                }
                catch (Exception e)
                {
                    return null;
                }
            }
            else if (i == arguments.size()) // sub command names
            {
                return subCommands.values().stream()
                                  .filter(cmd -> cmd.hasPermission(sender))
                                  .map(CommandBuilder::getName)
                                  .collect(Collectors.toList());
            }
            else
            {
                CommandBuilder subCmd = subCommands.get(args[i]);
                if (subCmd == null)
                    return null;
                return subCmd.tabComplete(sender, Arrays.copyOfRange(args, i, args.length));
            }
        }
        // current arg being written
        final String finalArg = args[i];
        var arg = getAnyArgumentAt(i);

        if (arg.isPresent())
        {
            return arg.get().tabComplete(sender, env).stream()
                      .filter(s -> s.startsWith(finalArg))
                      .collect(Collectors.toList());
        }
        else if (i == arguments.size()) // sub command names
        {
            return subCommands.values().stream()
                              .filter(cmd -> cmd.hasPermission(sender))
                              .map(CommandBuilder::getName)
                              .filter(s -> s.startsWith(finalArg))
                              .collect(Collectors.toList());
        }
        return null;
    }

    private Optional<CommandArgument> getAnyArgumentAt(int index)
    {
        CommandArgument arg = null;
        if (index < arguments.size())
            arg = arguments.get(index);
        else if (index < optionalArguments.size())
            arg = optionalArguments.get(index - arguments.size());
        return Optional.ofNullable(arg);
    }

    public CommandBuilder playerOnly(boolean playerOnly)
    {
        this.playerOnly = playerOnly;
        return this;
    }
}
