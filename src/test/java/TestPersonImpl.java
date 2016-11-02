import cache.Cache;

import java.util.Objects;

import static cache.CacheType.FILE;
import static cache.CacheType.MEMORY;

/**
 * Created by Valentina on 21.10.2016.
 */
public class TestPersonImpl implements TestPerson {
    private  String name;
    private int age;

    public TestPersonImpl(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Cache(cacheType= FILE)
    @Override
    public String changeName(String name) {
        this.name = name;
        return this.name;
    }

    @Override
    public int changeAge(int age) {
        this.age=age;
        return this.age;
    }

    @Cache(cacheType= MEMORY)
    @Override
    public TestPerson child(String name, int age) {
        return new TestPersonImpl (name, age);
    }


    @Override
    public String toString() {
        return "PersonImpl{" +
                "name='" + name + '\'' +
                ", age=" + age +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestPersonImpl that = (TestPersonImpl) o;
        return age == that.age &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age);
    }
}
