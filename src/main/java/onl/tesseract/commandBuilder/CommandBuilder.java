package onl.tesseract.commandBuilder;

import lombok.AccessLevel;
import lombok.Getter;
import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Used to build complex feature-rich commands. Can handle sub commands, arguments, optional
 * arguments, permissions, help messages, player-ony commands.
 *
 * <p>
 * To disable default help messages, use {@link CommandBuilder#CommandBuilder(String, boolean)} with false.
 * </p>
 *
 * @see CommandContext Annotation-based command builder
 */
final class CommandBuilder {

    final List<CommandArgumentDefinition<?>> arguments = new ArrayList<>();
    final List<CommandArgumentDefinition<?>> optionalArguments = new ArrayList<>();
    final List<CommandArgumentDefinition<?>> bodyArguments = new ArrayList<>();
    BiConsumer<CommandEnvironment, CommandDefinition> consumer;
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
    private final List<String> aliases = new ArrayList<>();
    private final List<Pair<String, Function<CommandEnvironment, Object>>> envInserters = new ArrayList<>();

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
                .command((env, self) -> {
                    Integer page = env.get("page", Integer.class);
                    if (page == null)
                        page = 1;
                    try
                    {
                        env.getSender().sendMessage(self.helpGetPage(env.getSender(), page - 1));
                    }
                    catch (IllegalArgumentException e)
                    {
                        env.getSender().sendMessage(self.helpGetPage(env.getSender(), 0));
                    }
                }));
    }

    @NotNull
    public Permission getPermission()
    {
        return permission;
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
        this.consumer = (env, self) -> consumer.accept(env);
        return this;
    }

    private CommandBuilder command(BiConsumer<CommandEnvironment, CommandDefinition> consumer)
    {
        this.consumer = consumer;
        return this;
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

    public String getName()
    {
        return name;
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

    public CommandDefinition build()
    {
        Map<String, CommandDefinition> commands = new HashMap<>();
        Map<String, CommandDefinition> subAliases = new HashMap<>();
        subCommands.forEach((subCommandName, builder) -> {
            CommandDefinition def = builder.build();
            commands.put(subCommandName, def);
            builder.aliases.forEach(alias -> subAliases.put(alias, def));
        });
        return new CommandDefinition(
                arguments,
                optionalArguments,
                bodyArguments,
                consumer,
                commands,
                subAliases,
                description,
                name,
                permission,
                playerOnly,
                predicates,
                this.aliases,
                envInserters
        );
    }
}
