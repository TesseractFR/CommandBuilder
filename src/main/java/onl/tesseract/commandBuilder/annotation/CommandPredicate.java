package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Predicates.class)
public @interface CommandPredicate {

    String value();
}

