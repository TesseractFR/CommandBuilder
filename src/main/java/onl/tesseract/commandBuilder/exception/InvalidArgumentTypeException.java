package onl.tesseract.commandBuilder.exception;

public class InvalidArgumentTypeException extends CommandBuildException {

    public InvalidArgumentTypeException()
    {
    }

    public InvalidArgumentTypeException(final String message)
    {
        super(message);
    }

    public InvalidArgumentTypeException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public InvalidArgumentTypeException(final Throwable cause)
    {
        super(cause);
    }

    public InvalidArgumentTypeException(final String message, final Throwable cause, final boolean enableSuppression,
                                        final boolean writableStackTrace)
    {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
