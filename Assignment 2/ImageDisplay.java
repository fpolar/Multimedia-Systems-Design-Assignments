
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
	int width = 512;
	int height = 512;

	//debug flag
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
					
					img.setRGB(x,y,pix);
					ind++;

					int R = r & 0xFF;
					int G = g & 0xFF;
					int B = b & 0xFF;

					double[] hsvs = rgb2hsv(R, G, B);
					if(hsvs[0] < h1){
						hsvs[1] = 0;
					}
					if(hsvs[0] > h2){
						hsvs[1] = 0;
					}

					double[] convertedRGBArr = hsv2rgb(hsvs[0], hsvs[1], hsvs[2]);
					R = (int)convertedRGBArr[0];
					G = (int)convertedRGBArr[1];
					B = (int)convertedRGBArr[2];
					int convertedRGB = 0xff000000 | ((R) << 16) | ((G) << 8) | (B);
					img.setRGB(x,y,convertedRGB);
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
		
		//color_val_debug_flag = true;
		
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

		double rD = r/255.0;
		double gD = g/255.0;
		double bD = b/255.0;

		//Max and Mins for rgb values
		//Int Max stored as well to avoid float comparisons
		int rgbPeakInt = Math.max(r, Math.max(g, b));
		double rgbPeak = Math.max(rD, Math.max(gD, bD));
		double rgbValley = Math.min(rD, Math.min(gD, bD));

		//saturation and value
		hsv[2] = rgbPeak;

		if(rgbPeak==0){
			hsv[1] = 0;
		}else{
			hsv[1] = (rgbPeak-rgbValley)/rgbPeak;
		}

		if(rgbPeak == rgbValley){
			hsv[0] = 0;
			return hsv;
		}

		//getting hue values
		if(r == rgbPeakInt){
			hsv[0] = (gD - bD) / (rgbPeak-rgbValley);
		}
		else if(g == rgbPeakInt){
			hsv[0] = 2.0 + (bD - rD) / (rgbPeak-rgbValley);
		}
		else{
			hsv[0] = 4.0 + (rD - gD) / (rgbPeak-rgbValley);
		}
		hsv[0]*=60;

		//clamping h
		//red someimtes goes negative so clamp both sides to high end
		//I noticed some red bleed at low h1 vals, possibly caused by this
		if(hsv[0]>359 || hsv[0]<0) hsv[0] = 359;

	    if(color_val_debug_flag){
			System.out.println("HSV OUT - "+hsv[0]+" "+hsv[1]+" "+hsv[2] );
		}
		return hsv;
	}


	public double[] hsv2rgb(double h, double s, double v) {

		double[] rgb = new double[3];
		double[] hsv = new double[3];

		double hP = h/60.0;
		double c = v * s;
		double x = c * (1-Math.abs(hP%2-1));

	    if (hP < 0){
	    	rgb[0] = 0;
	    	rgb[1] = 0;
	    	rgb[2] = 0;
	    }else if (hP <=1){
	    	rgb[0] = c;
	    	rgb[1] = x;
	    	rgb[2] = 0;
	    }else if (hP <=2){
	    	rgb[0] = x;
	    	rgb[1] = c;
	    	rgb[2] = 0;
	    }else if (hP <=3){
	    	rgb[0] = 0;
	    	rgb[1] = c;
	    	rgb[2] = x;
	    }else if (hP <=4){
	    	rgb[0] = 0;
	    	rgb[1] = x;
	    	rgb[2] = c;
	    }else if (hP <=5){
	    	rgb[0] = x;
	    	rgb[1] = 0;
	    	rgb[2] = c;
	    }else if(hP <=6){
	    	rgb[0] = c;
	    	rgb[1] = 0;
	    	rgb[2] = x;
	    }
	    rgb[0]+=v-c;
	    rgb[1]+=v-c;
	    rgb[2]+=v-c;
	    rgb[0]*=255;
	    rgb[1]*=255;
	    rgb[2]*=255;

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
