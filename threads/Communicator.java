package nachos.threads;

import nachos.machine.*;
import java.util.*; 

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    public Communicator() {
    	numSpeaker = 0;
    	numListener = 0;
    	buffer = new LinkedList<Integer>();
    	lck = new Lock();
    	listener = new Condition(lck);
    	speaker = new Condition(lck);
    	finished = false;
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param	word	the integer to transfer.
     */
    
    public void speak(int word) {
    	lck.acquire();
    	buffer.add((Integer) word);
    	numSpeaker++;
    	while (numListener == 0){
    		finished = false;
    		speaker.sleep();
    	}
    	listener.wake(); //wake up remaining listener 
    	while (!finished){
    		speaker.sleep();
    	}
    	numListener--;
    	finished = false;
    	lck.release();
    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return	the integer transferred.
     */    
    public int listen() {
    	lck.acquire();
    	numListener++;
    	while (numSpeaker == 0 && buffer.isEmpty()){
    		listener.sleep();
    	}
    	speaker.wake();
    	numSpeaker--;
    	finished = true;
    	lck.release();
    	return ((int) buffer.removeFirst());
    }
    
    private int numSpeaker;
    private int numListener;
    private LinkedList<Integer> buffer;
    private Condition speaker;
    private Condition listener;
    private Lock lck;
    private boolean finished;
}
