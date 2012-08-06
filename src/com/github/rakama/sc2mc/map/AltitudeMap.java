package com.github.rakama.sc2mc.map;

/**
 * Copyright (c) 2012, RamsesA <ramsesakama@gmail.com>
 * 
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
 * REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
 * LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
 * OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */

public class AltitudeMap
{
    public static final int width = 128;
    public static final int height = 128;
    
    private enum Rotation{NONE, CLOCKWISE90, CLOCKWISE180, CLOCKWISE270};
    private enum Slope{HIGH, LOW, SLOPE, CORNER_HIGH, CORNER_LOW};
    
    private static final Rotation[] xterRotation = {
        Rotation.NONE,
        Rotation.NONE, Rotation.CLOCKWISE90, Rotation.CLOCKWISE180, Rotation.CLOCKWISE270,
        Rotation.NONE, Rotation.CLOCKWISE90, Rotation.CLOCKWISE180, Rotation.CLOCKWISE270,
        Rotation.NONE, Rotation.CLOCKWISE90, Rotation.CLOCKWISE180, Rotation.CLOCKWISE270,
        Rotation.NONE
    };

    private static final Slope[] xterSlope = {
        Slope.LOW,
        Slope.SLOPE, Slope.SLOPE, Slope.SLOPE, Slope.SLOPE, 
        Slope.CORNER_LOW, Slope.CORNER_LOW, Slope.CORNER_LOW, Slope.CORNER_LOW, 
        Slope.CORNER_HIGH, Slope.CORNER_HIGH, Slope.CORNER_HIGH, Slope.CORNER_HIGH, 
        Slope.HIGH,
    };
    
    int[] terrain;
    int[] water;
    Slope[] slope;
    Rotation[] rotation;
    boolean[] underwater;
    
    protected AltitudeMap(byte[] altm, byte[] xter)
    {
        terrain = new int[width * height];
        water = new int[width * height];
        slope = new Slope[width * height];
        rotation = new Rotation[width * height];
        underwater = new boolean[width * height];
        
        for(int x=0; x<width; x++)
        {
            for(int y=0; y<height; y++)
            {
                int index = toIndex(x, y);
                
                terrain[index] = getTerrainAltitude(altm, x, y);
                water[index] =  getWaterAltitude(altm, x, y);                
                slope[index] = getSlopeType(xter, x, y);            
                rotation[index] = getSlopeRotation(xter, x, y);
                underwater[index] = getUnderwater(xter, x, y);
            }
        }
    }
    
    public int getWidth()
    {
        return width;
    }
    
    public int getHeight()
    {
        return height;
    }
    
    public int getTerrainAltitude(int x, int y)
    {
        checkBounds(x, y);
        return terrain[toIndex(x, y)];
    }
    
    public float getSmoothAltitude(float x, float y)
    {
        int xi = (int)x;
        int yi = (int)y;
        
        checkBounds(xi, yi);

        int index = toIndex(xi, yi);  
        float altitude = terrain[index];
        
        float temp;
        float xf = x - xi;
        float yf = y - yi;
        
        switch(rotation[index])
        {
        case CLOCKWISE90:
            temp = xf;
            xf = yf;
            yf = 1 - temp;
            break;
        case CLOCKWISE180:
            xf = 1 - xf;
            yf = 1 - yf;
            break;
        case CLOCKWISE270:
            temp = xf;
            xf = 1 - yf;
            yf = temp;
            break;
        }

        switch(slope[index])
        {
        case LOW:
            return altitude;
        case HIGH:
            return altitude + 1;
        case SLOPE:
            return altitude + 1 - yf;
        case CORNER_LOW:
            if(underwater[index])
                return altitude + getQuadraticCornerLow(xf, yf);
            else if(yf > xf)
                return altitude + getCornerLow(xf, yf);
            else
                return altitude + 1;
        case CORNER_HIGH:
            if(underwater[index])
                return altitude + getQuadraticCornerHigh(xf, yf);
            else if(yf < xf)
                return altitude + getCornerHigh(xf, yf);
            else
                return altitude;
        }
        
        return altitude;
    }

    protected float getCornerLow(float xf, float yf)
    {
        return 1 - yf + xf;
    }

    protected float getCornerHigh(float xf, float yf)
    {
        return xf - yf;
    }
    
    protected float getQuadraticCornerLow(float xf, float yf)
    {
        return (float)Math.min(1, (1 - xf) * (1 - yf) + xf * (1 - yf) + xf * yf + 0.05);
    }

    protected float getQuadraticCornerHigh(float xf, float yf)
    {
        return (float)Math.min(1, (1 - yf) * xf + 0.05);
    }
    
    public int getWaterAltitude(int x, int y)
    {
        checkBounds(x, y);
        return water[toIndex(x, y)];
    }

    protected static Slope getSlopeType(byte[] data, int x, int y)
    {
        int index = (127 - x) + (y << 7);
        return xterSlope[0xF & data[index]];        
    }

    protected static Rotation getSlopeRotation(byte[] data, int x, int y)
    {
        int index = (127 - x) + (y << 7);
        return xterRotation[0xF & data[index]];        
    }

    protected static boolean getUnderwater(byte[] data, int x, int y)
    {
        int index = (127 - x) + (y << 7);
        return (data[index] & 0x30) != 0;        
    }

    protected static int getTerrainAltitude(byte[] data, int x, int y)
    {
        int index = ((127 - x) + (y << 7)) << 1;
        return data[index + 1] & 0xF;
    }
    
    protected static int getWaterAltitude(byte[] data, int x, int y)
    {
        int index = ((127 - x) + (y << 7)) << 1;
        int byte1 = data[index] & 0xFF;
        int byte2 = data[index + 1] & 0xFF;
        int val = byte2 | (byte1 << 8);
        return (val >> 5) & 0xF;
    }
        
    protected static int toIndex(int x, int y)
    {
        return x + (y << 7);
    }

    protected static void checkBounds(int x, int y)
    {
        if(!inBounds(x, y))
            throw new IndexOutOfBoundsException("(" + x + ", " + y + ")");
    }

    protected static boolean inBounds(int x, int y)
    {
        return x == (x & 0x7F) && y == (y & 0x7F);
    }
}