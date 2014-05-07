package svg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileCreator {
	
	// n-bit Gray codes
	static int n = 9;
	// there are a total of 2^n codes
	static int numOfCodes = (int) Math.pow(2, n);
	
	// Read in codes from specified file.
	public static int[][] readCodesFromFile(String filePath) throws IOException{
		int codes[][] = new int[numOfCodes][n];
		
		File codeInputFile = new File(filePath);
		BufferedReader br = new BufferedReader(new FileReader(codeInputFile));
		
		int i = 0;
		String str;
		int oneCode[] = new int[n];
		
		while(i < numOfCodes && br.ready()) {
			str = br.readLine().trim();
			//System.out.println(sone);
			
			for(int j=0; j<n; j++) {
				oneCode[j] = Integer.parseInt(Character.toString(str.charAt(j)));
			}
			
			codes[i] = (int[]) oneCode.clone();
			
			i++;
		}		
		
		br.close();
		return codes;
		
	}
	
	// Create and write a SVG document of Gray codes.
	public static void main(String[] args) throws Exception {
		
		int xWidth = 20;
		int boxHeight = (int) 1.0865 * xWidth / n;
		int xOffset = 0;
		
		int bandHeight = Math.max(boxHeight / 4, 1);
		int padding = Math.max(boxHeight / 2, 2);
		
		int codes[][] = readCodesFromFile("graycodes.csv");
		
		File outputFile = new File("gray.html");
		
		FileWriter fw = new FileWriter(outputFile.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		
		bw.write("<?xml version=\"1.0\" standalone=\"no\"?>" +
				"<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"" + 
				"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">" +
				"<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">");
		
		
		for(int i=0; i<numOfCodes; i++) {
			bw.write(GrayCreator.codeToRect(codes[i], xOffset, xWidth, padding, boxHeight));
			xOffset += xWidth;
		}
		
		bw.write("<rect x=\"0\" y=\"0\" width=\"" + (numOfCodes * xWidth) + "\" height=\"" + bandHeight + "\"  style=\"fill:black;stroke-width:0;stroke:rgb(0,0,0)\"/>");
		bw.write("<rect x=\"0\" y=\"" + (2 * padding + n * boxHeight - bandHeight) + "\" width=\"" + (numOfCodes * xWidth) + "\" height=\"" + bandHeight + "\"  style=\"fill:black;stroke-width:0;stroke:rgb(0,0,0)\"/>");
		
		System.out.println("writing...");
		
		bw.write("</svg>");
		
		bw.close();
		System.out.println("done.");
		
	}
}
