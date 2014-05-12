package svg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileCreator {
	// Purpose of this class: to create an SVG file from given 9-bit Gray codes.
	
	// n-bit Gray codes
	static int N = 9;
	// there are a total of 2^n codes
	static int NUMBER_OF_CODES = (int) Math.pow(2, N);
	
	public static int[][] readCodesFromFile(String filePath) throws IOException{
		// Read in codes from specified file.
		int codes[][] = new int[NUMBER_OF_CODES][N];
		
		File codeInputFile = new File(filePath);
		BufferedReader br = new BufferedReader(new FileReader(codeInputFile));
		
		int i = 0;
		String str;
		int oneCode[] = new int[N];
		
		while(i < NUMBER_OF_CODES && br.ready()) {
			str = br.readLine().trim();
			//System.out.println(sone);
			
			for(int j=0; j<N; j++) {
				oneCode[j] = Integer.parseInt(Character.toString(str.charAt(j)));
			}
			
			codes[i] = (int[]) oneCode.clone();
			
			i++;
		}		
		
		br.close();
		return codes;
		
	}
	
	public static String codeToRect(int[] bitArray, int xOffset, int xWidth, int padding, int boxHeight) {
		String result = "";
		
		int y = 0;
		
		for(int bit : bitArray) {
			if(bit == 1) {
				result += "<rect x=\"" + xOffset + "\" y=\"" + (y + padding) + "\" width=\"" + xWidth + "\" height=\"" + boxHeight +
						"\"  style=\"fill:black;stroke-width:0;stroke:rgb(0,0,0)\"/>";
				result += "\n";
			}
			
			y += boxHeight;
		}
		
		
		
		return result;
	}
	
	public static void main(String[] args) throws Exception {
		// Create and write a SVG document of Gray codes.
		
		int xWidth = 20;
		int boxHeight = (int) 1.0865 * xWidth / N;
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
		
		
		for(int i=0; i<NUMBER_OF_CODES; i++) {
			bw.write(FileCreator.codeToRect(codes[i], xOffset, xWidth, padding, boxHeight));
			xOffset += xWidth;
		}
		
		bw.write("<rect x=\"0\" y=\"0\" width=\"" + (NUMBER_OF_CODES * xWidth) + "\" height=\"" + bandHeight + "\"  style=\"fill:black;stroke-width:0;stroke:rgb(0,0,0)\"/>");
		bw.write("<rect x=\"0\" y=\"" + (2 * padding + N * boxHeight - bandHeight) + "\" width=\"" + (NUMBER_OF_CODES * xWidth) + "\" height=\"" + bandHeight + "\"  style=\"fill:black;stroke-width:0;stroke:rgb(0,0,0)\"/>");
		
		System.out.println("writing...");
		
		bw.write("</svg>");
		
		bw.close();
		System.out.println("done.");
		
	}
}
