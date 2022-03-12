package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ErrorHandlers.class)
public @interface ErrorHandler {

    Class<? extends Exception> value();
}

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface ErrorHandlers {

    ErrorHandler[] value() default {};
}