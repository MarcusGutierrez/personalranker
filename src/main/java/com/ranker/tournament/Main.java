package com.ranker.tournament;

import com.ranker.tournament.TournamentController;
import com.ranker.tournament.TournamentView;

public class Main {
	
	public static void main(String[] args) {
		
		TournamentController controller = new TournamentController(new TournamentView());
		controller.runTournament();
	}
	
}