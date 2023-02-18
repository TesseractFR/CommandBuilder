package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Defines a permission needed to use a command. Players that don't have the permission will not be able to execute the command and to see it on help
 * messages and tab completions. If a command does not have a permission, any player can use it.
 * </p>
 *
 * <h2>Recursive permissions</h2>
 * A permission can have a parent permission. A permission 'c' having a parent 'b' having itself a parent 'a' will give the effective permission
 * "a.b.c". To use a command, a player has to have either the permission of the command, or a parent permission (i.e, "a", "a.b" or "a.b.c"). A
 * permission takes precedence over its parent.
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Perm {

    /**
     * Name of the permission
     */
    String value() default "";

    /**
     * For subcommands, if absolute is true, then the permission will not be a child permission of the parent command.
     */
    boolean absolute() default false;

    /**
     * Define which permission mode should be applied to the command
     */
    Mode mode() default Mode.INHERIT;

    /**
     * Define how a command's permission name is computed.
     */
    enum Mode {
        /**
         * Commands that have an auto permission will have a permission name automatically set to the command's name, as a sub permission of the
         * command's parent if there is one. For example, let's consider a command 'parent' with a sub command 'child' both in auto mode, they will
         * have the permission names "parent" and "parent.child" respectively.<br/>
         * The permission name can be overridden by setting {@link Perm#value()}.
         */
        AUTO,
        /**
         * Commands in manual permission mode can have a permission set with {@link Perm#value()}. For subcommands, if {@link Perm#absolute()} is
         * false, then the permission name will be a sub permission of the parent command's permission. If {@link Perm#absolute()} is true, then
         * {@link  Perm#value()} will be kept as is.
         */
        MANUAL,
        /**
         * Specify that a subcommand should inherit its permission mode from its parent. If it has no parent, then the behavior is similar to
         * {@link Mode#MANUAL}.
         */
        INHERIT,
    }
}
