import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Plant implements Runnable {
    // How long do we want to run the juice processing
    public static final long PROCESSING_TIME = 5 * 1000;

    private static final int NUM_PLANTS = 2;

    private static final int NUM_PEELERS = 6;
    private static final int NUM_SQUEEZERS = 4;
    private static final int NUM_BOTTLERS = 3;
    private static final int TOTAL_WORKERS = NUM_PEELERS + NUM_SQUEEZERS + NUM_BOTTLERS;



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
        long sleepTime = Math.max(1, time);
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            System.err.println(errMsg);
        }
    }

    public final int ORANGES_PER_BOTTLE = 3;

    private final Thread thread;
    private int orangesProvided;
    private volatile boolean timeToWork;

    private final BlockingQueue<Orange> peelQueue;
    private final BlockingQueue<Orange> squeezeQueue;
    private final BlockingQueue<Orange> bottleQueue;
    private final BlockingQueue<Orange> doneQueue;


    private final Worker[] workers;

    Plant(int threadNum) {
        orangesProvided = 0;
        peelQueue = new LinkedBlockingQueue<>(10);
        squeezeQueue = new LinkedBlockingQueue<>(10);
        bottleQueue = new LinkedBlockingQueue<>(10);
        doneQueue = new LinkedBlockingQueue<>();
        thread = new Thread(this, "Plant[" + threadNum + "]");

        workers = new Worker[TOTAL_WORKERS];

        int ind = 0;
        for (int i = 0; i < NUM_PEELERS; i++) {
            workers[ind] = new Worker(ind+1,peelQueue,squeezeQueue,Orange.State.Peeled);
            ind++;
        }

        for (int i = 0; i < NUM_SQUEEZERS; i++) {
            workers[ind] = new Worker(ind+1,squeezeQueue,bottleQueue,Orange.State.Squeezed);
            ind++;
        }

        for (int i = 0; i < NUM_BOTTLERS; i++) {
            workers[ind] = new Worker(ind+1,bottleQueue,doneQueue,Orange.State.Bottled);
            ind++;
        }
    }

    /** Sets timeToWork to true, starts thread */
    public void startPlant() {
        timeToWork = true;
        thread.start();

        for (Worker w : workers) {
            w.start();
        }
    }

    /** Sets timeToWork to false */
    public void stopPlant() {
        timeToWork = false;

        for (Worker w : workers) {
            w.stop();
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
        }
        System.out.println(Thread.currentThread().getName() + " Done");
    }

    /**
     * Processes an entire orange. Until the orange is bottled, this
     * process will run.
     * @param o Orange to process
     */
    public void peelOrange(Orange o) {
        try{
            final boolean orangeAdded = peelQueue.offer(o,100, TimeUnit.MILLISECONDS);
            if(!orangeAdded) {
                System.out.println(Thread.currentThread().getName() + " couldn't add an orange because the queue is full.");
            }
        }catch(InterruptedException e){
            System.out.println(Thread.currentThread().getName() + " stop malfunction while attempting to add orange to queue");
        }
    }

    public int getOrangesProvided() {
        return orangesProvided;
    }

    public int getOrangesBottled(){
        return doneQueue.size() / ORANGES_PER_BOTTLE;
    }

    public int getOrangesProcessed(){
        return doneQueue.size();
    }

    public int getOrangesNotBottled() {
        return doneQueue.size() % ORANGES_PER_BOTTLE;
    }

    public int getOrangesWasted(){
        return doneQueue.size() % ORANGES_PER_BOTTLE + getOrangesLeftInQueue();
    }

    public int getOrangesLeftInQueue(){
        return peelQueue.size() + squeezeQueue.size() + bottleQueue.size();
    }
}
