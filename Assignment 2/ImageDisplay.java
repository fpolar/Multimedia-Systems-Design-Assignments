
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.LinkedList;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	BufferedImage imgOne;
	BufferedImage imgTwo;
	int width = 352;
	int height = 288;

	int inY;
	int inU;
	int inV;
	int inQ;

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img, boolean modFlag)
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

			double[][] yChannel =  new double[height][width];
			double[][] uChannel =  new double[height][width];
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

					if(modFlag){
						int R = r & 0xFF;
						int G = g & 0xFF;
						int B = b & 0xFF;

						double[] yuvs = rgb2yuv(R, G, B);
						yChannel[y][x] = yuvs[0];
						uChannel[y][x] = yuvs[1];
						vChannel[y][x] = yuvs[2];
					}
				}
			}
			if(modFlag){

				//up-sampling each yuv channel
				for(int i = 0; i < height; i++) {
					for(int j = 0; j < width; j++) {
						if(inY !=0){
							yChannel = channelSample(yChannel, inY, width, i, j);
						}
						if(inU != 0){
							uChannel = channelSample(uChannel, inU, width, i, j);
						}
						if(inV != 0){
							vChannel = channelSample(vChannel, inV, width, i, j);
						}
					}
				}

				//init buckets befor quantization
				boolean qFlag = true;
				Integer[] bucket = null;
				if(inQ <= 256){
					double stepSize = 256/ (double) inQ;
					bucket = createBucketArray(stepSize);
				}else{
					qFlag = false;
				}

				//converting yuv to rgb, Quantization and writing image pixel by pixel
				for(int i = 0; i < height; i++) {
					for(int j = 0; j < width; j++) {
						
						int[] convertedRGB = yuv2rgb(yChannel[i][j], uChannel[i][j], vChannel[i][j]);
						int R = convertedRGB[0];
						int G = convertedRGB[1];
						int B = convertedRGB[2];

						if( qFlag) {
							int[] quantizedRGB = quantize(R, G, B, bucket);
							R = quantizedRGB[0];
							G = quantizedRGB[1];
							B = quantizedRGB[2];
						}					
						// int processedPixel = 0xff000000 | ((R & 0xff) << 16) | ((G & 0xff) << 8) | (B & 0xff);
						int processedPixel = 0xff000000 | ((R) << 16) | ((G) << 8) | (B);
						img.setRGB(j, i, processedPixel);
						// img.setRGB(i, j, processedPixel);
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
		inY = Integer.parseInt(args[1]);
		inU = Integer.parseInt(args[2]);
		inV = Integer.parseInt(args[3]);
		inQ = Integer.parseInt(args[4]);
		System.out.println("The parameters were: " + inY + " " + inU + " " + inV + " " + inQ);

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imgTwo = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne, false);
		readImageRGB(width, height, args[0], imgTwo, true);

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

	//Takes a Y U or V channel for an image as a 2D array 
	//and up samples it into the same arrays and returns it
	private double[][] channelSample(double[][] yuvChannel, int gap, int width, int i, int j) {

		int k = j % gap;

		if(k != 0) {
			int prev = j-k;
			int next = j+gap-k; 

			if(next < width) {
				double prevYUV = yuvChannel[i][prev];
				double currentYUV = yuvChannel[i][j];
				double nextYUV = yuvChannel[i][next];
				yuvChannel[i][j] = ((gap - k)* prevYUV + (k * nextYUV))/gap;		
			} else {
				double prevYUV = yuvChannel[i][prev];
				for(int m = prev+1; m < width; m++) {
					yuvChannel[i][m] = prevYUV;	
				}
			}

		}

		return yuvChannel;
	}


	public double[] rgb2yuv(int r, int g, int b) {
		double[] yuv = new double[3];

		yuv[0] = (0.299 * r + 0.587 * g + 0.114 * b);
		yuv[1] = (0.596 * r + (-0.274 * g) + (-0.322 * b));
		yuv[2] = (0.211 * r + (-0.523 * g) + (0.312 * b));

		return yuv;
	}

	public int[] yuv2rgb(double y, double u, double v) {
		int[] rgb = new int[3];

		rgb[0] = (int) (1.000 * y + 0.956 * u + 0.621 * v);
		rgb[1] = (int) (1.000 * y + (-0.272 * u) + (-0.647 * v));
		rgb[2] = (int) (1.000 * y + (-1.106 * u) + (1.703 * v));

		return rgb;
	}

	public Integer[] createBucketArray(double step) {
		LinkedList<Integer> list = new LinkedList<Integer>();
		double currentValue = 0;

		while(currentValue <= 255){
			currentValue = currentValue + step;
			list.add((int) Math.round(currentValue));
		}

		Integer[] bucket = new Integer[list.size()];
		bucket = list.toArray(bucket);

		return bucket;
	} 

	//quantizes the 3 rgb values using an averaging strategy with the given bucket layout
 	public int[] quantize(int r, int g, int b, Integer[] bucket) {

		for(int i=0; i < bucket.length-1; i++) {
			if(r >= bucket[i] && r <= bucket[i+1]){				
				int avg = (int) Math.round((bucket[i] + bucket[i+1])/(double)2);
				if(r < avg){
					r = bucket[i];
				}else{
					r = bucket[i+1];
				}
				break;
			}

			if(g >= bucket[i] && g <= bucket[i+1]){				
				int avg = (int) Math.round((bucket[i] + bucket[i+1])/(double)2);
				if(g < avg){
					g = bucket[i];
				}else{
					g = bucket[i+1];
				}
				break;
			}

			if(b >= bucket[i] && b <= bucket[i+1]){				
				int avg = (int) Math.round((bucket[i] + bucket[i+1])/(double)2);
				if(b < avg){
					b = bucket[i];
				}else{
					b = bucket[i+1];
				}
				break;
			}
		}

		//clamping
		if(r > 255){
			r = 255;
		}else if(r < 0){
			r = 0;
		}

		if(g > 255){
			g = 255;
		}else if(g < 0){
			g = 0;
		}

		if(b > 255){
			b = 255;
		}else if(b < 0){
			b = 0;
		}

		int[] rgb = new int[]{r, g, b};
		return rgb;
	}

	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}
