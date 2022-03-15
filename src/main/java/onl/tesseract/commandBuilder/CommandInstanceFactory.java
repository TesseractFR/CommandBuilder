package onl.tesseract.commandBuilder;

@FunctionalInterface
public interface CommandInstanceFactory {

    Object getClassInstance(Class<?> clazz);
}
