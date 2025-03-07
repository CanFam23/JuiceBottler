import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The {@code Plant} class represents a processing plant in which workers bottle oranges. Using several threads and different queues,
 * the plant has different {@link Worker} objects who do one of three tasks:
 * <br> 1. Peel oranges <br>
 * 2. Squeeze oranges <br>
 * 3. Bottle oranges <br>
 * The workers receive and pass on oranges using {@link LinkedBlockingQueue}.
 */
public class Plant implements Runnable {
    /** How long do we want to run the juice processing. */
    private static final long PROCESSING_TIME = 5 * 1000;

    /** Number of oranges put in each bottle. */
    private static final int ORANGES_PER_BOTTLE = 3;

    /** Number of plants that will be running. */
    private static final int NUM_PLANTS = 2;

    /** Number of workers who will peel oranges. */
    private static final int NUM_PEELERS = 6;

    /** Number of workers who will squeeze oranges. */
    private static final int NUM_SQUEEZERS = 4;

    /** Number of workers who will bottle oranges. */
    private static final int NUM_BOTTLERS = 3;

    /** Total number of workers working in each plant. */
    private static final int TOTAL_WORKERS = NUM_PEELERS + NUM_SQUEEZERS + NUM_BOTTLERS;

    /**
     * Main method, creates plants and starts them, the gives them time to work before stopping them and gathering data.
     *
     * @param args arguments provided.
     */
    public static void main(String[] args) {
        // Startup the plants
        final Plant[] plants = new Plant[NUM_PLANTS];
        for (int i = 0; i < NUM_PLANTS; i++) {
            plants[i] = new Plant(i + 1);
            plants[i].startPlant();
        }

        // Give the plants time to do work
        delay(PROCESSING_TIME, "Plant malfunction");

        // Stop the plant, and waits for it to shut down
        for (Plant p : plants) {
            p.stopPlant();
        }

        for (Plant p : plants) {
            p.waitToStop();
        }

        // Summarize the results
        int totalProvided = 0;
        int totalBottled = 0;
        int totalProcessed = 0;
        int totalWasted = 0;
        int totalLeftInQueue = 0;
        int totalNotBottled = 0;
        int totalRemoved = 0;
        for (Plant p : plants) {
            totalProvided += p.getOrangesProvided();
            totalProcessed += p.getOrangesProcessed();
            totalBottled += p.getOrangesBottled();
            totalWasted += p.getOrangesWasted();
            totalLeftInQueue += p.getOrangesLeftInQueue();
            totalNotBottled += p.getOrangesNotBottled();
            totalRemoved += p.getOrangesRemovedFromQueues();
        }
        System.out.println();
        System.out.println("=".repeat(10) + "Results" + "=".repeat(10));
        System.out.println("Total provided/processed = " + totalProvided + "/" + totalProcessed);
        System.out.println("Total left in queues = " + totalLeftInQueue);
        System.out.println("Total leftover after bottling oranges = " + totalNotBottled);
        System.out.println("Total removed from queues = " + totalRemoved);
        System.out.println("Created " + totalBottled +
                ", wasted " + totalWasted + " oranges");
    }

    /**
     * Gives the other plants time to do work by making this thread sleep for given time
     *
     * @param time   Time to sleep for
     * @param errMsg Error message to show if an error occurs
     */
    private static void delay(long time, String errMsg) {
        final long sleepTime = Math.max(1, time);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            System.err.println(errMsg);
        }
    }

    /** Thread for plant. */
    private final Thread thread;

    /** Keeps track of oranges provided to the workers. */
    private int orangesProvided;

    /** Keeps track of how many oranges were removed from queues because they were put in the wrong one. */
    private int orangesRemovedFromQueues;

    /** If true, then plant should be working. */
    private volatile boolean timeToWork;

    /** Queue that holds oranges in peel state. */
    private final BlockingQueue<Orange> peelQueue;

    /** Queue that holds oranges in squeeze state. */
    private final BlockingQueue<Orange> squeezeQueue;

    /** Queue that holds oranges in bottle state. */
    private final BlockingQueue<Orange> bottleQueue;

    /** Queue that holds oranges that are fully processed. */
    private final BlockingQueue<Orange> doneQueue;

    /** Array of workers who 'work' in this plant. */
    private final Worker[] workers;

    /**
     * Creates a new Plant object.
     *
     * @param threadNum Number of this thread
     */
    public Plant(int threadNum) {
        peelQueue = new LinkedBlockingQueue<>(10);
        squeezeQueue = new LinkedBlockingQueue<>(10);
        bottleQueue = new LinkedBlockingQueue<>(10);
        doneQueue = new LinkedBlockingQueue<>();

        thread = new Thread(this, "Plant[" + threadNum + "]");

        workers = new Worker[TOTAL_WORKERS];

        orangesProvided = 0;
        orangesRemovedFromQueues = 0;

        // Create given amount of each worker and add them to the workers array
        int ind = 0;
        for (int i = 0; i < NUM_PEELERS; i++) {
            workers[ind] = new Worker(threadNum, ind + 1, peelQueue, squeezeQueue, Orange.State.Peeled);
            ind++;
        }

        for (int i = 0; i < NUM_SQUEEZERS; i++) {
            workers[ind] = new Worker(threadNum, ind + 1, squeezeQueue, bottleQueue, Orange.State.Squeezed);
            ind++;
        }

        for (int i = 0; i < NUM_BOTTLERS; i++) {
            workers[ind] = new Worker(threadNum, ind + 1, bottleQueue, doneQueue, Orange.State.Bottled);
            ind++;
        }
    }

    /** Sets timeToWork to true, starts thread. */
    public void startPlant() {
        timeToWork = true;
        thread.start();

        // Start the workers too
        for (Worker w : workers) {
            w.start();
        }
    }

    /** Sets timeToWork to false. */
    public void stopPlant() {
        timeToWork = false;

        // Stop the workers too
        for (Worker w : workers) {
            w.stop();
        }
    }

    /**
     * Waits for thread to stop. Calls waitToStop on each worker before waiting to stop itself. <br>
     * From <a href="https://stackoverflow.com/questions/53405013/how-does-thread-join-work-conceptually">stack overflow</a> : <br>
     * The classic implementation of Thread.join is to lock
     * the Thread object, test to see if is alive and
     * if not wait on the Thread object. As a thread exits,
     * it locks its instance and calls notifyAll.
     */
    public void waitToStop() {
        for (Worker w : workers) {
            w.waitToStop();
        }

        try {
            thread.join();
        } catch (InterruptedException e) {
            System.err.println(thread.getName() + " stop malfunction");
        }
    }

    /**
     * Runs thread until it is no longer time to work. Continues
     * to create oranges and distribute them.
     */
    public void run() {
        System.out.println(Thread.currentThread().getName() + " Processing oranges");
        while (timeToWork) {
            if(distributeOrange(new Orange())){
                orangesProvided++;
            }
            checkQueues();
        }
        System.out.println(Thread.currentThread().getName() + " Done");
    }

    /**
     * The Line Inspector, removes oranges from each queue if they are not in the correct {@link Orange.State}.
     * <ul>
     *     <li>Removes oranges from {@link #peelQueue} who's state isn't fetched.</li>
     *     <li>Removes oranges from {@link #squeezeQueue} who's state isn't peeled.</li>
     *     <li>Removes oranges from {@link #bottleQueue} who's state isn't squeezed.</li>
     *     <li>Removes oranges from {@link #doneQueue} who's state isn't bottled.</li>
     * </ul>
     */
    private void checkQueues() {
        // Remove oranges from peel queue if state isn't fetched
        final int peelQueueSize = peelQueue.size();
        boolean removedFromQueue = peelQueue.removeIf(orange -> orange.getState() != Orange.State.Fetched);
        if (removedFromQueue) {
            orangesRemovedFromQueues += (peelQueueSize - peelQueue.size());
            System.err.println("Removed " + (peelQueueSize - peelQueue.size()) + " orange(s) from peel queue with incorrect state(s).");
        }

        // Remove oranges from squeeze queue if state isn't peeled
        final int squeezeQueueSize = squeezeQueue.size();
        removedFromQueue = squeezeQueue.removeIf(orange -> orange.getState() != Orange.State.Peeled);
        if (removedFromQueue) {
            orangesRemovedFromQueues += (squeezeQueueSize - squeezeQueue.size());
            System.err.println("Removed " + (squeezeQueueSize - squeezeQueue.size()) + " orange(s) from squeeze queue with incorrect state(s).");
        }

        // Remove oranges from bottle queue if state isn't squeezed
        final int bottleQueueSize = bottleQueue.size();
        removedFromQueue = bottleQueue.removeIf(orange -> orange.getState() != Orange.State.Squeezed);
        if (removedFromQueue) {
            orangesRemovedFromQueues += (bottleQueueSize - bottleQueue.size());
            System.err.println("Removed " + (bottleQueueSize - bottleQueue.size()) + " orange(s) from bottle queue with incorrect state(s).");
        }

        // Remove oranges from done queue if state isn't bottled
        final int qSize = doneQueue.size();
        final boolean removedFromDoneQ = doneQueue.removeIf(orange -> orange.getState() != Orange.State.Bottled);
        if (removedFromDoneQ) {
            orangesRemovedFromQueues += doneQueue.size() - qSize;
            System.err.println("Removed Oranges from doneQueue that didn't have state of bottled.");
        }
    }

    /**
     * Offers an orange to the peelQueue, if full, it will wait 100 milliseconds to add one.
     *
     * @param o Orange to process
     * @return {@code true} if the orange was distributed to the peelQueue, {@code false} otherwise.
     */
    private boolean distributeOrange(Orange o) {
        // Make sure orange is in correct state (Fetched)
        if (o.getState() != Orange.State.Fetched) {
            System.err.println("Orange state supposed to be fetched, got '" + o.getState() + "' during distributing! Not adding to peelQueue.");
        } else {
            try {
                // Add orange to queue. If full, it will wait until space becomes available.
                peelQueue.put(o);
                return true;
            } catch (InterruptedException e) {
                System.err.println(Thread.currentThread().getName() + " stop malfunction while attempting to add orange to queue");
            }
        }
        return false;
    }

    /**
     * Gets the number of oranges provided to workers.
     *
     * @return The number of oranges provided.
     */
    public int getOrangesProvided() {
        return orangesProvided;
    }

    /**
     * Gets the number of oranges bottled, which is the size of the doneQueue (
     * Oranges fully processed) divided by the
     * constant {@link #ORANGES_PER_BOTTLE}.
     *
     * @return Number of oranges bottled.
     */
    public int getOrangesBottled() {
        return doneQueue.size() / ORANGES_PER_BOTTLE;
    }

    /**
     * Gets the number of oranges processed, which is the length of the doneQueue.
     *
     * @return Number of oranges processed.
     */
    public int getOrangesProcessed() {
        return doneQueue.size();
    }

    /**
     * Gets the number of oranges not bottled, which is the length of the doneQueue mod {@link #ORANGES_PER_BOTTLE}.
     *
     * @return number of oranges not bottled.
     */
    public int getOrangesNotBottled() {
        return doneQueue.size() % ORANGES_PER_BOTTLE;
    }

    /**
     * Gets the number of oranges wasted, which is the number of oranges not bottled and the number of oranges left in queues
     * other than the doneQueue.
     *
     * @return The number of oranges wasted.
     */
    public int getOrangesWasted() {
        return doneQueue.size() % ORANGES_PER_BOTTLE + getOrangesLeftInQueue() + getOrangesRemovedFromQueues();
    }

    /**
     * Gets the number of oranges left in all queues besides the doneQueue.
     *
     * @return The number of oranges left in queues.
     */
    public int getOrangesLeftInQueue() {
        return peelQueue.size() + squeezeQueue.size() + bottleQueue.size();
    }

    /**
     * Gets how many oranges were removed from queues because they were in the wrong one.
     *
     * @return How many oranges were removed from queues because they were in the wrong one.
     */
    public int getOrangesRemovedFromQueues() {
        return orangesRemovedFromQueues;
    }
}
