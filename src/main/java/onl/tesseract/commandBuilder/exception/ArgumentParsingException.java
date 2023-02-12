package onl.tesseract.commandBuilder.exception;

public class ArgumentParsingException extends CommandExecutionException {

    public ArgumentParsingException()
    {
    }

    public ArgumentParsingException(final String message)
    {
        super(message);
    }

    public ArgumentParsingException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public ArgumentParsingException(final Throwable cause)
    {
        super(cause);
    }

    public ArgumentParsingException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
