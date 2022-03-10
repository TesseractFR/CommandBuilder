package onl.tesseract.commandBuilder.exception;

public class CommandBuildException extends RuntimeException {
    public CommandBuildException()
    {
    }

    public CommandBuildException(final String message)
    {
        super(message);
    }

    public CommandBuildException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public CommandBuildException(final Throwable cause)
    {
        super(cause);
    }

    public CommandBuildException(final String message, final Throwable cause, final boolean enableSuppression, final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
