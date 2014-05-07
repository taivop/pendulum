package esimene;

import ij.IJ;
import ij.ImagePlus;

import java.util.HashSet;
import java.util.Set;

public class ColumnFilling {
	static final int Green = 0;
	static final int Red = 1;
	
	static Set<RoiAndNumber> toBeFilled = new HashSet<RoiAndNumber>();

	// Fills all the Rois from toBeFilled with the color specified.
	public static void fillRois(ImagePlus imp, boolean[] includedColumns) {
		for(RoiAndNumber ran : toBeFilled) {
			imp.setRoi(ran.r);
			IJ.setForegroundColor(118, 255, 0);
			if(!includedColumns[ran.number]) {
				IJ.setForegroundColor(255, 118, 0);
			}
			IJ.run(imp, "Fill", "slice");
		}
	}
}
