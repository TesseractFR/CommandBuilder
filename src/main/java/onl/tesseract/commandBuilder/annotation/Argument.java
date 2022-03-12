package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Argument {

    String label();

    Class<?> clazz();

    boolean optional() default false;

    String def() default "";
}
