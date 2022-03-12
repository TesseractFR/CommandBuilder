package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.*;

/**
 * A method annotated with @CommandBody will be considered as the default command's behavior if no subcommands is called.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommandBody {
}
