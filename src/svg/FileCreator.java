package svg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class FileCreator {
	
	// n-bitised koodid
	static int n = 9;
	// koode on kokku 2^n
	static int arv = (int) Math.pow(2, n);
	
	public static int[][] readCodesFromFile(String filePath) throws IOException{
		int koodid[][] = new int[arv][n];
		
		File codeInputFile = new File(filePath);
		BufferedReader br = new BufferedReader(new FileReader(codeInputFile));
		
		int i = 0;
		String sone;
		int massiiv[] = new int[n];
		
		while(i < arv && br.ready()) {
			sone = br.readLine().trim();
			//System.out.println(sone);
			
			for(int j=0; j<n; j++) {
				massiiv[j] = Integer.parseInt(Character.toString(sone.charAt(j)));
				/*if(sone.charAt(j) == '1') {
					massiiv[j] = 1;
				} else {
					massiiv[j] = 0;
				}*/
			}
			
			koodid[i] = (int[]) massiiv.clone();
			
			i++;
		}		
			
		return koodid;
		
	}

	public static void main(String[] args) throws Exception {
		
		int xlaius = 20;
		//int kastikorgus = 70;
		int kastikorgus = (int) 1.0865 * xlaius / n;
		int xnihe = 0;
		
		int ribakorgus = Math.max(kastikorgus / 4, 1);
		int padding = Math.max(kastikorgus / 2, 2);
		
		int koodid[][] = readCodesFromFile("graycodes.csv");
		
		File output = new File("gray.html");
		
		FileWriter fw = new FileWriter(output.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		
		bw.write("<?xml version=\"1.0\" standalone=\"no\"?>" +
				"<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 1.1//EN\"" + 
				"http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd\">" +
				"<svg xmlns=\"http://www.w3.org/2000/svg\" version=\"1.1\">");
		
		
		for(int i=0; i<arv; i++) {
			/*System.out.print(koodid[i][0]);
			System.out.print(koodid[i][1]);
			System.out.print(koodid[i][2]);
			System.out.print(koodid[i][3]);
			System.out.print(koodid[i][4]);
			System.out.print(koodid[i][5]);
			System.out.print(koodid[i][6]);
			System.out.println(koodid[i][7]);*/
			bw.write(GrayCreator.kood(koodid[i], xnihe, xlaius, padding, kastikorgus));
			xnihe += xlaius;
		}
		
		bw.write("<rect x=\"0\" y=\"0\" width=\"" + (arv * xlaius) + "\" height=\"" + ribakorgus + "\"  style=\"fill:black;stroke-width:0;stroke:rgb(0,0,0)\"/>");
		bw.write("<rect x=\"0\" y=\"" + (2 * padding + n * kastikorgus - ribakorgus) + "\" width=\"" + (arv * xlaius) + "\" height=\"" + ribakorgus + "\"  style=\"fill:black;stroke-width:0;stroke:rgb(0,0,0)\"/>");
		
		System.out.println("writing...");
		
		bw.write("</svg>");
		
		bw.close();
		System.out.println("done.");
		
	}
}
