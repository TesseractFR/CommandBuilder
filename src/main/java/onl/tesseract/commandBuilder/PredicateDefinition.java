package onl.tesseract.commandBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class PredicateDefinition {
    private final Predicate<CommandEnvironment> predicate;
    private final boolean strict;

    public PredicateDefinition(Predicate<CommandEnvironment> predicate, boolean strict) {
        this.predicate = predicate;
        this.strict = strict;
    }

    public Predicate<CommandEnvironment> getPredicate() {
        return predicate;
    }

    public boolean isStrict() {
        return strict;
    }

    public boolean test(@NotNull final CommandEnvironment env) {
        return predicate.test(env);
    }
}
