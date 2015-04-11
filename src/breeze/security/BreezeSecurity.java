package breeze.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

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
 * imagemagick also (sudo apt-get install imagemagick)
 * 
 * 
 * @author hadjsalah
 *
 */
public class BreezeSecurity {
	private int updateMs;
	private Process proc;
	private String BS_DIR;
	private long lastSendFTPMs;

	public BreezeSecurity(String host, String username, String pwd, int port) throws Exception {
		proc = null;
		updateMs = 1800; // 1.8s
		lastSendFTPMs = System.currentTimeMillis();
		BS_DIR = "/home/adrien/breezesecurity/";
		initFTPUploader(host, username, pwd);
	}


	/**
	 * 
	 * @param filename
	 * @param width
	 * @param height
	 * @throws IOException
	 */
	public void takeScreenshot(String filename, int width, int height) throws IOException {
		// option no banner
		proc = Runtime.getRuntime().exec("fswebcam  --rotate 180 -r "+width+"x"+height+" "+BS_DIR+filename);
	}

	/**
	 * 
	 * @param angle
	 * @param filename
	 * @throws IOException
	 */
	public void rotate(int angle, String filename, String outputfilename) throws IOException {
		proc = Runtime.getRuntime().exec("chmod 755 " + BS_DIR+filename);
		proc = Runtime.getRuntime().exec("convert -rotate "+angle+" "+BS_DIR+filename+" "+BS_DIR+outputfilename);
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
	public void sendFTP(String filename) throws Exception {
		if ((System.currentTimeMillis()-lastSendFTPMs) > 10000) {
// To have no corrupted file / TODO check file
			proc = Runtime.getRuntime().exec("cp "+BS_DIR+filename + " " + BS_DIR+"tmp.jpg");
			Thread.sleep(1000);
			
			System.out.println("Start sending FTP");
			proc = Runtime.getRuntime().exec("chmod 755 " + BS_DIR+"tmp.jpg");

			uploadFile(BS_DIR+"tmp.jpg", filename, "/www/");
		
			lastSendFTPMs = System.currentTimeMillis();
		}
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


						while (deamonOn) {
							try {
								String filename = "survey.jpg";
								bs.takeScreenshot(filename, 640, 480);	
								bs.sleepMs(2000);
								
//								bs.rotate(180, filename);
//								bs.sleepMs(2000);
								
								bs.sendFTP(filename);
								bs.sleepMs(5000);
								
								bs.threatDetection();

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

	FTPClient ftp = null;

	public void initFTPUploader(String host, String user, String pwd) throws Exception{
		ftp = new FTPClient();
		ftp.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
		int reply;
		ftp.connect(host);
		reply = ftp.getReplyCode();
		if (!FTPReply.isPositiveCompletion(reply)) {
			ftp.disconnect();
			throw new Exception("Exception in connecting to FTP Server");
		}
		ftp.login(user, pwd);
		ftp.setFileType(FTP.BINARY_FILE_TYPE);
		ftp.enterLocalPassiveMode();
	}
	public void uploadFile(String localFileFullName, String fileName, String hostDir)
			throws Exception {
		InputStream input = new FileInputStream(new File(localFileFullName));

		if (input != null) {
			this.ftp.storeFile(hostDir + fileName, input);
		}
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


}
