package onl.tesseract.commandBuilder;

public class StringCommandArgument extends CommandArgument {
    public StringCommandArgument(final String name)
    {
        super(name, String.class);
        supplier((input, env) -> input);
    }
}
