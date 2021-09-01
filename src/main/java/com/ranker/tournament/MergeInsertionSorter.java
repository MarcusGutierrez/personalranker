package com.ranker.tournament;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

/**
 * Online Merge-Insertion sort, also known as Ford-Johnson algorithm. This is a turn-based version
 * of Merge-Insertion sort, keeping using an explicit stack, that is typically leveraged via recursion.
 * The algorithm takes an initially unsorted list of indices (0, 1, ... N-1) and starts in the SPLITTING
 * state.
 * 
 * In the SPLITTING state it pairs each element in the list up, then the preferred/winning elements in each duel
 * move on to the next layer, while the unpreferred/losing elements are set aside to be inserted later.
 * Each layer, cuts the list in half until all that is left is 1 element in the sortedList, then the algorithm
 * transitions to the MERGING state.
 * 
 * In the MERGING state, the algorithm will iteratively insert unsorted elements to the sorted list via binary
 * search for insertion.
 * 
 * Example:
 * 		Initial List:
 * 	g, b, e, f, a, c, d
 * 	
 * IN SPLITTING
 * 	1)
 * 	pairs:	g  e  a
 * 			|  |  |
 * 			b  f  c
 * duels:	(g vs b), (e vs f), (a vs c)
 * unpaired: d
 * 
 * to sort:		b  e  a
 * 				|  |  |
 * unsorted:	g  f  c  d
 * 
 * 2)
 * pairs:	b
 * 			|
 * 			e
 * duels:	(b vs e)
 * unpaired: a
 * 
 * to sort:		b
 * unsorted:	e  a
 * 
 * IN MERGING
 * 3)
 * 	i.	sorted:		b  e
 * 		unsorted:	a
 * 	
 * 	ii. sorted:		a  b  e
 * 
 * 4) pairs:	a  b  e
 * 				|  |  |
 * 			 d  c  g  f
 * 
 *  i.	sorted:		a  b  e  f
 *  	unsorted:	d  c  g
 *  
 *  ii. sorted:		a  b  c  e  f
 *  	unsorted:	d  g
 *  
 *  iii. sorted:	a  b  c  e  f  g
 *  	unsorted:	d
 *  
 *  iv.	sorted:		a  b  c  d  e  f  g
 *  
 *  COMPLETED
 *  
 *  final sort:		a  b  c  d  e  f  g
 * 
 * @author Marcus
 * @date 8/30/2021
 */
public class MergeInsertionSorter implements Serializable {

	private static final long serialVersionUID = 7835113064248175019L;

	/** modified Jacobsthal (A001045) sequence where each aspect is reduced by 1 to account for array indexing*/
	private final static int[] JACOBSTHAL = {0, 2, 4, 10, 20, 42, 84, 170, 340, 682, 1364, 2730};

	/** 
	 * Defines the current state of the MergeInsertionSort the state traversal is as follows:
	 * SPLITTING -> MERGING -> COMPLETED
	 */
	private State currentState;

	/** The final sorted list once sorting is complete (COMPLETED state only) */
	private ArrayList<Integer> finalSorting;

	/**
	 * The primary list in question. In the MERGING state, these are the higher ranked elements
	 * in each pair. Then, when merging, this ArrayList is the pre-sorted list that elements insert
	 * into.
	 * 
	 * (used in SPLITTING & MERGING states)
	 */
	private ArrayList<Integer> currentSortedArray;

	/**
	 * Items separated during SPLITTING at each layer. During the MERGING, these array lists
	 * are iteratively inserted to the pre-sorted `currentSortedArray`.
	 * 
	 * (used in SPLITTING & MERGING states)
	 */
	private Deque<ArrayList<Integer>> unsortedStack;
	private ArrayList<Integer> currentUnsortedArray;

	/**
	 * Reversible bidirectional mapping at each layer where each integer maps to its
	 * paired element. Any non-paired element in a layer (in the case of odd-sized lists) is
	 * paired with -1.
	 * 
	 * (used in SPLITTING & MERGING states)
	 */
	private Deque<HashMap<Integer, Integer>> pairingStack;
	private HashMap<Integer, Integer> currentPairing;

	/**
	 * Duels generated from the pairings in `currentPairing`. These duels are used to split apart
	 * `currentSortedArray`.
	 * 
	 * (SPLITTING state only)
	 */
	private Deque<Duel<Integer>> currentDuelQueue;
	private Duel<Integer> currentDuel;

	/**
	 * Ordering to insert an `currentUnsortedArray` into the `currentSortedArray` (MERGING state only)
	 * Uses the reverse Jacobsthal sequence (A001045 on OEIS) on `currentUnsortedArray` to form the 
	 * insertion queue. This sequence has been shown to reduce total needed comparisons.
	 */
	private Deque<Integer> insertionQueue;

	/** lower bound index for binary search insertion (MERGING state only) */
	private Integer currentInsertionElement;

	/** lower bound index for binary search insertion (MERGING state only) */
	private int lowerIdx;

	/** upper bound index for binary search insertion (MERGING state only) */
	private int upperIdx;

	/** middle index for binary search insertion (State.MERGING only) */
	private int middleIdx;


	/**
	 * The sole constructor. Initializes all the stacks and queues needed for the initial SPLITTING state.
	 * 
	 * @param N number of elements to sort
	 */
	public MergeInsertionSorter(int N) {
		if(N < 1)
			throw new IllegalArgumentException("Must enter N >= 1");

		currentState = State.SPLITTING;

		ArrayList<Integer> toSort = new ArrayList<Integer>();
		for(int i = 0; i < N; i++)
			toSort.add(i);

		unsortedStack = new LinkedList<ArrayList<Integer>>();
		pairingStack = new LinkedList<HashMap<Integer, Integer>>();

		currentPairing = generatePairMap(toSort);
		currentDuelQueue = getDuelsFromPairings(currentPairing);

		currentSortedArray = new ArrayList<Integer>();
		currentUnsortedArray = new ArrayList<Integer>();

		if(currentPairing.containsKey(-1))
			currentUnsortedArray.add(currentPairing.get(-1));

		currentDuel = currentDuelQueue.pollFirst();
	}


	/**
	 * Generates a paired integer map where each pair maps to its paired partner.
	 * In the case of an odd-sized array, the unpaired element at the end of the list and -1 map to each other.
	 * 
	 * @throws IllegalStateException when called while not in the initial SPLITTING state
	 * @param arr 
	 * @return an integer biMap where each key points to another integer key in the map, forming paired relationships
	 */
	private HashMap<Integer, Integer> generatePairMap(ArrayList<Integer> arr) {
		if(currentState != State.SPLITTING)
			throw new IllegalStateException("Can only generate pairMap while splitting");

		if(arr.size() == 0)
			return null;
		
		ArrayList<Integer> poissonQueue = new ArrayList<Integer>(arr);
		HashMap<Integer, Integer> pairingMap = new HashMap<Integer, Integer>();
		
		while(poissonQueue.size() > 1) {
			Integer pairElement1 = poissonQueue.get(0);
			int eleIdx2 = Integer.min(getPoisson(1), poissonQueue.size()-1);
			Integer pairElement2 = poissonQueue.get(eleIdx2);
			
			poissonQueue.remove(eleIdx2);
			poissonQueue.remove(0);
			
			pairingMap.put(pairElement1, pairElement2);
			pairingMap.put(pairElement2, pairElement1);
		}
		
		if(poissonQueue.size() == 1) {
			Integer last = poissonQueue.get(0);
			pairingMap.put(last, -1);
			pairingMap.put(-1, last);
		}
		
		return pairingMap;
	}


	/**
	 * Generates and returns a Duel<Integer> queue from a paired integer BiMap. Each duel inserted into the queue
	 * is created from the paired relationships in the pairing biMap. Any unpaired integer (that paired with -1)
	 * is not added to the duel queue.
	 * 
	 * @param pairing Integer BiMap of paired integers
	 * @return a queue of Duels generated from the pairs found in the pairing BiMap
	 */
	private Deque<Duel<Integer>> getDuelsFromPairings(HashMap<Integer, Integer> pairing){
		Deque<Duel<Integer>> duelQueue = new LinkedList<Duel<Integer>>();

		HashSet<Integer> inserted = new HashSet<Integer>();
		// Only add integers paired together, not the lone/odd 
		if(pairing.containsKey(-1)) {
			inserted.add(-1);
			inserted.add(pairing.get(-1));
		}
		LinkedList<Integer> keySet = new LinkedList<Integer>(pairing.keySet());
		Collections.shuffle(keySet);
		for(Integer element : keySet) {
			if(!inserted.contains(element) && !inserted.contains(pairing.get(element))) {
				Integer pairedElement = pairing.get(element);
				Duel<Integer> matchup = new Duel<Integer>(element, pairedElement);
				duelQueue.add(matchup);
				inserted.add(element);
				inserted.add(pairedElement);
			}
		}
		
		return duelQueue;
	}


	/**
	 * Returns current Duel needed to answered.
	 * 
	 * @throws IllegalStateException when sorting is complete and there are no more comparisons needed
	 * @return current needed comparison to progress with sorting
	 */
	public Duel<Integer> getCurrentComparison() {
		if(currentState == State.COMPLETED)
			throw new IllegalStateException("There are no more comparisons to be made");
		return currentDuel;
	}


	/**
	 * When declaring the winner of the current duel comparison, the sorter progresses in sorting based on its
	 * current state. The sorter starts by progressively splitting the array, then moves on to merging where the
	 * the split halves are iteratively stitched together to form sorted lists.
	 * 
	 * @throws IllegalStateException when sorting is complete and there is no currentDuel is address
	 * @param winner A or B response to the current Duel in question
	 */
	public void declareWinner(DuelWinner winner) {
		if(currentState == State.SPLITTING)
			split(winner);
		else if(currentState == State.MERGING)
			insert(winner);
		else
			throw new IllegalStateException("Sorting is finished. There are no winners to declare");
	}


	/**
	 * Uses duels to split the winner and loser into the "to be sorted" and "unsorted" piles respectively.
	 * Initially, half of the list is paired up (with any odd, unpaired item ending up in the 
	 * currentUnsortedArray). Once paired up, each pair is formed into a Duel<Integer> and thrown into
	 * the currentDuelQueue. Once every duel in the duel queue has been compared, the "to be sorted" pile
	 * represented by the currentSortedArray then becomes the next list to be paired and split up. This cycle
	 * ends when the currentSortedArray reduces to only a single element.
	 * 
	 * @throws IllegalStateException when currently not SPLITTING
	 * @param winner enum of the which element won in currentDuel. If winner == DuelWinner.A, currentDuel.getA()
	 */
	private void split(DuelWinner winner) {
		String error = "Must be in SPLITTING state to call split(). Check if COMPLETED or if in MERGING.";
		if(currentState != State.SPLITTING)
			throw new IllegalStateException(error);

		int winnerIdx = (winner == DuelWinner.A) ? currentDuel.getA() : currentDuel.getB();
		int loserIdx = (winner == DuelWinner.A) ? currentDuel.getB() : currentDuel.getA();

		currentSortedArray.add(winnerIdx);
		currentUnsortedArray.add(loserIdx);

		// finished splitting if there is no way to to continue splitting and we have traversed the last duel queue
		boolean isFinishedSplitting = currentDuelQueue.isEmpty() && currentSortedArray.size() <= 1;

		if(isFinishedSplitting) {
			currentState = State.MERGING;

			// rearrange currentUnsortedArray into insertionQueue
			int[] jacobsSequence = reverseJacobsthal(currentSortedArray.size());
			insertionQueue = new LinkedList<Integer>();
			for(int idx = 0; idx < jacobsSequence.length; idx++) {
				Integer pairedElement = currentSortedArray.get(jacobsSequence[idx]);
				insertionQueue.addLast(currentPairing.get(pairedElement));
			}

			// if unpaired element exists, add unpaired element last
			if(currentPairing.containsKey(-1))
				insertionQueue.addLast(currentPairing.get(-1));

			currentInsertionElement = insertionQueue.pollFirst();
			currentDuel = new Duel<Integer>(currentInsertionElement, currentSortedArray.get(0));
		} else { 
			// finished splitting the current layer, move down a layer
			if(currentDuelQueue.isEmpty()) {
				ArrayList<Integer> previousSort = currentSortedArray;
				unsortedStack.push(currentUnsortedArray);

				ArrayList<Integer> newToSort = new ArrayList<Integer>();
				currentSortedArray  = newToSort;
				ArrayList<Integer> newUnsorted = new ArrayList<Integer>();
				currentUnsortedArray  = newUnsorted;

				pairingStack.push(currentPairing);

				HashMap<Integer, Integer> nextPairing = generatePairMap(previousSort);
				currentPairing = nextPairing;
				currentDuelQueue = getDuelsFromPairings(nextPairing);

				if(currentPairing.containsKey(-1))
					currentUnsortedArray.add(currentPairing.get(-1));
			}

			currentDuel = currentDuelQueue.pollFirst();
		}
	}


	/**
	 * Once in the MERGING state, we continue the insertion process. There are 3 major mini-states in
	 * MERGING that are:
	 * 1) searching: uses binary search to find where to insert `currentInsertionElement` into `currentSortedArray`
	 * 2) inserting: index location found, insert `currentInsertionElement` into `currentSortedArray`
	 * 3) popping queue: pop `currentUnsortedArray` to convert into the new `insertionQueue`
	 * 
	 * This method adds a single item from `currentUnsortedArray` via `insertionQueue`. Once queue is
	 * empty, pop the `unsortedStack` to convert into the new `insertionQueue`. If `unsortedStack` is
	 * empty, then sorting is finished.
	 * 
	 * @throws IllegalStateException when currently not MERGING
	 * @param winner
	 */
	private void insert(DuelWinner winner) {
		String error = "Must be in MERGING state to call insert(). Check if state is still SPLITTING or COMPLETED";
		if(currentState != State.MERGING)
			throw new IllegalStateException(error);

		boolean lessThanMiddleIdx = (winner == DuelWinner.A);

		// if still searching to insert, move boundaries and compare to new middleIdx
		if(lowerIdx < upperIdx) {
			if(lessThanMiddleIdx) {
				// if A is winner, then currentInsertionElement < currentSorted.get(middleIdx)
				upperIdx = middleIdx-1;
			} else {
				// else currentSorted.get(middleIdx) >= currentInsertionElement
				lowerIdx = middleIdx+1;
			}
			middleIdx = (lowerIdx+upperIdx)/2;

			currentDuel = new Duel<Integer>(currentInsertionElement, currentSortedArray.get(middleIdx));
			return;
		}

		// binary-search completed, insert either to left or right of middleIdx
		if(lessThanMiddleIdx)
			currentSortedArray.add(middleIdx, currentInsertionElement);
		else
			currentSortedArray.add(middleIdx+1, currentInsertionElement);

		// insertionQueue not empty, continue 
		if(!insertionQueue.isEmpty()) {
			// Still more elements from currentUnsortedArray to insert
			currentInsertionElement = insertionQueue.pollFirst();
			setupBinarySearch();
			currentDuel = new Duel<Integer>(currentInsertionElement, currentSortedArray.get(middleIdx));
		} else if(unsortedStack.size() > 0) { // move down the stack to insert the next layer of unsortedArrays
			// pop all the stacks to move up a level of recursion
			currentUnsortedArray = unsortedStack.pop();
			currentPairing = pairingStack.pop();

			// rearrange unsorted into insertionQueue
			int[] jacobsSequence = reverseJacobsthal(currentSortedArray.size());
			for(int idx = 0; idx < jacobsSequence.length; idx++) {
				Integer pairedElement = currentSortedArray.get(jacobsSequence[idx]);
				insertionQueue.addLast(currentPairing.get(pairedElement));
			}

			// if unpaired element exists, add unpaired element last
			if(currentPairing.containsKey(-1))
				insertionQueue.addLast(currentPairing.get(-1));

			currentInsertionElement = insertionQueue.pollFirst();
			setupBinarySearch();
			currentDuel = new Duel<Integer>(currentInsertionElement, currentSortedArray.get(middleIdx));
		} else { // nothing left in the isnertionQueue and all unsortedArrays inserted, finished sorting
			currentState = State.COMPLETED;
			currentDuel = null;
			finalSorting = currentSortedArray;
		}
	}


	/**
	 * Initializes the values of lowerIdx, upperIdx, and middleIdx for binary insertion sort
	 * 
	 * @throws IllegalStateException when calling outside of the MERGING state
	 */
	private void setupBinarySearch() {
		if(currentState != State.MERGING)
			throw new IllegalStateException("Only executes binary search for insertion while MERGING.");

		if(currentPairing.get(currentInsertionElement) != -1) {
			// we already know the current pairing is greater than its paired element, so search beyond that
			lowerIdx = currentSortedArray.indexOf(currentPairing.get(currentInsertionElement))+1;
		} else {
			// is unpaired element, search entire currentSortedArray
			lowerIdx = 0;
		}
		upperIdx = currentSortedArray.size()-1;
		middleIdx = (lowerIdx+upperIdx)/2;
	}


	/**
	 * Calculates the ordering for a Jacobsthal sequence (A001045 on OEIS) up to a specific input.
	 * This sequence has been shown to reduce the total number of comparisons needed for binary-search insertion
	 * in Merge-Insertion sort. To include every element up to N, the resurned array will count down
	 * from the current Jacobsthal integer down to the previous Jacobsthal integer before iterating to the next
	 * Jacobsthal integer.
	 * 
	 * For example, 10 -> 9 -> 8 -> 7 -> 6 -> 5 -> 20 -> 19 -> ...
	 * 
	 * @param num number of elements in the sequence
	 * @return an array containing a modified Jacobsthal sequence (A001045)
	 */
	public static int[] reverseJacobsthal(int num) {
		int[] seq = new int[num];
		seq[num-1] = 0;

		if(num == 1)
			return seq;

		int idx = 1;
		int jIdx = 1;
		while(idx < num) {
			for(int j = Integer.min(JACOBSTHAL[jIdx], num-1); idx < num && j > JACOBSTHAL[jIdx-1]; j--, idx++)
				seq[num-idx-1] = j;
			jIdx++;
		}
		return seq;
	}


	/**
	 * Notifies if the sorting is complete
	 * 
	 * @return true if sorting is finished, false otherwise
	 */
	public boolean isComplete() {
		return (currentState == State.COMPLETED);
	}


	/**
	 * Returns a fresh copy of the ArrayList of the fully sorted candidate integers.
	 * 
	 * @throws IllegalStateException if not COMPLETED
	 * @return
	 */
	public ArrayList<Integer> getFinalSorting(){
		if(currentState != State.COMPLETED)
			throw new IllegalStateException("Not finished sorting. Use isComplete() to check if final sorted is ready");

		ArrayList<Integer> sorting = new ArrayList<Integer>(finalSorting);
		return sorting;
	}


	/**
	 * Enum to denote the state of the MergeInsertionSort object
	 */
	private enum State {
		SPLITTING, MERGING, COMPLETED;
	}

	
	/**
	 * returns a randomly polled Poisson distribution with the given average from `lambda`.
	 * 
	 * @param lambda expected average
	 * @return an integer from a polled Poisson distribution
	 */
	public static int getPoisson(double lambda) {
		double L = Math.exp(-lambda);
		double p = 1.0;
		int k = 0;

		do {
			k++;
			p *= Math.random();
		} while (p > L);

		return k;
	}
}
