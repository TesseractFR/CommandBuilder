package onl.tesseract.commandBuilder.annotation;

import onl.tesseract.commandBuilder.CommandArgument;

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
    String label();

    /**
     * Class used to parse the argument
     */
    Class<? extends CommandArgument> clazz();

    boolean optional() default false;

    /**
     * Default value for optional arguments
     */
    String def() default "";
}
