package cache;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static cache.CacheType.MEMORY;

/**
 * Created by Valentina on 21.10.2016.
 */
@Target({METHOD})
@Retention(RUNTIME)
public @interface Cache {
    CacheType cacheType() default MEMORY;
    String path() default "";
}
