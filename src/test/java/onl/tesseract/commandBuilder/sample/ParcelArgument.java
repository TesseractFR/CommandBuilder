package onl.tesseract.commandBuilder.sample;

import onl.tesseract.Guild;
import onl.tesseract.Parcel;
import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandEnvironment;
import onl.tesseract.commandBuilder.v2.ArgumentErrorHandlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ParcelArgument extends CommandArgument<Parcel> {
    public ParcelArgument(@NotNull final String name)
    {
        super(name);
    }

    @Override
    protected @NotNull Parcel parser(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        Parcel parcel = Parcel.forName(environment.get("guild", Guild.class), input);
        if (parcel == null)
            throw new IllegalArgumentException();
        return parcel;
    }

    @Override
    protected @Nullable List<String> tabCompletion(@NotNull final String input, @NotNull final CommandEnvironment environment)
    {
        return null;
    }

    @Override
    protected void errors(final ArgumentErrorHandlers handlers)
    {
        handlers.on(IllegalArgumentException.class, "invalid parcel");
    }
}
