
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.util.*;
import java.awt.Color;
import static java.lang.Math.cos;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	JLabel lbIm2;
	JLabel lbIm3;
	BufferedImage imgOG;
	BufferedImage imgDCT;
	BufferedImage imgDWT;
	int width = 512;
	int height = 512;

	static double[][] cosMatrix;

	int[][] rChannel;
	int[][] gChannel;
	int[][] bChannel;

	int[][] rChannelDCT;
	int[][] gChannelDCT;
	int[][] bChannelDCT;

	int[][] rChannelIDCT;
	int[][] gChannelIDCT;
	int[][] bChannelIDCT;

	double[][] rChannelDWT;
	double[][] gChannelDWT;
	double[][] bChannelDWT;

	int[][] rChannelIDWT;
	int[][] gChannelIDWT;
	int[][] bChannelIDWT;

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readAndCompressImage(int width, int height, String imgPath, int n)
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
					
					imgOG.setRGB(x,y,pix);
					ind++;
				}
			}

			//Calculate the initial cosine transform values in matrix
			for(int i = 0;i<8;i++) {
				for(int j = 0;j<8;j++) {
					cosMatrix[i][j] = cos((2*i+1)*j*3.14159/16.00);
					// cosMatrix[i][j] = cos((2*i+1)*j*MATH.PI/16.00);
				}
			}

			if(n != -1){
				int m = n/4096;
				System.out.println("m = "+ m + " n="+n);
				
				//Discrete Cosine Transform (DCT): Do DCT and Zig-Zag traversal
				DCT(rChannel, gChannel, bChannel, m);
				
				//Do IDCT
				IDCT();

				//Discrete Wavelet Transform (DWT): Do DWT and Zig-Zag traversal
				rChannelDWT = DWT(rChannel, n);
				gChannelDWT = DWT(gChannel, n);
				bChannelDWT = DWT(bChannel, n);

				rChannelIDWT = IDWT(rChannelDWT);
				gChannelIDWT = IDWT(gChannelDWT);
				bChannelIDWT = IDWT(bChannelDWT);


			}else{
				int iteration = 1;
				for(int i=4096; i <= 512*512; i=i+4096) {
					
					n = i;
					int m = n/4096;
					System.out.println("iteration="+ iteration + " n = "+ i + " m="+m);
					
					//Discrete Cosine Transform (DCT): Do DCT and Zig-Zag traversal
					DCT(rChannel, gChannel, bChannel, m);

					//Do IDCT
					IDCT();

					//Discrete Wavelet Transform (DWT): Do DWT and Zig-Zag traversal
					rChannelDWT = DWT(rChannel, n);
					gChannelDWT = DWT(gChannel, n);
					bChannelDWT = DWT(bChannel, n);

					rChannelIDWT = IDWT(rChannelDWT);
					gChannelIDWT = IDWT(gChannelDWT);
					bChannelIDWT = IDWT(bChannelDWT);
				}
			}	

			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					//Display IDCT matrix
					int r = rChannelIDCT[i][j];
					int g = gChannelIDCT[i][j];
					int b = bChannelIDCT[i][j];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					imgDCT.setRGB(j,i,pix);
					
					//Display IDWT matrix
					int rr = (int) rChannelIDWT[i][j];
					int gg = (int) gChannelIDWT[i][j];
					int bb = (int) bChannelIDWT[i][j];

					int pixx = 0xff000000 | ((rr & 0xff) << 16) | ((gg & 0xff) << 8) | (bb & 0xff);
					imgDWT.setRGB(j,i,pixx);					
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

	private double[][] transpose(double[][] matrix) {
		double[][] temp = new double[height][width];
		for(int i=0; i<height; i++) {
			for(int j=0; j< width; j++) {
				temp[i][j] = matrix[j][i]; 
			}
		}
		return temp;
	}

	private double[] comp(double[] array) {
		int h = 1;
		while (h <= array.length) {
			array = compStep(array, h);
			h = h*2;
		}
		return array;
	}

	private double[] compStep(double[] array, int h) {
		double[] dArray = Arrays.copyOf(array, array.length);
		for(int i=0; i < h/2; i++) {
			dArray[2*i] = array[i] + array[h/2 + i];
			dArray[2*i + 1] = array[i] - array[h/2 + i];
		}		
		return dArray;
	}


	private double[] decomposition(double[] array) {
		int h = array.length;
		while (h > 0) {
			array = decompStep(array, h);
			h = h/2;
		}
		return array;
	}

	private double[] decompStep(double[] array, int h) {
		double[] dArray = Arrays.copyOf(array, array.length);
		for(int i=0; i < h/2; i++) {
			//Low 
			dArray[i] = (array[2*i] + array[2*i + 1]) / 2;		
			//High	
			dArray[h/2 + i] = (array[2*i] - array[2*i + 1]) / 2;	
		}
		return dArray;
	}

	public double[][] zigZag(double[][] matrix, int m) {
		int i = 0;
		int j = 0;
		int length = matrix.length-1;
		int count = 1;

		//for upper triangle of matrix
		if(count > m){
			matrix[i][j]=0; count++;
		}else{
			count++;
		}

		while(true) {

			j++;
			if(count > m){
				matrix[i][j]=0; count++;
			}else{
				count++;
			}

			while(j!=0) {
				i++;
				j--;

				if(count > m){
					matrix[i][j]=0; count++;
				}else{
					count++;
				}
			}
			i++;
			if(i > length) {
				i--;
				break;
			}

			if(count > m){
				matrix[i][j]=0; count++;
			}else{
				count++;
			}

			while(i!=0) {
				i--;
				j++;
				if(count > m){
					matrix[i][j]=0; count++;
				}else{
					count++;
				}
			}
		}

		//for lower triangle of matrix
		while(true) {
			j++;
			if(count > m){
				matrix[i][j]=0; count++;
			}else{
				count++;
			}

			while(j != length)
			{
				j++;
				i--;

				if(count > m){
					matrix[i][j]=0; count++;
				}else{
					count++;
				}
			}
			i++;
			if(i > length)
			{
				i--;
				break;
			}

			if(count > m){
				matrix[i][j]=0; count++;
			}else{
				count++;
			}

			while(i != length)
			{
				i++;
				j--;
				if(count > m){
					matrix[i][j]=0; count++;
				}else{
					count++;
				}
			}
		}
		return matrix;
	}

	private double[][] DWT(int[][] matrix,int n) {
		//Copy to a double matrix
		double[][] dMatrix = new double[height][width];
		for(int i=0; i<height; i++)
			for(int j=0; j<width; j++)
				dMatrix[i][j] = matrix[i][j];

		//All rows first
		for(int row=0; row < width; row++){
			dMatrix[row] = decomposition(dMatrix[row]);
		}		

		//Then all columns
		dMatrix = transpose(dMatrix);
		for(int col=0; col < height; col++) {
			dMatrix[col] = decomposition(dMatrix[col]);
		}		
		dMatrix = transpose(dMatrix);		

		//Do Zig Zag traversal
		dMatrix = zigZag(dMatrix, n);

		return dMatrix;
	}

	private int[][] IDWT(double[][] matrix) {
		int[][] iMatrix = new int[height][width];

		//First Columns
		matrix = transpose(matrix);
		for(int col=0; col < height; col++) {
			matrix[col] = comp (matrix[col]);
		}	
		matrix = transpose(matrix);

		//Then all rows
		for(int row=0; row < width; row++){
			matrix[row] = comp(matrix[row]);
		}

		//Copy the comp matrix to int matrix of R,G,B values. Limit values 0 to 255
		for(int i=0; i < height; i++){
			for(int j=0; j<width; j++){
				iMatrix[i][j] = (int) Math.round(matrix[i][j]);
				if(iMatrix[i][j] < 0){
					iMatrix[i][j] = 0;
				}
				if(iMatrix[i][j] > 255){
					iMatrix[i][j] = 255;
				}
			}
		}

		return iMatrix;
	}

	// private void DCT(int[][] rChannel, int[][] gChannel, int[][] bChannel, double[][] cosMatrix, double[][] outRChannel, double[][] outGChannel, double[][] outBChannel, int m) {
	private void DCT(int[][] rChannel, int[][] gChannel, int[][] bChannel, int m) {

		int height = 512;
		int width = 512;

		for(int i = 0; i < height; i+=8) {
			for(int j = 0; j < width;j+=8) { 

				//Store block values
				double[][] rBlock = new double[8][8];
				double[][] gBlock = new double[8][8];
				double[][] bBlock = new double[8][8];

				for(int u = 0; u < 8; u++) {
					for(int v = 0; v < 8; v++) {  

						float cu = 1.0f, cv = 1.0f;
						float rResult = 0.00f, gResult = 0.00f, bResult = 0.00f;

						if(u == 0)
							cu =  0.707f;
						if(v == 0)
							cv = 0.707f;

						for(int x = 0; x<8; x++) {
							for(int y = 0;y<8;y++) { 

								int iR, iG, iB;                                

								iR = (int) rChannel[i+x][j+y];
								iG = (int) gChannel[i+x][j+y];
								iB = (int) bChannel[i+x][j+y];

								rResult += iR*cosMatrix[x][u]*cosMatrix[y][v];
								gResult += iG*cosMatrix[x][u]*cosMatrix[y][v];
								bResult += iB*cosMatrix[x][u]*cosMatrix[y][v];

							}
						}//end x  

						rBlock[u][v] = (int) Math.round(rResult * 0.25*cu*cv);
						gBlock[u][v] = (int) Math.round(gResult * 0.25*cu*cv);
						bBlock[u][v] = (int) Math.round(bResult * 0.25*cu*cv);
					}//end v
				}//end u

				//Do Zig Zag traversal
				rBlock = zigZag(rBlock, m);
				gBlock = zigZag(gBlock, m);
				bBlock = zigZag(bBlock, m);

				for(int u = 0; u < 8; u++) {
					for(int v = 0; v < 8; v++) { 
						rChannelDCT[i+u][j+v] = (int) rBlock[u][v];
						gChannelDCT[i+u][j+v] = (int) gBlock[u][v];
						bChannelDCT[i+u][j+v] = (int) bBlock[u][v];
					}//end v
				}//end u

			}
		}

	}

	public void IDCT() {        
		int height = 512;
		int width = 512;

		for(int i = 0;i<height;i+=8) {
			for(int j = 0;j<width;j+=8) {                                
				for(int x = 0;x<8;x++) {
					for(int y = 0;y<8;y++) {                                                
						float fRRes = 0.00f, fGRes = 0.00f, fBRes = 0.00f;                                                    

						for(int u = 0;u<8;u++) {
							for(int v = 0;v<8;v++) {
								float fCu = 1.0f, fCv = 1.0f;                                
								if(u == 0)
									fCu =  0.707f;
								if(v == 0)
									fCv = 0.707f;

								double iR, iG, iB;                                                                
								iR = rChannelDCT[i + u][j + v];
								iG = gChannelDCT[i + u][j + v];
								iB = bChannelDCT[i + u][j + v];

								//IDCT Formula calculation                               
								fRRes += fCu * fCv * iR*cosMatrix[x][u]*cosMatrix[y][v];
								fGRes += fCu * fCv * iG*cosMatrix[x][u]*cosMatrix[y][v];
								fBRes += fCu * fCv * iB*cosMatrix[x][u]*cosMatrix[y][v];
							}
						}

						fRRes *= 0.25;
						fGRes *= 0.25;
						fBRes *= 0.25;                        

						//Check R, G, B values for overflow and limit it between 0 to 255
						if(fRRes <= 0)
							fRRes = 0;
						else if(fRRes >= 255)
							fRRes = 255;

						if(fGRes <= 0)
							fGRes = 0;
						else if(fGRes >= 255)
							fGRes = 255;

						if(fBRes <= 0)
							fBRes = 0;
						else if(fBRes >= 255)
							fBRes = 255; 

						rChannelIDCT[i + x][j + y]  = (int)fRRes;
						gChannelIDCT[i + x][j + y]  = (int)fGRes;
						bChannelIDCT[i + x][j + y]  = (int)fBRes;
					}
				}                                               
			}
		}
	}

	public void showIms(String[] args){

		// Read a parameter from command line
		int a = Integer.parseInt(args[1]);
		System.out.println("The parameters was: " + a );

		// Read in the specified image
		imgOG = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imgDCT = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imgDWT = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		readAndCompressImage(width, height, args[0], a);

		// Use label to display the image
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOG));
		lbIm2 = new JLabel(new ImageIcon(imgDCT));
		lbIm3 = new JLabel(new ImageIcon(imgDWT));

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

		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 1;
		frame.getContentPane().add(lbIm3, c);

		frame.pack();
		frame.setVisible(true);
	}
	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}


}
