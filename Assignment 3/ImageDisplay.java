package hw2;

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
	
	static double[][] cosMatrix = new double[8][8];
	int[][] rChannel = new int[height][width];
	int[][] gChannel = new int[height][width];
	int[][] bChannel = new int[height][width];

	int[][] rChannelDCT = new int[height][width];
	int[][] gChannelDCT = new int[height][width];
	int[][] bChannelDCT = new int[height][width];

	int[][] rChannelIDCT = new int[height][width];
	int[][] gChannelIDCT = new int[height][width];
	int[][] bChannelIDCT = new int[height][width];

	double[][] rChannelDWT = new double[height][width];
	double[][] gChannelDWT = new double[height][width];
	double[][] bChannelDWT = new double[height][width];

	int[][] rChannelIDWT = new int[height][width];
	int[][] gChannelIDWT = new int[height][width];
	int[][] bChannelIDWT = new int[height][width];

	public void showIms(String[] args) {		
		
		try {
			int frameLength = width*height*3;

			File file = new File(args[0]);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);
			imgOG = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			imgDCT = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			imgDWT = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

			int ind = 0;
			for(int i = 0; i < height; i++){
				for(int j = 0; j < width; j++){
					int r = bytes[ind];
					int g = bytes[ind+height*width];
					int b = bytes[ind+height*width*2]; 

					r = r & 0xFF;
					g = g & 0xFF;
					b = b & 0xFF;

					rChannel[i][j] = r;
					gChannel[i][j] = g;
					bChannel[i][j] = b;

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					//int pix = ((a << 24) + (r << 16) + (g << 8) + b);
					imgOG.setRGB(j,i,pix);
					ind++;
				}
			}

			//Calculate the initial cosine transform values in matrix
			for(int i = 0;i<8;i++) {
				for(int j = 0;j<8;j++) {
					cosMatrix[i][j] = cos((2*i+1)*j*3.14159/16.00);
				}
			}

			int n = Integer.parseInt(args[1]);
			if(n != -1){
				int m = n/4096;
				//System.out.println("m = "+ m + " n="+n);
				
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

				//Display DCT and DWT Image
				displayDctimgDWT(0);
			}else{
				int iteration = 1;
				for(int i=4096; i <= 512*512; i=i+4096) {
					
					n = i;
					int m = n/4096;
					//System.out.println("iteration="+ iteration + " n = "+ i + " m="+m);
					
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

					//Display DCT and DWT Image with a delay of 1 second
					try { 
						Thread.sleep(1000);
					} catch (InterruptedException e) {}
					
					displayDctimgDWT(iteration);
					iteration++;
					
					if(i == 512*512){	//continous loop condition
						i = 0;
						iteration=1;
					}
				}
			}				

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		
	}
	
	/**
	 * Display DCT and DWT images side by side as per compression algorithm
	 */
	private void displayDctimgDWT(int iteration) {		
		//Display the Image matrix
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

		System.out.println("Displaying Original, DCT and DWT images, respectively");
		frame = new JFrame();
		GridBagLayout gLayout = new GridBagLayout();
		frame.getContentPane().setLayout(gLayout);

		lbIm1 = new JLabel(new ImageIcon(imgOG));
		lbIm2 = new JLabel(new ImageIcon(imgDCT));
		lbIm3 = new JLabel(new ImageIcon(imgDWT));

		lbIm1.setIcon(new ImageIcon(imgOG));
		lbIm2.setIcon(new ImageIcon(imgDCT));
		lbIm3.setIcon(new ImageIcon(imgDWT));

		GridBagConstraints c = new GridBagConstraints();
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

	/**
	 * Create Matrix Transpose
	 * @param matrix
	 * @return
	 */
	private double[][] transpose(double[][] matrix) {
		double[][] temp = new double[height][width];
		for(int i=0; i<height; i++) {
			for(int j=0; j< width; j++) {
				temp[i][j] = matrix[j][i]; 
			}
		}
		return temp;
	}
	

	private double[] composition(double[] array) {
		int h = 1;
		while (h <= array.length) {
			array = compositionStep(array, h);
			h = h*2;
		}
		return array;
	}

	private double[] compositionStep(double[] array, int h) {
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
			array = decompositionStep(array, h);
			h = h/2;
		}
		return array;
	}

	private double[] decompositionStep(double[] array, int h) {
		double[] dArray = Arrays.copyOf(array, array.length);
		for(int i=0; i < h/2; i++) {
			dArray[i] = (array[2*i] + array[2*i + 1]) / 2;			//Low pass output
			dArray[h/2 + i] = (array[2*i] - array[2*i + 1]) / 2;	//High pass output
		}
		return dArray;
	}


	/**
	 * DWT Standard decomposition function
	 * @param matrix
	 * @param n
	 * @return
	 */
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


	/**
	 * IDWT Algorithm doing reverse of DWT and recreating the image
	 * @param matrix
	 * @return
	 */
	private int[][] IDWT(double[][] matrix) {
		int[][] iMatrix = new int[height][width];

		//First Columns
		matrix = transpose(matrix);
		for(int col=0; col < height; col++) {
			matrix[col] = composition (matrix[col]);
		}	
		matrix = transpose(matrix);

		//Then all rows
		for(int row=0; row < width; row++){
			matrix[row] = composition(matrix[row]);
		}

		//Copy the composition matrix to int matrix of R,G,B values. Limit values 0 to 255
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

	/**
	 * DCT Transformation as per compression Algorithm
	 * @param rChannel
	 * @param gChannel
	 * @param bChannel
	 * @param m
	 */
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

	/**
	 * Zig-zag matrix traversal for counting the coefficients
	 * @param matrix
	 * @param m
	 * @return
	 */
	public double[][] zigZag(double[][] matrix, int m) {
        int row = 0;
        int column = 0;
        int length = matrix.length - 1;
        int count = 1;

        matrix[row][column] = count > m ? 0 : matrix[row][column];
        count++;

        while (true) {

            column++;
            matrix[row][column] = count > m ? 0 : matrix[row][column];
            count++;

            while (column != 0) {
                row++;
                column--;
                matrix[row][column] = count > m ? 0 : matrix[row][column];
                count++;
            }
            row++;
            if (row > length) {
                row--;
                break;
            }

            matrix[row][column] = count > m ? 0 : matrix[row][column];
            count++;

            while (row != 0) {
                row--;
                column++;
                matrix[row][column] = count > m ? 0 : matrix[row][column];
                count++;
            }
        }

        while (true) {
            column++;
            count++;

            if (count > m) {
                matrix[row][column] = 0;
            }

            while (column != length) {
                column++;
                row--;
                matrix[row][column] = count > m ? 0 : matrix[row][column];
                count++;
            }

            row++;
            if (row > length) {
                row--;
                break;
            }
            matrix[row][column] = count > m ? 0 : matrix[row][column];
            count++;

            while (row < length) {
                row++;
                column--;
                matrix[row][column] = count > m ? 0 : matrix[row][column];
                count++;
            }
        }
        return matrix;
	}

	/**
	 * Inverse DCT Transformation as per formula doing the computation
	 */
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

	public static void main(String[] args) {
		//Input example:
		//D:\\workspace\\....path to file....\\rgbimages\\Lenna.rgb -1
		
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}
}