package edu.eci.arsw.primefinder;

import java.util.LinkedList;
import java.util.List;

/**
 * The {@code PrimeFinderThread} class extends {@code Thread} and is responsible
 * for finding all prime numbers within a given range [a, b). It supports pausing
 * and resuming execution via a {@link PauseControl} object.
 */
public class PrimeFinderThread extends Thread {
    /**
     * Start of the range (inclusive).
     */
    int a;

    /**
     * End of the range (exclusive).
     */
    int b;

    /**
     * List to store the prime numbers found in the range.
     */
    private List<Integer> primes;

    /**
     * Control object used to pause and resume execution.
     */
    private PauseControl pauseControl;

    /**
     * Constructs a {@code PrimeFinderThread} to search for prime numbers
     * in the range [a, b), with support for pausing and resuming.
     *
     * @param a             the starting number of the range (inclusive)
     * @param b             the ending number of the range (exclusive)
     * @param pauseControl  the control object used for pausing/resuming
     */
    public PrimeFinderThread(int a, int b, PauseControl pauseControl) {
        super();
        this.primes = new LinkedList<>();
        this.a = a;
        this.b = b;
        this.pauseControl = pauseControl;
    }

    /**
     * Runs the thread, checking each number in the specified range for primality.
     * If the number is prime, it is added to the {@code primes} list and printed to the console.
     * The thread respects the pause state and waits when necessary.
     */
    @Override
    public void run() {
        for (int i = a; i < b; i++) {
            try {
                pauseControl.pauseIfNeeded();
            } catch (InterruptedException e) {
                return;
            }

            if (isPrime(i)) {
                primes.add(i);
                System.out.println(i);
            }
        }
    }

    /**
     * Checks whether a given number is prime.
     *
     * @param n the number to check
     * @return {@code true} if {@code n} is a prime number, {@code false} otherwise
     */
    boolean isPrime(int n) {
        boolean ans;
        if (n > 2) {
            ans = n % 2 != 0;
            for (int i = 3; ans && i * i <= n; i += 2) {
                ans = n % i != 0;
            }
        } else {
            ans = n == 2;
        }
        return ans;
    }

    /**
     * Returns the list of prime numbers found by this thread.
     *
     * @return a list of prime numbers in the specified range
     */
    public List<Integer> getPrimes() {
        return primes;
    }
}
