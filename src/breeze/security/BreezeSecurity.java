package breeze.security;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.imageio.ImageIO;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * 
 * Breeze Security is a simple webcam video security system which take screenshot and analyze
 * picture to detect intruders.
 * 
 * fswebcam must be installed (sudo apt-get install fswebcam)
 * 
 * 
 * @author hadjsalah
 *
 */
public class BreezeSecurity {
	// Parameters
	//------------------------------
	private int updateMs;
	private Process proc;
	private String BS_DIR;

	// FTP
	//------------------------------
	private String ftp_params[];
	private FTPClient ftp;
	private long lastFtpSendMs;
	private int UPLOAD_DELAY_MS = 8000;

	// Variables
	//------------------------------
	private int widthPx;
	private int heightPx;
	private BufferedImage previousImg;
	private int THRESHOLD_DETECTOR;
	private int intruderIdx;

	public BreezeSecurity(String host, String username, String pwd, int port) throws Exception {
		proc = null;
		updateMs = 500; // 0.5s
		THRESHOLD_DETECTOR = 20;
		intruderIdx = 0;
		widthPx = 640;
		heightPx = 480;
		BS_DIR = "/home/adrien/breezesecurity/";
		previousImg = null;

		ftp = null;
		ftp_params = new String[5];
		ftp_params[0] = host;
		ftp_params[1] = username;
		ftp_params[2] = pwd;
		lastFtpSendMs = System.currentTimeMillis();

		initFTPUploader();
	}


	/**
	 * 
	 * @param filename
	 * @param width
	 * @param height
	 * @throws IOException
	 */
	public void takeScreenshot(String filename) throws IOException {
		
		File f = new File(BS_DIR + filename);
		if (f.exists()) {
			proc = Runtime.getRuntime().exec("cp "+BS_DIR+filename + " " + BS_DIR+"previous.jpg");
			readOutput(proc);
		}
		
		// option no banner
		proc = Runtime.getRuntime().exec("fswebcam  --rotate 180 -r "+widthPx+"x"+heightPx+" "+BS_DIR+filename);
		readOutput(proc);
	}


	/**
	 * Send through FTP picture taken
	 * 
	 * @param host
	 * @param username
	 * @param pwd
	 * @param port
	 * @throws Exception 
	 */
	public void sendFTP(String filename, String serverFilename) throws Exception {

		// To have no corrupted file / TODO check file
		proc = Runtime.getRuntime().exec("cp "+BS_DIR+filename + " " + BS_DIR+"tmp.jpg");
		readOutput(proc);

		System.out.println("Start sending FTP");
		proc = Runtime.getRuntime().exec("chmod 755 " + BS_DIR+"tmp.jpg");
		readOutput(proc);
		
		// TODO java copy of file
		// Using thread and isPendingTransaction to wait for next upload with a buffer
		uploadFile(BS_DIR+"tmp.jpg", serverFilename, "/www/");
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

		BufferedReader stdError = new BufferedReader(new
				InputStreamReader(p.getErrorStream()));

		// read the output from the command
		while ((s = stdInput.readLine()) != null) {
			// System.out.println(s);
		}

		// read the error from the command
		while ((s = stdError.readLine()) != null) {
			// System.out.println(s);
		}
	}

	public int getUpdateMs() {
		return updateMs;
	}

	public void sleepMs(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static void main(final String[] args) {
		Thread breezeSecurity = new Thread() {
			public void run() {

				System.out.println("Breeze Security");
				System.out.println("-------------------------------");

				if (args.length == 0) {
					System.out.println("missing password ftp in parameter");
				}
				else {
					boolean deamonOn = true;
					BreezeSecurity bs;
					try {
						bs = new BreezeSecurity("ftp.breezeuav.com", "breezeuaca", args[0], 21);

						// Calibration
						//-----------------------------------------------
						System.out.println("Initialize by throwing some screenshot to wait for cam stabilization");
						int nbScreenshotThrows = 3;
						for (int i = 0; i < nbScreenshotThrows; i ++) {
							String filename = "survey.jpg";
							bs.takeScreenshot(filename);
						}
						System.out.println("Calibration done - Survey will begin in few seconds");

						// Loop
						//-----------------------------------------------
						while (deamonOn) {
							try {
								String filename = "survey.jpg";
								bs.takeScreenshot(filename);

								if (System.currentTimeMillis() - bs.lastFtpSendMs > bs.UPLOAD_DELAY_MS) {
									bs.sendFTP(filename, filename);
									bs.lastFtpSendMs = System.currentTimeMillis();
								}

								bs.threatDetection(filename);

							} catch (IOException e1) {
								e1.printStackTrace();
							} catch (Exception e) {
								e.printStackTrace();
							}


						}

						bs.disconnect();

					} catch (Exception e2) {
						e2.printStackTrace();
					}

				}




			}
		};


		breezeSecurity.start();		
	}


	public void initFTPUploader() throws Exception{
		ftp = new FTPClient();
		ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
		int reply;
		ftp.connect(ftp_params[0]);
		reply = ftp.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			ftp.disconnect();
			throw new Exception("Exception in connecting to FTP Server");
		}
		ftp.login(ftp_params[1], ftp_params[2]);
		ftp.setFileType(FTP.BINARY_FILE_TYPE);
		ftp.enterLocalPassiveMode();
	}
	public void uploadFile(String localFileFullName, String fileName, String hostDir) throws Exception
	{
		try {
			InputStream input = new FileInputStream(new File(localFileFullName));

			if (input != null) {
				this.ftp.storeFile(hostDir + fileName, input);
			}
		}
		catch (Exception e) {
			if (ftp.isConnected() == false) {
				ftpReconnect();
			}
		}
	}

	private void ftpReconnect() throws Exception {
		initFTPUploader();
	}


	public void disconnect(){
		if (this.ftp.isConnected()) {
			try {
				this.ftp.logout();
				this.ftp.disconnect();
			} catch (IOException f) {
				// do nothing as file is already saved to server
			}
		}
	}


	protected void threatDetection(String filename) {
		int nbCells = 10;
		int cellWidthPx = widthPx / nbCells;
		int cellHeightPx = heightPx / nbCells;
		int currentStartX = 0;
		int currentStartY = 0;

		BufferedImage img = null;

		try {
			img = ImageIO.read(new File(BS_DIR+filename));
			File previous = new File(BS_DIR+"previous.jpg");

			if (previous.exists()) {
				previousImg = ImageIO.read(previous);

				boolean intruderDetected = false;
				int diffAverage = 0, diffMax = 0;
				// For each blox
				int i = 0;
				int j = 0;

				while (i < nbCells && intruderDetected == false) {

					currentStartX = i * cellWidthPx;

					while (j < nbCells && intruderDetected == false) {

						currentStartY = j * cellHeightPx;

						BufferedImage subImage = img.getSubimage(currentStartX, currentStartY, cellWidthPx, cellHeightPx);
						BufferedImage previousSubImage = previousImg.getSubimage(currentStartX, currentStartY, cellWidthPx, cellHeightPx);

						int averageRGBImage = averageRGV(subImage, cellWidthPx, cellHeightPx);
						int averageRGBPreviousImage = averageRGV(previousSubImage, cellWidthPx, cellHeightPx);

						diffAverage = Math.abs(averageRGBPreviousImage-averageRGBImage);
						
						if (diffAverage > THRESHOLD_DETECTOR) {
							intruderDetected = true;
						}
						if (diffAverage > diffMax) {
							diffMax = diffAverage;
						}
						
						j++;
					}

					i++;
				}

				

				if (intruderDetected) {
					sendFTP(filename, "intruder-"+intruderIdx+".jpg");
					intruderIdx ++;

					System.out.println("INTRUDER #"+intruderIdx+" DETECTED");
					System.out.println("diff_average=" + diffAverage);

					if (intruderIdx > 100) {
						intruderIdx = 0;
					}
				}
				else {
					System.out.println("Home secure");
					System.out.println("diff_max=" + diffMax);
				}

			}

		} catch (IOException e) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private int averageRGV(BufferedImage img, int w, int h) {
		int output = 0;
		int pixels[];

		for (int i = 0; i < w; i ++) {
			for (int j = 0; j < h; j ++) {
				pixels = img.getRaster().getPixel(i, j, new int[3]);
				output += (int)((pixels[0] + pixels[1] + pixels[2]) / 3.0);
			}
		}

		return (int)(output / (w*h));
	}

}
