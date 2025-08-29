package edu.eci.arsw.primefinder;

import java.util.Scanner;

public class Control extends Thread {

    private final static int NTHREADS = 3;
    private final static int MAXVALUE = 30000000;
    private final static int TMILISECONDS = 5000;

    private final int NDATA = MAXVALUE / NTHREADS;

    private PrimeFinderThread[] pft;
    private PauseControl pauseControl;

    private Control() {
        super();
        pauseControl = new PauseControl();
        pft = new PrimeFinderThread[NTHREADS];

        int i;
        for (i = 0; i < NTHREADS - 1; i++) {
            pft[i] = new PrimeFinderThread(i * NDATA, (i + 1) * NDATA, pauseControl);
        }
        pft[i] = new PrimeFinderThread(i * NDATA, MAXVALUE + 1, pauseControl);
    }

    public static Control newControl() {
        return new Control();
    }

    @Override
    public void run() {
        for (PrimeFinderThread thread : pft) {
            thread.start();
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                Thread.sleep(TMILISECONDS);

                // Pausar hilos
                pauseControl.setPaused(true);

                // Mostrar conteo de primos
                int totalPrimes = 0;
                for (PrimeFinderThread thread : pft) {
                    totalPrimes += thread.getPrimes().size();
                }
                System.out.println("\n== PAUSA == Total de primos encontrados: " + totalPrimes);
                System.out.println("Presiona ENTER para continuar...");
                scanner.nextLine();

                // Reanudar hilos
                pauseControl.setPaused(false);

                // Verificar si todos los hilos terminaron
                boolean allFinished = true;
                for (PrimeFinderThread thread : pft) {
                    if (thread.isAlive()) {
                        allFinished = false;
                        break;
                    }
                }
                if (allFinished) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
