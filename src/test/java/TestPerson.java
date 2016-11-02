import cache.Cache;

import static cache.CacheType.FILE;
import static cache.CacheType.MEMORY_AND_FILE;

/**
 * Created by Valentina on 21.10.2016.
 */
public interface TestPerson{
    @Cache(cacheType=MEMORY_AND_FILE, path="src/main/resources")
    public String changeName(String name);

    public int changeAge(int age);

    @Cache(cacheType = FILE)
    public TestPerson child(String name, int age);
}
