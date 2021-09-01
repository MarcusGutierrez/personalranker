package com.ranker.tournament;

import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * My attempt at decoupling the Tournament and text-based console interface via the Model-View-Controller
 * Design pattern. This controller acts as the intermediary between the `Tournament` model that does the
 * sorting and handles all the processing and the View (console interface) that answers the Duel
 * comparison questions that the `Tournament` needs to properly sort the candidates in question.
 *
 * @author Marcus
 */
public class TournamentController {

	/** JSON text file saving used by the `TournamentStateSaver` to save to and load from */
	private static final String TOURNAMENT_FILE = "tournament.txt";
	
	/** Model that runs the duel tournament and handles the sorting and which comparisons are needed next */
	private Tournament model;
	
	/** Text-based console interface */
	private final TournamentView view;

	
	/**
	 * Constructor 
	 *
	 */
	public TournamentController(TournamentView view) {
		this.view = view;
	}


	/**
	 * Runs a full tournament to gather a ranking of candidates found in `INPUT_FILE`. This method
	 * acts as the in-between communication between the model (`Tournament` object) and the view
	 * (which currently is a text-based console view). The flow of this method is as follows:
	 * 
	 * 	Each round while the model has not finished sorting:
	 * 		1) The Tournament model asks a Duel comparison it does not know the answer to
	 * 		2) This controller takes the duel comparison and asks the view for the answer
	 * 		3) The view poses the question to the user (in this case, via console)
	 * 		4) Once the user declares the winner, the view tells this Controller, which tells the model
	 * 		5) The model then handles all the processing with the declared Duel
	 * 
	 */
	public void runTournament() {
		TournamentStateSaver saver;

		boolean newRanking = view.intro();
		
		// If loading previous save, load JSON object from text file, otherwise create a new `TournamentStateSaver`
		/* File written to by the `CandidateFileManager` that will contain the final rankings when sorting is done */
		String OUTPUT_FILE;
		if(!newRanking) {
			saver = new TournamentStateSaver(TOURNAMENT_FILE);
			model = saver.loadFromFile();
			
			OUTPUT_FILE = model.getFinalRankingFilename();
		} else { // create fresh `Tournament` model
			String[] filenames = view.acquireIOFilenames();
			/* File read from by the `CandidateFileManager` that has all the unsorted candidate names */
			String INPUT_FILE = filenames[0];
			OUTPUT_FILE = filenames[1];
			
			CandidateFileManager cfm;
			try {
				cfm = new CandidateFileManager(INPUT_FILE, OUTPUT_FILE);
				model = new Tournament(cfm.importCandidates(), OUTPUT_FILE);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			saver = new TournamentStateSaver(TOURNAMENT_FILE, model);
			
			view.printNewRanking();
		}
		
		// Until the model is finished sorting, 
		boolean complete = false;
		while(!complete) {
			Duel<String> duel;
			if(model.getActiveDuel() == null)
				duel = model.nextQuery();
			else
				duel = model.getActiveDuel();

			int action = view.askDuel(duel.getA(), duel.getB(), model.currentRound(), model.currentProgress());
			switch(action) {
			case 1: // candidate A won the duel
				model.declareWinner(DuelWinner.A);
				break;
			case 2: // candidate B won the duel
				model.declareWinner(DuelWinner.B);
				break;
			case 3: // undo previous duel
				//Helping
				break;
			case 4: // save and exit
				saver.saveToFile();
				System.exit(0);
			default:
				throw new IllegalStateException();
			}

			if(model.isComplete())
				complete = true;
		}

		ArrayList<String> ranking = model.getRanking();
		System.out.println(System.lineSeparator() + "Final Rankings: ");
		for(int playerIdx = 0; playerIdx < ranking.size(); playerIdx++)
			System.out.println((playerIdx+1) + ". " +  ranking.get(playerIdx));
		
		CandidateFileManager cfm = new CandidateFileManager();
		cfm.setWriteFile(OUTPUT_FILE);
		cfm.exportRankings(ranking);
	}

}
