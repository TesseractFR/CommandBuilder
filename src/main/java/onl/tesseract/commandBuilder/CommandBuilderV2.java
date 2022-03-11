package onl.tesseract.commandBuilder;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

public class CommandBuilderV2 implements CommandExecutor, TabCompleter {

    final CommandBuilder builder;

    public CommandBuilderV2()
    {
        builder = new CommandBuilderProvider().provideFor(this);
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

    CommandBuilder provideFor(final CommandBuilderV2 commandBuilder)
    {
        ClassAnnotationReader reader = new ClassAnnotationReader(commandBuilder.getClass());
        return provide(reader)
                .command(reader.readCommandBody());
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

        List<CommandArgument> arguments = reader.readArguments();
        for (final CommandArgument argument : arguments)
        {
            res.withArg(argument);
        }
        return res;
    }
}
