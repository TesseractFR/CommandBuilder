package onl.tesseract.commandBuilder;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Used to build complex feature-rich commands. Can handle sub commands, arguments, optional
 * arguments, permissions, help messages, player-ony commands.
 *
 * To disable default help messages, use {@link CommandBuilder#CommandBuilder(String, boolean)} with false.
 *
 * @see CommandContext Annotation-based command builder
 */
public class CommandBuilder {
    ArrayList<CommandArgument> arguments = new ArrayList<>();
    ArrayList<CommandArgument> optionalArguments = new ArrayList<>();
    Consumer<CommandEnvironment> consumer;
    // Use linked hashmap to keep insertion order
    // Useful to display help messages with subcommands in a pertinent order
    private final HashMap<String, CommandBuilder> subCommands = new LinkedHashMap<>();
    private final HashMap<String, CommandBuilder> subCommandsAliases = new HashMap<>();
    private String description;
    private final String name;
    private String permission;
    private boolean playerOnly;
    private final List<Predicate<CommandEnvironment>> predicates = new ArrayList<>();
    private List<String> aliases = new ArrayList<>();
    private List<Pair<String, Function<CommandEnvironment, Object>>> envInserters = new ArrayList<>();

    /**
     * Start building a new command with auto generated help message
     *
     * @param name Command's name
     *
     * @see CommandBuilder#CommandBuilder(String, boolean)
     */
    public CommandBuilder(String name)
    {
        this(name, true);
    }

    /**
     * Start building a new command
     *
     * @param name Name of the command
     * @param generateDefaultHelp If true, will generate a default help message showing all arguments and subcommands. The help message will be
     * printed to the player on 3 scenarios :
     * <ul>
     *     <li>The player performed the command /command help</li>
     *     <li>The player performed the command /command, and no default behavior is specified</li>
     *     <li>The player performed the command with a syntax error (ex: missing required argument)</li>
     * </ul>
     * You can register you own help message by override the commande 'help'
     */
    public CommandBuilder(String name, boolean generateDefaultHelp)
    {
        this.name = name;
        if (!generateDefaultHelp)
            return;
        subCommand(new CommandBuilder("help", false)
                .description("Obtenir de l'aide sur une commande.")
                .withOptionalArg(new CommandArgument("page", Integer.class)
                        .supplier((input, env) -> Integer.parseInt(input))
                        .defaultValue(env -> 1)
                        .error(NumberFormatException.class, "Nombre invalide"))
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
        return permission == null || permission.isEmpty() || sender.hasPermission(permission);
    }

    /**
     * Set a permission to this command. CommandSender who don't have this permission will not be
     * able to perform this command, and won't see it in help messages and tab completions
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
    public CommandBuilder withOptionalArg(CommandArgument optArg)
    {
        if (!subCommands.isEmpty() && (subCommands.size() > 1 || !subCommands.containsKey("help")))
            throw new IllegalStateException("Optional arguments cannot be used in commands containing subcommands.");
        optionalArguments.add(optArg);
        return this;
    }

    /**
     * Set the function to be called when the command is performed by a CommandSender. If your command only has subcommands, and no direct behavior,
     * you can set this to null.
     * If no command is specified, the help message will be printed.
     *
     * @param consumer Function
     *
     * @return this
     */
    public CommandBuilder command(Consumer<CommandEnvironment> consumer)
    {
        this.consumer = consumer;
        return this;
    }

    /**
     * @deprecated In favor of {@link CommandBuilder#command(Consumer)} since the sender is included in the environment
     */
    @Deprecated
    public CommandBuilder command(BiConsumer<CommandSender, CommandEnvironment> consumer)
    {
        this.consumer = env -> consumer.accept(env.getSender(), env);
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
        res[res.length - 1] = ChatColor.YELLOW + "\\".repeat(16) + ChatColor.GOLD + " ??? "
                + ChatColor.YELLOW + "/".repeat(16);

        return res;
    }

    private String[] helpGetAllLines(CommandSender sender)
    {
        List<CommandBuilder> validSubCommands = subCommands.values().stream()
                                                           .filter(cmd -> cmd.hasPermission(sender))
                                                           .collect(Collectors.toList());
        String[] msg = new String[validSubCommands.size() + (description == null || description.isEmpty() ? 0 : 1)];
        String argList = helpGetArgList();

        int i = 0;
        if (description != null && !description.isEmpty())
            msg[i++] = ChatColor.GREEN + name + " " + argList + ChatColor.DARK_GRAY + " : " + ChatColor.DARK_GREEN
                    + description;
        for (CommandBuilder subCommand : validSubCommands)
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
        return description != null && !description.isEmpty();
    }

    /**
     * Executes this command
     *
     * @param sender Command sender
     * @param args Sent command arguments
     */
    public void execute(CommandSender sender, String[] args)
    {
        execute(sender, new CommandEnvironment(sender), args);
    }

    public void execute(CommandSender sender, CommandEnvironment env, String[] args)
    {
        if (playerOnly && !(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.RED + "This command is player-only");
            return;
        }
        for (var predicate : predicates)
        {
            if (!predicate.test(env))
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
        else if (i < args.length && (subCommands.containsKey(args[i]) || subCommandsAliases.containsKey(args[i])))
        {
            executeEnvInserters(env);
            CommandBuilder subCommand = getSubCommandOrAlias(args[i]);
            if (subCommand.hasPermission(sender))
                subCommand.execute(sender, env, Arrays.copyOfRange(args, i + 1, args.length));
            else
                sender.sendMessage(ChatColor.RED + "You don't have the permission to perform this command");
            return;
        }

        executeEnvInserters(env);
        if (consumer != null)
            consumer.accept(env);
        else
            help(sender);
    }

    private void executeEnvInserters(CommandEnvironment env) {
        envInserters.forEach(pair -> {
            env.set(pair.getLeft(), pair.getRight().apply(env));
        });
    }

    private CommandBuilder getSubCommandOrAlias(String name)
    {
        return subCommands.getOrDefault(name, subCommandsAliases.get(name));
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
                sender.sendMessage(ChatColor.RED + argument.onError(e));
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
        subCommand.aliases.forEach(alias -> subCommandsAliases.put(alias, subCommand));
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
        return tabComplete(sender, new CommandEnvironment(sender), args);
    }

    @Nullable
    public List<String> tabComplete(CommandSender sender, CommandEnvironment env, String[] args)
    {
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
                CommandBuilder subCmd = getSubCommandOrAlias(args[i]);
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

    public boolean isPlayerOnly()
    {
        return playerOnly;
    }

    /**
     * Add a predicate deciding if a player should be able to perform the command. If not, the error message will be printed, and the command will not
     * show up in tab completion.
     *
     * @return this
     */
    public CommandBuilder predicate(Predicate<CommandEnvironment> predicate, String errorMessage)
    {
        predicates.add(env -> {
            if (!predicate.test(env))
            {
                env.getSender().sendMessage(ChatColor.RED + errorMessage);
                return false;
            }
            return true;
        });
        return this;
    }

    public CommandBuilder predicate(Predicate<CommandEnvironment> predicate)
    {
        predicates.add(predicate);
        return this;
    }

    public CommandBuilder alias(String alias)
    {
        aliases.add(alias);
        return this;
    }

    public CommandBuilder envInserter(String key, Function<CommandEnvironment, Object> function)
    {
        envInserters.add(new Pair<>(key, function));
        return this;
    }

    Map<String, CommandBuilder> getSubCommands()
    {
        return Collections.unmodifiableMap(subCommands);
    }
}
