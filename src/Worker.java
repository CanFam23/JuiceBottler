import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class Worker implements Runnable{

    private static final int ORANGES_PER_BOTTLE = 3;

    private final Thread thread;
    private volatile boolean timeToWork;

    private final BlockingQueue<Orange> takeQueue;
    private final BlockingQueue<Orange> giveQueue;

    private Orange.State job;

    public Worker(int threadNum, BlockingQueue<Orange> takeQueue,BlockingQueue<Orange> giveQueue, Orange.State job){
        this.takeQueue = takeQueue;
        this.giveQueue = giveQueue;
        this.thread = new Thread(this,"Worker["+threadNum+"]");
        this.job = job;
    }

    public void start(){
        timeToWork = true;
        thread.start();
    }

    public void stop(){
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
             Orange o = takeQueue.poll(100, TimeUnit.MILLISECONDS);
             if(o != null){
                 bottleOrange(o);
                 giveQueue.offer(o,100,TimeUnit.MILLISECONDS);
             }
         } catch(InterruptedException e){
             System.out.println(Thread.currentThread().getName()+" interrupted when waiting to get orange from queue.");
         }
     }
     System.out.println();
     System.out.println(Thread.currentThread().getName()+" has finished working.");
    }

    public void bottleOrange(Orange o){
        while (o.getState() != job) {
            o.runProcess();
        }
//        System.out.println("Orange bottled, queue size is now: " + orangesQueue.size());
    }

    public void waitToStop() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            System.err.println(thread.getName() + " stop malfunction");
        }
    }
}
