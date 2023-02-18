package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.*;

/**
 * Defines the class or method as a command. <br/>
 * If put on a method, the method's parameters will be auto-injected, given the following rules, in priority order :
 * <ul>
 *     <li>Parameter annotated {@link Argument}: Injected with the parsed value of the argument. Possibly null for optional arguments</li>
 *     <li>Parameter annotated {@link Env}: Injected with the value of the specified env variable. See {@link onl.tesseract.commandBuilder.CommandEnvironment}</li>
 *     <li>Parameter of type {@link onl.tesseract.commandBuilder.CommandEnvironment}: Injected with the command environment</li>
 *     <li>Parameter of type {@link org.bukkit.command.CommandSender}: Injected with the command sender</li>
 *     <li>Parameter of type {@link org.bukkit.entity.Player}: Injected with the player performing the command, for player-only commands</li>
 * </ul>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Command {

    /**
     * Command's name. If not specified, a default name is computed from the class/method's name
     */
    String name() default "";

    String[] alias() default {};

    Perm permission() default @Perm("");

    /**
     * Description shown in help messages
     */
    String description() default "";

    boolean playerOnly() default false;

    Argument[] args() default {};

    /**
     * Usable only on classes
     */
    Class<?>[] subCommands() default {};

    int helpPriority() default 50;
}
