package com.ranker.tournament;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * A candidate tournament to be held that tries to minimize the total number of comparisons for
 * online sorting using Merge-Insertion sort. This tournament makes an assumption of a transitive
 * property such that if, transitive inferences are made when applicable.
 * 
 * For example, if the duels are revealed and declared:
 * (a > b)
 * (b > c)
 * 
 * then the tournament will infer that (a > c) or "a is preferred over c" so the duel (a vs c) will not be need
 * to be queried. Furthermore, this tournament takes this transitivity one step further where any time a duel
 * winner is declared, then it checks any inferences such that:
 * 
 * W > i > j > L where `i` is the winner of the duel, `j` is the loser of the duel, and `W` is
 * the set of all known candidates that are preferred to `i` and `L` is the set of all known
 * candidates that are not preferred to `j`.
 * 
 * The tournament continues until the `sorter` finishes its merg-insertion sort.
 * 
 * @author Marcus
 */
public class Tournament implements Serializable {

	private static final long serialVersionUID = -533420259704779151L;
	
	/**
	 * File name to write to final rankings to. This class does not actually write to the file, 
	 * but instead keeps the String name here to since this object's state can be saved in the
	 * `TournamentStateSaver`. 
	 */
	private final String finalRankingFilename;

	/** Total number of elements to be compared. Total possible comparisons are (N choose 2).*/
	private final int N;
	
	/** Current round, where a round is defined as pair of nextQuery() & declareWinner() method calls */
	private int round;
	
	/** Total number of comparisons to be known (evaluated as N choose 2) */
	private final int totalKnowledge;
	
	/** Number comparisons known from 0 to (N choose 2) */
	private int currentKnowledge;
	
	/** Reversible candidate name-to-index and index-to-name bimap */
	private final HashMap<String, String> candidateBimap;
	
	/** head-to-head matchups. candidateMatchups[i][j] determines if `i` is preferred over `j`. */
	private final Boolean[][] candidateMatchups;
	
	/** Sorter performing online merge-insertion sort */
	private final MergeInsertionSorter sorter;
	
	/** Current duel to consume and declare winner. When null, there is no active duel and nextQuery() must be called.*/
	private Duel<String> activeDuel = null;


	/**
	 * Receives a list of Strings representing the candidates ready to be ranked.
	 * 
	 * @param initialRanking a List Strings for the candidate names to be ranked
	 */
	public Tournament(List<String> initialRanking, String finalRankingFilename) {
		this.finalRankingFilename = finalRankingFilename;
		N = initialRanking.size();
		
		round = 1;
		
		candidateBimap = new HashMap<>();
		
		candidateMatchups = new Boolean[N][N];
		int idx = 0;
		for(String cName : initialRanking) {
			candidateBimap.put(cName, Integer.toString(idx));
			candidateBimap.put(Integer.toString(idx), cName);
			idx++;
		}
		
		totalKnowledge = N*(N-1)/2; // N choose 2
		currentKnowledge = 0;
		sorter = new MergeInsertionSorter(N);
	}


	/**
	 * If sorting is complete according to the MergeInsertionSorter, return true
	 * 
	 * @return true if sorter.isComplete() returns true
	 */
	public boolean isComplete() {
		return sorter.isComplete();
	}
	
	
	/**
	 * Returns the currently active duel that needs to declared without changing any values.
	 * 
	 * @return active Duel<String> that is in question to be fed into the sorter
	 */
	public Duel<String> getActiveDuel() {
		return activeDuel;
	}
	
	
	/**
	 * Calculates and returns the percentage of completion of sorting based on known information about
	 * head-to-head matchups of candidates compared to the total (N choose 2) possible duels.
	 * 
	 * @return percentage of completion of sorting
	 */
	public double currentProgress() {
		return (double) currentKnowledge / (double) totalKnowledge;
	}
	
	
	/**
	 * Asserts the preferences that:
	 * W  > L, such that W is the set containing the winner candidate and of all candidates that are preferred
	 * to the winner; while L is the set containing the losing candidate and all candidates where the loser is
	 * preferred. To assert W > L, we iterate through all pairs of candidates in W and candidates in L and add the win:
	 * that w is preferred over l (w > l) where w is the an candidate in W and l is an candidate in L.
	 * 
	 * @param winner the declared winner of the duel (A or B)
	 */
	public void declareWinner(DuelWinner winner) {
		if(activeDuel == null)
			throw new IllegalStateException("Must call nextQuery() before declare the winner of a duel.");
		
		int winnerIndex;
		int loserIndex;
		if(winner == DuelWinner.A) {
			winnerIndex = getIdx(activeDuel.getA());
			loserIndex = getIdx(activeDuel.getB());
		} else {
			winnerIndex = getIdx(activeDuel.getB());
			loserIndex = getIdx(activeDuel.getA());
		}

		// Set of all candidates that beat the winner
		HashSet<Integer> winnerSet = new HashSet<>();
		winnerSet.add(winnerIndex);

		// Set of all candidates that loses to loser
		HashSet<Integer> loserSet = new HashSet<>();
		loserSet.add(loserIndex);

		// Add all k that are preferred over winner to the winnerSet
		// Add all k where loser is preferred over k to the loserSet
		for(int k = 0; k < N; k++) {
			// if k is not the winner nor loser
			if(k != winnerIndex && k != loserIndex) {
				if(candidateMatchups[k][winnerIndex]!= null && candidateMatchups[k][winnerIndex])
					winnerSet.add(k);
				else if(candidateMatchups[loserIndex][k] != null && candidateMatchups[loserIndex][k]) {
					loserSet.add(k);
				}
			}
			
		}

		// Add a win for every candidate in the winner set over every candidate in the loser set
		for(Integer w : winnerSet) {
			for(Integer l : loserSet) {
				String winnerSetName = getName(w);
				String loserSetName = getName(l);
				
				// by convention, Duel.getA() will always be the winner in the removal stack
				if((w != winnerIndex || l != loserIndex) && candidateMatchups[w][l] == null)
					System.out.println("--> inferring " + winnerSetName + " beats " + loserSetName);
				
				if(candidateMatchups[w][l] == null) {
					candidateMatchups[w][l] = true;
					candidateMatchups[l][w] = false;
					currentKnowledge++;
				}
			}
		}
		
		sorter.declareWinner(winner);
		activeDuel = null;
		round++;
	}
	
	
	/**
	 * Standard getter method for current round
	 * 
	 * @return current Round
	 */
	public int currentRound() {
		return round;
	}

	
	/**
	 * Acquires the next duel in question for the sorter to progress. Some matchups the sorter asks
	 * may already be cached in `candidateMatchups`, which will be continuously answered
	 * until the sorter responds with a Duel that is not cached.
	 * 
	 * @return current Duel to be answered by the user
	 */
	public Duel<String> nextQuery() {
		if(activeDuel != null)
			throw new IllegalStateException("Must call declareWinner() first before querying the next duel.");
		
		//activeDuel = duelQueries.remove(0);
		Duel<Integer> idxDuel = null;
		boolean knownAnswer = true;
		while(knownAnswer) {
			idxDuel = sorter.getCurrentComparison();
			if(candidateMatchups[idxDuel.getA()][idxDuel.getB()] != null && candidateMatchups[idxDuel.getA()][idxDuel.getB()]) {
				sorter.declareWinner(DuelWinner.A);
			} else if(candidateMatchups[idxDuel.getB()][idxDuel.getA()] != null && candidateMatchups[idxDuel.getB()][idxDuel.getA()]) {
				sorter.declareWinner(DuelWinner.B);
			} else
				knownAnswer = false;
		}
		
		activeDuel = new Duel<>(getName(idxDuel.getA()), getName(idxDuel.getB()));
		
		return activeDuel;
	}
	
	
	/**
	 * Converts candidate names to candidate index using reversible bimap
	 * 
	 * @param str candidate's name with index mapping
	 * @return index of the mapped candidate name
	 */
	private int getIdx(String str) {
		return Integer.parseInt(candidateBimap.get(str));
	}
	
	
	/**
	 * Converts candidate index to candidate name using reversible bimap
	 * 
	 * @param idx index of the mapped name
	 * @return candidate's name with index mapping
	 */
	private String getName(int idx) {
		return candidateBimap.get(Integer.toString(idx));
	}
	
	
	/**
	 * Standard getter for file name to write the final ranking to.
	 * 
	 * @return filename to write final ranking to
	 */
	public String getFinalRankingFilename() {
		return finalRankingFilename;
	}

	
	/**
	 * Returns fresh Arraylist of final ranking only after sorting is complete
	 * 
	 * @return Arraylist of final ranked candidates
	 */
	public ArrayList<String> getRanking(){
		if(!sorter.isComplete())
			throw new IllegalStateException("Cannot form full ranking with incomplete knowledge. You must answer all queries in the queue first.");
		
		ArrayList<Integer> sortedIdx = sorter.getFinalSorting();
		ArrayList<String> finalRanking = new ArrayList<>();
		for(Integer idx : sortedIdx)
			finalRanking.add(getName(idx));
		return finalRanking;
	}
	

}