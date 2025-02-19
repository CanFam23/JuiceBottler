import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Plant implements Runnable {
    // How long do we want to run the juice processing
    public static final long PROCESSING_TIME = 5 * 1000;

    private static final int NUM_PLANTS = 2;

    private static final int NUM_WORKERS = 2;

    public static void main(String[] args) {
        // Startup the plants
        Plant[] plants = new Plant[NUM_PLANTS];
        for (int i = 0; i < NUM_PLANTS; i++) {
            plants[i] = new Plant(i+1);
            plants[i].startPlant();
        }

        // Give the plants time to do work
        delay(PROCESSING_TIME, "Plant malfunction");

        // Stop the plant, and wait for it to shutdown
        for (Plant p : plants) {
            p.stopPlant();
        }

        for (Plant p : plants) {
            p.waitToStop();
        }

        // Summarize the results
        int totalProvided = 0;
        int totalBottled = 0;
        int totalBottles = 0;
        int totalWasted = 0;
//        for (Plant p : plants) {
//            totalProvided += p.getProvidedOranges();
//            totalProcessed += p.getProcessedOranges();
//            totalBottles += p.getBottles();
//            totalWasted += p.getWaste();
//        }
        System.out.println("Total provided/processed = " + totalProvided + "/" + totalBottled);
        System.out.println("Created " + totalBottles +
                           ", wasted " + totalWasted + " oranges");
    }

    /**
     * Gives the other plants time to do work by making this thread sleep for given time
     * @param time Time to sleep for
     * @param errMsg Error message to show if an error occurs
     */
    private static void delay(long time, String errMsg) {
        long sleepTime = Math.max(1, time);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            System.err.println(errMsg);
        }
    }

    public final int ORANGES_PER_BOTTLE = 3;

    private final Thread thread;
    private int orangesPeeled;
    private int orangesProvided;
    private volatile boolean timeToWork;

    private final BlockingQueue<Orange> orangesQueue;

    private final Worker[] workers;

    Plant(int threadNum) {
        orangesPeeled = 0;
        orangesProvided = 0;
        orangesQueue = new LinkedBlockingQueue<>(10);
        thread = new Thread(this, "Plant[" + threadNum + "]");

        workers = new Worker[NUM_WORKERS];
        for (int i = 0; i < NUM_WORKERS; i++) {
            workers[i] = new Worker(i+1,orangesQueue);
        }
    }

    /** Sets timeToWork to true, starts thread */
    public void startPlant() {
        timeToWork = true;
        thread.start();

        for (Worker w : workers) {
            w.startWorker();
        }
    }

    /** Sets timeToWork to false */
    public void stopPlant() {
        timeToWork = false;

        for (Worker w : workers) {
            w.stopWorker();
        }
    }

    /** Waits for thread to stop <br>
     * From <a href="https://stackoverflow.com/questions/53405013/how-does-thread-join-work-conceptually">stack overflow</a> : <br>
     * The classic implementation of Thread.join
     * (other implementations are possible, is to lock
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
     * to process oranges.
     */
    public void run() {
        System.out.println(Thread.currentThread().getName() + " Processing oranges");
        while (timeToWork) {
            peelOrange(new Orange());
            orangesProvided++;
            System.out.print(".");
        }
        System.out.println("");
        System.out.println(Thread.currentThread().getName() + " Done");
    }

    /**
     * Processes an entire orange. Until the orange is bottled, this
     * process will run.
     * @param o Orange to process
     */
    public void peelOrange(Orange o) {
        while (o.getState() != Orange.State.Peeled) {
            o.runProcess();
        }
        try{
            final boolean orangeAdded = orangesQueue.offer(o,100, TimeUnit.MILLISECONDS);
            if(!orangeAdded) {
                System.out.println(Thread.currentThread().getName() + " couldn't add an orange because the queue is full.");
            }
        }catch(InterruptedException e){
            System.out.println(Thread.currentThread().getName() + " stop malfunction while attempting to add orange to queue");
        }
        orangesPeeled++;
    }

    public int getOrangesPeeled() {
        return orangesPeeled;
    }

    public int getOrangesProvided() {
        return orangesProvided;
    }
}
