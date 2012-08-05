package com.github.rakama.sc2mc.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

public class SC2Map
{
    static final int max_buffer_size = 65536;
    
    Map<String, Segment> segments;
    int fileSize;
    
    protected SC2Map(int fileSize)
    {
        this.segments = new HashMap<String, Segment>();
        this.fileSize = fileSize;
    }
    
    public int getSize()
    {
        return fileSize;
    }
    
    protected void addSegment(String chunk, byte[] data)
    {
        Segment segment = new Segment(chunk, data);
        segments.put(chunk, segment);
    }
    
    public Segment getSegment(String chunk)
    {
        return segments.get(chunk);
    }

    public Collection<Segment> getSegments()
    {
        return Collections.unmodifiableCollection(segments.values());
    }
    
    public static SC2Map loadMap(InputStream in) throws IOException
    {
        // confirm that this file is IFF format
        checkHeader(in, "FORM");
        
        int fileSize = readSize(in, "FORM");   
        
        // confirm this file is a SimCity 2000 map
        checkHeader(in, "SCDH");
        
        SC2Map map = new SC2Map(fileSize + 4);
        
        // read segments
        String segmentHeader = readString(in, 4);        
        while(segmentHeader != null)
        {
            int segmentSize = readSize(in, segmentHeader);
            
            byte[] data;            
            if(isCompressed(segmentHeader))
                data = decompress(in, segmentSize);
            else
                data = checkedRead(in, segmentSize);

            map.addSegment(segmentHeader, data);
            segmentHeader = readString(in, 4);
        }
        
        return map;
    }
    
    private static boolean isCompressed(String header)
    {
        if(header.equals("ALTM") || header.equals("CNAM"))
            return false;
        
        return true;
    }
    
    private static String checkHeader(InputStream in, String header) throws IOException
    {
        String str = readString(in, header.length());

        if(str == null)
            throw new IOException("File ended prematurely!");
        
        if(!str.equals(header))
            throw new IOException("Invalid header '" + str + "' (expected '" + header + "')");
    
        return str;
    }
        
    private static String readString(InputStream in, int bytes) throws IOException
    {
        byte[] data = new byte[bytes];
        
        if(in.read(data) < 0)
            return null;
        
        return new String(data);
    }

    private static int readSize(InputStream in, String header) throws IOException
    {
        long size = readInteger(in, 4);

        if(size < 0 || size > Integer.MAX_VALUE)
            throw new IOException("Invalid size for segment '" + header + "'!");
        
        return (int)size;
    }
    
    private static long readInteger(InputStream in, int bytes) throws IOException
    {
        byte[] data = new byte[bytes];
        
        if(in.read(data) < 0)
            return -1;
        
        return toBigEndian(data);
    }
    
    private static byte[] decompress(InputStream in, int bytes) throws IOException
    {
        byte[] buffer = new byte[max_buffer_size];
        
        int size = 0;
        int reads = 0;
        while(reads < bytes)
        {
            int count = checkedRead(in);
            reads++;
            
            if(count == 0 || count == 128)
                throw new IOException("Invalid compression format!");
            
            if(count < 128)
            {
                for(int j=0; j<count; j++)
                    buffer[size++] = (byte)checkedRead(in);
                
                reads += count;
            }
            else
            {
                count -= 127;
                byte val = (byte)checkedRead(in);                
                for(int j=0; j<count; j++)
                    buffer[size++] = val;
                
                reads++;
            }
        }
        
        byte[] data = new byte[size];
        System.arraycopy(buffer, 0, data, 0, size);
        
        return data;
    }
    
    private static int checkedRead(InputStream in) throws IOException
    {
        int val = in.read();
        
        if(val < 0)
            throw new IOException("File ended prematurely!");
        
        return val;
    }
    
    private static byte[] checkedRead(InputStream in, int bytes) throws IOException
    {
        byte[] data = new byte[bytes];
        if(in.read(data) < 0)
            throw new IOException("File ended prematurely!");   
        return data;
    }
    

    private static long toBigEndian(byte[] bytes)
    {
        long val = 0;
        for(byte b : bytes)
            val = (b & 0xFF) | (val << 8);
        
        return val;
    }
}