/*
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

package rakama.sc2mc.io;

public class Segment
{
    static final int max_buffer_size = 65536;

    String name;
    byte[] raw, decompressed;
    
    protected Segment(String name, byte[] raw)
    {
        this.name = name;
        this.raw = raw;
    }
    
    public String getName()
    {
        return name;
    }
    
    public byte[] getRawData()
    {
        return raw;
    }

    public byte[] getDecompressedData()
    {
        return decompressed;
    }
    
    public void decompressData()
    {        
        byte[] buffer = new byte[max_buffer_size];
        
        int size = 0;
        int index = 0;
        while(index < raw.length)
        {
            int count = 0xFF & raw[index++];
                        
            if(count == 0 || count == 128)
                throw new RuntimeException("Invalid compression format!");
            
            if(count < 128)
            {
                for(int j=0; j<count; j++)
                    buffer[size++] = raw[index++];
            }
            else
            {
                count -= 127;
                byte val = raw[index++];                
                for(int j=0; j<count; j++)
                    buffer[size++] = val;
            }
        }

        decompressed = new byte[size];
        System.arraycopy(buffer, 0, decompressed, 0, size);
    }
    
    public int getRawSize()
    {
        return raw.length;
    }

    public int getDecompressedSize()
    {
        if(decompressed == null)
            return -1;
        
        return decompressed.length;
    }
}