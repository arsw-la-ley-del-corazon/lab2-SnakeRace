# Snake Race — ARSW Lab #2 

[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.9-blue.svg)](https://maven.apache.org/)

**Asignatura:** Arquitectura de Software  
**Estudiantes:**
- [Alexandra Moreno](https://github.com/AlexandraMorenoL)
- [Alison Valderrama](https://github.com/LIZVALMU)
- [Jeisson Sánchez](https://github.com/JeissonS02)
- [Valentina Gutierrez](https://github.com/LauraGutierrezr)

---


## Parte I Calentamiento
1) Programa **PrimeFinder**

## Correcion de codigo

Se diseñó una clase auxiliar llamada **PauseControl**, que actúa como monitor compartido entre todos los hilos. Dentro de esta clase se encuentra el lock implícito de los métodos sincronizados y una bandera booleana paused, que indica si los hilos deben estar en pausa o no.
Los métodos pauseIfNeeded(), setPaused() e isPaused() están marcados como synchronized, lo que garantiza la exclusión mutua y el acceso seguro a la variable paused.
```java
public class PauseControl {
    private boolean paused = false;

    public synchronized void pauseIfNeeded() throws InterruptedException {
        while (paused) {
            wait();
        }
    }

    public synchronized void setPaused(boolean paused) {
        this.paused = paused;
        if (!paused) {
            notifyAll();
        }
    }

    public synchronized boolean isPaused() {
        return paused;
    }
}

```

El método está marcado como synchronized, lo que significa que está usando el monitor del objeto PauseControl (es decir, this) como lock.

Lo mismo ocurre con los otros métodos sincronizados (setPaused, isPaused). Todos ellos están sincronizando sobre el mismo lock: this, es decir, el objeto de tipo PauseControl.

Tambien se modifica el metodo run en **Control** para la ejecucion de hilos.

Los **lost wakeups** (despertares perdidos) ocurren cuando un hilo pierde una señal de notify() o notifyAll() antes de entrar en wait(). Esto puede causar que el hilo quede bloqueado indefinidamente.

Java evita este problema con las siguientes buenas prácticas, que sí se están aplicando correctamente en el codigo.

Esto asegura que todos los hilos esperando serán notificados cuando paused sea puesto en false.

```java
@Override
    public void run() {
        for (PrimeFinderThread thread : pft) {
            thread.start();
        }

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                Thread.sleep(TMILISECONDS);
                pauseControl.setPaused(true);
                int totalPrimes = 0;
                for (PrimeFinderThread thread : pft) {
                    totalPrimes += thread.getPrimes().size();
                }
                System.out.println("\n== PAUSA == Total de primos encontrados: " + totalPrimes);
                System.out.println("Presiona ENTER para continuar...");
                scanner.nextLine();
                pauseControl.setPaused(false);
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
```

Y en **PrimeFinderThread** se cambia el **run** para que se comporte de la manera que deseamos

```java
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
```
Y de manera concurrente imprime la longitud de los numeros primos encontrados en ese tiempo lo cual con la **tecla enter** podemos continuar el proceso de ejecucion para que siga corriendo nuevamiente otra vez 5000ms. 


## Cómo ejecutar

```bash
mvn clean verify
mvn -q -DskipTests exec:java -Dsnakes=4
```

- `-Dsnakes=N` → inicia el juego con **N** serpientes (por defecto 2).
- **Controles**:
  - **Flechas**: serpiente **0** (Jugador 1).
  - **WASD**: serpiente **1** (si existe).
  - **Espacio** o botón **Action**: Pausar / Reanudar.

---

## Reglas del juego (resumen)

- **N serpientes** corren de forma autónoma (cada una en su propio hilo).
- **Ratones**: al comer uno, la serpiente **crece** y aparece un **nuevo obstáculo**.
- **Obstáculos**: si la cabeza entra en un obstáculo hay **rebote**.
- **Teletransportadores** (flechas rojas): entrar por uno te **saca por su par**.
- **Rayos (Turbo)**: al pisarlos, la serpiente obtiene **velocidad aumentada** temporal.
- Movimiento con **wrap-around** (el tablero “se repite” en los bordes).

---

## Arquitectura (carpetas)

```
co.eci.snake
├─ app/                 # Bootstrap de la aplicación (Main)
├─ core/                # Dominio: Board, Snake, Direction, Position
├─ core/engine/         # GameClock (ticks, Pausa/Reanudar)
├─ concurrency/         # SnakeRunner (lógica por serpiente con virtual threads)
└─ ui/legacy/           # UI estilo legado (Swing) con grilla y botón Action
```

---

# Actividades del laboratorio

## Parte I — (Calentamiento) `wait/notify` en un programa multi-hilo

1. Toma el programa [**PrimeFinder**](https://github.com/ARSW-ECI/wait-notify-excercise).
2. Modifícalo para que **cada _t_ milisegundos**:
   - Se **pausen** todos los hilos trabajadores.
   - Se **muestre** cuántos números primos se han encontrado.
   - El programa **espere ENTER** para **reanudar**.
3. La sincronización debe usar **`synchronized`**, **`wait()`**, **`notify()` / `notifyAll()`** sobre el **mismo monitor** (sin _busy-waiting_).
4. Entrega en el reporte de laboratorio **las observaciones y/o comentarios** explicando tu diseño de sincronización (qué lock, qué condición, cómo evitas _lost wakeups_).

> Objetivo didáctico: practicar suspensión/continuación **sin** espera activa y consolidar el modelo de monitores en Java.

---

## Parte II — SnakeRace concurrente (núcleo del laboratorio)

### 1) Análisis de concurrencia

- Explica **cómo** el código usa hilos para dar autonomía a cada serpiente.
- **Identifica** y documenta en **`el reporte de laboratorio`**:
  - Posibles **condiciones de carrera**.
  - **Colecciones** o estructuras **no seguras** en contexto concurrente.
  - Ocurrencias de **espera activa** (busy-wait) o de sincronización innecesaria.

### 2) Correcciones mínimas y regiones críticas

- **Elimina** esperas activas reemplazándolas por **señales** / **estados** o mecanismos de la librería de concurrencia.
- Protege **solo** las **regiones críticas estrictamente necesarias** (evita bloqueos amplios).
- Justifica en **`el reporte de laboratorio`** cada cambio: cuál era el riesgo y cómo lo resuelves.

### 3) Control de ejecución seguro (UI)

- Implementa la **UI** con **Iniciar / Pausar / Reanudar** (ya existe el botón _Action_ y el reloj `GameClock`).
- Al **Pausar**, muestra de forma **consistente** (sin _tearing_):
  - La **serpiente viva más larga**.
  - La **peor serpiente** (la que **primero murió**).
- Considera que la suspensión **no es instantánea**; coordina para que el estado mostrado no quede “a medias”.

### 4) Robustez bajo carga

- Ejecuta con **N alto** (`-Dsnakes=20` o más) y/o aumenta la velocidad.
- El juego **no debe romperse**: sin `ConcurrentModificationException`, sin lecturas inconsistentes, sin _deadlocks_.
- Si habilitas **teleports** y **turbo**, verifica que las reglas no introduzcan carreras.

> Entregables detallados más abajo.

---

## Entregables

1. **Código fuente** funcionando en **Java 21**.
2. Todo de manera clara en **`**el reporte de laboratorio**`** con:
   - Data races encontradas y su solución.
   - Colecciones mal usadas y cómo se protegieron (o sustituyeron).
   - Esperas activas eliminadas y mecanismo utilizado.
   - Regiones críticas definidas y justificación de su **alcance mínimo**.
3. UI con **Iniciar / Pausar / Reanudar** y estadísticas solicitadas al pausar.

---

## Criterios de evaluación (10)

- (3) **Concurrencia correcta**: sin data races; sincronización bien localizada.
- (2) **Pausa/Reanudar**: consistencia visual y de estado.
- (2) **Robustez**: corre **con N alto** y sin excepciones de concurrencia.
- (1.5) **Calidad**: estructura clara, nombres, comentarios; sin _code smells_ obvios.
- (1.5) **Documentación**: **`reporte de laboratorio`** claro, reproducible;

---

## Tips y configuración útil

- **Número de serpientes**: `-Dsnakes=N` al ejecutar.
- **Tamaño del tablero**: cambiar el constructor `new Board(width, height)`.
- **Teleports / Turbo**: editar `Board.java` (métodos de inicialización y reglas en `step(...)`).
- **Velocidad**: ajustar `GameClock` (tick) o el `sleep` del `SnakeRunner` (incluye modo turbo).

---

## Cómo correr pruebas

```bash
mvn clean verify
```

Incluye compilación y ejecución de pruebas JUnit. Si tienes análisis estático, ejecútalo en `verify` o `site` según tu `pom.xml`.

---

## Créditos

Este laboratorio es una adaptación modernizada del ejercicio **SnakeRace** de ARSW. El enunciado de actividades se conserva para mantener los objetivos pedagógicos del curso.

**Base construida por el Ing. Javier Toquica.**