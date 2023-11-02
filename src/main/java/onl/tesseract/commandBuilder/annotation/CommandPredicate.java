package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Predicates.class)
public @interface CommandPredicate {

    String value();

    /**
     * Strict predicates are also executed during tab completion, which has the effect of hiding the subcommand from
     * tab completion if the predicate returns false
     */
    boolean strict() default false;
}

