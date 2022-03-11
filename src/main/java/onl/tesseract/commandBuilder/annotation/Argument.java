package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.*;

@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Argument {

    String label();

    Class<?> clazz() default void.class;

    boolean optional() default false;
}
