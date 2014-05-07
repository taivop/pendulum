package svg;

public class GrayCreator {
	
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

	public static void main(String[] args) {
		// Do some testing
		// int m[] = {1, 1, 0, 0, 0, 1, 0, 1};
		//System.out.println(kood(m, 0, 50, 50));

	}

}
