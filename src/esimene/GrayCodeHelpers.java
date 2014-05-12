package esimene;

import java.io.IOException;
import java.util.HashMap;

public class GrayCodeHelpers {
	static boolean vocal = MainCameraWatcher.IS_VOCAL;
	
	public static String codeToString(int[] code) {
		// Turn code int[] representation to string representation
		String output = "";
		for(int i=0; i<code.length; i++) {
			output += code[i];
		}
		return output;
	}
	
	public static HashMap<String,Integer> getGrayCodes() throws IOException {
		// Get Gray codes from file
		HashMap<String,Integer> hm = new HashMap<String,Integer>();
		int[][] codes = svg.FileCreator.readCodesFromFile("graycodes.csv");
		for(int i=0; i<codes.length; i++) {
			Integer[] codesInt = new Integer[codes[i].length];
			for(int j=0; j<codes[i].length; j++) {
				codesInt[j] = new Integer(codes[i][j]);
			}
			
			hm.put(codeToString(codes[i]), new Integer(i));
			//System.out.println(codeToString(codes[i]));
		}
		return hm;
	}
	
	public static int grayToValue(String code) {
		// Get integer value from Gray code.
		code = new StringBuilder(code).reverse().toString();	// Reverse the string
		HashMap<String,Integer> hm = null;
		try {
			hm = GrayCodeHelpers.getGrayCodes();
		} catch(IOException e) {
			e.printStackTrace();
		}
		//System.out.println(code);
		return hm.get(code);
	}
	
	public static int grayToValue(int[] code) {
		return grayToValue(codeToString(code));
	}
	
	public static double codeValueToAngle(int codeValue) {
		// Get angle value for a Gray code.
		// We have 512 codes on a disk of 360 degrees.
		return ((double)codeValue) / 512 * 360;
	}
	
	public static void main(String[] args) {
		String code = "001101101";
		System.out.println(grayToValue(code));
	}
}

