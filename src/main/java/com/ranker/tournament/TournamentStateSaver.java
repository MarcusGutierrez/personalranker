package com.ranker.tournament;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.gson.Gson; 
import com.google.gson.GsonBuilder; 

/**
 * This class uses GSON to bidirectionally convert a `Tournament` object to a JSON string and
 * similarly convert a JSON string to build a `Tournament` object. The JSON strings generated
 * and read from are saved to `tournamentFile`.
 * 
 * @author Marcus
 * @date 8/15/2021
 */
public class TournamentStateSaver {
	
	/** Gson object that performs the (Object <-> JSON string) conversions */
	private Gson gson;
	
	/** `Tournament` object that will be converted to JSON string and saved to `tournamentFile` */
	private Tournament loadedObject = null;
	
	/** File that holds saved `Tournament` objects as JSON strings to be saved to or loaded from */
	private File tournamentFile = null;
	
	
	/**
	 * Constructor used to load a `Tournament` object from a JSON string found in `tournamentFile`
	 * 
	 * @param fileName JSON file to be read file
	 */
	public TournamentStateSaver(String fileName) {
		tournamentFile = new File(fileName);
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.setPrettyPrinting();
		gson = gsonBuilder.create();
	}

	
	/**
	 * Constructor used when the `Tournament` object is already built/instanced
	 * and is ready to be written to file with the `saveToFile()` method.
	 * 
	 * @param fileName save file for JSON objects
	 * @param tourney `Tournament` object used to convert into a JSON string to write
	 */
	public TournamentStateSaver(String fileName, Tournament tourney) {
		tournamentFile = new File(fileName);
		loadedObject = tourney;
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.setPrettyPrinting();
		gson = gsonBuilder.create();
	}

	
	/**
	 * Converts the `loadedObject` Tournament into a JSON string that can then be loaded at a later time
	 * to build a `Tournament` object to continue ranking.
	 * 
	 * @return
	 */
	public boolean saveToFile() {
		try {
			String tournamentJson = gson.toJson(loadedObject);
			BufferedWriter writer = new BufferedWriter(new FileWriter(tournamentFile));
			writer.write(tournamentJson);
			writer.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return false;
	}

	
	/**
	 * Reads the JSON from `tournamentFile` to generate a `Tournament` object that was in the middle
	 * of execution.
	 * 
	 * @return a `Tournament` object in the middle of execution from saved JSON file
	 */
	public Tournament loadFromFile() {
		//BufferedReader reader = new BufferedReader(new FileReader(tournamentFile));
		String tournamentJson;
		try {
			tournamentJson = new String(Files.readAllBytes(Paths.get(tournamentFile.getName())), StandardCharsets.UTF_8);
			Tournament tournament = gson.fromJson(tournamentJson, Tournament.class);
			this.loadedObject = tournament;
			return tournament;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
	
}