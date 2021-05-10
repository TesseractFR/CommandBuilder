package onl.tesseract.commandBuilder;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class CommandBuilderTest {

    @Test
    public void SimpleMoney()
    {
        // Given
        Player test = mock(Player.class);
        CommandBuilder moneyCommand = new CommandBuilder();
        moneyCommand.withArg(new CommandArgument("player", Player.class)
                             .supplier(input -> test))
                    .command(env -> {
            env.get("player", Player.class).sendMessage("test");
        });

        moneyCommand.execute(List.of("GabRay"));
        verify(test, times(1)).sendMessage("test");
    }
}