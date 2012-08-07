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

public class TerrainMap
{
    protected static final int width = 128;
    protected static final int height = 128;    
    protected static final float canal_depth = 0.3f;
    
    protected enum Rotation{NONE, CLOCKWISE90, CLOCKWISE180, CLOCKWISE270};    
    protected enum Type{HIGH, LOW, SLOPE, CORNER_HIGH, CORNER_LOW, WATERFALL, CANAL};
    
    protected static final Rotation[] xterRotation = {Rotation.NONE,
        Rotation.NONE, Rotation.CLOCKWISE90, Rotation.CLOCKWISE180, Rotation.CLOCKWISE270,
        Rotation.NONE, Rotation.CLOCKWISE90, Rotation.CLOCKWISE180, Rotation.CLOCKWISE270,
        Rotation.NONE, Rotation.CLOCKWISE90, Rotation.CLOCKWISE180, Rotation.CLOCKWISE270,
        Rotation.NONE, Rotation.NONE, Rotation.NONE
    };

    protected static final Type[] xterSlope = {Type.LOW,
        Type.SLOPE, Type.SLOPE, Type.SLOPE, Type.SLOPE, 
        Type.CORNER_LOW, Type.CORNER_LOW, Type.CORNER_LOW, Type.CORNER_LOW, 
        Type.CORNER_HIGH, Type.CORNER_HIGH, Type.CORNER_HIGH, Type.CORNER_HIGH, 
        Type.HIGH, Type.LOW, Type.LOW
    };
    
    protected int[] terrainAltitude;
    protected int[] waterAltitude;
    protected Type[] terrainType;
    protected Rotation[] terrainRotation;
    protected boolean[] terrainUnderwater;
    
    protected TerrainMap(byte[] altm, byte[] xter)
    {
        terrainAltitude = new int[width * height];
        waterAltitude = new int[width * height];
        terrainType = new Type[width * height];
        terrainRotation = new Rotation[width * height];
        terrainUnderwater = new boolean[width * height];
        
        for(int x=0; x<width; x++)
        {
            for(int y=0; y<height; y++)
            {
                int index = toIndex(x, y);
                
                terrainAltitude[index] = computeTerrainAltitude(altm, x, y);
                waterAltitude[index] =  computeWaterAltitude(altm, x, y);                
                terrainType[index] = computeType(xter, x, y);            
                terrainRotation[index] = computeRotation(xter, x, y);
                terrainUnderwater[index] = computeUnderwater(xter, x, y);
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

    public boolean isFlat(int x, int y)
    {
        checkBounds(x, y);
        Type type = terrainType[toIndex(x, y)];
        return type == Type.LOW || type == Type.HIGH;
    }

    public boolean isSlope(int x, int y)
    {
        checkBounds(x, y);
        Type type = terrainType[toIndex(x, y)];
        return type == Type.SLOPE || type == Type.CORNER_HIGH || type == Type.CORNER_LOW;
    }
    
    public boolean isWaterfall(int x, int y)
    {
        checkBounds(x, y);
        return terrainType[toIndex(x, y)] == Type.WATERFALL;
    }

    public boolean isCanal(int x, int y)
    {        
        checkBounds(x, y);
        return terrainType[toIndex(x, y)] == Type.CANAL;
    }

    public boolean isFlooded(int x, int y)
    {        
        checkBounds(x, y);
        return terrainUnderwater[toIndex(x, y)] || isCanal(x, y) || isWaterfall(x, y);
    }
    
    public int getWaterAltitude(int x, int y)
    {
        checkBounds(x, y);
        return waterAltitude[toIndex(x, y)];
    }
    
    public int getTerrainAltitude(int x, int y)
    {
        checkBounds(x, y);
        return terrainAltitude[toIndex(x, y)];
    }
    
    public float getSmoothAltitude(float x, float y)
    {
        int xi = (int)x;
        int yi = (int)y;        
        checkBounds(xi, yi);

        int index = toIndex(xi, yi);
        float altitude = terrainAltitude[index];
        Type type = terrainType[index];
        Rotation rotation = terrainRotation[index];        
        boolean underwater = terrainUnderwater[index];
        
        float xf = x - xi;
        float yf = y - yi;

        // treat grid like canal
        if(type == Type.CANAL || type == Type.WATERFALL)
            return altitude - getCanalDepth(xi, yi, xf, yf);

        // find altitude to bridge neighboring canals
        float canalAltitude = altitude + 1;
        if(isFlooded(xi, yi) && hasAdjacentCanal(xi, yi))
            canalAltitude -= getCanalDepth(xi, yi, xf, yf);

        float swap;
        
        // find grid rotation
        switch(rotation)
        {
        case CLOCKWISE90:
            swap = xf;
            xf = yf;
            yf = 1 - swap;
            break;
        case CLOCKWISE180:
            xf = 1 - xf;
            yf = 1 - yf;
            break;
        case CLOCKWISE270:
            swap = xf;
            xf = 1 - yf;
            yf = swap;
            break;
        }
        
        // find altitude for this grid
        switch(type)
        {
        case LOW:
            break;
        case HIGH:
            altitude++;
            break;
        case SLOPE:
            if(underwater)
                altitude += getFloodedSlope(xf, yf);
            else
                altitude += getSlope(xf, yf);
            break;
        case CORNER_LOW:
            if(underwater)
                altitude += getFloodedCornerLow(xf, yf);
            else
                altitude += getCornerLow(xf, yf);
            break;
        case CORNER_HIGH:
            if(underwater)
                altitude += getFloodedCornerHigh(xf, yf);
            else
                altitude += getCornerHigh(xf, yf);
            break;
        }

        // if canal exists, "carve" it into the grid
        return Math.min(altitude, canalAltitude);
    }
    
    protected float getCanalDepth(int x, int y, float xf, float yf)
    {        
        float depth = canal_depth;

        // no rotation
        boolean c1 = !isFloodedHelper(x, y - 1);
        boolean c2 = !isFloodedHelper(x - 1, y - 1);
        boolean c3 = !isFloodedHelper(x - 1, y);
        
        depth = Math.min(depth, canalDepthHelper(xf, yf, c1, c2, c3));

        // clockwise 90 degrees
        c1 = !isFloodedHelper(x + 1, y);
        c2 = !isFloodedHelper(x + 1, y - 1);
        c3 = !isFloodedHelper(x, y - 1);

        depth = Math.min(depth, canalDepthHelper(yf, 1 - xf, c1, c2, c3));

        // clockwise 180 degrees
        c1 = !isFloodedHelper(x, y + 1);
        c2 = !isFloodedHelper(x + 1, y + 1);
        c3 = !isFloodedHelper(x + 1, y);

        depth = Math.min(depth, canalDepthHelper(1 - xf, 1 - yf, c1, c2, c3));

        // clockwise 270 degrees
        c1 = !isFloodedHelper(x - 1, y);
        c2 = !isFloodedHelper(x - 1, y + 1);
        c3 = !isFloodedHelper(x, y + 1);

        depth = Math.min(depth, canalDepthHelper(1 - yf, xf, c1, c2, c3));
        
        return depth;
    }
    
    protected boolean hasAdjacentCanal(int x, int y)
    {
        return isCanalHelper(x + 1, y)
            || isCanalHelper(x + 2, y + 1)
            || isCanalHelper(x, y + 1)
            || isCanalHelper(x - 1, y + 1)
            || isCanalHelper(x - 1, y)
            || isCanalHelper(x - 1, y - 1)
            || isCanalHelper(x, y - 1)
            || isCanalHelper(x + 1, y - 1);
    }
    
    private float canalDepthHelper(float xf, float yf, boolean c1, boolean c2, boolean c3)
    {
        if(c1 && c3)
            return 1 - getFloodedCornerLow(1 - xf, yf);
        else if(c1)
            return 1 - getFloodedSlope(1 - xf, yf);
        else if(c2)
            return 1 - getFloodedCornerHigh(1 - xf, yf);
        
        return 1;
    }

    private boolean isCanalHelper(int x, int y)
    {
        if(x < 0 || x >= width || y < 0 || y >= height)
            return true;
        else
            return isCanal(x, y);
    }
    
    private boolean isFloodedHelper(int x, int y)
    {
        if(x < 0 || x >= width || y < 0 || y >= height)
            return true;
        else
            return isFlooded(x, y);
    }

    protected static float getSlope(float xf, float yf)
    {
        return 1 - yf;
    }
    
    protected static float getCornerLow(float xf, float yf)
    {
        if(yf > xf)
            return 1 - yf + xf;
        else
            return 1;
    }

    protected static float getCornerHigh(float xf, float yf)
    {
        if(yf < xf)
            return xf - yf;
        else
            return 0;
    }

    protected static float getFloodedSlope(float xf, float yf)
    {
        return 1 - yf + 0.05f;
    }
    
    protected static float getFloodedCornerLow(float xf, float yf)
    {
        return (float)Math.min(1, (1 - xf) * (1 - yf) + xf * (1 - yf) + xf * yf + 0.04f);
    }

    protected static float getFloodedCornerHigh(float xf, float yf)
    {
        return (float)Math.min(1, (1 - yf) * xf * 1.05f + 0.02f);
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
    
    private static Type computeType(byte[] xter, int x, int y)
    {
        int index = (127 - x) + (y << 7);
        int type = xter[index];
        if(type == 0x3E)
            return Type.WATERFALL;
        else if(type >= 0x30 && type <= 0x3D)
            return Type.CANAL;
        else if(type == 0x40 || type == 0x41)
            return Type.CANAL;
        else if(type >= 0x42 && type <= 0x45)
            return Type.CANAL;
        else
            return xterSlope[0xF & type];
    }

    private static Rotation computeRotation(byte[] xter, int x, int y)
    {
        int index = (127 - x) + (y << 7);
        int type = xter[index];
        return xterRotation[0xF & type];
    }

    private static boolean computeUnderwater(byte[] xter, int x, int y)
    {
        int index = (127 - x) + (y << 7);
        return (xter[index] & 0x30) != 0;        
    }

    private static int computeTerrainAltitude(byte[] altm, int x, int y)
    {
        int index = (127 - x) + (y << 7);
        return altm[index * 2 + 1] & 0xF;
    }
    
    private static int computeWaterAltitude(byte[] altm, int x, int y)
    {
        int index = (127 - x) + (y << 7);
        int byte1 = altm[index * 2] & 0xFF;
        int byte2 = altm[index * 2 + 1] & 0xFF;
        int val = byte2 | (byte1 << 8);
        return (val >> 5) & 0xF;
    }
}