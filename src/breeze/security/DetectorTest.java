package breeze.security;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class DetectorTest {
	private String BS_DIR = "/home/hadjsalah/Downloads/";
	private String image1 = "img1.jpg";
	private String image2 = "img2.jpg";
	private int widthPx;
	private int heightPx;
	private int intruderIdx;
	private int THRESHOLD_DETECTOR;

	public static void main(String[] args) {
		DetectorTest dt = new DetectorTest();
		dt.run();
	}

	public DetectorTest() {
		widthPx = 640;
		heightPx = 480;	
		intruderIdx = 0;
		THRESHOLD_DETECTOR = 4;
	}

	public void run() {
		threatDetection(image1, image2);
	}

	protected void threatDetection(String filename, String previousFilename) {
		int nbCells = 20;
		int cellWidthPx = widthPx / nbCells;
		int cellHeightPx = heightPx / nbCells;
		int currentStartX = 0;
		int currentStartY = 0;

		BufferedImage img = null;
		BufferedImage previousImg = null;

		try {
			img = ImageIO.read(new File(BS_DIR+filename));
			previousImg = ImageIO.read(new File(BS_DIR+previousFilename));

			if (previousImg != null) {
				boolean intruderDetected = false;
				int diffAverage = 0;
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

						diffAverage += Math.abs(averageRGBPreviousImage-averageRGBImage);

						

						j++;
					}

					i++;
				}

				if (diffAverage > THRESHOLD_DETECTOR) {
					intruderDetected = true;
				}
				
				if (intruderDetected) {
					//	sendFTP(filename, "intruder-"+intruderIdx+".jpg");
					intruderIdx ++;

					System.out.println("INTRUDER #"+intruderIdx+" DETECTED WITH DIFF " + diffAverage);

					if (intruderIdx > 100) {
						intruderIdx = 0;
					}
				}
				else {
					System.out.println("diff = " + diffAverage);
				}
				
			}

			previousImg = img;
		} catch (IOException e) {
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
