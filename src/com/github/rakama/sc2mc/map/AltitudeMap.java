package com.github.rakama.sc2mc.map;

public class AltitudeMap
{
    public static final int width = 128;
    public static final int height = 128;
    
    private static final int[][] slopeData = {
        {0,0,0,0}, {1,1,0,0}, {0,1,0,1}, {0,0,1,1},
        {1,0,1,0}, {1,1,0,1}, {0,1,1,1}, {1,0,1,1},
        {1,1,1,0}, {0,1,0,0}, {0,0,0,1}, {0,0,1,0},
        {1,0,0,0}, {1,1,1,1} };
    
    int[] terrain;
    int[] water;
    int[][] slope;
    
    public AltitudeMap(byte[] altm, byte[] xter)
    {
        terrain = new int[width * height];
        water = new int[width * height];
        slope = new int[width * height][];
        
        for(int x=0; x<width; x++)
        {
            for(int y=0; y<height; y++)
            {
                int index = toIndex(x, y);
                
                int altitudeData = getAltitudeData(altm, x, y);
                terrain[index] = altitudeData & 0xF;
                water[index] =  (altitudeData >> 5) & 0xF;                
                slope[index] = getSlopeData(xter, x, y);
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
        int[] corner = slope[index];        
        float altitude = terrain[index];
        
        float xf = x - xi;
        float yf = y - yi;
        
        altitude += corner[0] * (1 - xf) * (1 - yf);
        altitude += corner[1] * xf * (1 - yf);
        altitude += corner[2] * (1 - xf) * yf;
        altitude += corner[3] * xf * yf;
        
        return altitude;
    }
    
    public int getWaterAltitude(int x, int y)
    {
        checkBounds(x, y);
        return water[toIndex(x, y)];
    }

    protected static int[] getSlopeData(byte[] data, int x, int y)
    {
        int index = (127 - x) + (y << 7);
        return slopeData[0xF & data[index]];        
    }
    
    protected static int getAltitudeData(byte[] data, int x, int y)
    {
        int index = ((127 - x) + (y << 7)) << 1;
        int byte1 = 0xFF & data[index];
        int byte2 = 0xFF & data[index + 1];
        return (byte1 << 8) | byte2;
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