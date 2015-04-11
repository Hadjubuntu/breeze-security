package breeze.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 
 * Breeze Security is a simple webcam video security system which take screenshot and analyze
 * picture to detect intruders.
 * 
 * fswebcam must be installed (sudo apt-get install fswebcam)
 * imagemagick also (sudo apt-get install imagemagick)
 * 
 * 
 * @author hadjsalah
 *
 */
public class BreezeSecurity {
	private int securityFreqHz;
	private Process proc;
	private String BS_DIR;
	
	public BreezeSecurity() {
		proc = null;
		securityFreqHz = 2;
		BS_DIR = "/home/adrien/breezesecurity/";
	}
	
	/**
	 * 
	 * @param filename
	 * @param width
	 * @param height
	 * @throws IOException
	 */
	public void takeScreenshot(String filename, int width, int height) throws IOException {
		Runtime.getRuntime().exec("cd "+BS_DIR);
		proc = Runtime.getRuntime().exec("fswebcam -r "+width+"x"+height+" --no-banner "+filename);
	}
	
	/**
	 * 
	 * @param angle
	 * @param filename
	 * @throws IOException
	 */
	public void rotate(int angle, String filename) throws IOException {
		Runtime.getRuntime().exec("cd "+BS_DIR);
		proc = Runtime.getRuntime().exec("convert -rotate "+angle+" "+filename+" output.jpg");
	}
	
	/**
	 * Send through FTP picture taken
	 * 
	 * @param host
	 * @param username
	 * @param pwd
	 * @param port
	 */
	public void sendFTP(String host, String username, String pwd, int port) {
		
	}
	
	protected void threatDetection() {
// Use lib to detect human TODO		
	}
	
	/**
	 * 
	 * @param p
	 * @throws IOException
	 */
	public void readOutput(Process p) throws IOException {
		String s = null;
		BufferedReader stdInput = new BufferedReader(new
				InputStreamReader(p.getInputStream()));

//		BufferedReader stdError = new BufferedReader(new
//				InputStreamReader(p.getErrorStream()));

		// read the output from the command
		while ((s = stdInput.readLine()) != null) {
			System.out.println(s);
		}
	}
	
	public int getFreqHz() {
		return securityFreqHz;
	}
	
	public static void main(String[] args) {
		Thread breezeSecurity = new Thread() {
			public void run() {
				BreezeSecurity bs = new BreezeSecurity();
				System.out.println("Breeze Security");
				System.out.println("-------------------------------");
				
				while (true) {
					try {
						String filename = "survey.jpg";
						bs.takeScreenshot(filename, 1024, 768);						
						bs.rotate(180, filename);
						bs.sendFTP("localhost", "test", "", 21);
						bs.threatDetection();
						
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					
					try {
						Thread.sleep((long) (1.0/bs.getFreqHz()*1000.0));
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				


				

			
			}
		};
		
		
		breezeSecurity.start();		
	}

	
}
