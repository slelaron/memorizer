package ru.ifmo.ctddev.kostlivtsev.Memorizer;

import java.util.ArrayList;
import java.util.Random;
import java.util.function.Function;

/**
 * Created by nikita on 01.06.17.
 */
public class MemorizerTest {
    public static void main(String[] args) {
        ArrayList<Thread> threads = new ArrayList<>();
        Function<Integer, Integer> func = (a) -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException ex) {
                System.out.println("Interrupted");
            }
            return 2 * a;
        };
        Memorizer<Integer, Integer> memorizer = new Memorizer<>(func);
        try {
            Random rand = new Random();
            for (int i = 0; i < 100; i++) {
                final int now = Math.abs(rand.nextInt()) % 10;
                threads.add(i, new Thread(() -> {
                    int result = memorizer.apply(now);
                    if (result != 2 * now) {
                        System.out.println("Incorrect");
                    } else {
                        System.out.println("Correct");
                    }
                }));
                threads.get(i).start();
                //threads.get(i).join();
            }
            for (int i = 0; i < 100; i++) {
                threads.get(i).join();
            }
        } catch (InterruptedException ex) {
            System.out.println("Interrupted");
        }
    }
}
