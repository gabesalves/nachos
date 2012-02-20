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
		lck = new Lock();
		listenerWaitingQueue = new Condition(lck);
		speakerWaitingQueue = new Condition(lck);
		listenerRecieving = new Condition(lck);
		speakerSending = new Condition(lck);
		listenerWaiting = false;
		speakerWaiting = false;
		recieved = false;
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
		while(speakerWaiting){ //there was a speaker before this one
			speakerWaitingQueue.sleep(); //put this speaker to a waiting queue
		}

		speakerWaiting = true;
		// Until speakerWaiting is set to false, the process below is inaccessable to other speakers//

		holder = word;
		while(!listenerWaiting || !recieved){ //no listener waiting or no listener have recieved
			listenerRecieving.wake(); //wake up a potential partner
			speakerSending.sleep(); //put this speaker to the sending queue
		}

		// at this point a listener has recieved the word and finished...like a boss
		// speaker stays and clean up...listener's mess
		listenerWaiting = false; //set it to false so other listener can get to the recievingQueue
		speakerWaiting = false; //set it to false so other speaker can get to the sendingQueue
		recieved = false;
		speakerWaitingQueue.wake(); //wake up a waiting speaker
		listenerWaitingQueue.wake();
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
		while(listenerWaiting){
			listenerWaitingQueue.sleep();
		}

		listenerWaiting = true;
		// Until listenerWaiting is set to false, the process below is inaccessable to other listners//	

		while(!speakerWaiting){ //no speaker, go into loop
			listenerRecieving.sleep(); //set this thread to be the first thread to recieve a message
		}

		//There is 1 speaker Sending message
		speakerSending.wake(); // wake up the sleeping speaker in sendingQueue
		recieved = true;
		lck.release();
		return holder;
	}


	private Condition speakerWaitingQueue; //queue of speakers that are waiting for listeners
	private Condition speakerSending; //queue of speaker that just sent out a word (should have length 1)
	private Condition listenerWaitingQueue; //queue of listeners that are waiting for speakers
	private Condition listenerRecieving;	//queue of listeners that are waiting for a word (should have length 1)
	private Lock lck;
	private boolean listenerWaiting; //boolean indicating if a listener is waiting
	private boolean speakerWaiting; //boolean indicating if a speaker is waiting
	private boolean recieved; //boolean indicating if a word has been recieved
	private int holder;

}
