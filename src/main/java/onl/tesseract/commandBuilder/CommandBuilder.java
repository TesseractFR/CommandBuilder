package onl.tesseract.commandBuilder;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Used to build complex feature-rich commands. Can handle sub commands, arguments, optional
 * arguments, permissions, help messages, player-ony commands.
 */
public class CommandBuilder {
    ArrayList<CommandArgument> arguments = new ArrayList<>();
    ArrayList<OptionalCommandArgument> optionalArguments = new ArrayList<>();
    BiConsumer<CommandSender, CommandEnvironment> consumer;
    private final HashMap<String, CommandBuilder> subCommands = new HashMap<>();
    private String description;
    private final String name;
    private String permission;
    private boolean playerOnly;

    public CommandBuilder(String name)
    {
        this(name, true);
    }

    public CommandBuilder(String name, boolean generateDefaultHelp)
    {
        this.name = name;
        if (!generateDefaultHelp)
            return;
        subCommand(new CommandBuilder("help", false)
                           .description("Obtenir de l'aide sur une commande.")
                           .withOptionalArg(new OptionalCommandArgument("page", Integer.class)
                                                    .supplier((input, env) -> Integer.parseInt(input))
                                                    .error(NumberFormatException.class, "Nombre invalide"))
                           .command((sender, env) -> {
                               Integer page = env.get("page", Integer.class);
                               if (page == null)
                                   page = 1;
                               try
                               {
                                   sender.sendMessage(helpGetPage(sender, page - 1));
                               }
                               catch (IllegalArgumentException e)
                               {
                                   sender.sendMessage(helpGetPage(sender, 0));
                               }
                           }));
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

    /**
     * Set a permission to this command. CommandSender who don't have this permission will not be
     * able to perform this command.
     *
     * @param permission Permission to set
     *
     * @return this
     */
    public CommandBuilder permission(final String permission)
    {
        this.permission = permission;
        return this;
    }

    /**
     * Add an argument to this command.
     *
     * @param argument Argument
     *
     * @return this
     */
    public CommandBuilder withArg(CommandArgument argument)
    {
        arguments.add(argument);
        return this;
    }

    /**
     * Add an optional argument to this command. This is incompatible with the use of subcommands
     *
     * @param optArg Optional arg
     *
     * @return this
     */
    public CommandBuilder withOptionalArg(OptionalCommandArgument optArg)
    {
        if (!subCommands.isEmpty() && (subCommands.size() > 1 || !subCommands.containsKey("help")))
            throw new IllegalStateException("Optional arguments cannot be used in commands containing subcommands.");
        optionalArguments.add(optArg);
        return this;
    }

    /**
     * Set the function to be called when the command is performed by a CommandSender
     *
     * @param consumer Function
     *
     * @return this
     */
    public CommandBuilder command(BiConsumer<CommandSender, CommandEnvironment> consumer)
    {
        this.consumer = consumer;
        return this;
    }

    public String[] helpGetPage(CommandSender sender, int page)
    {
        final String[] lines = helpGetAllLines(sender);
        final int totalPageCount = lines.length / 8 + 1;
        if (page < 0 || page >= totalPageCount)
            throw new IllegalArgumentException("Page index must be between 0 and pageCount - 1");

        int start = page * 8;
        int end = (page + 1) * 8 + 1;
        end = Math.min(lines.length, end);

        String[] res = new String[4 + (end - start)];
        Arrays.fill(res, "");
        res[1] = ChatColor.GRAY + " --> help page " + (page + 1) + "/" + totalPageCount;
        // Copy command usages
        System.arraycopy(lines, start, res, 2, end - start);
        // Decoration
        String boxing = "================";
        res[0] = ChatColor.YELLOW + boxing + ChatColor.GOLD + " " + name + " "
                + ChatColor.YELLOW + boxing;
        res[res.length - 2] = "";
        res[res.length - 1] = ChatColor.YELLOW + "\\".repeat(16) + ChatColor.GOLD + " • "
                + ChatColor.YELLOW + "/".repeat(16);

        return res;
    }

    private String[] helpGetAllLines(CommandSender sender)
    {
        List<CommandBuilder> validSubCommands = subCommands.values().stream()
                                                           .filter(cmd -> cmd.hasPermission(sender))
                                                           .collect(Collectors.toList());
        String[] msg = new String[validSubCommands.size() + (description == null ? 0 : 1)];
        String argList = helpGetArgList();

        int i = 0;
        if (description != null)
            msg[i++] = ChatColor.GREEN + name + " " + argList + ChatColor.DARK_GRAY + " : " + ChatColor.DARK_GREEN
                    + description;
        for (CommandBuilder subCommand : validSubCommands)
            msg[i++] = ChatColor.GREEN + name + " " + argList + " " + subCommand.getName() + " " + subCommand
                    .helpGetArgList()
                    + (subCommand.hasDescription() ? ChatColor.DARK_GRAY + " : " + ChatColor.DARK_GREEN + subCommand
                    .getDescription() : "");
        return msg;
    }

    public void help(CommandSender sender)
    {
        CommandBuilder helpCommand = subCommands.get("help");
        if (helpCommand != null)
            helpCommand.execute(sender, new String[0]);
    }

    private String helpGetArgList()
    {
        StringJoiner argListJoiner = new StringJoiner(" ");
        for (CommandArgument arg : arguments)
            argListJoiner.add("{" + arg.getName() + "}");
        for (CommandArgument arg : optionalArguments)
            argListJoiner.add("[" + arg.getName() + "]");
        return argListJoiner.toString();
    }

    /**
     * Set a description for this command. The description will be shown in default help messages.
     *
     * @param description description
     *
     * @return this
     */
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

    /**
     * Executes this command
     *
     * @param sender Command sender
     * @param args Sent command arguments
     */
    public void execute(CommandSender sender, String[] args)
    {
        execute(sender, new CommandEnvironment(), args);
    }

    public void execute(CommandSender sender, CommandEnvironment env, String[] args)
    {
        if (consumer == null)
            throw new IllegalStateException("Missing command function.");
        if (playerOnly && !(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être faite en jeu.");
            return;
        }
        if (args.length < arguments.size())
        {
            help(sender);
            return;
        }
        if (!optionalArguments.isEmpty() && args.length > arguments.size() + optionalArguments.size())
        {
            help(sender);
            return;
        }

        // Parse mandatory arguments
        int i;
        for (i = 0; i < arguments.size(); i++)
        {
            if (arguments.get(i) instanceof TextCommandArgument)
            {
                String[] textParts = Arrays.copyOfRange(args, i, args.length);
                StringJoiner text = new StringJoiner(" ");
                for (String part : textParts)
                    text.add(part);
                parseArgument(env, arguments.get(i), text.toString(), sender);
                break;
            }
            if (!parseArgument(env, arguments.get(i), args[i], sender))
                return;
        }

        // Parse optional arguments
        if (!optionalArguments.isEmpty())
        {
            for (i = 0; i + arguments.size() < args.length && i < optionalArguments.size(); i++)
            {
                if (!parseArgument(env, optionalArguments.get(i), args[i + arguments.size()], sender))
                    return;
            }
            // Default values
            for (; i < optionalArguments.size(); i++)
                env.set(optionalArguments.get(i).getName(), optionalArguments.get(i).getDefault(env));
        }
        else if (i < args.length && subCommands.containsKey(args[i]))
        {
            CommandBuilder subCommand = subCommands.get(args[i]);
            if (subCommand.hasPermission(sender))
                subCommand.execute(sender, env, Arrays.copyOfRange(args, i + 1, args.length));
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
            {
                sender.sendMessage(ChatColor.RED + argument.onError(e.getClass()));
            }
            else
            {
                sender.sendMessage(ChatColor.RED
                                           + "Une erreur inattendue est survenue pendant l'execution de cette commande. Contactez un administrateur pour obtenir de l'aide.");
                System.err.println("Error while executing command");
                e.printStackTrace();
            }
            return false;
        }
        env.set(argument.getName(), parsed);
        return true;
    }

    /**
     * Add a subcommand. This is incompatible with the use of optional arguments
     *
     * @param subCommand Sub command
     *
     * @return this
     */
    public CommandBuilder subCommand(CommandBuilder subCommand)
    {
        if (!optionalArguments.isEmpty() && !subCommand.getName().equals("help"))
            throw new IllegalStateException("Optional arguments cannot be used in commands containing subcommands.");
        subCommands.put(subCommand.getName(), subCommand);
        return this;
    }

    /**
     * Returns the tab completion list of this command, based on tab completion list of
     * arguments and subcommand names.
     *
     * @param sender Sender
     * @param args Sent args
     *
     * @return Tab completion list
     */
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
                    env.set(arg.get().getName(), arg.get().supplier.apply(args[i], env));
                }
                catch (Exception e)
                {
                    return null;
                }
            }
            else
            {
                CommandBuilder subCmd = subCommands.get(args[i]);
                if (subCmd == null)
                    return null;
                return subCmd.tabComplete(sender, Arrays.copyOfRange(args, i + 1, args.length));
            }
        }
        // current arg being written
        final String finalArg = args[i];
        var arg = getAnyArgumentAt(i);

        if (arg.isPresent())
        {
            List<String> res = arg.get().tabComplete(sender, env);
            if (res == null)
                return null;
            return res.stream()
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

    /**
     * Set whether this command should only be performed by a player.
     *
     * @param playerOnly flag
     *
     * @return this
     */
    public CommandBuilder playerOnly(boolean playerOnly)
    {
        this.playerOnly = playerOnly;
        return this;
    }
}
