package esimene;

import java.io.IOException;
import java.util.HashMap;

public class GeneralHelpers {
	static boolean vocal = MainCameraWatcher.vocal;
	
	public static String codeToString(int[] code) {
		String output = "";
		for(int i=0; i<code.length; i++) {
			output += code[i];
		}
		return output;
	}
	
	public static HashMap<String,Integer> getGrayCodes() throws IOException {
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
		code = new StringBuilder(code).reverse().toString();	// Reverse the string
		HashMap<String,Integer> hm = null;
		try {
			hm = GeneralHelpers.getGrayCodes();
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
		return ((double)codeValue) / 512 * 360;
	}
	
	// Round to given number of decimals
	public static double roundDecimals(double number, int howManyDecimals) throws Exception {
		double result = 0;
		
		if(howManyDecimals < 1 || howManyDecimals >= 10) {
			throw new Exception("Desired number of decimals out of allowed range.");
		} else {
			double coeff = Math.pow(10, howManyDecimals);
			result = ((double) Math.round(number * coeff)) / coeff;
		}
		
		return result;
	}
	
	public static void main(String[] args) {
		String code = "001101101";
		System.out.println(grayToValue(code));
	}
}

