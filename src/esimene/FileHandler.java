package esimene;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

// This class handles writing data to files.
public class FileHandler {
	static boolean vocal = MainCameraWatcher.IS_VOCAL;
	static String outputFilenameBase = "pendliandmed/andmed_";
	
	public static void writeDataToFile(double angle, Date time) {
		DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
		
		// Get timestamp as a string
		String timestamp = sdf.format(time);
		
		String outputFilename = getDataFileName(time);
		
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
	public static String getDataFileName(Date time) {
		DateFormat sdf = new SimpleDateFormat("MMM");
		
		String month = sdf.format(time);
		sdf = new SimpleDateFormat("yyyy");
		String year = sdf.format(time);
		
		return outputFilenameBase + month + year + ".csv";
	}
	
	// Read in properties from specified file with fileName
	public static Properties readProperties(String fileName) throws IOException {
		Properties prop = new Properties();
		
		InputStream input = new FileInputStream(fileName);
		
		// Load the properties
		prop.load(input);
		
		return prop;
	}
	
	// Print out all properties with their values
	public static void printProperties(Properties prop) {
		for (Object propObj : prop.keySet()) {
			String propName = (String) propObj;
			System.out.printf("%s=%s\n", propName, prop.getProperty(propName));
		}
	}

	public static void main(String[] args) {
		try {
			Properties prop = readProperties("config.properties");
			printProperties(prop);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}