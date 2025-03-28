package onl.tesseract.commandBuilder;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import onl.tesseract.commandBuilder.exception.ArgumentParsingException;
import onl.tesseract.commandBuilder.exception.CommandExecutionException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
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
    private CommandDefinition parent;
    @NotNull
    private final Permission permission;
    private final boolean playerOnly;
    private final List<PredicateDefinition> predicates;
    private final List<String> aliases;
    private final List<Pair<String, Function<CommandEnvironment, Object>>> envInserters;
    private final boolean isAsync;
    @Setter(AccessLevel.PACKAGE)
    private Plugin plugin;

    void setParent(CommandDefinition parent) {
        this.parent = parent;
    }

    CommandDefinition(final List<CommandArgumentDefinition<?>> arguments, final List<CommandArgumentDefinition<?>> optionalArguments,
                      final List<CommandArgumentDefinition<?>> bodyArguments, final BiConsumer<CommandEnvironment, CommandDefinition> consumer,
                      final Map<String, CommandDefinition> subCommands,
                      final Map<String, CommandDefinition> subCommandsAliases, final String description, final String name,
                      @NotNull final Permission permission,
                      final boolean playerOnly, final List<PredicateDefinition> predicates, final List<String> aliases,
                      final List<Pair<String, Function<CommandEnvironment, Object>>> envInserters, boolean isAsync)
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
        this.isAsync = isAsync;

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
                            for (Component line : helpGetPage(env.getSender(), page - 1)) {
                                env.getSender().sendMessage(line);
                            }
                        }
                        catch (IllegalArgumentException e)
                        {
                            for (Component line : helpGetPage(env.getSender(), 0)) {
                                env.getSender().sendMessage(line);
                            }
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
            sender.sendMessage(Component.text("Une erreur est survenue pendant l'exécution de la commande. Contactez un administrateur pour obtenir de l'aide.", NamedTextColor.RED));
            return false;
        }
    }

    private boolean preExecutionChecks(CommandExecutionContext context)
    {
        CommandSender sender = context.getEnvironment().getSender();
        if (playerOnly && !(sender instanceof Player))
        {
            sender.sendMessage(Component.text("This command is player-only", NamedTextColor.RED));
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
//        else if (context.hasNextArg())
//        {
//            help(sender);
//            return false;
//        }

        if (!getPermission().hasPermission(sender))
        {
            sender.sendMessage(Component.text("You don't have the permission to perform this command", NamedTextColor.RED));
            return false;
        }
        executeEnvInserters(env);
        if (consumer != null) {
            if (!isAsync)
                consumer.accept(env, this);
            else {
                Objects.requireNonNull(plugin, "Plugin is required for async commands")
                        .getServer().getScheduler().runTaskAsynchronously(plugin, () -> consumer.accept(env, this));
            }
        }
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

    public List<Component> helpGetPage(CommandSender sender, int page) {
        List<Component> lines = helpGetAllLines(sender);
        int totalPageCount = (lines.size() - 1) / 8 + 1;

        if (page < 0 || page >= totalPageCount) {
            throw new IllegalArgumentException("Page index must be between 0 and pageCount - 1");
        }

        int start = page * 8;
        int end = Math.min(lines.size(), (page + 1) * 8);

        List<Component> result = new ArrayList<>();

        String boxing = "============";
        result.add(Component.text(boxing + " " + name + " " + boxing, NamedTextColor.YELLOW, TextDecoration.BOLD));
        result.add(
                Component.text("⟹ ", NamedTextColor.GRAY)
                        .append(Component.text("Aide commandes page ", NamedTextColor.GRAY))
                        .append(Component.text((page + 1), NamedTextColor.GOLD))
                        .append(Component.text("/", NamedTextColor.GRAY))
                        .append(Component.text(totalPageCount, NamedTextColor.GOLD))
        );
        result.add(Component.empty());

        for (int i = start; i < end; i++) {
            result.add(lines.get(i));
        }
        result.add(Component.empty());

        int currentPage = page + 1;
        List<Component> navigation = new ArrayList<>();

        if (currentPage > 1) {
            Component previousPage = Component.text("[Page Précédente]", NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/" + name + " help " + (currentPage - 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour accéder à la page précédente !", NamedTextColor.YELLOW)));
            navigation.add(previousPage);
        }

        if (currentPage < totalPageCount) {
            Component nextPage = Component.text("[Page Suivante]", NamedTextColor.GOLD)
                    .clickEvent(ClickEvent.runCommand("/" + name + " help " + (currentPage + 1)))
                    .hoverEvent(HoverEvent.showText(Component.text("Cliquez pour accéder à la page suivante !", NamedTextColor.YELLOW)));
            navigation.add(nextPage);
        }

        if (!navigation.isEmpty()) {
            Component navLine = Component.empty();
            for (int i = 0; i < navigation.size(); i++) {
                navLine = navLine.append(navigation.get(i));
                if (i < navigation.size() - 1)
                    navLine = navLine.append(Component.text("  "));
            }
            result.add(navLine);
        }

        result.add(Component.text("\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\", NamedTextColor.YELLOW)
                .append(Component.text(" • ", NamedTextColor.GOLD))
                .append(Component.text("//////////////////", NamedTextColor.YELLOW)));

        return result;
    }

    public List<Component> helpGetAllLines(CommandSender sender) {
        List<CommandDefinition> validSubCommands = subCommands.values().stream()
                .filter(cmd -> cmd.hasPermission(sender))
                .toList();

        List<Component> lines = new ArrayList<>();

        boolean showRootCommand = parent != null || validSubCommands.isEmpty();

        // Affiche "/{commandePrincipale}"
        if (showRootCommand) {
            lines.add(buildHelpLine(this, parent == null));
        }

        // Affiche "... {sousCommandes}"
        for (CommandDefinition sub : validSubCommands) {
            if (sub.getName().equals("help") && parent != null) continue;
            lines.add(buildHelpLine(sub, parent == null));
        }

        return lines;
    }

    private Component buildHelpLine(CommandDefinition cmd, boolean isRoot) {
        StringBuilder builder = new StringBuilder();

        if (cmd.parent == null) {
            builder.append("/").append(cmd.name);
        } else if (isRoot) {
            builder.append("/").append(name).append(" ").append(cmd.name);
        } else {
            builder.append("... ").append(cmd.name);
        }

        String args = cmd.helpGetArgList();
        if (!args.isBlank()) {
            builder.append(" ").append(args.trim());
        }

        TextComponent line = Component.text(builder.toString().replaceAll(" +", " "), NamedTextColor.GREEN);
        if (cmd.hasDescription()) {
            line = line.append(Component.text(" : ", NamedTextColor.DARK_GRAY))
                    .append(Component.text(cmd.description, NamedTextColor.DARK_GREEN));
        }
        return line;
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

    public String getFullPath() {
        if (parent == null) return name;
        return parent.getFullPath() + " " + name;
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
                    .filter(cmd ->
                            cmd.getPredicates().stream()
                                    .filter(PredicateDefinition::isStrict)
                                    .allMatch(predicate -> predicate.test(env))
                    )
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
        else if (index - arguments.size() < optionalArguments.size())
            arg = optionalArguments.get(index - arguments.size());
        return Optional.ofNullable(arg);
    }
}
