package onl.tesseract.commandBuilder.v2.argument;

import onl.tesseract.commandBuilder.CommandEnvironment;
import onl.tesseract.commandBuilder.exception.ArgumentParsingException;
import onl.tesseract.commandBuilder.v2.CommandArgument;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentTest {

    private CommandSender sender;

    @BeforeEach
    void setup()
    {
        sender = Mockito.mock(CommandSender.class);
    }

    @Test
    void parseInteger_isNumber_ok() throws ArgumentParsingException
    {
        IntegerArgument argument = new IntegerArgument("number");

        argument.parseInput("42", new CommandEnvironment(sender));

        assertEquals(42, argument.get());
    }

    @Test
    void parseInteger_invalidArgNotHandled()
    {
        IntegerArgument argument = new IntegerArgument("number");

        assertThrows(ArgumentParsingException.class, () -> argument.parseInput("foo", new CommandEnvironment(sender)));
    }

    @Test
    void parseInteger_invalidArgHandled() throws ArgumentParsingException
    {
        IntegerArgument argument = new IntegerArgument("number");
        // TODO: handle errors

        boolean res = argument.parseInput("foo", new CommandEnvironment(sender));

        assertFalse(res);
    }
}

class IntegerArgument extends CommandArgument<Integer> {

    protected IntegerArgument(@NotNull final String name)
    {
        super(name);
    }

    @Override
    protected @NotNull Integer parser(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return Integer.parseInt(input);
    }

    @Override
    protected @NotNull List<String> tabCompletion(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return List.of("1", "2", "3");
    }
}
