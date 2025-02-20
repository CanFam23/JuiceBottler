import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The Plant class represents a processing plant in which workers bottle oranges. Using several threads and different queues,
 * the plant has different {@link Worker} objects who do one of three tasks:
 * <br> 1. Peel oranges <br>
 * 2. Squeeze oranges <br>
 * 3. Bottle oranges <br>
 * The workers receive and pass on oranges using {@link LinkedBlockingQueue}.
 */
public class Plant implements Runnable {
    /** How long do we want to run the juice processing. */
    public static final long PROCESSING_TIME = 5 * 1000;

    /** Number of oranges put in each bottle. */
    public static final int ORANGES_PER_BOTTLE = 3;

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

    /** Max amount of time a plant will wait to add orange to a queue. */
    private static final int MAX_TIMEOUT_TIME_MILLIS = 100;

    /** Main method, creates plants and starts them, the gives them time to work before stopping them and gathering data. */
    public static void main(String[] args) {
        // Startup the plants
        final Plant[] plants = new Plant[NUM_PLANTS];
        for (int i = 0; i < NUM_PLANTS; i++) {
            plants[i] = new Plant(i+1);
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
        for (Plant p : plants) {
            totalProvided += p.getOrangesProvided();
            totalProcessed += p.getOrangesProcessed();
            totalBottled += p.getOrangesBottled();
            totalWasted += p.getOrangesWasted();
            totalLeftInQueue += p.getOrangesLeftInQueue();
            totalNotBottled += p.getOrangesNotBottled();
        }
        System.out.println("Total provided/processed = " + totalProvided + "/" + totalProcessed);
        System.out.println("Total left in queues = " + totalLeftInQueue);
        System.out.println("Total leftover after bottling oranges = " + totalNotBottled);
        System.out.println("Created " + totalBottled +
                           ", wasted " + totalWasted + " oranges");
    }

    /**
     * Gives the other plants time to do work by making this thread sleep for given time
     * @param time Time to sleep for
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
     * @param threadNum Number of this thread
     */
    Plant(int threadNum) {
        peelQueue = new LinkedBlockingQueue<>(10);
        squeezeQueue = new LinkedBlockingQueue<>(10);
        bottleQueue = new LinkedBlockingQueue<>(10);
        doneQueue = new LinkedBlockingQueue<>();

        thread = new Thread(this, "Plant[" + threadNum + "]");

        workers = new Worker[TOTAL_WORKERS];

        orangesProvided = 0;

        // Create given amount of each worker and add them to the workers array
        int ind = 0;
        for (int i = 0; i < NUM_PEELERS; i++) {
            workers[ind] = new Worker(threadNum,+1,peelQueue,squeezeQueue,Orange.State.Peeled);
            ind++;
        }

        for (int i = 0; i < NUM_SQUEEZERS; i++) {
            workers[ind] = new Worker(threadNum,ind+1,squeezeQueue,bottleQueue,Orange.State.Squeezed);
            ind++;
        }

        for (int i = 0; i < NUM_BOTTLERS; i++) {
            workers[ind] = new Worker(threadNum,ind+1,bottleQueue,doneQueue,Orange.State.Bottled);
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

    /** Waits for thread to stop. Calls waitToStop on each worker before waiting to stop itself. <br>
     * From <a href="https://stackoverflow.com/questions/53405013/how-does-thread-join-work-conceptually">stack overflow</a> : <br>
     * The classic implementation of Thread.join is to lock
     * the Thread object, test to see if is alive and
     * if not wait on the Thread object. As a thread exits,
     * it locks its instance and calls notifyAll.
     * */
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
            distributeOrange(new Orange());
            orangesProvided++;
            System.out.print(".");
        }
        System.out.println(" ");
        System.out.println(Thread.currentThread().getName() + " Done");
    }

    /**
     * Offers an orange to the peelQueue, if full, it will wait 100 milliseconds to add one.
     * @param o Orange to process
     */
    public void distributeOrange(Orange o) {
        try{
            // Add orange to queue if it's not full, else wait 100 milliseconds to see if a space is freed
            final boolean orangeAdded = peelQueue.offer(o,MAX_TIMEOUT_TIME_MILLIS, TimeUnit.MILLISECONDS);
            if(!orangeAdded) {
                System.out.println(Thread.currentThread().getName() + " couldn't add an orange because the queue is full.");
            }
        }catch(InterruptedException e){
            System.out.println(Thread.currentThread().getName() + " stop malfunction while attempting to add orange to queue");
        }
    }

    /**
     * Gets the number of oranges provided to workers.
     * @return The number of oranges provided.
     */
    public int getOrangesProvided() {
        return orangesProvided;
    }

    /** Gets the number of oranges bottled, which is the size of the doneQueue (
     * Oranges fully processed) divided by the
     * constant {@link #ORANGES_PER_BOTTLE}.
     * @return Number of oranges bottled.
     */
    public int getOrangesBottled(){
        return doneQueue.size() / ORANGES_PER_BOTTLE;
    }

    /**
     * Gets the number of oranges processed, which is the length of the doneQueue.
     * @return Number of oranges processed.
     */
    public int getOrangesProcessed(){
        return doneQueue.size();
    }

    /**
     * Gets the number of oranges not bottled, which is the length of the doneQueue mod {@link #ORANGES_PER_BOTTLE}.
     * @return number of oranges not bottled.
     */
    public int getOrangesNotBottled() {
        return doneQueue.size() % ORANGES_PER_BOTTLE;
    }

    /**
     * Gets the number of oranges wasted, which is the number of oranges not bottled and the number of oranges left in queues
     * other than the doneQueue.
     * @return The number of oranges wasted.
     */
    public int getOrangesWasted(){
        return doneQueue.size() % ORANGES_PER_BOTTLE + getOrangesLeftInQueue();
    }

    /**
     * Gets the number of oranges left in all queues besides the doneQueue.
     * @return The number of oranges left in queues.
     */
    public int getOrangesLeftInQueue(){
        return peelQueue.size() + squeezeQueue.size() + bottleQueue.size();
    }
}
