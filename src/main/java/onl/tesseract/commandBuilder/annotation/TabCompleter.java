package onl.tesseract.commandBuilder.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method annotated with TabCompleter is used to propose a tab completion to the player
 * The method's parameter will be auto-injected, given the following rules, in priority order :
 * <ul>
 *     <li>Parameter annotated {@link Env}: Injected with the value of the specified env variable. See {@link onl.tesseract.commandBuilder.CommandEnvironment}</li>
 *     <li>Parameter of type {@link onl.tesseract.commandBuilder.CommandEnvironment}: Injected with the command environment</li>
 *     <li>Parameter of type {@link org.bukkit.command.CommandSender}: Injected with the command sender</li>
 *     <li>Parameter of type {@link org.bukkit.entity.Player}: Injected with the player performing the command, for player-only commands</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TabCompleter {
}
