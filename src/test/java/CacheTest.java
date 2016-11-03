import cache.Cache;
import cache.CacheHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static cache.CacheType.FILE;
import static cache.CacheType.MEMORY;
import static cache.CacheType.MEMORY_AND_FILE;

/**
 * Created by Valentina on 21.10.2016.
 */
public class CacheTest {
    TestMultiplyFor proxyMulti;
    TestMultiplyForTwo mult = new TestMultiplyForTwo();
    TestPersonImpl person = new TestPersonImpl("Иван", 21);
    TestPerson proxyPerson;
    @Before
    public void setUp() {
        proxyMulti=(TestMultiplyFor) Proxy.newProxyInstance(TestMultiplyForTwo.class.getClassLoader(),
                TestMultiplyForTwo.class.getInterfaces(),
                new CacheHandler(mult));


         proxyPerson=(TestPerson) Proxy.newProxyInstance(TestPersonImpl.class.getClassLoader(),
                 TestPersonImpl.class.getInterfaces(),
                new CacheHandler(person));
    }

    @Test
    public void testReWriteFile() throws Exception {
        int result = proxyMulti.multiply(5);

        for (int i = 0; i < 10; i++)
        Assert.assertTrue(proxyMulti.multiply(5) == result);
    }

    @Test
    public void testCacheWriteMemoryAndFile() throws Exception {

        int result = proxyMulti.multiplyCount(8);

        for (int i = 0; i < 5; ++i)
            Assert.assertTrue(proxyMulti.multiplyCount(8) == result);
    }

    @Test
    public void testWithoutCache() throws Exception {

        int result = proxyMulti.powOfTwo(6);
        Assert.assertTrue(proxyMulti.powOfTwo(6) == result);
    }
    @Test
    public void testWithoutCachePerson() throws Exception {

        int age = proxyPerson.changeAge(25);
        Assert.assertTrue(proxyPerson.changeAge(25) == age);
    }

    @Test
    public void testCacheMemoryAndFileAndPath() throws Exception {

       String name = proxyPerson.changeName("Ивен");
        Assert.assertTrue(proxyPerson.changeName("Ивен").equals(name));
    }

    @Test
    public void testNotSerializable() throws Exception {

        TestPerson child = proxyPerson.child("Таня", 25);
        Assert.assertTrue(proxyPerson.child("Таня", 25).equals(child));
    }
    @Test
    public void testThread() throws Exception {
        ExecutorService executor= Executors.newFixedThreadPool(3);

        List<Future<Integer>> multFuture= IntStream.range(0, 5)
                .mapToObj(i->executor.submit(()->proxyMulti.multiply(3)))
                .collect(Collectors.toList());
        List<Integer> resultTest=new ArrayList<>();
        multFuture.forEach(i -> {
            try {
                resultTest.add(i.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        });
        Assert.assertTrue(resultTest.containsAll(Arrays.asList(6,6,6,6,6)));
    }
}

