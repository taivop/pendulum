package esimene;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

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

public class KaameraPilt
{
	static long startTime;
	static double elapsed = 0;
    final static int sleepDuration = 60000;				// For how long will we be recording?
    final static boolean isWritingFiles = true;		// Do we want to save files?
    static int frameDensity = 5;					// We will be saving a frame every 'frameDensity'-th frame.
    
	
	public static void main(String[] args) throws CaptureException
	{	
		// Initialise some stuff
		int chosenCamera = -1;
		String chosenCameraID = "";
		VideoFormat chosenFormat = null;
		int chosenHeight = 1200;
		
		startTime = System.nanoTime();
		frameDensity = frameDensity == 0 ? 1 : frameDensity;
		
		// Communicating with all available cameras
		CaptureSystemFactory factory = DefaultCaptureSystemFactorySingleton.instance();
		CaptureSystem system = factory.createCaptureSystem();
		system.init();
		List list = system.getCaptureDeviceInfoList();
		
		// Picking the right camera
		for (int i = 0; i < list.size(); ++i)
		{
			System.out.print("\n[...] Looking for camera, listing available cameras...");
			
			CaptureDeviceInfo info = (CaptureDeviceInfo) list.get(i);
			
			System.out.print("\n[...] Device number: " + i );
			System.out.print("\n[...] -> Device ID: " + info.getDeviceID());
			System.out.print("\n[...] -> Description: " + info.getDescription());
			
			if(info.getDescription().equals("PC VGA Camer@ Plus"))
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
		MyCaptureObserver observer = new MyCaptureObserver(chosenHeight);
		captureStream.setObserver(observer);
		
		System.out.print("\n[...] Setting video format...");
		captureStream.setVideoFormat(chosenFormat);
		System.out.print("\n[OK ] Video format set.");

		System.out.print("\n[...] Starting capture stream...");
		captureStream.start();
		System.out.print("\n[OK ] Capture stream started.");
		
		
		try
		{
			Thread.sleep(sleepDuration);
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

class MyCaptureObserver implements CaptureObserver
{
	// upon construction, create a window to display the webcam image
	
	JFrame raam;
	Container sisu;
	int inputHeight;
	int inputWidth;
	int displayHeight;
	int displayWidth;
	int frameCounter;
	
	
	MyCaptureObserver(int height) {
		inputHeight = height;
		inputWidth = height * 4 / 3;
		displayHeight = inputHeight / 2;
		displayWidth = inputWidth / 2;
		System.out.print("\n[OK ] Observer started.");
		
		raam = new JFrame("Live feed from camera");
	    raam.setSize(displayWidth, displayHeight);
	    raam.setLocation(400, 100);
	    raam.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

	    sisu = raam.getContentPane();
	    sisu.setLayout(new FlowLayout());
	    
	    raam.setVisible(true);
	}

	public void onError(CaptureStream sender, CaptureException e)
	{	System.err.println("onError " + sender);
		e.printStackTrace();
	}


	public void onNewImage(CaptureStream sender, Image image)
	{	
		
		// convert and resize image from CaptureStream
		BufferedImage inputImage = AWTImageConverter.toBufferedImage(image);
		
		ImageProcessor ip = new ColorProcessor(inputImage);
		ImagePlus imp = new ImagePlus("pilt", ip);
		//IJ.run(imp, "Make Binary", "");
		//IJ.run(imp, "Find Edges", "");
		//IJ.run(imp, "Flip Horizontally", "");
		ip = imp.getProcessor();
		ip.setInterpolate(true);
		ip.setInterpolationMethod(ImageProcessor.BILINEAR);
		ImageProcessor ip_small = ip.resize(displayWidth, displayHeight);
		BufferedImage resizedImage2 = ip_small.getBufferedImage();
		try {
			ImageIO.write(ip.getBufferedImage(), "jpg", new File("imagejimage.jpg"));
		} catch (IOException e) {
			
		}
		
		
		// draw image and update canvas
		ImageIcon icon = new ImageIcon(resizedImage2);
		JLabel label = new JLabel("", icon, JLabel.CENTER);
		sisu.removeAll();
		sisu.add(label);
		System.out.printf("\n[OK ] frame %03d, ", ++frameCounter);
		sisu.validate();
		
		// display time
		long currentTime = System.nanoTime();
		NumberFormat nf = NumberFormat.getNumberInstance(Locale.UK);	// formatting issues
		DecimalFormat df = (DecimalFormat)nf;
		df.setRoundingMode(RoundingMode.HALF_UP);
		df.setMaximumFractionDigits(2);
		KaameraPilt.elapsed = (currentTime - KaameraPilt.startTime) * Math.pow(10, -9);
		KaameraPilt.elapsed = Double.parseDouble(df.format(KaameraPilt.elapsed));
		System.out.printf("t = %5.2f s", KaameraPilt.elapsed);
		
		if(KaameraPilt.isWritingFiles && (frameCounter % KaameraPilt.frameDensity == 0)) {
	
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
						System.out.println("\n[OK ] Created directory '\\" + folderName + "'");
					} else {
						throw(new Exception("Could not create directory."));
					}
				}
				
				String outFileName = folderName + "/" + fileNameBase + frameCounter + ".jpg";
				ImageIO.write(ip.getBufferedImage(), "jpg", new File(outFileName));
				System.out.print(", write success");
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
