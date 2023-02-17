package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.definition.CommandArgumentDefinition;
import onl.tesseract.commandBuilder.exception.ArgumentParsingException;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import onl.tesseract.commandBuilder.exception.CommandExecutionException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
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
    private static final Logger logger = LoggerFactory.getLogger(CommandBuilder.class);

    List<CommandArgumentDefinition<?>> arguments = new ArrayList<>();
    List<CommandArgumentDefinition<?>> optionalArguments = new ArrayList<>();
    List<CommandArgumentDefinition<?>> bodyArguments = new ArrayList<>();
    Consumer<CommandEnvironment> consumer;
    // Use linked hashmap to keep insertion order
    // Useful to display help messages with subcommands in a pertinent order
    private final HashMap<String, CommandBuilder> subCommands = new LinkedHashMap<>();
    private final HashMap<String, CommandBuilder> subCommandsAliases = new HashMap<>();
    private String description;
    private final String name;
    @NotNull
    private Permission permission = Permission.NONE;
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
                }));
    }

    public String getName()
    {
        return name;
    }

    @NotNull
    public Permission getPermission()
    {
        return permission;
    }

    public boolean hasPermission(CommandSender sender)
    {
        return this.permission.hasPermission(sender);
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
        this.permission = Permission.get(permission);
        return this;
    }

    /**
     * Add an argument to this command.
     *
     * @param argument Argument
     *
     * @return this
     */
    public <T> CommandBuilder withArg(CommandArgumentDefinition<T> argument)
    {
        arguments.add(argument);
        return this;
    }

    public <T> CommandBuilder withArg(CommandArgument<T> arg)
    {
        try
        {
            return withArg(CommandArgumentBuilder.getBuilder((Class<? extends CommandArgument<T>>) arg.getClass(), arg.getName()).build());
        }
        catch (ReflectiveOperationException e)
        {
            throw new CommandBuildException(e);
        }
    }

    public CommandBuilder withBodyArg(CommandArgumentDefinition<?> arg)
    {
        bodyArguments.add(arg);
        return this;
    }

    /**
     * Add an optional argument to this command. This is incompatible with the use of subcommands
     *
     * @param optArg Optional arg
     *
     * @return this
     */
    public CommandBuilder withOptionalArg(CommandArgumentDefinition<?> optArg)
    {
        if (!subCommands.isEmpty() && (subCommands.size() > 1 || !subCommands.containsKey("help")))
            throw new IllegalStateException("Optional arguments cannot be used in commands containing subcommands.");
        optionalArguments.add(optArg);
        return this;
    }

    public <T> CommandBuilder withOptionalArg(CommandArgument<T> optArg, @Nullable String def)
    {
        try
        {
            return withOptionalArg(CommandArgumentBuilder.getBuilder((Class<? extends CommandArgument<T>>) optArg.getClass(), optArg.getName()).setDefaultInput(def).build());
        }
        catch (ReflectiveOperationException e)
        {
            throw new CommandBuildException(e);
        }
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
        for (CommandArgumentDefinition<?> arg : arguments)
            argListJoiner.add("{" + arg.getName() + "}");
        for (CommandArgumentDefinition<?> arg : optionalArguments)
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
        CommandBuilder subCommand = getSubCommandOrAlias(commandName);
        if (subCommand.hasPermission(sender))
            return subCommand.execute(sender, context);
        else
        {
            sender.sendMessage(ChatColor.RED + "You don't have the permission to perform this command");
            return false;
        }
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

        executeEnvInserters(env);
        if (consumer != null)
            consumer.accept(env);
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
        envInserters.forEach(pair -> {
            env.set(pair.getLeft(), pair.getRight().apply(env));
        });
    }

    private CommandBuilder getSubCommandOrAlias(String name)
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
        if (this.permission != Permission.NONE && subCommand.permission == Permission.NONE)
            subCommand.permission = this.permission.getChild(subCommand.name);
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
