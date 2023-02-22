package onl.tesseract.commandBuilder;

import lombok.AccessLevel;
import lombok.Getter;
import onl.tesseract.commandBuilder.exception.ArgumentParsingException;
import onl.tesseract.commandBuilder.exception.CommandExecutionException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Getter(AccessLevel.PACKAGE)
public class CommandDefinition {
    private static final Logger logger = LoggerFactory.getLogger(CommandDefinition.class);

    private final List<CommandArgumentDefinition<?>> arguments;
    private final List<CommandArgumentDefinition<?>> optionalArguments;
    private final List<CommandArgumentDefinition<?>> bodyArguments;
    private final BiConsumer<CommandEnvironment, CommandDefinition> consumer;
    // Use linked hashmap to keep insertion order
    // Useful to display help messages with subcommands in a pertinent order
    private final Map<String, CommandDefinition> subCommands;
    private final Map<String, CommandDefinition> subCommandsAliases;
    private final String description;
    private final String name;
    @NotNull
    private final Permission permission;
    private final boolean playerOnly;
    private final List<Predicate<CommandEnvironment>> predicates;
    private final List<String> aliases;
    private final List<Pair<String, Function<CommandEnvironment, Object>>> envInserters;

    CommandDefinition(final List<CommandArgumentDefinition<?>> arguments, final List<CommandArgumentDefinition<?>> optionalArguments,
                      final List<CommandArgumentDefinition<?>> bodyArguments, final BiConsumer<CommandEnvironment, CommandDefinition> consumer,
                      final Map<String, CommandDefinition> subCommands,
                      final Map<String, CommandDefinition> subCommandsAliases, final String description, final String name,
                      @NotNull final Permission permission,
                      final boolean playerOnly, final List<Predicate<CommandEnvironment>> predicates, final List<String> aliases,
                      final List<Pair<String, Function<CommandEnvironment, Object>>> envInserters)
    {
        this.arguments = arguments;
        this.optionalArguments = optionalArguments;
        this.bodyArguments = bodyArguments;
        this.consumer = consumer;
        this.subCommands = subCommands;
        this.subCommandsAliases = subCommandsAliases;
        this.description = description;
        this.name = name;
        this.permission = permission;
        this.playerOnly = playerOnly;
        this.predicates = predicates;
        this.aliases = aliases;
        this.envInserters = envInserters;

        if (!subCommands.containsKey("help") && !name.equals("help"))
        {
            subCommands.put("help", new CommandBuilder("help")
                    .description("Obtenir de l'aide sur une commande.")
                    .withOptionalArg(new IntegerArgument("page"), "1")
                    .command(env -> {
                        Integer page = env.get("page", Integer.class);
                        if (page == null)
                            page = 1;
                        try
                        {
                            env.getSender().sendMessage(helpGetPage(env.getSender(), page - 1));
                        }
                        catch (IllegalArgumentException e)
                        {
                            env.getSender().sendMessage(helpGetPage(env.getSender(), 0));
                        }
                    })
                    .build(this)
            );
        }
    }

    /**
     * Executes this command
     *
     * @param sender Command sender
     * @param args Sent command arguments
     */
    public boolean execute(CommandSender sender, String[] args)
    {
        try
        {
            return execute(sender, new CommandExecutionContext(new CommandEnvironment(sender), this, args));
        }
        catch (CommandExecutionException e)
        {
            logger.error("Unhandled exception during command execution", e);
            sender.sendMessage(ChatColor.RED + "Une erreur est survenue pendant l'exécution de la commande. Contactez un administrateur pour obtenir de l'aide.");
            return false;
        }
    }

    private boolean preExecutionChecks(CommandExecutionContext context)
    {
        CommandSender sender = context.getEnvironment().getSender();
        if (playerOnly && !(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.RED + "This command is player-only");
            return false;
        }
        for (var predicate : predicates)
        {
            if (!predicate.test(context.getEnvironment()))
                return false;
        }
        if (context.countRemainingArgs() < arguments.size())
        {
            help(sender);
            return false;
        }
        return true;
    }

    private boolean processArgs(CommandExecutionContext context, List<CommandArgumentDefinition<?>> argList,
                                boolean optionals) throws ArgumentParsingException
    {
        for (int i = 0; i < argList.size(); i++)
        {
            CommandArgumentDefinition<?> arg = argList.get(i);
            if (arg.getType().equals(TextCommandArgument.class))
            {
                String[] textParts = Arrays.copyOfRange(context.getArgs(), i, context.getArgs().length);
                StringJoiner text = new StringJoiner(" ");
                for (String part : textParts)
                    text.add(part);
                parseArgument(context.getEnvironment(), arg, text.toString());
                break;
            }
            if (!context.hasNextArg())
            {
                if (!optionals)
                {
                    // No value provided for mandatory arg => Error
                    help(context.getEnvironment().getSender());
                    return false;
                }
                else if (arg.hasDefault())
                    context.getEnvironment().setArgument(arg.getName(), arg.getDefault(context.getEnvironment()));
            }
            else if (!parseArgument(context.getEnvironment(), arg, context.nextArg()))
                return false;
        }
        return true;
    }

    private boolean execSubCommand(CommandExecutionContext context, String commandName) throws CommandExecutionException
    {
        CommandSender sender = context.getEnvironment().getSender();
        executeEnvInserters(context.getEnvironment());
        CommandDefinition subCommand = getSubCommandOrAlias(commandName);
        return subCommand.execute(sender, context);
    }

    public boolean execute(CommandSender sender, CommandExecutionContext context) throws CommandExecutionException
    {
        if (!preExecutionChecks(context))
            return false;

        CommandEnvironment env = context.getEnvironment();

        // Parse mandatory arguments
        if (!processArgs(context, arguments, false))
            return false;

        if (context.hasNextArg() && hasCommand(context.peekNextArg()))
            return execSubCommand(context, context.nextArg());

        // Parse optional arguments
        if (!optionalArguments.isEmpty())
        {
            if (!processArgs(context, optionalArguments, true))
                return false;
        }
        else if (context.hasNextArg() && (subCommands.containsKey(context.peekNextArg()) || subCommandsAliases.containsKey(context.peekNextArg())))
            return execSubCommand(context, context.nextArg());
        else if (context.hasNextArg() && !bodyArguments.isEmpty())
        {
            if (!processArgs(context, bodyArguments, false))
                return false;
        }
        else if (context.hasNextArg())
        {
            help(sender);
            return false;
        }

        if (!getPermission().hasPermission(sender))
        {
            sender.sendMessage(ChatColor.RED + "You don't have the permission to perform this command");
            return false;
        }
        executeEnvInserters(env);
        if (consumer != null)
            consumer.accept(env, this);
        else
            help(sender);
        return true;
    }

    private boolean hasCommand(String name)
    {
        return subCommands.containsKey(name) || subCommandsAliases.containsKey(name);
    }

    private void executeEnvInserters(CommandEnvironment env)
    {
        envInserters.forEach(pair -> env.set(pair.getLeft(), pair.getRight().apply(env)));
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
        List<CommandDefinition> validSubCommands = subCommands.values().stream()
                                                              .filter(cmd -> cmd.hasPermission(sender))
                                                              .collect(Collectors.toList());
        String[] msg = new String[validSubCommands.size() + (description == null || description.isEmpty() ? 0 : 1)];
        String argList = helpGetArgList();

        int i = 0;
        if (description != null && !description.isEmpty())
            msg[i++] = ChatColor.GREEN + name + " " + argList + ChatColor.DARK_GRAY + " : " + ChatColor.DARK_GREEN
                    + description;
        for (CommandDefinition subCommand : validSubCommands)
            msg[i++] = ChatColor.GREEN + name + " " + argList + " " + subCommand.getName() + " " + subCommand
                    .helpGetArgList()
                    + (subCommand.hasDescription() ? ChatColor.DARK_GRAY + " : " + ChatColor.DARK_GREEN + subCommand
                    .getDescription() : "");
        return msg;
    }

    /**
     * Send the help message to the given CommandSender
     */
    public void help(CommandSender sender)
    {
        CommandDefinition helpCommand = subCommands.get("help");
        if (helpCommand != null)
            helpCommand.execute(sender, new String[0]);
    }

    private String helpGetArgList()
    {
        StringJoiner argListJoiner = new StringJoiner(" ");
        for (CommandArgumentDefinition<?> arg : arguments)
            argListJoiner.add("{" + arg.getName() + "}");
        for (CommandArgumentDefinition<?> arg : optionalArguments)
            argListJoiner.add("[" + arg.getName() + "]");
        return argListJoiner.toString();
    }

    private CommandDefinition getSubCommandOrAlias(String name)
    {
        return subCommands.getOrDefault(name, subCommandsAliases.get(name));
    }

    private boolean parseArgument(CommandEnvironment env, CommandArgumentDefinition<?> argument, String input) throws ArgumentParsingException
    {
        CommandArgument<?> instance = argument.newInstance(input, env);
        if (instance == null)
            return false;
        env.setArgument(argument.getName(), instance);
        return true;
    }

    public boolean hasPermission(CommandSender sender)
    {
        return this.permission.hasPermission(sender);
    }

    public String getName()
    {
        return name;
    }

    public String getDescription()
    {
        return description;
    }

    public boolean hasDescription()
    {
        return description != null && !description.isEmpty();
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
        return tabComplete(sender, new CommandEnvironment(sender), args);
    }

    @Nullable
    public List<String> tabComplete(CommandSender sender, CommandEnvironment env, String[] args)
    {
        int i = 0;
        for (; i < args.length - 1; i++)
        {
            Optional<CommandArgumentDefinition<?>> arg = getAnyArgumentAt(i);

            if (arg.isPresent())
            {
                try
                {
                    env.setArgument(arg.get().getName(), arg.get().newInstance(args[i], env));
                }
                catch (Exception e)
                {
                    return null;
                }
            }
            else
            {
                CommandDefinition subCmd = getSubCommandOrAlias(args[i]);
                if (subCmd == null)
                    return null;
                return subCmd.tabComplete(sender, env, Arrays.copyOfRange(args, i + 1, args.length));
            }
        }
        // current arg being written
        final String finalArg = args[i];
        var arg = getAnyArgumentAt(i);

        if (arg.isPresent())
        {
            List<String> res = arg.get().tabComplete(finalArg, env);
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
                              .map(builder -> {
                                  ArrayList<String> strings = new ArrayList<>(builder.aliases);
                                  strings.add(builder.getName());
                                  return strings;
                              })
                              .flatMap(Collection::stream)
                              .filter(s -> s.startsWith(finalArg))
                              .collect(Collectors.toList());
        }
        return null;
    }

    private Optional<CommandArgumentDefinition<?>> getAnyArgumentAt(int index)
    {
        CommandArgumentDefinition<?> arg = null;
        if (index < arguments.size())
            arg = arguments.get(index);
        else if (index < optionalArguments.size())
            arg = optionalArguments.get(index - arguments.size());
        return Optional.ofNullable(arg);
    }
}
