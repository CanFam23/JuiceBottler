import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The {@code Worker} class is meant to be spawned from the {@link Plant} class. A worker will have a {@link #job}, which
 * represents when to stop doing work on an {@link Orange}. The worker will get oranges to do work on from the {@link #takeQueue},
 * and then add the oranges to the {@link #giveQueue} when the worker is done doing its job on it.
 */
public class Worker implements Runnable {
    /** Max amount of time a worker will wait to get/add an orange from/to a queue. */
    private static final int MAX_TIMEOUT_TIME_MILLIS = 100;

    /** Thread for the worker. */
    private final Thread thread;

    /** When true, the worker should do work. */
    private volatile boolean timeToWork;

    /** Queue to get oranges from to process them. */
    private final BlockingQueue<Orange> takeQueue;

    /** Queue to add oranges to after the workers job is complete. */
    private final BlockingQueue<Orange> giveQueue;

    /** {@link Orange.State} object which represents when the worker should stop processing an orange. */
    private final Orange.State job;

    /**
     * Creates a new Worker object.
     *
     * @param plantNum  Number of plant this worker is working in, used to name worker thread.
     * @param threadNum Number of thread/worker.
     * @param takeQueue Queue to take oranges from.
     * @param giveQueue Queue to add oranges to after the workers job is complete
     * @param job       The worker will process an orange until it's {@link Orange.State} equals job.
     */
    public Worker(int plantNum, int threadNum, BlockingQueue<Orange> takeQueue, BlockingQueue<Orange> giveQueue, Orange.State job) {
        this.takeQueue = takeQueue;
        this.giveQueue = giveQueue;
        this.thread = new Thread(this, "Worker[" + plantNum + "." + threadNum + "]");
        this.job = job;
    }

    /** Starts thread by setting {@link #timeToWork} to true and calling {@link Thread#start()}. */
    public void start() {
        timeToWork = true;
        thread.start();
    }

    /**
     * Stops thread from working by setting {@link #timeToWork} to false,
     * but doesn't stop thread from running. (To stop thread from running, call {@link #waitToStop()})
     */
    public void stop() {
        timeToWork = false;
    }

    /**
     * Runs this thread. While {@link #timeToWork} is true, the thread will repeatedly take oranges from it's {@link #takeQueue},
     * process the {@link Orange} until the threads given {@link #job} is done, and then pass the orange off to it's {@link #giveQueue}.
     */
    @Override
    public void run() {
        while (timeToWork) {
            try {
                // Attempt to get an orange from the takeQueue, waits up to 100 milliseconds if none available
                final Orange o = takeQueue.poll(MAX_TIMEOUT_TIME_MILLIS, TimeUnit.MILLISECONDS);
                if (o != null) {
                    processOrange(o);

                    // Put an orange in the giveQueue, waits if queue is full
                    giveQueue.put(o);
                }
            } catch (InterruptedException e) {
                System.err.println(Thread.currentThread().getName() + " interrupted when waiting to get orange from queue.");
            }
        }
    }

    /**
     * Runs {@link Orange#runProcess()} on given orange until this thread has done it's {@link #job}.
     *
     * @param o Orange to run process on.
     */
    private void processOrange(Orange o) {
        while (o.getState() != job) {
            o.runProcess();
        }
    }

    /** Waits for thread to stop by calling {@link Thread#join()}. */
    public void waitToStop() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            System.err.println(thread.getName() + " stop malfunction");
        }
    }
}
