package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommandBuilderV2Test {

    @Test
    public void CommandOnCommandBuilderV2ClassTest()
    {
        NoContentCommand command = new NoContentCommand();

        assertEquals("test", command.builder.getName());
        assertEquals("test", command.builder.getPermission());
        assertEquals("A test command", command.builder.getDescription());
        assertTrue(command.builder.isPlayerOnly());
    }
}

@Command(name = "test", permission = "test", playerOnly = true, description = "A test command")
class NoContentCommand extends CommandBuilderV2 {

}

@Command(name = "test", permission = "test", playerOnly = true, description = "A test command")
class TestCommand extends CommandBuilderV2 {

    @Command(permission = "test.foo", description = "A sub command")
    public void foo(@Argument(label = "arg", clazz = CommandArgument.class) CommandArgument argument)
    {

    }

    @Command(permission = "bar", description = "A sub command handled by a class",
            args = {
                    @Argument(label = "arg", clazz = CommandArgument.class)
            })
    class Bar {

        @CommandBody
        public void command(CommandEnvironment environment)
        {

        }
    }
}
