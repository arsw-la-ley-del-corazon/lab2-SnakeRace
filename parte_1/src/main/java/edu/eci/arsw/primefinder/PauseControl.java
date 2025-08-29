package edu.eci.arsw.primefinder;

/**
 * The {@code PauseControl} class provides thread-safe control to pause and resume
 * the execution of threads. This is useful in scenarios where a thread needs to
 * be temporarily halted and resumed later without terminating it.
 * 
 * <p>It uses a boolean flag {@code paused} to track the pause state and
 * {@code wait/notifyAll} for thread coordination.
 */
public class PauseControl {
    /**
     * Indicates whether the execution is currently paused.
     */
    private boolean paused = false;

    /**
     * Causes the calling thread to wait if the pause flag is set.
     * 
     * <p>This method should be called periodically by worker threads to check
     * whether they should pause execution. If {@code paused} is {@code true},
     * the calling thread will block until {@code setPaused(false)} is called
     * and {@code notifyAll()} is triggered.
     * 
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public synchronized void pauseIfNeeded() throws InterruptedException {
        while (paused) {
            wait();
        }
    }

    /**
     * Sets the pause state of the execution.
     * 
     * @param paused {@code true} to pause execution, {@code false} to resume
     *               any waiting threads
     */
    public synchronized void setPaused(boolean paused) {
        this.paused = paused;
        if (!paused) {
            notifyAll();
        }
    }

    /**
     * Returns the current pause state.
     * 
     * @return {@code true} if execution is paused, {@code false} otherwise
     */
    public synchronized boolean isPaused() {
        return paused;
    }
}
