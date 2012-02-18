package nachos.threads;
import nachos.ag.BoatGrader;
import java.util.LinkedList;

import nachos.machine.*;


public class Boat
{
	static BoatGrader bg;

	public static void selfTest()
	{
		BoatGrader b = new BoatGrader();

		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		//  	begin(1, 2, b);

		//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		//  	begin(3, 3, b);
	}

	public static void begin( int adults, int children, BoatGrader b )
	{
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;

		// Instantiate global variables here
		int totalChildren = children;  //only accessible in begin and isFinished()
		int totalAdults = adults;      //only accessible in begin and isFinished()

		// Condition variables for problem, children and adults
		lck = new Lock();
		adultConditionVar = new Condition(lck);
		childConditionVar = new Condition(lck);
		boatProblem = new Condition(lck);

		// boat starts on Oahu
		boatLocation = "Oahu";

		// No one on boat in the beginning
		waitingChild = new LinkedList<KThread>();

		// No one has been to Molokai yet
		reportedChildrenOnMolokai = 0;
		reportedAdultsOnMolokai = 0;
		// Everyone starts on Oahu //
		reportedChildrenOnOahu = children; //can this be globle?
		reportedAdultsOnOahu = adults;		//can this be globle?

		// Create threads here. See section 3.4 of the Nachos for Java
		// Walkthrough linked from the projects page.
		for(int i=0; i<adults; i++){
			Runnable r = new Runnable() {
				public void run() {
					AdultItinerary();
				}
			};
			KThread t = new KThread(r);
			t.setName("Adult Thread on Oahu");
			t.fork(); 	//place the thread to readyQueue,
			//but running thread doesn't have to yield
		}

		for(int j=0; j<children; j++){
			Runnable r = new Runnable() {
				public void run() {
					ChildItinerary();
				}
			};
			KThread t = new KThread(r);
			t.setName("Child Thread on Oahu");
			t.fork(); //place the thread to readyQueue;
		}

		done = false;

		lck.acquire();
		while(!isFinished(totalChildren, totalAdults)){ 
			childConditionVar.wake();
			adultConditionVar.wake();
			boatProblem.sleep();
		}
		done = true; 
		lck.release();

	}

	static void AdultItinerary()
	{
		/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
		 */  	
		lck.acquire();
		while (adultCase() == 0){
			boatProblem.wake();  		//to check if finished
			childConditionVar.wake();  	//wake up other threads
			adultConditionVar.wake(); 
			adultConditionVar.sleep(); //put running adult to waiting queue
		}
		// case = 1
		adultRun(boatLocation); 	 //run, update stuffs
		boatProblem.wake();   		// check if finished
		childConditionVar.wake(); 	//attemp to wake up a child on Molokai
		adultConditionVar.sleep();
		lck.release();
	}

	static void ChildItinerary()
	{
		lck.acquire();
		while (!done){
			while (childCase() == 0){
				boatProblem.wake(); 		//to check if finished
				adultConditionVar.wake(); //wake up adult
				childConditionVar.wake(); //wake up a child 
				childConditionVar.sleep(); // put running child to waiting queue
			}	
			// case = 1
			childRun(boatLocation); 	//run, update stuffs
			boatProblem.wake();			//check if finished
			childConditionVar.wake();	//wake up other threads
			adultConditionVar.wake();
			childConditionVar.sleep();
		}
		lck.release();
	}

	public static void adultRun(String location){
		if (location.equals("Oahu")){
			bg.AdultRowToMolokai();
			// update transferable message
			reportedAdultsOnMolokai++;
			reportedAdultsOnOahu--;
			// update location
			boatLocation = "Molokai"; 
			// update thread location
			KThread.currentThread().setName("Adult Thread on Molokai");
		}else{
			System.out.println("Adult should never leave Molokai. Error in adultCase");
		}
	}

	public static void childRun(String location){
		if (location.equals("Oahu")){
			if (waitingChild.isEmpty()){ 	//this is first child on boat
				bg.ChildRowToMolokai();		// as pilot
				reportedChildrenOnOahu--; 	// child goes from Oahu to Boat
				if (reportedChildrenOnOahu == 0){ //No more children on Oahu
					boatLocation = "Molokai"; //child leaves
					KThread.currentThread().setName("Child Thread on Molokai");
					reportedChildrenOnMolokai++;
				}else{ //else wait for next child to get on boat
					KThread.currentThread().setName("Child Thread on Boat");
					waitingChild.add(KThread.currentThread());
					//boatLocation is still Oahu
				}
			}else{ //Second child on boat
				bg.ChildRideToMolokai(); 	//as passenger
				reportedChildrenOnOahu--; 	//child goes from Oahu to Boat
				boatLocation = "Molokai"; 	//leaving Oahu to Molokai
				KThread.currentThread().setName("Child Thread on Molokai");
				KThread firstChild = waitingChild.removeFirst();
				firstChild.setName("Child Thread on Molokai");
				reportedChildrenOnMolokai = reportedChildrenOnMolokai + 2; //update for both
			}
		}else{ // location == Molokai; only take 1 child back to Oahu
			if ((reportedChildrenOnOahu > 0) || (reportedAdultsOnOahu > 0)){ //not done yet
				bg.ChildRowToOahu();
				reportedChildrenOnMolokai--;
				reportedChildrenOnOahu++;
				KThread.currentThread().setName("Child Thread on Oahu");
				boatLocation = "Oahu";
			}
		}
	}

	// return 1 => Go
	// return 0 => wait
	public static int adultCase(){
		if( boatLocation.equals("Oahu") ){
			if( KThread.currentThread().getName().equals("Adult Thread on Oahu") &&
					reportedChildrenOnMolokai > 0 && waitingChild.isEmpty())
			{
				//boat on Oahu, adult on Oahu, 1+ child on Molokai, no child on boat => good to go
				return 1;
			}else{
				// boat on Oahu, adult NOT on Oahu or no children on Molokai => wait
				return 0;
			}
		}else{
			// boat is on Molokai => Adult never leaves Molokai
			return 0;
		}
	}

	// return 1 => Go
	// return 0 => wait
	public static int childCase(){
		if( boatLocation.equals("Oahu") ){
			if( KThread.currentThread().getName().equals("Child Thread on Oahu")
					&& waitingChild.size() < 2){
				//boat on Oahu, child on Oahu, boat is not full => Good to go
				return 1;
			}else{
				//Child is on other bank or boat is full => can't get on boat
				return 0;
			}
		}else{
			if( KThread.currentThread().getName().equals("Child Thread on Molokai") 
					&& (reportedChildrenOnOahu > 0 || reportedAdultsOnOahu > 0 )
					&& waitingChild.size() == 0){
				// boat on Molokai, Child on Molokai, still people on Oahu => go get them
				return 1;
			}else{
				//child not on Molokai or finsihed
				return 0; 
			}
		}
	}


	public static boolean isFinished(int totalChildren, int totalAdults){
		if (boatLocation.equals("Molokai") && 
				reportedAdultsOnMolokai == totalAdults &&
				reportedChildrenOnMolokai == totalChildren){
			return true;
		}else{
			return false;
		}
	}

	static void SampleItinerary()
	{
		// Please note that this isn't a valid solution (you can't fit
		// all of them on the boat). Please also note that you may not
		// have a single thread calculate a solution and then just play
		// it back at the autograder -- you will be caught.
		System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
		bg.AdultRowToMolokai();
		bg.ChildRideToMolokai();
		bg.AdultRideToMolokai();
		bg.ChildRideToMolokai();
	}

	//"Globle" variables 
	private static Condition adultConditionVar;
	private static Condition childConditionVar;
	private static Condition boatProblem;
	private static Lock lck;
	private static String boatLocation; 
	private static int reportedChildrenOnMolokai;
	private static int reportedAdultsOnMolokai;
	private static int reportedChildrenOnOahu;
	private static int reportedAdultsOnOahu;
	private static LinkedList<KThread> waitingChild;
	private static boolean done;
}
