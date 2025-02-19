import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Worker implements Runnable{

    private static final int ORANGES_PER_BOTTLE = 3;

    private final Thread thread;
    private int orangesProvided;
    private int orangesProcessed;
    private volatile boolean timeToWork;

    private final BlockingQueue<Orange> orangesQueue;

    public Worker(int threadNum,BlockingQueue<Orange> orangesQueue){
        orangesProvided = 0;
        orangesProcessed = 0;
        this.orangesQueue = orangesQueue;
        this.thread = new Thread(this,"Worker["+threadNum+"]");
    }

    public void startWorker(){
        timeToWork = true;
        thread.start();
    }

    public void stopWorker(){
        timeToWork = false;
    }

    /**
     * Runs this operation.
     */
    @Override
    public void run() {
     System.out.println(Thread.currentThread().getName()+" is working.");
     while(timeToWork){
         try{
//             Orange o = orangesQueue.take();
             Orange o = orangesQueue.poll(100, TimeUnit.MILLISECONDS);
             if(o != null){
                 bottleOrange(o);
                 orangesProvided++;
             }
         } catch(InterruptedException e){
             System.out.println(Thread.currentThread().getName()+" interrupted when waiting to get orange from queue.");
         }
     }
     System.out.println();
     System.out.println(Thread.currentThread().getName()+" has finished working.");
    }

    public void bottleOrange(Orange o){
        while (o.getState() != Orange.State.Bottled) {
            o.runProcess();
        }
//        System.out.println("Orange bottled, queue size is now: " + orangesQueue.size());
        orangesProcessed++;
    }

    public void waitToStop() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            System.err.println(thread.getName() + " stop malfunction");
        }
    }

    /** Gets the number of oranges provided */
    public int getProvidedOranges() {
        return orangesProvided;
    }

    /** Gets the number of oranges processed */
    public int getProcessedOranges() {
        return orangesProcessed;
    }

    /**
     * Gets the number of bottles made
     * @return the number of bottles made
     */
    public int getBottles() {
        return getProcessedOranges() / ORANGES_PER_BOTTLE;
    }

    /**
     * Gets the waste (Leftover processed oranges(
     * @return The number of wasted oranges
     */
    public int getWaste() {
        return getProcessedOranges() % ORANGES_PER_BOTTLE;
    }
}
