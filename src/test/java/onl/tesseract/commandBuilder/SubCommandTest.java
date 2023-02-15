package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Command;
import onl.tesseract.commandBuilder.annotation.CommandBody;
import org.junit.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SubCommandTest {

    @Test
    public void defaultHelpCommandTest()
    {
        SimpleCommand simpleCommand = new SimpleCommand();

        Map<String, CommandBuilder> subCommands = simpleCommand.builder.getSubCommands();
        assertEquals(1, subCommands.size());
        assertTrue(subCommands.containsKey("help"));
    }
}

@Command
class SimpleCommand extends CommandContext {
    @CommandBody
    public void body()
    {

    }
}
