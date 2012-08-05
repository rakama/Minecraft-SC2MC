package com.github.rakama.sc2mc.io;

import java.io.IOException;
import java.io.InputStream;

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

public class SegInputStream
{
    private InputStream in;
    
    public SegInputStream(InputStream in)
    {
        this.in = in;
    }
    
    public InputStream getInputStream()
    {
        return in;
    }

    public Segment readSegment() throws IOException
    {
        String header = readString(4);        
        if(header == null)
            return null;
        
        long size = readUInt32();
        if(size < 0 || size > Integer.MAX_VALUE)
            throw new IOException("Invalid size for segment '" + header + "'!");
        
        byte[] data = new byte[(int)size];
        if(in.read(data) < 0)
            throw new IOException("File ended prematurely!");
        
        return new Segment(header, data);
    }

    public String readString(int bytes) throws IOException
    {
        byte[] data = new byte[bytes];
        
        if(in.read(data) < 0)
            return null;
        
        return new String(data);
    }    

    public long readUInt32() throws IOException
    {
        byte[] data = new byte[4];
        
        if(in.read(data) < 0)
            return -1;
        
        return toBigEndian(data);
    }
    
    private static long toBigEndian(byte[] bytes)
    {
        long val = 0;
        for(byte b : bytes)
            val = (b & 0xFF) | (val << 8);
        
        return val;
    }
}