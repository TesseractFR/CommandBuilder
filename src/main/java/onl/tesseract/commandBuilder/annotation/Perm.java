package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a permission needed to use a command. Players that don't have the permission will not be able to execute the command and to see it on help
 * messages and tab completions. If a command does not have a permission, any player can use it.
 *
 * <h2>Recursive permissions</h2>
 * A permission can have a parent permission. A permission 'c' having a parent 'b' having itself a parent 'a' will give the effective permission
 * "a.b.c". To use a command, a player has to have either the permission of the command, or a parent permission (i.e, "a", "a.b" or "a.b.c"). A
 * permission takes precedence over its parent.
 *
 * <h3>Globbing</h3>
 * It is possible to set a permission for every child using '*'
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Perm {

    /**
     * Name of the permission
     */
    String value();

    /**
     * For subcommands, if absolute is true, then the permission will not be a child permission of the parent command.
     */
    boolean absolute() default false;
}
