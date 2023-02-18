package onl.tesseract.commandBuilder.sample;

import onl.tesseract.Guild;
import onl.tesseract.Parcel;
import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandArgumentBuilderSteps;
import org.jetbrains.annotations.NotNull;

public class ParcelArgument extends CommandArgument<Parcel> {
    public ParcelArgument(@NotNull final String name)
    {
        super(name);
    }

    @Override
    public void define(final CommandArgumentBuilderSteps.@NotNull Parser<Parcel> builder)
    {
        builder.parser((input, env) -> {
                   Parcel parcel = Parcel.forName(env.get("guild", Guild.class), input);
                   if (parcel == null)
                       throw new IllegalArgumentException();
                   return parcel;
               })
               .tabCompleter((inut, env) -> null)
               .errorHandler(IllegalArgumentException.class, "Invalid parcel");
    }
}
