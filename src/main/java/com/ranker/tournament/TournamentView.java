package com.ranker.tournament;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Scanner;

/**
 * 
 * 
 * @date 8/15/2021
 * @author Marcus
 */
public class TournamentView {

	private Scanner input;
	
	private static final String NEW_LINE = System.lineSeparator();
	
	private static final String INTRO = 
			"Welcome to the online ranker! Start a new ranking or load your previous ranking" + NEW_LINE;
	
	private static final String INTRO_OPTIONS = 
			"1. Start new ranking" + NEW_LINE +
			"2. Load previous ranking";
	
	private static final String INTRO_NEW_RANKING = 
			"Starting a new ranking list. You will be asked a sequence of preference questions until "
			+ "a full ranking can be acquired." + NEW_LINE
			+ "At any point, you can ask for help by typing /help or save and exit with /save";
	
	private static final String INTRO_LOAD_RANKING = 
			"Loading previous ranking" + NEW_LINE
			+ "At any point, you can undo a previous answer by typing /undo or you save and exit with /save";
	
	private static final String FILE_INTRO = 
			"Before starting, we need some input and output files";
	
	private static final String ASK_INPUT_FILE = 
			"File to read candidate names from (do not forget file extensions):";
	
	private static final String ASK_OUTPUT_FILE = 
			"File to write final rankings to (do not forget file extensions):";
	
	private static final String SAVE_AND_EXIT =
			"Ranking is saved. Select the load option next time to pick up where you left off.";
	
	private static final String HELP =
			"Each round you have 4 precise input choices:" + NEW_LINE
			+ "   '1':		Chooses candidate 1 as your preference in a duel" + NEW_LINE
			+ "   '2':		Chooses candidate 2 as your preference in a duel" + NEW_LINE
			+ "   '/help':	opens this help guide" + NEW_LINE
			+ "   '/save':	saves current state of tournament and exits program;" + NEW_LINE
			+ "		When starting program, loading will return to this state";
	
	public TournamentView() {
		input = new Scanner(System.in);
	}
	
	
	/**
	 * 
	 * 
	 * @return
	 */
	public boolean intro() {
		boolean startingNewRanking = false;
		
		System.out.println(INTRO);
		System.out.println(INTRO_OPTIONS);
		
		boolean valid_response = false;
		while(!valid_response) {
			String response = input.nextLine().toLowerCase();
			switch(response) {
			case "1": case "start new ranking": case "new":
				valid_response = true;
				startingNewRanking = true;
				break;
			case "2": case "load" : case "load previous": case "load previous ranking":
				valid_response = true;
				startingNewRanking = false;
				System.out.println(NEW_LINE + INTRO_LOAD_RANKING + NEW_LINE);
				break;
			default:
			}
		}
		return startingNewRanking;
	}
	
	
	public String[] acquireIOFilenames() {
		String[] filenames = new String[2];
		System.out.println(FILE_INTRO + NEW_LINE);
		
		System.out.println(ASK_INPUT_FILE);
		filenames[0] = input.next();
		
		System.out.println(NEW_LINE + ASK_OUTPUT_FILE);
		filenames[1] = input.next();
		
		return filenames;
	}
	
	
	public void printNewRanking() {
		System.out.println(NEW_LINE + INTRO_NEW_RANKING + NEW_LINE);
	}
	
	
	/**
	 * 
	 * 
	 * @param optionA candidate A in question
	 * @param optionB candidate B in question
	 * @param round current round (can also be interpreted as number of duels answered + 1)
	 * @param progress percentage of knowledge known to total knowledge (from N choose 2 duels)
	 * @return 
	 */
	public int askDuel(String optionA, String optionB, int round, double progress) {
		BigDecimal progressBD = BigDecimal.valueOf(progress*100).setScale(4, RoundingMode.HALF_UP);
		
		System.out.println(NEW_LINE + "Round " + round + ": (Progress Completed: " + progressBD + "%)");
		System.out.println("1. " + optionA);
		System.out.println("2. " + optionB);
		
		int responseInt = 0;
		boolean valid_response = false;
		while(!valid_response) {
			String response = input.nextLine().toLowerCase();
			switch(response) {
			case "1":
				valid_response = true;
				responseInt = 1;
				break;
			case "2":
				valid_response = true;
				responseInt = 2;
				break;
			case "/help":
				valid_response = true;
				responseInt = 3;
				System.out.println(NEW_LINE + HELP + NEW_LINE);
				break;
			case "/save":
				valid_response = true;
				responseInt = 4;
				System.out.println(NEW_LINE + SAVE_AND_EXIT);
				break;
			default:
			}
		}
		
		return responseInt;
	}
	
}
