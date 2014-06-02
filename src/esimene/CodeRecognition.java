package esimene;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;


public class CodeRecognition {
	
	// Some constants that will be used throughout the code.
	// TODO: read these in from a file or implement a better system for organising them.
	static int IMAGE_HEIGHT = 1200;
	static int IMAGE_WIDTH = 1600;
	static int columnWidth = 10;
	static int middleX = IMAGE_WIDTH / 2;
	static int columnSpacing = 50;
	static int lineStdevLimit = 10;
	static double angleCircvarLimit = 0.01;
	static double decisionLimitRow = 0.4;
	static double decisionLimitSample = 0.75;	
	static final boolean show = false;
	static boolean vocal = MainCameraWatcher.IS_VOCAL;
	static int M = MainCameraWatcher.M;
	static int N = MainCameraWatcher.N;
	
	public static double imageToResult(ImagePlus imp) throws UnreliableMeasurementException {
		// Turn an image to an angle.
		
		int[][] columnData = new int[N][9];
		int[][] lineData = new int[N][2];
		double[] angles = new double[N];
		
		if(show) { imp.show(); }
		
		IJ.run(imp, "Enhance Contrast...", "saturated=15");
		IJ.run(imp, "Maximum...", "radius=2"); 			// radius = 5 is also worth trying
		IJ.run(imp, "Make Binary", "");
		
		// If the top 20 rows are black, we invert the picture
		// Necessary because the "Make Binary function" binarises the wrong way if image is too dark.
		double topRowsAverage = StatisticsHelpers.getAverage(imp, new Roi(1, 1, IMAGE_WIDTH, 20));
		//System.out.printf("Average value of top row pixels: %2f\n", topRowsAverage);
		if(topRowsAverage > 0.75) {
			IJ.run(imp, "Invert", "");
		}
		
		
		
		// Find the top and bottom boundaries at each column.
		for(int i=-M; i <= M; i++) {
			
			int leftBound = middleX + i * columnSpacing + i * columnWidth + (int) (Math.signum(i) * columnWidth / 2);
			if(i == 0) {
				leftBound = middleX - columnWidth / 2;
				imp.setRoi(leftBound, 0, columnWidth, IMAGE_HEIGHT);
			} else {
				if(i > 0) {
					leftBound = leftBound - columnWidth;
				}
				imp.setRoi(leftBound, 0, columnWidth, IMAGE_HEIGHT);
			}
			
			lineData[i+M] = ImageHelpers.topAndBottom(imp, imp.getRoi(), IMAGE_HEIGHT, decisionLimitRow);
		}
		
		
		
		int[] topLineData = new int[N];
		int[] bottomLineData = new int[N];
		for(int i=0; i<N; i++) {
			topLineData[i] = lineData[i][0];
			bottomLineData[i] = lineData[i][1];
		}
		
		// Rectify top and bottom lines' data
		if(vocal) {
			System.out.printf("\n[...] Fitting top line...");
		}
		double [] topLineDataDouble = StatisticsHelpers.intArrayToDouble(topLineData);
		topLineDataDouble = StatisticsHelpers.rectifyLineData(topLineDataDouble, lineStdevLimit);
		topLineData = StatisticsHelpers.doubleArrayToInt(topLineDataDouble);
		if(vocal) {
			System.out.printf("\n[OK ] Top line fitted.");
		}
		
		if(vocal) {
			System.out.printf("\n[...] Fitting bottom line...");
		}
		double [] bottomLineDataDouble = StatisticsHelpers.intArrayToDouble(bottomLineData);
		bottomLineDataDouble = StatisticsHelpers.rectifyLineData(bottomLineDataDouble, lineStdevLimit);
		bottomLineData = StatisticsHelpers.doubleArrayToInt(bottomLineDataDouble);
		if(vocal) {
			System.out.printf("\n[OK ] Bottom line fitted.");
		}
		
		// Find the bar code values at each column.
		for(int i=-M; i <= M; i++) {
			
			int leftBound = middleX + i * columnSpacing + i * columnWidth + (int) (Math.signum(i) * columnWidth / 2);
			if(i == 0) {
				leftBound = middleX - columnWidth / 2;
				imp.setRoi(leftBound, 0, columnWidth, IMAGE_HEIGHT);
			} else {
				if(i > 0) {
					leftBound = leftBound - columnWidth;
				}
				imp.setRoi(leftBound, 0, columnWidth, IMAGE_HEIGHT);
			}
			
			int[] code = processColumn(imp, imp.getRoi(), topLineData[i+M], bottomLineData[i+M], i+M);
			columnData[i+M] = code;
			int codeValue = GrayCodeHelpers.grayToValue(code);
			double angle = GrayCodeHelpers.codeValueToAngle(codeValue);
			angles[i+M] = angle;
			if(vocal) {
				System.out.printf("\n[INF] Column %+d: %s\tValue: %d\t Angle: %.2f", i, GrayCodeHelpers.codeToString(code), codeValue, angle);			
			}
		}
		
		if(vocal) {
			System.out.printf("\n[...] Making decision...");
		}
		double decision = StatisticsHelpers.makeDecision(angles, angleCircvarLimit);
		
		if(decision >= 0) {
			System.out.printf("\n[OK ] Decision: %.1f degrees.", decision);
		} else {
			throw new UnreliableMeasurementException();
			//System.out.printf("\n[ERR] Could not make reliable decision.", decision);
		}
		
		// Draw some markers
		IJ.run(imp, "RGB Color", "");
		
		ColumnFilling.fillRois(imp, StatisticsHelpers.includedColumns);
		ColumnFilling.toBeFilled.clear();
		return decision;
	}
	
	
	
	public static int[] processColumn(ImagePlus imp, Roi columnRoi, int codeTop, int codeBottom, int colNum) {
		// Turn a column into a binary array (barcode).
		
		int[] code = new int[9];

		int codeHeight = codeBottom - codeTop;
		int barHeight = codeHeight / 11;			// The height of one bit in the barcode.
		int sampleWidth = columnWidth / 2;
		int sampleHeight = sampleWidth;
		
		imp.setRoi(columnRoi);
		int x = columnRoi.getBounds().x;
		int y = columnRoi.getBounds().y;
		//System.out.printf("x: %d, y: %d\n", x, y);
		
		for(int i=1; i<=9; i++) {					// Look at all bars containing information
			int samplex = x + (columnWidth / 2 - sampleWidth / 2);
			int sampley = codeTop + (i * barHeight + barHeight / 2 - sampleHeight / 2);
			Roi sampleRoi = new Roi(samplex, sampley, sampleWidth, sampleHeight);
			ColumnFilling.toBeFilled.add(new RoiAndNumber(sampleRoi, colNum));
			
			double sampleAverage = StatisticsHelpers.getAverage(imp, sampleRoi);
			int bit = 0;
			if(sampleAverage > decisionLimitSample) {
				bit = 1;
			}
			
			code[i - 1] = bit;
			
			
		}
		
		// Add markers
		int markerHeight = 10;
		ColumnFilling.toBeFilled.add(new RoiAndNumber(new Roi(x, y + codeTop - markerHeight, columnWidth, markerHeight), colNum));
		ColumnFilling.toBeFilled.add(new RoiAndNumber(new Roi(x, y + codeBottom, columnWidth, markerHeight), colNum));
	
		
		return code;
	}
	
	
	
	
	
	
	public static void main(String[] args) {
		// For testing purposes		
		// Do some offline testing without the camera.
		String filePath = "images/frame0.jpg";
		ImagePlus imp = new ImagePlus(filePath);
		//imageToResult(imp);
		
		
		
		String outputPath = "images/frame0_proc";
		IJ.saveAs(imp, "jpg", outputPath);
		imp.deleteRoi();
		
		
		/*
		for(int i=5; i<=195; i+=5) {
			String filePath = "C:\\Users\\dell\\Desktop\\katse5\\frame" + i + ".jpg";
			ImagePlus imp = new ImagePlus(filePath);
			System.out.printf("\nframe %d", i);
			imageToResult(imp);
			IJ.run(imp, "RGB Color", "");
			IJ.setForegroundColor(118, 255, 0);

			//ColumnFilling.fillRois(imp, StatisticsHelpers.includedColumns);
			//ColumnFilling.toBeFilled.clear();
			
			String outputPath = "C:\\Users\\dell\\Desktop\\katse5\\processed\\frame" + i + "proc.jpg";
			IJ.saveAs(imp, "jpg", outputPath);
		}
		*/
		
		
	}

}
