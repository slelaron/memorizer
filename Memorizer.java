package ru.ifmo.ctddev.kostlivtsev.Memorizer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * Created by nikita on 01.06.17.
 */
public class Memorizer<Arg, Res> {
    private final Map<Arg, Res> map;
    private final Function<Arg, Res> func;
    private final Set<Arg> set;
    private final Map<Arg, ReentrantLock> locks;
    private final Map<Arg, Wrapper> wrappers;

    /**
     * Class that contains condition and lock for the condition.
     */
    private class Wrapper {
        ReentrantLock condLock;
        Condition condition;

        Wrapper() {
            condLock = new ReentrantLock();
            condition = condLock.newCondition();
        }

        void lock() {
            condLock.lock();
        }

        void unlock() {
            condLock.unlock();
        }

        void await() throws InterruptedException {
            condition.await();
        }

        void signal() {
            condition.signal();
        }

        void signalAll() {
            condition.signalAll();
        }
    }

    /**
     * Initializes the Memorizer. It is the point of creation all internal resources.
     * @param func - function to be used for computation.
     */
    public Memorizer(Function<Arg, Res> func) {
        map = new ConcurrentHashMap<>();
        this.func = func;
        set = ConcurrentHashMap.newKeySet();
        wrappers = new ConcurrentHashMap<>();
        locks = new ConcurrentHashMap<>();
    }

    /**
     * Constructs function that will be executed in Thread Pool.
     * @param args args need for the function.
     * @return function that will be executed in Thread Pool.
     */
    private Runnable executableFunction(Arg args) {
        final Wrapper wrapper = wrappers.get(args);
        return () -> {
            wrapper.lock();
            try {
                map.put(args, func.apply(args));
                System.err.println("Executed function with args: " + args);
                wrapper.signalAll();
            } finally {
                wrapper.unlock();
            }
        };
    }

    /**
     * Returns result of the function or null if execution was interrupted by user.
     * @param args - arguments need function to be calculated.
     * @return result of computation.
     */
    public Res apply(Arg args) {
        boolean result;
        locks.putIfAbsent(args, new ReentrantLock());
        locks.get(args).lock();
        try {
            if (!(result = set.contains(args))) {
                set.add(args);
                wrappers.put(args, new Wrapper());
            }
        } finally {
            locks.get(args).unlock();
        }

        Wrapper wrapper = wrappers.get(args);
        if (!result) {
            Thread thread = new Thread(executableFunction(args));
            thread.start();
            try {
                thread.join();
            } catch (InterruptedException e) {
                try {
                    locks.get(args).lock();
                    set.remove(args);
                    wrapper.signalAll();
                } finally {
                    locks.get(args).unlock();
                }
                return null;
            }
        }

        wrapper.lock();
        try {
            while (set.contains(args) && !map.containsKey(args)) {
                wrapper.await();
            }
        } catch (InterruptedException e) {
            System.err.println("Stopped computation");
        } finally {
            wrapper.unlock();
        }
        return map.get(args);
    }
}
