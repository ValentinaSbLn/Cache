package cache;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CacheHandler implements InvocationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheHandler.class);

    private final Object delegate;
    private final ConcurrentMap<String, ConcurrentMap<List<Object>, Object>> cache;
    private final ConcurrentMap<Method, Lock> lockMap;

    public CacheHandler(Object delegate) {
        this.delegate = delegate;
        cache = new ConcurrentHashMap<>();
        lockMap = new ConcurrentHashMap<>();
        init();
    }

    private void init() {
        Method[] methods = delegate.getClass().getDeclaredMethods();
        for (Method m : methods) {
            if (m.isAnnotationPresent(Cache.class)) {
                cache.put(m.getName(), new ConcurrentHashMap<>());
            }
        }
    }

    private String getOrCreateFile(Method method) {
        Path pathFile;
        String fileName = method.getName() + ".ser";

        if (method.getDeclaredAnnotation(Cache.class).path().length() == 0) {
            pathFile = Paths.get(System.getProperty("java.io.tmpdir") + '/' + fileName);
        } else {
            pathFile = Paths.get(method.getDeclaredAnnotation(Cache.class).path() + '/' + fileName);
            if (!Files.exists(pathFile.getParent()))
                try {
                    Files.createDirectories(pathFile.getParent());
                } catch (IOException e) {
                    LOGGER.error("Не удалось создать " + pathFile.getParent() + "\n" + e);
                }
        }
        if (!Files.exists(pathFile))
            try {
                Files.createFile(pathFile);
            } catch (IOException e) {
                LOGGER.error("Не удалось создать файл " + pathFile + "\n" + e);
            }
        return pathFile.toString();
    }

    private void writeCache(Method method, ConcurrentMap<List<Object>, Object> cache) throws IOException {
        String fileName = getOrCreateFile(method);
        try (FileOutputStream fos = new FileOutputStream(fileName);
             ObjectOutputStream out = new ObjectOutputStream(fos)) {
            out.writeObject(cache);
        }
    }

    @SuppressWarnings("unchecked")
    private ConcurrentMap<List<Object>, Object> readCache(Method method) throws IOException {
        Object cache;
        String fileName = getOrCreateFile(method);

        try (FileInputStream fis = new FileInputStream(fileName);
             ObjectInputStream in = new ObjectInputStream(fis)) {
            cache = in.readObject();

        } catch (ClassNotFoundException | EOFException e) {
            return new ConcurrentHashMap<>();
        }
        return (ConcurrentMap<List<Object>, Object>) cache;
    }

    private boolean isSerialize(Method method) {
        boolean sParam = false;
        for (Parameter p : method.getParameters()) {
            Class<?> clazz = p.getType();
            if (isSerializeClass(clazz)) sParam = true;
        }
        if (isSerializeClass(method.getReturnType()) && sParam) return true;

        return false;
    }

    private boolean isSerializeClass(Class<?> clazz) {
        if (clazz.isPrimitive()) return true;
        for (Class<?> intf : clazz.getInterfaces()) {
            if (intf == Serializable.class)
                return true;
        }
        return false;
    }


    private void inFile(Method method, List<Object> arg, Object result) {
        ConcurrentMap<List<Object>, Object> cache;
        try {
            cache = readCache(method);
            cache.put(arg, result);
            writeCache(method, cache);

        } catch (IOException e) {
            LOGGER.error("Запись в файл не удалась \n" + e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Object result;
        if (cache.containsKey(method.getName())) {
            lockMap.putIfAbsent(method, new ReentrantLock());
            result = choiceCache(method, args);

        } else {
            LOGGER.debug("Метод " + method.getName() + " не кэшируется.");
            result = method.invoke(delegate, args);
        }
        return result;
    }

    private Object cacheFile(Method method, Object[] args, List<Object> listArgs) throws IOException, InvocationTargetException, IllegalAccessException {

        final Lock lockFile = lockMap.get(method);
        Object result;
        try {
            lockFile.lock();
            if (isSerialize(method)) {
                ConcurrentMap<List<Object>, Object> value = readCache(method);
                if (value.containsKey(listArgs)) {
                    LOGGER.debug("Cache.FILE, from " + method.getName() + ".ser args" + Arrays.toString(listArgs.toArray()));
                    result = readCache(method).get(listArgs);
                } else {
                    result = method.invoke(delegate, args);
                    LOGGER.debug("Cache.FILE, to " + method.getName() + ".ser args" + Arrays.toString(listArgs.toArray()));
                    inFile(method, listArgs, result);
                }
            } else {
                result = method.invoke(delegate, args);
                LOGGER.debug(method.getName() + " не сериализуется");
            }
            return result;
        } finally {
            lockFile.unlock();
        }
    }

    private Object cacheFileAndMemory(Method method, Object[] args, List<Object> listArgs) throws IOException, InvocationTargetException, IllegalAccessException {
        Object result;

        final Lock lockMemoryAndFile = lockMap.get(method);
        try {
            lockMemoryAndFile.lock();

            if (isSerialize(method)) {
                cache.get(method.getName()).putAll(readCache(method));
            }
            ConcurrentMap<List<Object>, Object> value = cache.get(method.getName());

            if (value.containsKey(listArgs)) {
                LOGGER.debug("Cache.MEMORY_AND_FILE: " + method.getName() + " from cache " + Arrays.toString(args));
                result = cache.get(method.getName()).get(listArgs);

            } else {
                result = method.invoke(delegate, args);
                LOGGER.debug("Cache.MEMORY_AND_FILE:" + method.getName() + " to cache " + Arrays.toString(args));

                cache.get(method.getName()).put(listArgs, result);
                if (isSerialize(method)) inFile(method, listArgs, result);
            }

            return result;
        } finally {
            lockMemoryAndFile.unlock();
        }
    }

    private Object cacheMemory(Method method, Object[] args, List<Object> listArgs) throws InvocationTargetException, IllegalAccessException {
        Object result;
        final Lock lockMemory = lockMap.get(method);
        try {
            lockMemory.lock();

            ConcurrentMap<List<Object>, Object> value = cache.get(method.getName());

            if (value.containsKey(listArgs)) {
                LOGGER.debug("MEMORY from cache " + method.getName());
                result = cache.get(method.getName()).get(listArgs);
            } else {
                result = method.invoke(delegate, args);
                LOGGER.debug("MEMORY to cache " + method.getName());
                cache.get(method.getName()).put(listArgs, result);
            }
            return result;
        } finally {
            lockMemory.unlock();
        }
    }

    private Object choiceCache(Method method, Object[] args) throws InvocationTargetException, IllegalAccessException, IOException {
        List<Object> listArgs = new CopyOnWriteArrayList<>();
        listArgs.addAll(Arrays.asList(args));
        Object result = null;
        switch (method.getDeclaredAnnotation(Cache.class).cacheType()) {

            case FILE: {
                result = cacheFile(method, args, listArgs);
                break;
            }
            case MEMORY_AND_FILE: {
                result = cacheFileAndMemory(method, args, listArgs);
                break;
            }
            case MEMORY: {
                result = cacheMemory(method, args, listArgs);
                break;
            }
        }
        return result;
    }
}


