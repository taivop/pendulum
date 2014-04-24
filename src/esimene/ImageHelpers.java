package esimene;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.util.Set;

public class ImageHelpers {
	static boolean vocal = MainCameraWatcher.vocal;

	// returns y-coordinates of [top, bottom] edges
		public static int[] topAndBottom(ImagePlus imp, Roi columnRoi, int imageHeight, double decisionLimitRow) {
			// Duplicate the original image and then cut it, leaving just the desired column
			ImagePlus smallImp = imp.duplicate();
			smallImp.setRoi(columnRoi);
			IJ.run(smallImp, "Crop", "");
			//imp.setRoi(columnRoi);
			//IJ.run(imp, "Find Edges", "");
			//IJ.run(smallImp, "Find Edges", "");
			
			// Find top and bottom edge of the barcode
			ImageProcessor ip = smallImp.getProcessor();
			int rowWidth = ip.getWidth();
			int firstBlackRow = 1;
			int lastBlackRow = imageHeight;			
			for(int i=1; i<=imageHeight; i++) {
				double average = 0;
				int[] row = new int[rowWidth];
				ip.getRow(1, i, row, rowWidth);
				for(int j=0; j<rowWidth; j++) {			// Look at all the pixels in this row
					average += row[j];
				}
				average = average / 255 / (rowWidth - 1);
				
				if(average > decisionLimitRow) {		// The current row is black
					//System.out.printf("row %d is black\n", i);
					if(firstBlackRow == 1) {
						firstBlackRow = i;
					}
					lastBlackRow = i;					// After looking at all rows, 'lastBlackRow' will represent the last black row
				}
			}
			
			int codeTop = firstBlackRow;
			int codeBottom = lastBlackRow;
			int[] result = {codeTop, codeBottom};
			
			return result;
		}
		
		public static void fillRois(ImagePlus imp, Set<Roi> toBeFilled) {
			for(Roi r : toBeFilled) {
				imp.setRoi(r);
				IJ.run(imp, "Fill", "slice");
			}
		}
}
