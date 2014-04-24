package svg;

public class GrayCreator {
	
	public static String kood(int[] massiiv, int xnihe, int xlaius, int padding, int kastikorgus) {
		String tulemus = "";
		
		int y = 0;
		
		for(int bitt : massiiv) {
			if(bitt == 1) {
				tulemus += "<rect x=\"" + xnihe + "\" y=\"" + (y + padding) + "\" width=\"" + xlaius + "\" height=\"" + kastikorgus +
						"\"  style=\"fill:black;stroke-width:0;stroke:rgb(0,0,0)\"/>";
				tulemus += "\n";
			}
			
			y += kastikorgus;
		}
		
		
		
		return tulemus;
	}

	public static void main(String[] args) {
		int m[] = {1, 1, 0, 0, 0, 1, 0, 1};
		//System.out.println(kood(m, 0, 50, 50));

	}

}
