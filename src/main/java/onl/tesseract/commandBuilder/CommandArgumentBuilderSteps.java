package onl.tesseract.commandBuilder;

import lombok.Getter;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

public interface CommandArgumentBuilderSteps {

    interface Parser<T> {
        Completer<T> parser(BiFunction<String, CommandEnvironment, T> parser);
    }

    interface Completer<T> {
        Builder<T> tabCompleter(BiFunction<String, CommandEnvironment, List<String>> tabCompleter);
    }

    interface Builder<T> {
        Builder<T> errorHandler(Class<? extends Throwable> type, UnaryOperator<String> messageSupplier);

        Builder<T> errorHandler(Class<? extends Throwable> type, String message);
    }
}

final class ArgumentBuilderStepsImpl implements CommandArgumentBuilderSteps {
    private ArgumentBuilderStepsImpl()
    {
    }

    static class Parser<T> implements CommandArgumentBuilderSteps.Parser<T> {
        private final CommandArgumentBuilder<T> builder;

        public Parser(final CommandArgumentBuilder<T> builder)
        {
            this.builder = builder;
        }

        @Override
        public CommandArgumentBuilderSteps.Completer<T> parser(final BiFunction<String, CommandEnvironment, T> parser)
        {
            this.builder.setParser(parser);
            return new Completer<>(builder);
        }
    }

    static class Completer<T> implements CommandArgumentBuilderSteps.Completer<T> {

        private final CommandArgumentBuilder<T> builder;

        Completer(final CommandArgumentBuilder<T> builder)
        {
            this.builder = builder;
        }

        @Override
        public CommandArgumentBuilderSteps.Builder<T> tabCompleter(final BiFunction<String, CommandEnvironment, List<String>> tabCompleter)
        {
            builder.setTabCompleter(tabCompleter);
            return new Builder<>(builder);
        }
    }

    static class Builder<T> implements CommandArgumentBuilderSteps.Builder<T> {
        @Getter
        private final CommandArgumentBuilder<T> builder;

        Builder(final CommandArgumentBuilder<T> builder)
        {
            this.builder = builder;
        }

        @Override
        public CommandArgumentBuilderSteps.Builder<T> errorHandler(final Class<? extends Throwable> type, final UnaryOperator<String> messageSupplier)
        {
            builder.getErrorHandlers().on(type, messageSupplier);
            return this;
        }

        @Override
        public CommandArgumentBuilderSteps.Builder<T> errorHandler(final Class<? extends Throwable> type, final String message)
        {
            builder.getErrorHandlers().on(type, message);
            return this;
        }
    }
}
