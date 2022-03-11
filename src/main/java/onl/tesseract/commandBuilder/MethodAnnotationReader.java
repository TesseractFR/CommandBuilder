package onl.tesseract.commandBuilder;

import onl.tesseract.commandBuilder.annotation.Argument;
import onl.tesseract.commandBuilder.annotation.Command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

final class MethodAnnotationReader extends AnnotationReader {
    private final Method method;

    MethodAnnotationReader(final Method method)
    {
        super(method.getAnnotation(Command.class));
        this.method = method;
        if (commandAnnotation == null)
            throw new IllegalStateException(method.getName() + " method should be annotated with @Command");
    }

    @Override
    String readName()
    {
        return readName(method.getName());
    }

    @Override
    List<CommandArgument> readArguments() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException
    {
        List<CommandArgument> args = super.readArguments();

        Parameter[] parameters = method.getParameters();
        for (Parameter parameter : parameters)
        {
            Argument argAnnotation = parameter.getAnnotation(Argument.class);
            Class<?> clazz = argAnnotation.clazz() == void.class
                             ? parameter.getType()
                             : argAnnotation.clazz();
            String name = argAnnotation.label();

            //noinspection unchecked
            args.add(instantiateArgument((Class<? extends CommandArgument>) clazz, name));
        }
        return args;
    }
}
