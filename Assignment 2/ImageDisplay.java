
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
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

	//java default RGB to HSB and vice versa for reference
	boolean refFlag = false;
	boolean color_val_debug_flag = false;

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

					int R = r & 0xFF;
					int G = g & 0xFF;
					int B = b & 0xFF;

					if(refFlag){

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

						int convertedRGB = Color.HSBtoRGB(hsvs[0], hsvs[1], hsvs[2]);
						//System.out.println("Out " + hsvs[0] + " " + hsvs[1] + " " + hsvs[2] + " " + convertedRGB);
						img.setRGB(x,y,convertedRGB);
					}else{
						double[] hsvs = rgb2hsv(R, G, B);
						if(hsvs[0]*360 < h1){
							//hsvs[0] = 0;
							hsvs[1] = 0;
						}
						if(hsvs[0]*360 > h2){
							//hsvs[0] = 0;
							hsvs[1] = 0;
						}

						double[] convertedRGBArr = hsv2rgb(hsvs[0], hsvs[1], hsvs[2]);
						R = (int)convertedRGBArr[0];
						G = (int)convertedRGBArr[1];
						B = (int)convertedRGBArr[2];
						// System.out.println("Out " + hsvs[0] + " " + hsvs[1] + " " + hsvs[2] + " " + convertedRGB);
						int convertedRGB = 0xff000000 | ((R) << 16) | ((G) << 8) | (B);
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

	    if(color_val_debug_flag){
			System.out.println("RGB IN - "+r+" "+g+" "+b );
		}

		double[] rgbNormalized = new double[3];
		double[] hsv = new double[3];

		rgbNormalized[0] = r;///255.0;
		rgbNormalized[1] = g;///255.0;
		rgbNormalized[2] = b;///255.0;

		double rgbPeak = Math.max(rgbNormalized[0], Math.max(rgbNormalized[1], rgbNormalized[2]));
		double rgbValley = Math.min(rgbNormalized[0], Math.min(rgbNormalized[1], rgbNormalized[2]));

		hsv[2] = rgbPeak;
		if(rgbPeak == rgbValley){
			hsv[0] = 0;
			hsv[1] = 0;
			return hsv;
		}

		if(r == rgbPeak){
			hsv[0] = ((rgbPeak - rgbNormalized[2]) - (rgbPeak - rgbNormalized[1]))/(rgbPeak-rgbValley);
		}
		else if(g == rgbPeak){
			hsv[0] = 2.0 + ((rgbPeak - rgbNormalized[0]) - (rgbPeak - rgbNormalized[2]))/(rgbPeak-rgbValley);
		}
		else{
			hsv[0] = 4.0 + ((rgbPeak - rgbNormalized[1]) - (rgbPeak - rgbNormalized[0]))/(rgbPeak-rgbValley);
		}
		hsv[0] = (hsv[0]/6.0)%1.0;

		if(rgbPeak == 0){
			hsv[1] = 0;
		}else{
			hsv[1] = (rgbPeak-rgbValley)/rgbPeak;
		}

	    if(color_val_debug_flag){
			System.out.println("HSV OUT - "+hsv[0]+" "+hsv[1]+" "+hsv[2] );
		}
		return hsv;
	}


	public double[] hsv2rgb(double h, double s, double v) {
		if(s == 0){
			double[] rgb = {v,v,v};
			return rgb;
		}
		double[] rgb = new double[3];
		double[] hsv = new double[3];
		int i = (int)(h*6.0);
		double delta = h*6.0 - i;
		double cA = v*(1.0-s);
		double cB = v*(1.0-s*delta);
		double cC = v*(1.0-s*(1.0-delta));
		i = i%6;

	    if (i == 0){
	    	rgb[0] = v;
	    	rgb[1] = cC;
	    	rgb[2] = cA;
	    }else if (i == 1){
	    	rgb[0] = cB;
	    	rgb[1] = v;
	    	rgb[2] = cA;
	    }else if (i == 2){
	    	rgb[0] = cA;
	    	rgb[1] = v;
	    	rgb[2] = cC;
	    }else if (i == 3){
	    	rgb[0] = cA;
	    	rgb[1] = cB;
	    	rgb[2] = v;
	    }else if (i == 4){
	    	rgb[0] = cC;
	    	rgb[1] = cA;
	    	rgb[2] = v;
	    }else if (i == 5){
	    	rgb[0] = v;
	    	rgb[1] = cA;
	    	rgb[2] = cB;
	    }



	    if(rgb[0] == 0){
		    System.out.println("R = 0: "+i);
		}

	    if(color_val_debug_flag){
		    System.out.println("RGB OUT - "+rgb[0] + " " + rgb[1] + " " +rgb[2]);
		}
		return rgb;
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}
