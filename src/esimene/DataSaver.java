package esimene;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

// This class handles writing data to files.
public class DataSaver {
	static boolean vocal = MainCameraWatcher.IS_VOCAL;
	static String outputFilenameBase = "pendliandmed/andmed_";
	
	public static void writeToFile(double angle, Date time) {
		DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
		
		// Get timestamp as a string
		String timestamp = sdf.format(time);
		
		String outputFilename = getFilename(time);
		
		// Try and append the data point to the desired file
		try {
			PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(outputFilename, true)));
			out.println((timestamp + ";" + angle).replaceAll("\\.", ","));
			out.close();
		} catch(Exception e) {
			// Fail gracefully... kind of.
			System.out.print("\n[ERR] Did not write measurement to file.");
			if(vocal) {
				System.out.println();
				e.printStackTrace();
			}
		}	
	}
	
	// Get filename based on filename base and given time
	public static String getFilename(Date time) {
		DateFormat sdf = new SimpleDateFormat("MMM");
		
		String month = sdf.format(time);
		sdf = new SimpleDateFormat("yyyy");
		String year = sdf.format(time);
		
		return outputFilenameBase + month + year + ".csv";
	}

	public static void main(String[] args) {
		
		writeToFile(151/3.0, new GregorianCalendar().getTime());
		//System.out.println(getFilename(new GregorianCalendar().getTime()));
	}

}