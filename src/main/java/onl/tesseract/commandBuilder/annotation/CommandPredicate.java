package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Predicates.class)
public @interface CommandPredicate {

    String value();
}

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface Predicates {

    CommandPredicate[] value();
}