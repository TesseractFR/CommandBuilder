package onl.tesseract.commandBuilder.v2.argument;

import onl.tesseract.commandBuilder.CommandEnvironment;
import onl.tesseract.commandBuilder.definition.CommandArgumentDefinition;
import onl.tesseract.commandBuilder.exception.ArgumentParsingException;
import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandArgumentBuilder;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

class ArgumentTest {

    private CommandSender sender;

    @BeforeEach
    void setup()
    {
        sender = Mockito.mock(CommandSender.class);
    }

    @Test
    void parseInteger_isNumber_ok() throws ArgumentParsingException, ReflectiveOperationException
    {
        CommandArgumentDefinition<Integer> definition = CommandArgumentBuilder.getBuilder(IntegerArgument.class, "number").build();

        CommandArgument<Integer> arg = definition.newInstance("42", new CommandEnvironment(sender));

        assertNotNull(arg);
        assertEquals(42, arg.get());
    }

    @Test
    void parseInteger_invalidNumber_NotHandled() throws ReflectiveOperationException
    {
        CommandArgumentDefinition<Integer> definition = CommandArgumentBuilder.getBuilder(IntegerArgument.class, "number").build();

        assertThrows(ArgumentParsingException.class, () -> definition.newInstance("-1", new CommandEnvironment(sender)));
    }

    @Test
    void parseInteger_invalidNumber_handled() throws ArgumentParsingException, ReflectiveOperationException
    {
        CommandArgumentDefinition<Integer> definition = CommandArgumentBuilder.getBuilder(IntegerArgument.class, "number").build();

        CommandArgument<Integer> arg = definition.newInstance("foo", new CommandEnvironment(sender));

        assertNull(arg);
    }
}
