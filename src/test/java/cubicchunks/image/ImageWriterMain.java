/*******************************************************************************
 * Copyright (c) 2014 Jeff Martin.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Jeff Martin - initial API and implementation
 ******************************************************************************/
package test.java.cubicchunks.image;
import java.util.Random;

import libnoiseforjava.module.Simplex;

public class ImageWriterMain
{
	public static void main(String[] args)
	{
		Random rand = new Random();
		int rnd = rand.nextInt();
		Simplex baseContinentDef_pe0 = new Simplex();
		baseContinentDef_pe0.setSeed(0);
		baseContinentDef_pe0.setFrequency(1.0);
		baseContinentDef_pe0.setPersistence(0.5);
		baseContinentDef_pe0.setLacunarity(2.2089);
		baseContinentDef_pe0.setOctaveCount(14);
		baseContinentDef_pe0.build();

	    double xStart = 0;
	    double xEnd = 960;
	    double yStart = 0;
	    double yEnd = 540;

	    int xResolution = 1920;
	    int yResolution = 1080;

	    double[][] result = new double[xResolution][yResolution];

	    for(int i = 0; i < xResolution; i++)
	    {
	        for(int j = 0; j < yResolution; j++)
	        {
	            int x = (int) (xStart + i * ((xEnd - xStart) / xResolution));
	            int y = (int) (yStart + j * ((yEnd - yStart) / yResolution));
	            result[i][j] = 0.5 * (1 + baseContinentDef_pe0.getValue(x, y, 0));
	        }
	    }
	    
	    ImageWriter.greyWriteImage(result);
	}
}