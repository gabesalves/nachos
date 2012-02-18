package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see	nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param	conditionLock	the lock associated with this condition
     *				variable. The current thread must hold this
     *				lock whenever it uses <tt>sleep()</tt>,
     *				<tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
    	this.conditionLock = conditionLock;
    	//this.threadCount = 0;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	
	conditionLock.release();
	boolean intStatus = Machine.interrupt().disable();
	//waitQueue.waitForAccess(KThread.currentThread()); //to waiting queue
	waitQueue.add(KThread.currentThread());
	KThread.sleep(); // atomically go to sleep
	//threadCount++;
	Machine.interrupt().restore(intStatus);
	conditionLock.acquire();
    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	//if (threadCount > 0){
	if (!waitQueue.isEmpty()){
		boolean intStatus = Machine.interrupt().disable();
		KThread thread = waitQueue.removeFirst();
		if (thread != null){
			thread.ready();
		}
		//threadCount--; 
		Machine.interrupt().restore(intStatus);
	}
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
	Lib.assertTrue(conditionLock.isHeldByCurrentThread());
	while(!waitQueue.isEmpty()){
		wake();
	}
    }
    
    private Lock conditionLock;
    // globle
    //private ThreadQueue waitQueue = ThreadedKernel.scheduler.newThreadQueue(false);
    private LinkedList<KThread> waitQueue = new LinkedList<KThread>();
    //private int threadCount; 

}
