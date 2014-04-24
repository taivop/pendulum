package esimene;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import org.apache.commons.math3.analysis.function.Atan2;
import org.apache.commons.math3.analysis.function.Cos;
import org.apache.commons.math3.analysis.function.Sin;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.regression.SimpleRegression;

public class StatisticsHelpers {
	static boolean vocal = MainCameraWatcher.vocal;
	static int N = MainCameraWatcher.N;
	static int M = MainCameraWatcher.M;
	static boolean[] includedColumns;
	
	// Fit all data points to a line found from valid data points
	public static double[] correctWithRegression(double[] lineData, boolean[] inclusionArray) {
		SimpleRegression reg = new SimpleRegression(true);
		
		// Add data
		for(int i=0; i<lineData.length; i++) {
			if(inclusionArray[i]) {
				//System.out.printf("[REG] using point: x=%d, y=%.2f\n", i, lineData[i]);
				reg.addData(i, lineData[i]);
			}
		}
		
		double intercept = reg.getIntercept();
		double slope = reg.getSlope();
		
		if(vocal) {
			System.out.printf("\n[INF] -> Intercept: %.2f\tSlope: %.2f\tRSQ: %.2f", intercept, slope, reg.getRSquare());
		}
		
		// Fit all the points to the line found
		for(int i=0; i<lineData.length; i++) {
			lineData[i] = intercept + slope * i;
		}
		
		return lineData;
	}
	
	// Finds the data point that lies the furthest from median
	public static int getOutlier(double[] lineDataIn, double[] originalLineData, boolean onARing) {

		Median med = new Median();
		double median = med.evaluate(lineDataIn);
		int furthestPointFromMedian = 0;
		double furthestDistanceFromMedian = 0;
		
		for(int i=0; i<lineDataIn.length; i++) {
			
			double comparison = lineDataIn[i];
			if(Math.abs(lineDataIn[i] - median) > 180) {
				comparison = lineDataIn[i] + 360;
			}
			double distance = Math.abs(median - comparison); 
			if(distance > furthestDistanceFromMedian) {
				furthestPointFromMedian = i;
				furthestDistanceFromMedian = distance;
			}
		}
		
		for(int i=0; i<originalLineData.length; i++) {
			if(originalLineData[i] == lineDataIn[furthestPointFromMedian]) {
				return i;
			}
		}
		
		return furthestPointFromMedian;
	}
	
	
	
	// Removes the given data point
	public static double[] removeDataPoint(double[] lineDataIn, double[] originalLineData, int outlier, boolean isAngle) {
		double[] lineDataOut = new double[lineDataIn.length-1];
		//System.out.printf("\n--- lineDataIn length %d, removing element %d", lineDataIn.length, toBeRemoved);
		int toBeRemoved = outlier;
		if(!isAngle) {
			for(int i=0; i<lineDataIn.length; i++) {
				if(lineDataIn[i] == originalLineData[outlier]) {
					toBeRemoved = i;
				}
			}
		}
		
		int counter=0;
		for(int i=0; i<lineDataIn.length; i++) {
			if(i != toBeRemoved) {
				lineDataOut[counter] = lineDataIn[i];
				counter++;
			}
		}
		
		return lineDataOut;
	}
	
	// Finds the average value of pixels over the given region
	public static double getAverage(ImagePlus imp, Roi roi) {
		double result = 0;
		int count = 0;
		ImageProcessor ip = imp.getProcessor();
		
		
		for(int i = roi.getBounds().x; i <= roi.getBounds().x + roi.getBounds().width; i++) {
			for(int j = roi.getBounds().y; j <= roi.getBounds().y + roi.getBounds().height; j++) {
				result += ip.getPixelValue(i, j);
				count++;
			}
		}
		
		result = result / (count * 255);
		return result;
	}
	
	// Converts an array of ints to an array of doubles
	public static double[] intArrayToDouble(int[] input) {
		int numOfDataPoints = input.length;
		double[] output = new double[numOfDataPoints];
		for(int i=0; i<numOfDataPoints; i++) {
			output[i] = (double) input[i];
		}
		return output;
	}
	
	// Converts an array of doubles to ints
	public static int[] doubleArrayToInt(double[] input) {
		int numOfDataPoints = input.length;
		int[] output = new int[numOfDataPoints];
		for(int i=0; i<numOfDataPoints; i++) {
			output[i] = (int) Math.round(input[i]);
		}
		return output;
	}
	
	
	public static void rectifyColumnData(int[][] columnData) {
		for(int rowNum=0; rowNum < 9; rowNum++) {
			for(int colNum=1; colNum < N-1; colNum ++) {			// Do not check the leftmost and rightmost columns
				if(columnData[colNum-1] == columnData[colNum+1]) {
					//columnData[colNum] = columnData[colNum-1];
				}
			}
		}
	}
	
	public static double[] rectifyLineData(double[] lineDataDouble, int stdevLimit) {
		double[] originalLineData = lineDataDouble.clone();
		boolean[] inclusionArray = new boolean[originalLineData.length];
		for(int i=0; i<originalLineData.length; i++) {
			inclusionArray[i] = true;					// At first, try to use all data points
		}
		StandardDeviation std = new StandardDeviation();	
		
		// If there are at least 4 data points left and stdev is too high, remove one data point.
		while(std.evaluate(lineDataDouble) > stdevLimit && lineDataDouble.length >= 4) {
			int outlier = StatisticsHelpers.getOutlier(lineDataDouble, originalLineData, false);
			if(vocal) {
				System.out.printf("\n[...] -> Data point %d is outlier, removing it...", outlier);
			}
			inclusionArray[outlier] = false;
			lineDataDouble = StatisticsHelpers.removeDataPoint(lineDataDouble, originalLineData, outlier, false);
		}
		if(vocal) {
			System.out.printf("\n[OK ] -> stdev = %.2f, moving on.", std.evaluate(lineDataDouble));
		}
		
		int numOfDataPoints = lineDataDouble.length;
		for(int i=0; i<numOfDataPoints; i++) {
			//System.out.println(lineDataDouble[i]);
		}
		
		originalLineData = StatisticsHelpers.correctWithRegression(originalLineData, inclusionArray);
		return originalLineData;
	}
	
	static double makeDecision(double[] angles, double angleVarianceLimit) {
		double[] anglesOriginal = angles.clone();
		double decision = Double.POSITIVE_INFINITY;
		boolean[] inclusionArray = new boolean[angles.length];
		for(int i=0; i<inclusionArray.length; i++) {
			inclusionArray[i] = true;
		}
		
		while(circularVariance(angles) > angleVarianceLimit && angles.length >= 3) {
			// If we don't have 3 proper columns to base our decision on, then quit.
			if(angles.length == 3) {
				return Double.POSITIVE_INFINITY;
			}
			
			int outlier = StatisticsHelpers.getOutlier(angles, anglesOriginal, true);
			if(vocal) {
				System.out.printf("\n[...] -> Angle from column %d is outlier, removing it...", outlier - M);
			}
			inclusionArray[outlier] = false;
			angles = StatisticsHelpers.removeDataPoint(angles, anglesOriginal, outlier, true);
		}
		
		if(vocal) {
			System.out.printf("\n[OK ] -> Angles' circvar = %.4f, moving on.", circularVariance(angles));
		}
		
		double[] predictions = new double[N];
	
		for(int i=0; i<N; i++) {
			// Let's look at all the columns that show a reasonable value
			if(inclusionArray[i]) {
				double myDistance = ((i-M) * 360.0 / 512 / 3.45);		// The distance of this column from the middle one. The last divisor is the ratio of columnspacings per codewidth. Value has been measured from an image.
				predictions[i] = anglesOriginal[i] + myDistance;
				if(predictions[i] < 0) {
					predictions[i] = 360 + predictions[i];
				}
				if(vocal) {
					System.out.printf("\n[INF] Prediction from column %+d: %5.2f", i-M, predictions[i]);
				}
			} else {
				predictions[i] = Double.POSITIVE_INFINITY;
				if(vocal) {
					System.out.printf("\n[INF] Prediction from column %+d: removed.", i-M, predictions[i]);
				}
			}
		}
		
		includedColumns = inclusionArray.clone();;
		
		// Let's find the CIRCULAR mean of column predictions and use it as our decision
		decision = circularMean(predictions);
		
		//decision = (36.11 - mean);
		if(decision < 0) {
			return 360 + decision;
		}
		
		return decision;
	}
	
	// Find the circular mean of the given data
	static double circularMean(double[] data) {
		// From http://en.wikipedia.org/wiki/Circular_mean
		double result = 0;
		double sumOfSines = 0;
		double sumOfCosines = 0;
		int count=0;
		
		Sin sine = new Sin();
		Cos cosine = new Cos();
		Atan2 atan2 = new Atan2();
		
		
		for(int i=0; i<data.length; i++) {
			if(data[i] != Double.POSITIVE_INFINITY) {
				sumOfSines += sine.value(Math.toRadians(data[i]));
				sumOfCosines += cosine.value(Math.toRadians(data[i]));
				count++;
			}
		}
		
		result = Math.toDegrees(atan2.value(sumOfSines / count, sumOfCosines / count));
		
		return result;
	}
	
	static double circularVariance(double[] data) {
		// From http://www.ebi.ac.uk/thornton-srv/software/PROCHECK/nmr_manual/man_cv.html
		int count = 0;
		double sumOfSines = 0;
		double sumOfCosines = 0;
		Sin sine = new Sin();
		Cos cosine = new Cos();
		
		for(int i=0; i<data.length; i++) {
			if(data[i] != Double.POSITIVE_INFINITY) {
				sumOfSines += sine.value(Math.toRadians(data[i]));
				sumOfCosines += cosine.value(Math.toRadians(data[i]));
				count++;
			}
		}
		
		double R = Math.sqrt(Math.pow(sumOfSines, 2) + Math.pow(sumOfCosines, 2));
		
		return 1 - R / count;
	}

}
