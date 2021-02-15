
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

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img)
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
		int inY = Integer.parseInt(args[1]);
		int inU = Integer.parseInt(args[2]);
		int inV = Integer.parseInt(args[3]);
		int inQ = Integer.parseInt(args[4]);
		System.out.println("The parameters were: " + inY + " " + inU + " " + inV + " " + inQ);

		// Read in the specified image
		imgOne = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imgTwo = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readImageRGB(width, height, args[0], imgOne);

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

	//TODO understand and modify, I think:
	//Takes YUV channels for an image as a 2D array 
	//and up samples them into the same arrays
	//u
	public void upSample(double[][] yChannel, double[][] uChannel, double[][] vChannel, int gap, int width, int i, int j, Sample sample) {

		int k = j % gap;

		if(k != 0) {
			int prev = j-k;
			int next = j+gap-k; 

			if(next < width) {
				double prevY = yChannel[i][prev];
				double prevU = uChannel[i][prev];
				double prevV = vChannel[i][prev];
				double currentY = yChannel[i][j];
				double currentU = uChannel[i][j];
				double currentV = vChannel[i][j];
				double nextY = yChannel[i][next];
				double nextU = uChannel[i][next];
				double nextV = vChannel[i][next];
				
				//TODO y r these different?				
				if(sample == Sample.SAMPLE_Y) {
					//currentYUV.y = (prevYUV.y + nextYUV.y)/2;
					yChannel[i][j] = ((gap - k)* prevY + (k * nextY))/gap;
				}else if(sample == Sample.SAMPLE_U) {
					//currentYUV.u = (prevYUV.u + nextYUV.u)/2;
					uChannel[i][j] = ((gap - k)* prevU + (k * nextU))/gap;
				}else if(sample == Sample.SAMPLE_V) {
					//currentYUV.v = (prevYUV.v + nextYUV.v)/2;
					vChannel[i][j] = ((gap - k)* prevV + (k * nextV))/gap;
				}				
			} else {
				//System.out.println("else-> prev = "+ prev + " next ="+next+" k="+k);
				double prevY = yChannel[i][prev];
				double prevU = uChannel[i][prev];
				double prevV = vChannel[i][prev];

				for(int m = prev+1; m < width; m++) {
					if(sample == Sample.SAMPLE_Y) {
						yChannel[i][j] = prevY;
					}else if(sample == Sample.SAMPLE_U) {
						uChannel[i][j] = prevU;
					}else if(sample == Sample.SAMPLE_V) {
						vChannel[i][j] = prevV;
					}
				}
			}

		}

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
		int value = 0;

		list.add(value);
		while(true){
			currentValue = currentValue + step;
			value = (int) Math.round(currentValue);

			if(value > 255){
				break;
			}
			list.add(value);
		}

		Integer[] bucket = new Integer[list.size()];
		bucket = list.toArray(bucket);

		return bucket;
	}

	//TEST FOR ACCURACT VS 1
	public Integer[] createBucketArray2(double step) {
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

	//TODO try dividing method
	public Integer[] createBucketArray3(double step) {
		LinkedList<Integer> list = new LinkedList<Integer>();
		double currentValue = 0;
		int value = 0;

		list.add(value);
		while(true){
			currentValue = currentValue + step;
			value = (int) Math.round(currentValue);

			if(value > 255){
				break;
			}
			list.add(value);
		}

		Integer[] bucket = new Integer[list.size()];
		bucket = list.toArray(bucket);

		return bucket;
	}

 	public int[] quantize(int r, int g, int b, Integer[] bucket) {

		for(int i=0; i < bucket.length-1; i++) {
			if(r >= bucket[i] && r <= bucket[i+1]){				
				int mean = (int) Math.round((bucket[i] + bucket[i+1])/(double)2);
				if(r < mean){
					r = bucket[i];
				}else{
					r = bucket[i+1];
				}
				break;
			}

			if(g >= bucket[i] && g <= bucket[i+1]){				
				int mean = (int) Math.round((bucket[i] + bucket[i+1])/(double)2);
				if(g < mean){
					g = bucket[i];
				}else{
					g = bucket[i+1];
				}
				break;
			}

			if(b >= bucket[i] && b <= bucket[i+1]){				
				int mean = (int) Math.round((bucket[i] + bucket[i+1])/(double)2);
				if(b < mean){
					b = bucket[i];
				}else{
					b = bucket[i+1];
				}
				break;
			}
		}
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
