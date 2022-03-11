package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Command;

final class ClassAnnotationReader extends AnnotationReader {

    private final Class<? extends CommandBuilderV2> clazz;

    ClassAnnotationReader(final Class<? extends CommandBuilderV2> clazz)
    {
        super(clazz.getAnnotation(Command.class));
        this.clazz = clazz;
        if (commandAnnotation == null)
            throw new IllegalStateException(clazz.getName() + " should be annotated with @Command");
    }

    String readName()
    {
        String name = commandAnnotation.name();
        return name.isEmpty()
               ? readName(clazz.getSimpleName())
               : name;
    }
}
