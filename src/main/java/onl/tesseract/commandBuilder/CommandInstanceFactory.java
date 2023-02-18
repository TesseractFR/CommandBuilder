package onl.tesseract.commandBuilder;

@FunctionalInterface
interface CommandInstanceFactory {

    Object getClassInstance(Class<?> clazz);
}
