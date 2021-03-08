
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.LinkedList;
import java.awt.Color;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage imgOne;
	BufferedImage imgTwo;
	// int width = 352;
	// int height = 288;
	int width = 512;
	int height = 512;

	//java default RGB to HSB
	boolean refFlag = true;

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageHSV(int width, int height, String imgPath, BufferedImage img, int h1, int h2)
	{
		try
		{
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

			double[][] hChannel =  new double[height][width];
			double[][] sChannel =  new double[height][width];
			double[][] vChannel =  new double[height][width];

			int ind = 0;
			for(int y = 0; y < height; y++)
			{
				for(int x = 0; x < width; x++)
				{
					byte a = 0;
					byte r = bytes[ind];
					byte g = bytes[ind+height*width];
					byte b = bytes[ind+height*width*2]; 

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					img.setRGB(x,y,pix);
					ind++;

					if(refFlag){
						int R = r & 0xFF;
						int G = g & 0xFF;
						int B = b & 0xFF;

						//float to double scares me
						float[] hsvs = Color.RGBtoHSB(R, G, B, null);
						if(hsvs[0]*360 < h1){
							// System.out.println("h1 thresh: " + hsvs[0]);
							// hsvs[0] = 0;
							hsvs[1] = 0;
						}
						if(hsvs[0]*360 > h2){
							// System.out.println("h2 thresh: " + hsvs[0]);
							// hsvs[0] = 0;
							hsvs[1] = 0;
						}
						hChannel[y][x] = (double)hsvs[0];
						sChannel[y][x] = (double)hsvs[1];
						vChannel[y][x] = (double)hsvs[2];

						int convertedRGB = Color.HSBtoRGB(hsvs[0], hsvs[1], hsvs[2]);
						// System.out.println("Out " + hsvs[0] + " " + hsvs[1] + " " + hsvs[2] + " " + convertedRGB);
						img.setRGB(x,y,convertedRGB);
					}
				}
			}
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	public void showIms(String[] args){

		// Read a parameter from command line
		int h1 = Integer.parseInt(args[1]);
		int h2 = Integer.parseInt(args[2]);
		System.out.println("The parameters were: " + h1 + " " + h2);

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imgTwo = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageHSV(width, height, args[0], imgOne, 0, 359);
		readImageHSV(width, height, args[0], imgTwo, h1, h2);

		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOne));
		lbIm2 = new JLabel(new ImageIcon(imgTwo));

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.gridx = 0;
		c.gridy = 0;

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		frame.getContentPane().add(lbIm1, c);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 1;
		frame.getContentPane().add(lbIm2, c);

		frame.pack();
		frame.setVisible(true);
	}

	public double[] rgb2hsv(int r, int g, int b) {
		double[] rgbNormalized = new double[3];
		double[] hsv = new double[3];

		rgbNormalized[0] = r/255.0;
		rgbNormalized[1] = g/255.0;
		rgbNormalized[2] = b/255.0;

		double rgbPeak = Math.max(rgbNormalized[0], Math.max(rgbNormalized[1], rgbNormalized[2]));
		double rgbValley = Math.min(rgbNormalized[0], Math.min(rgbNormalized[1], rgbNormalized[2]));
		if(rgbPeak == rgbValley){
			hsv[0] = 0;
		}else{
			for(int x = 0; x < 3; x++)
			{
				if(rgbPeak == rgbNormalized[x]){
					System.out.println(x+" "+(x+1)%3+""+(x+2)%3);
					hsv[0] = (60 * (( rgbNormalized[(x+1)%3] - rgbNormalized[(x+2)%3] )/(rgbPeak-rgbValley)) +360)%360;
					break;
				}
			}
		}

		if(rgbPeak == 0){
			hsv[1] = 0;
		}else{
			hsv[1] = (rgbPeak-rgbValley)/rgbPeak;
		}

		hsv[2] = rgbPeak * 100;

		return hsv;
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}
