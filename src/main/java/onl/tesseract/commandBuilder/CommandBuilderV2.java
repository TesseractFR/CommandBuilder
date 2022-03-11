package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.exception.CommandBuildException;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class CommandBuilderV2 implements CommandExecutor, TabCompleter {

    final CommandBuilder builder;

    public CommandBuilderV2()
    {
        builder = new CommandBuilderProvider().provideFor(this.getClass());
    }

    @Override
    public final boolean onCommand(@NotNull final CommandSender sender, @NotNull final org.bukkit.command.Command command,
                                   @NotNull final String label,
                                   @NotNull final String[] args)
    {
        return false;
    }

    @Nullable
    @Override
    public final List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final org.bukkit.command.Command command,
                                            @NotNull final String alias,
                                            @NotNull final String[] args)
    {
        return null;
    }
}

final class CommandBuilderProvider {

    CommandBuilder provideFor(final Class<?> commandBuilder)
    {
        ClassAnnotationReader reader = new ClassAnnotationReader(commandBuilder);
        CommandBuilder command = provide(reader)
                .command(reader.readCommandBody());
        for (final CommandBuilder subCommand : reader.readSubCommands())
        {
            command.subCommand(subCommand);
        }
        return command;
    }

    CommandBuilder provideFor(final Method method)
    {
        return provide(new MethodAnnotationReader(method));
    }

    CommandBuilder provide(AnnotationReader reader)
    {
        CommandBuilder res = new CommandBuilder(reader.readName())
                .permission(reader.readPermission())
                .description(reader.readDescription())
                .playerOnly(reader.readPlayerOnly());

        Map<CommandArgument, Boolean> arguments = reader.readArguments();
        arguments.forEach((arg, optional) -> {
            if (optional)
            {
                try
                {
                    res.withOptionalArg((OptionalCommandArgument) arg);
                }
                catch (ClassCastException e)
                {
                    throw new CommandBuildException("Argument set as optional is not an instance of OptionalCommandArgument (" + arg.getName() + ")", e);
                }
            }
            else
                res.withArg(arg);
        });
        return res;
    }
}
