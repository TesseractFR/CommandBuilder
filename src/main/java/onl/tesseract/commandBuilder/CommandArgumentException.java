package onl.tesseract.commandBuilder;

public class CommandArgumentException extends RuntimeException {
    public CommandArgumentException()
    {
    }

    public CommandArgumentException(final String message)
    {
        super(message);
    }
}
