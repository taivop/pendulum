package esimene;

import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;

import com.lti.civil.CaptureDeviceInfo;
import com.lti.civil.CaptureException;
import com.lti.civil.CaptureObserver;
import com.lti.civil.CaptureStream;
import com.lti.civil.CaptureSystem;
import com.lti.civil.CaptureSystemFactory;
import com.lti.civil.DefaultCaptureSystemFactorySingleton;
import com.lti.civil.Image;
import com.lti.civil.VideoFormat;
import com.lti.civil.awt.AWTImageConverter;

public class MainCameraWatcher
{
	static boolean IS_WRITING_FILES;			// Do we want to save frames?
    static int FRAME_DELAY;						// We will be saving a frame every 'frameDelay' seconds
    static boolean ROTATE_180;					// Rotate the image by 180 degrees?
    static boolean IS_VOCAL;					// Do we want to print debug info all the time?
    static boolean SHOWING_IMAGES;			// Do we want to display images from webcam?
    static int M;									// The amount of sensing columns on either side of the center
    static int MIN_COLUMNS;						// The minimum amount of agreeing columns where we will still output a reliable decision.
    static long WALLTIME;					// For how long will we be recording (in seconds)?
    static String CAMERA_NAME;			// What's the name of the camera we are using in the device list?
    static float COLUMNSPACINGS_PER_CODEWITH;
    static int IMAGE_HEIGHT;
    
    static int N;							// The total amount of sensing columns
	static long startTime;
	static long lastFrameTime = 0;
	static double elapsed = 0;
    static ImagePlus currentImp;
    
    
	
	public static void main(String[] args) throws CaptureException
	{	
		System.out.println("Java library path:\n" + System.getProperty("java.library.path"));
		
		// Initialise properties that have been read in from file
		String propertiesFileName = "config.properties";
		String defaultPropertiesFileName = "config_default.properties";
		try {
			Properties prop = FileHandler.readProperties(propertiesFileName);
			IS_WRITING_FILES = Boolean.parseBoolean(prop.getProperty("IS_WRITING_FILES"));
			FRAME_DELAY = Integer.parseInt(prop.getProperty("FRAME_DELAY"));
			ROTATE_180 = Boolean.parseBoolean(prop.getProperty("ROTATE_180"));
			IS_VOCAL = Boolean.parseBoolean(prop.getProperty("IS_VOCAL"));
			SHOWING_IMAGES = Boolean.parseBoolean(prop.getProperty("SHOWING_IMAGES"));
			M = Integer.parseInt(prop.getProperty("M"));
			MIN_COLUMNS = Integer.parseInt(prop.getProperty("MIN_COLUMNS"));
			WALLTIME = Long.parseLong(prop.getProperty("WALLTIME"));
			COLUMNSPACINGS_PER_CODEWITH = Float.parseFloat(prop.getProperty("COLUMNSPACINGS_PER_CODEWITH"));
			CAMERA_NAME = prop.getProperty("CAMERA_NAME");
			IMAGE_HEIGHT = Integer.parseInt(prop.getProperty("IMAGE_HEIGHT"));
			
			N = 2 * M + 1;
			
			System.out.printf("\n[OK ] Successfully read settings from %s.", propertiesFileName);
			
		} catch (Exception e) {
			try {
			Properties prop = FileHandler.readProperties(defaultPropertiesFileName);
			IS_WRITING_FILES = Boolean.parseBoolean(prop.getProperty("IS_WRITING_FILES"));
			FRAME_DELAY = Integer.parseInt(prop.getProperty("FRAME_DELAY"));
			ROTATE_180 = Boolean.parseBoolean(prop.getProperty("ROTATE_180"));
			IS_VOCAL = Boolean.parseBoolean(prop.getProperty("IS_VOCAL"));
			SHOWING_IMAGES = Boolean.parseBoolean(prop.getProperty("SHOWING_IMAGES"));
			M = Integer.parseInt(prop.getProperty("M"));
			MIN_COLUMNS = Integer.parseInt(prop.getProperty("MIN_COLUMNS"));
			WALLTIME = Long.parseLong(prop.getProperty("WALLTIME"));
			COLUMNSPACINGS_PER_CODEWITH = Float.parseFloat(prop.getProperty("COLUMNSPACINGS_PER_CODEWITH"));
			CAMERA_NAME = prop.getProperty("CAMERA_NAME");
			IMAGE_HEIGHT = Integer.parseInt(prop.getProperty("IMAGE_HEIGHT"));
			
			N = 2 * M + 1;
			
			System.out.printf("\n[INF] Could not read settings from file %s. Falling back to default settings from %s.", propertiesFileName, defaultPropertiesFileName);
			} catch (Exception e2) {
				abnormalExit(String.format("Could not read settings from neither %s or %s.", propertiesFileName, defaultPropertiesFileName));
			}
		}
		
		
		// Initialise some stuff
		int chosenCamera = -1;
		String chosenCameraID = "";
		VideoFormat chosenFormat = null;
		
		startTime = System.nanoTime();
		
		// Communicating with all available cameras
		CaptureSystemFactory factory = DefaultCaptureSystemFactorySingleton.instance();
		CaptureSystem system = factory.createCaptureSystem();
		system.init();
		List list = system.getCaptureDeviceInfoList();
		
		// Picking the right camera
		for (int i = 0; i < list.size(); ++i)
		{
			System.out.print("\n[...] Looking for camera, listing available cameras...");
			System.out.printf("\n[...] The camera we want is called \"%s\"", MainCameraWatcher.CAMERA_NAME);
			
			CaptureDeviceInfo info = (CaptureDeviceInfo) list.get(i);
			
			System.out.print("\n[...] Device number: " + i );
			System.out.print("\n[...] -> Device ID: " + info.getDeviceID());
			System.out.print("\n[...] -> Description: " + info.getDescription());
			
			if(info.getDescription().equals(MainCameraWatcher.CAMERA_NAME))
			{
				chosenCamera = i;
				chosenCameraID = info.getDeviceID();
				System.out.print("\n[OK ] Camera found.");
				break;
			}
			
		}
		
		// If camera was not found, exit program.
		if(chosenCamera == -1) {
			abnormalExit("End of list, camera not found!");
		}
		
		CaptureStream captureStream = system.openCaptureDeviceStream(chosenCameraID);
		
		System.out.printf("\n[...] Choosing format... Looking for one with height=%d\n", IMAGE_HEIGHT);
		for (VideoFormat format : captureStream.enumVideoFormats())
		{	
			if(format.getWidth() != 0) {
				System.out.printf("        %s\n", videoFormatToString(format));
			}
			if(format.getHeight() == IMAGE_HEIGHT) {
				// Choose highest resolution format (for this camera, this means height = 1200px)
				chosenFormat = format;
			}			
		}
		if(chosenFormat == null) {
			// If we didn't find a suitable format
			abnormalExit("Could not choose video format. Are you sure this is the correct camera?");
		} else {
			System.out.print("\n[OK ] Format chosen: " + videoFormatToString(chosenFormat));
		}
		
		System.out.print("\n[...] Starting observer, height " + chosenFormat.getHeight() + "...");
		MyCaptureObserver2 observer = new MyCaptureObserver2(chosenFormat.getHeight());
		captureStream.setObserver(observer);
		
		System.out.print("\n[...] Setting video format...");
		captureStream.setVideoFormat(chosenFormat);
		System.out.print("\n[OK ] Video format set.");

		System.out.print("\n[...] Starting capture stream...");
		captureStream.start();
		System.out.print("\n[OK ] Capture stream started.");
		
		
		try
		{
			Thread.sleep(WALLTIME * 1000);
		}
		catch (InterruptedException e)
		{
			return;
		}
		
		System.out.println("\n[...] Stopping capture stream:");
		captureStream.stop();
		
		System.out.print("\n[...] Disposing stream...");
		captureStream.dispose();
		
		System.out.print("\n[...] Disposing system...");
		system.dispose();
		
		System.out.print("\n[OK ] Done. Normal exit.");
		

	}
	
	private static void abnormalExit(String errorMessage) {
		System.out.printf("\n[ERR] %s", errorMessage);
		System.out.print("\n[ERR] Abnormal exit.");
		System.exit(1);
	}
	
	public static String videoFormatToString(VideoFormat f)
	// Helper function for better display.
	{
		return formatTypeToString(f.getFormatType()) + " | size=" + f.getWidth() + "x" + f.getHeight() + " | FPS " + f.getFPS(); 
	}
	
	private static String formatTypeToString(int f)
	{
		switch (f)
		{
			case VideoFormat.RGB24:
				return "RGB24";
			case VideoFormat.RGB32:
				return "RGB32";
			default:
				return "" + f + " (unknown)";
		}
	}
	
}

class MyCaptureObserver2 implements CaptureObserver
{
	// The observer that receives images and turns them into an angle value.

	int inputHeight;
	int inputWidth;
	int displayHeight;
	int displayWidth;
	int frameCounter;
	
	
	MyCaptureObserver2(int height) {		
		// Size of input image
		inputHeight = height;
		inputWidth = height * 4 / 3;
		// Size of display, if displaying is activated.
		displayHeight = inputHeight / 2;
		displayWidth = inputWidth / 2;
		System.out.print("\n[OK ] Observer started.");
		
	}

	public void onError(CaptureStream sender, CaptureException e)
	{	System.err.println("onError " + sender);
		e.printStackTrace();
	}


	public void onNewImage(CaptureStream sender, Image image)
	{	
		boolean vocal = MainCameraWatcher.IS_VOCAL;
		long currentTime = System.nanoTime();
		long lastFrameTime = MainCameraWatcher.lastFrameTime;
		
		if(lastFrameTime == 0) {
			// If we just started recording
			lastFrameTime = currentTime;
			MainCameraWatcher.lastFrameTime = currentTime;
		}
		
		if(lastFrameTime == currentTime || (int) ((currentTime - lastFrameTime) / 1000000000.0) > MainCameraWatcher.FRAME_DELAY) {
			// If enough time has passed to accept another frame, start processing it. Otherwise, just ignore the frame.
			MainCameraWatcher.lastFrameTime = System.nanoTime();

			// close last image
			if(MainCameraWatcher.currentImp != null) {
				MainCameraWatcher.currentImp.close();
			}
			
			// convert image from CaptureStream
			BufferedImage inputImage = AWTImageConverter.toBufferedImage(image);
			ImageProcessor ip = new ColorProcessor(inputImage);
			MainCameraWatcher.currentImp = new ImagePlus("pilt", ip);
			
			// rotate image
			if (MainCameraWatcher.ROTATE_180) {
				MainCameraWatcher.currentImp.getProcessor().rotate(180);
			}
			
			// show current image
			if(MainCameraWatcher.SHOWING_IMAGES) {
				MainCameraWatcher.currentImp.show();
			}
			
			// get angle
			try {
				double angle = CodeRecognition.imageToResult(MainCameraWatcher.currentImp);
				
				// send angle to data saver to be saved
				Calendar calendar = new GregorianCalendar();
				Date time = calendar.getTime();
				DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
				String timestamp = sdf.format(time);
				System.out.printf(" " + timestamp);
				
				// write angle to file
		        FileHandler.writeDataToFile(angle, time);
		        
		        if(vocal) {
					System.out.printf("\n[INF] Current angle: %.2f", angle);
				}
		        
			} catch(Exception e) {
				System.out.printf("\n[ERR] Exception occurred: %s", e.getClass().getName());
				ColumnFilling.clearRoiSet();
			}
			
			
			MainCameraWatcher.currentImp.deleteRoi();
			
			
			// display time
			/*NumberFormat nf = NumberFormat.getNumberInstance(Locale.UK);	// formatting issues
			DecimalFormat df = (DecimalFormat)nf;
			df.setRoundingMode(RoundingMode.HALF_UP);
			df.setMaximumFractionDigits(2);
			MainCameraWatcher.elapsed = (currentTime - MainCameraWatcher.startTime) * Math.pow(10, -9);
			MainCameraWatcher.elapsed = Double.parseDouble(df.format(MainCameraWatcher.elapsed));
			if(vocal) {
				System.out.printf("\n[INF] t = %5.2f s", MainCameraWatcher.elapsed);
			}*/
			
			if(MainCameraWatcher.IS_WRITING_FILES) {
		
				// Encode as a JPEG	
				try
				{
					// Create folder if not exists
					String folderName = "outputImages";
					String fileNameBase = "frame";
					
					File dir = new File(folderName);
					boolean dirCreatedSuccess = false;
					if(!dir.exists()) {
						dirCreatedSuccess = dir.mkdir();
						if(dirCreatedSuccess) {
							if(vocal) {
								System.out.println("\n[OK ] Created directory '\\" + folderName + "'");
							}
						} else {
							throw(new Exception("Could not create directory."));
						}
					}
					
					String outFileName = folderName + "/" + fileNameBase + frameCounter + ".jpg";
					ImageIO.write(ip.getBufferedImage(), "jpg", new File(outFileName));
					if(vocal) {
						System.out.print(", write success");
					}
				}
				catch(FileNotFoundException fnfe) {
					System.out.println("[ERR] Could not write frame " + frameCounter + " to file!");
				}
				catch (Exception e)
				{	e.printStackTrace();
				}
			}
			
		}
		
	}
	
}
