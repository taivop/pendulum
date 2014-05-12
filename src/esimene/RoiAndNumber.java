package esimene;

import ij.gui.Roi;

public class RoiAndNumber {
// A simple class for keeping ROI (region-of-interest) objects with the number of column they belong to.
	Roi r;
	int number;
	
	RoiAndNumber(Roi r, int number) {
		this.r = r;
		this.number = number;
	}
}