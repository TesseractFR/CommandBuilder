package onl.tesseract.commandBuilder.annotation;

import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandEnvironment;
import onl.tesseract.commandBuilder.v2.ArgumentErrorHandlers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.*;
import java.util.List;

/**
 * Specifies that the annotated parameter should be automatically injected with a command argument
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Argument {

    /**
     * Name of the argument
     */
    String label();

    /**
     * Class used to parse the argument
     */
    Class<? extends CommandArgument<?>> clazz() default None.class;

    boolean optional() default false;

    /**
     * Default value for optional arguments
     */
    String def() default "";

    final class None extends CommandArgument<Void> {
        private None(@NotNull final String name)
        {
            super(name);
        }

        @Override
        protected @NotNull Void parser(@NotNull final String input, @NotNull final CommandEnvironment environment)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected @Nullable List<String> tabCompletion(@NotNull final String input, @NotNull final CommandEnvironment environment)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void errors(final ArgumentErrorHandlers handlers)
        {
            throw new UnsupportedOperationException();
        }
    }
}
