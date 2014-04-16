/**
 * Copyright 2010 Naval Postgraduate School
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.exi.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;




public class CompressionLearning {




    public static void compressData(byte[] data, OutputStream out)     throws IOException {
        Deflater d = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        DeflaterOutputStream dout = new DeflaterOutputStream(out, d);
        dout.write(data);
        dout.close();
    }




    public byte[] decompressData(InputStream in)      throws IOException {
        Inflater inflate = new Inflater(true);
        InflaterInputStream instr = new InflaterInputStream(in, inflate);
        ByteArrayOutputStream bout = new ByteArrayOutputStream(512);

        int b;
        while ((b = instr.read()) != -1) {
            bout.write(b);
        }
        instr.close();
        bout.close();
        return bout.toByteArray();
    }




    public static void makeDeflater(){
        //Creates a new compressor using the specified compression
        // level. If 'nowrap' is true then the ZLIB header and checksum
        // fields will not be used in order to support the compression
        // format used in both GZIP and PKZIP.
        Deflater d = new Deflater(Deflater.DEFAULT_COMPRESSION, true);

    }




    public static void main(String s[]){
        demoJAVA_JDK();
    }



    
    public static void demoJAVA_JDK() {
        try {
            // Encode a String into bytes
            String inputString = "blahblahblahhhhhhhhhhhhhhhhhh??";
            byte[] input = inputString.getBytes("UTF-8");


            System.out.println("original size = " + inputString.length());


            // buffers for the compressed data
            byte[] output = new byte[100];
            byte[] result = new byte[100];


            // Compress the bytes
            Deflater compresser = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            compresser.setInput(input);
            compresser.finish();
            int compressedDataLength = compresser.deflate(output);


            System.out.println("compressed size = " + compressedDataLength);


            // Decompress the bytes
            Inflater decompresser = new Inflater(true);
            decompresser.setInput(output, 0, compressedDataLength);

            int resultLength = decompresser.inflate(result);
            decompresser.end();

            // Decode the bytes into a String
            String outputString = new String(result, 0, resultLength, "UTF-8");

            System.out.println("decompressed size = " + outputString.length());



        } catch (java.io.UnsupportedEncodingException ex) {
            // handle
        } catch (java.util.zip.DataFormatException ex) {
            // handle
        }

    }
}
