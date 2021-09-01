package com.ranker.tournament;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * This operates as the File Reader/Writer for the candidates. This class doubles as a candidate name importer
 * and as the final candidate ranking exporter. `importCandidates()` should be used to import all the candidate
 * names from `readFile` and passed along to a `Tournament` object to acquire the final sorted ranking of the
 * candidates before calling `exportRanking()` to write the final obtained rankings to the `writeFile`.
 * 
 * @author Marcus
 * @date 8/15/2021
 */
public class CandidateFileManager implements Serializable {

	private static final long serialVersionUID = -1092685194562038606L;
	
	/** File to read/import candidate names */
	private File readFile;
	private static final String CANNOT_READ = "Cannot read from read file";
	
	/** File to write final rankings to */
	private File writeFile;
	
	/**
	 * Default constructor. Can use this constructor to readFile and writeFile later
	 */
	public CandidateFileManager() {
	}
	
	/**
	 * Sole constructor which takes in the string relative names of the `readFile` and `writeFile`
	 * 
	 * @param inFile relative string name of `readFile` to be converted into a File
	 * @param outFile relative name of `writeFile` to be converted to a File
	 * @throws FileNotFoundException if `readFile` cannot be found to read
	 */
	public CandidateFileManager(String inFile, String outFile) throws FileNotFoundException {
		this.readFile = new File(inFile);
		this.writeFile = new File(outFile);
		
		if(!this.readFile.canRead())
			throw new FileNotFoundException(CANNOT_READ);
	}
	
	
	/**
	 * Given a relative string name of `readFile`, convert into a File
	 * 
	 * @param readFile relative string name to be converted into a File
	 * @throws FileNotFoundException if `this.readFile` cannot be found
	 */
	public void setReadFile(String readFile) throws FileNotFoundException {
		this.readFile = new File(readFile);
		
		if(!this.readFile.canRead())
			throw new FileNotFoundException(CANNOT_READ);
	}
	
	
	/**
	 * Given the relative string name of `writeFile`, convert into a File
	 * 
	 * @param writeFile relative name of `writeFile` to be converted to a File
	 */
	public void setWriteFile(String writeFile) {
		this.writeFile = new File(writeFile);
	}
	
	
	/**
	 * Using `readFile`, this method reads through the file to collect the names of all
	 * the candidates to be ranked and passes them along as an (assumed) unranked
	 * list of candidate names ready to be ranked.
	 * 
	 * @return ArrayList<String> of candidate names to be ranked
	 */
	public ArrayList<String> importCandidates(){
		ArrayList<String> initialRankings = new ArrayList<String>();
		try {
			BufferedReader reader = new BufferedReader(new FileReader(readFile));
			
			String candidateName = reader.readLine();
			while(candidateName != null) {
				initialRankings.add(candidateName);
				candidateName = reader.readLine();
			}
			reader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return initialRankings;
	}
	
	
	/**
	 * Given an ArrayList<String> of rankings, this method writes the time and writes the rankings
	 * to `writeFile` in the order given. This method does not impose any rankings of its own
	 * and simply writes the ranking given to the `writeFile`.
	 * 
	 * @param rankings to be written to `writeFile`
	 */
	public void exportRankings(ArrayList<String> rankings) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(writeFile));
			
			Date now = new Date(); // java.util.Date, NOT java.sql.Date or java.sql.Timestamp!
			String today  = new SimpleDateFormat("MMM d, yyyy @ HH:mm", Locale.ENGLISH).format(now);
			String title = "Top " + rankings.size() + " Rankings (Completed on " + today + "):" + System.lineSeparator();
			writer.write(title);
			
			for(int i = 0; i < rankings.size(); i++) {
				String c = rankings.get(i);
				writer.write((i+1) + ". " + c.toString()); // writes ranking and name. E.g., "1. Title"
				if(i < rankings.size()-1)
					writer.write(System.lineSeparator());
			}
			writer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
}
