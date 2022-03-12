package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.*;

/**
 * A method parameter annotated with Env will be injected with the associated value in the command environment
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Env {

    /**
     * env key
     */
    String key();
}
