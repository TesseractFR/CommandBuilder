package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Command {

    String name() default "";

    String permission() default "";

    String description() default "";

    boolean playerOnly() default false;

    Argument[] args() default {};
}
