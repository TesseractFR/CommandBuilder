package onl.tesseract.commandBuilder.annotation;

import onl.tesseract.commandBuilder.CommandArgument;
import onl.tesseract.commandBuilder.CommandArgumentBuilderSteps;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.*;

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
    String value();

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
        public void define(final CommandArgumentBuilderSteps.@NotNull Parser<Void> builder)
        {
            throw new UnsupportedOperationException();
        }
    }
}
