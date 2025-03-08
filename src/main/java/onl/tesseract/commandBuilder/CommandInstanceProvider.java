package onl.tesseract.commandBuilder;

import org.jetbrains.annotations.Nullable;

public interface CommandInstanceProvider {

    @Nullable
    Object provideInstance(final Class<?> clazz);

    CommandInstanceProvider DEFAULT = (clazz) -> null;
}
