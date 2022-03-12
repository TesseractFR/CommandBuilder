package onl.tesseract.commandBuilder;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class Command implements CommandExecutor, TabCompleter {

    final CommandBuilder builder;

    public Command()
    {
        builder = new CommandBuilderProvider().provideForClass(this);
    }

    @Override
    public final boolean onCommand(@NotNull final CommandSender sender, @NotNull final org.bukkit.command.Command command,
                                   @NotNull final String label,
                                   @NotNull final String[] args)
    {
        builder.execute(sender, args);
        return true;
    }

    @Nullable
    @Override
    public final List<String> onTabComplete(@NotNull final CommandSender sender, @NotNull final org.bukkit.command.Command command,
                                            @NotNull final String alias,
                                            @NotNull final String[] args)
    {
        return builder.tabComplete(sender, args);
    }
}

final class CommandBuilderProvider {

    CommandBuilder provideForClass(final Object commandBuilder)
    {
        ClassAnnotationReader reader = new ClassAnnotationReader(commandBuilder);
        CommandBuilder command = provide(reader);
        for (final CommandBuilder subCommand : reader.readSubCommands())
        {
            command.subCommand(subCommand);
        }
        return command;
    }

    CommandBuilder provideFor(final Object instance, final Method method)
    {
        return provide(new MethodAnnotationReader(instance, method));
    }

    CommandBuilder provide(AnnotationReader reader)
    {
        CommandBuilder res = new CommandBuilder(reader.readName())
                .permission(reader.readPermission())
                .description(reader.readDescription())
                .playerOnly(reader.readPlayerOnly())
                .command(reader.readCommandBody(reader.getInstance()));

        Map<CommandArgument, Boolean> arguments = reader.readArguments();
        arguments.forEach((arg, optional) -> {
            if (optional)
                res.withOptionalArg(arg);
            else
                res.withArg(arg);
        });
        return res;
    }
}
