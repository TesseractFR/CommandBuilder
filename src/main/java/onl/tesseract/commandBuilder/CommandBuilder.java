package onl.tesseract.commandBuilder;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.util.Consumer;

public class CommandBuilder {
    ArrayList<CommandArgument> arguments = new ArrayList<>();
    Consumer<CommandEnvironment> consumer;

    public CommandBuilder withArg(CommandArgument argument)
    {
        arguments.add(argument);
        return this;
    }

    public CommandBuilder command(Consumer<CommandEnvironment> consumer)
    {
        this.consumer = consumer;
        return this;
    }

    public void execute(CommandSender sender, List<String> args)
    {
        CommandEnvironment env = new CommandEnvironment();
        for (int i = 0; i < args.size(); i++)
        {
            Object parsed;
            try
            {
                parsed = arguments.get(i).supplier.apply(args.get(i), env);
            }
            catch (Exception e)
            {
                if (arguments.get(i).hasError(e.getClass()))
                    sender.sendMessage(ChatColor.RED + arguments.get(i).onError(e.getClass()));
                else
                {
                    sender.sendMessage(ChatColor.RED + "Une erreur inattendue est survenue pendant l'execution de cette commande. Contactez un administrateur pour obtenir de l'aide.");
                    System.err.println("Error while executing command");
                    e.printStackTrace();
                }
                return;
            }
            env.set(arguments.get(i).name, parsed);
        }

        consumer.accept(env);
    }
}
