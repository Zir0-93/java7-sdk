package java.beans;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Stub implementation to use while developing JLM 6.0 without a 6.0 JRE.
 * 
 * @author gharley
 */
@Documented
@Target(value = CONSTRUCTOR)
@Retention(value = RUNTIME)
public @interface ConstructorProperties {

    public String[] value();

}
