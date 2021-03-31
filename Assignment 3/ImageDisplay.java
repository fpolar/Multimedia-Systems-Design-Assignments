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

	private void readAndCompressImage(String imgPath, int n){

		try {
			int frameLength = width*height*3;

			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			long len = frameLength;
			byte[] bytes = new byte[(int) len];

			raf.read(bytes);

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
					imgOG.setRGB(j,i,pix);
					ind++;
				}
			}

			for(int i = 0;i<8;i++) {
				for(int j = 0;j<8;j++) {
					cosMatrix[i][j] = cos((2*i+1)*j*3.14159/16.00);
				}
			}

			int m = n/4096;
			DCT(rChannel, gChannel, bChannel, m);
			IDCT();

			rChannelDWT = DWT(rChannel, n);
			gChannelDWT = DWT(gChannel, n);
			bChannelDWT = DWT(bChannel, n);

			rChannelIDWT = IDWT(rChannelDWT);
			gChannelIDWT = IDWT(gChannelDWT);
			bChannelIDWT = IDWT(bChannelDWT);

			for(int i = 0; i < height; i++) {
				for(int j = 0; j < width; j++) {
					int r = rChannelIDCT[i][j];
					int g = gChannelIDCT[i][j];
					int b = bChannelIDCT[i][j];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					imgDCT.setRGB(j,i,pix);
					
					int rr = (int) rChannelIDWT[i][j];
					int gg = (int) gChannelIDWT[i][j];
					int bb = (int) bChannelIDWT[i][j];

					int pixx = 0xff000000 | ((rr & 0xff) << 16) | ((gg & 0xff) << 8) | (bb & 0xff);
					imgDWT.setRGB(j,i,pixx);					
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void showIms(String[] args) {		
	
		imgOG = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imgDCT = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imgDWT = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		readAndCompressImage(args[0],  Integer.parseInt(args[1]));

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
		double[] outArray = Arrays.copyOf(array, array.length);
		for(int i=0; i < h/2; i++) {
			outArray[2*i] = array[i] + array[h/2 + i];
			outArray[2*i + 1] = array[i] - array[h/2 + i];
		}		
		return outArray;
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
		double[] outArray = Arrays.copyOf(array, array.length);
		for(int i=0; i < h/2; i++) {
			//Low
			outArray[i] = (array[2*i] + array[2*i + 1]) / 2;

			//High	
			outArray[h/2 + i] = (array[2*i] - array[2*i + 1]) / 2;
		}
		return outArray;
	}

	private double[][] DWT(int[][] matrix,int n) {
		double[][] outMatrix = new double[height][width];
		for(int i=0; i<height; i++){
			for(int j=0; j<width; j++){
				outMatrix[i][j] = matrix[i][j];
			}
		}
		for(int row=0; row < width; row++){
			outMatrix[row] = decomposition(outMatrix[row]);
		}		

		outMatrix = transpose(outMatrix);
		for(int col=0; col < height; col++) {
			outMatrix[col] = decomposition(outMatrix[col]);
		}		
		outMatrix = transpose(outMatrix);
		outMatrix = zigZag(outMatrix, n);

		return outMatrix;
	}


	private int[][] IDWT(double[][] matrix) {
		int[][] outMatrix = new int[height][width];

		matrix = transpose(matrix);
		for(int col=0; col < height; col++) {
			matrix[col] = composition (matrix[col]);
		}	
		matrix = transpose(matrix);

		for(int row=0; row < width; row++){
			matrix[row] = composition(matrix[row]);
		}

		for(int i=0; i < height; i++){
			for(int j=0; j<width; j++){
				outMatrix[i][j] = (int) Math.round(matrix[i][j]);
				//clampp
				if(outMatrix[i][j] < 0){
					outMatrix[i][j] = 0;
				}
				if(outMatrix[i][j] > 255){
					outMatrix[i][j] = 255;
				}
			}
		}

		return outMatrix;
	}

	private void DCT(int[][] rChannel, int[][] gChannel, int[][] bChannel, int m) {

		for(int i = 0; i < height; i+=8) {
			for(int j = 0; j < width;j+=8) { 

				double[][] rBlock = new double[8][8], gBlock = new double[8][8], bBlock = new double[8][8];

				for(int u = 0; u < 8; u++) {
					for(int v = 0; v < 8; v++) {  

						float cu = 1.0f, cv = 1.0f, rResult = 0.00f, gResult = 0.00f, bResult = 0.00f;

						if(u == 0){
							cu =  0.707f;
						}
						if(v == 0){
							cv = 0.707f;
						}

						for(int x = 0; x<8; x++) {
							for(int y = 0;y<8;y++) { 

								int currR, currG, currB;                                

								currR = (int) rChannel[i+x][j+y];
								currG = (int) gChannel[i+x][j+y];
								currB = (int) bChannel[i+x][j+y];

								rResult += currR*cosMatrix[x][u]*cosMatrix[y][v];
								gResult += currG*cosMatrix[x][u]*cosMatrix[y][v];
								bResult += currB*cosMatrix[x][u]*cosMatrix[y][v];

							}
						}
						rBlock[u][v] = (int) Math.round(rResult * 0.25*cu*cv);
						gBlock[u][v] = (int) Math.round(gResult * 0.25*cu*cv);
						bBlock[u][v] = (int) Math.round(bResult * 0.25*cu*cv);
					}
				}

				rBlock = zigZag(rBlock, m);
				gBlock = zigZag(gBlock, m);
				bBlock = zigZag(bBlock, m);

				for(int u = 0; u < 8; u++) {
					for(int v = 0; v < 8; v++) { 
						rChannelDCT[i+u][j+v] = (int) rBlock[u][v];
						gChannelDCT[i+u][j+v] = (int) gBlock[u][v];
						bChannelDCT[i+u][j+v] = (int) bBlock[u][v];
					}
				}

			}
		}

	}

	public double[][] zigZag(double[][] matrix, int m) {
        int row = 0;
        int col = 0;
        int count = 1;

        matrix[row][col] = count > m ? 0 : matrix[row][col];
        count++;

        while (true) {

            col++;
            matrix[row][col] = count > m ? 0 : matrix[row][col];
            count++;

            while (col != 0) {
                row++;
                col--;
                matrix[row][col] = count > m ? 0 : matrix[row][col];
                count++;
            }
            row++;
            if (row > matrix.length - 1) {
                row--;
                break;
            }

            matrix[row][col] = count > m ? 0 : matrix[row][col];
            count++;

            while (row != 0) {
                row--;
                col++;
                matrix[row][col] = count > m ? 0 : matrix[row][col];
                count++;
            }
        }

        while (true) {
            col++;
            count++;

            if (count > m) {
                matrix[row][col] = 0;
            }

            while (col != matrix.length - 1) {
                col++;
                row--;
                matrix[row][col] = count > m ? 0 : matrix[row][col];
                count++;
            }

            row++;
            if (row > matrix.length - 1) {
                row--;
                break;
            }
            matrix[row][col] = count > m ? 0 : matrix[row][col];
            count++;

            while (row < matrix.length - 1) {
                row++;
                col--;
                matrix[row][col] = count > m ? 0 : matrix[row][col];
                count++;
            }
        }
        return matrix;
	}

	public void IDCT() {        
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

								double currR, currG, currB;                                                                
								currR = rChannelDCT[i + u][j + v];
								currG = gChannelDCT[i + u][j + v];
								currB = bChannelDCT[i + u][j + v];

								fRRes += fCu * fCv * currR*cosMatrix[x][u]*cosMatrix[y][v];
								fGRes += fCu * fCv * currG*cosMatrix[x][u]*cosMatrix[y][v];
								fBRes += fCu * fCv * currB*cosMatrix[x][u]*cosMatrix[y][v];
							}
						}

						fRRes *= 0.25;
						fGRes *= 0.25;
						fBRes *= 0.25;                        

						//Clamp
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
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}
}