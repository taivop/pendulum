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
	final static boolean IS_WRITING_FILES = false;			// Do we want to save frames?
    final static int FRAME_DELAY = 5;						// We will be saving a frame every 'frameDelay' seconds
    final static boolean IS_VOCAL = false;					// Do we want to print debug info all the time?
    final static boolean SHOWING_IMAGES = true;			// Do we want to display images from webcam?
    final static int M = 6;									// The amount of sensing columns on either side of the center
    final static int N = 2 * M + 1;							// The total amount of sensing columns
    final static int MIN_COLUMNS = 7;						// The minimum amount of agreeing columns where we will still output a reliable decision.
    final static long WALLTIME = 120000;					// For how long will we be recording (in seconds)?
    final static String CAMERA_NAME = "PC VGA Camer@ Plus";//"/dev/video0";	// What's the name of the camera we are using in the device list?
    
	static long startTime;
	static long lastFrameTime = 0;
	static double elapsed = 0;
    static ImagePlus currentImp;
    
    
	
	public static void main(String[] args) throws CaptureException
	{	
		// Initialise some stuff
		int chosenCamera = -1;
		String chosenCameraID = "";
		VideoFormat chosenFormat = null;
		int chosenHeight = 1200;
		
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
			System.out.print("\n[ERR] End of list, camera not found!");
			System.out.print("\n[ERR] Abnormal exit.");
			System.exit(1);
		}
		
		CaptureStream captureStream = system.openCaptureDeviceStream(chosenCameraID);
		
		System.out.print("\n[...] Choosing format...");
		for (VideoFormat format : captureStream.enumVideoFormats())
		{
			if(format.getHeight() == chosenHeight) {
				// Choose highest resolution format (for this camera, this means height = 1200px)
				chosenFormat = format;
				System.out.print("\n[OK ] Format chosen: " + videoFormatToString(format));
			}			
		}
		
		System.out.print("\n[...] Starting observer, height " + chosenHeight + "...");
		MyCaptureObserver2 observer = new MyCaptureObserver2(chosenHeight);
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
	
	public static String videoFormatToString(VideoFormat f)
	// Helper function for better display.
	{
		return "Type=" + formatTypeToString(f.getFormatType()) + " Width=" + f.getWidth() + " Height=" + f.getHeight() + " FPS=" + f.getFPS(); 
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
		        DataSaver.writeToFile(angle, time);
		        
		        if(vocal) {
					System.out.printf("\n[INF] Current angle: %.2f", angle);
				}
		        
			} catch(Exception e) {
				System.out.printf("\n[ERR] Exception occurred: %s", e.getClass().getName());
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
