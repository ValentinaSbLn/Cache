import cache.Cache;

import static cache.CacheType.FILE;
import static cache.CacheType.MEMORY_AND_FILE;

/**
 * Created by Valentina on 21.10.2016.
 */
public interface TestMultiplyFor {
    @Cache(cacheType= FILE)
    int multiply(int d);

    int powOfTwo(int d);

    @Cache (cacheType= MEMORY_AND_FILE)
    int multiplyCount(int d);
}
