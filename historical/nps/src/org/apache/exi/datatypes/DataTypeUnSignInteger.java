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
package org.apache.exi.datatypes;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 *  ENCODES and DECODES EXI Unsigned Integers
 * 
 * @author Sheldon L. Snyder
 */
public class DataTypeUnSignInteger {

    /**
     * Writes this integer in EXI unsigned (7-bit) format to the output
     * stream povided.
     * 
     * @param outStream the output stream to write to
     * @param x the integer to convert to EXI UnsignedInt
     */
    public static void writeUnsignedInt(OutputStream outStream, int x) 
            throws IOException {
        int[] outBytes = getEncodedUsignedInt(x);
        for(int i = 0; i < outBytes.length; i++)  {
            outStream.write(outBytes[i]);
        }
    }




    /**
     * Read in an UsignedInt from an EXI file
     *
     * 1. Start with the initial value set to 0 and the initial multiplier
     * set to 1.
     *
     * 2. Read the next octet.
     * 
     * 3. Multiply the value of the unsigned number represented by the 7 least
     * significant bits of the octet by the current multiplier and add the
     * result to the current value.
     *
     * 4. Multiply the multiplier by 128.
     *
     * 5. If the most significant bit of the octet was 1, go back to step 2.
     *
     * @param istream the EXI input file
     * @return an int read in
     */
    public static int readUnsignedInt(InputStream istream) throws IOException {
        int multiplier = 1;
        int value = 0;
        int valueBuff = 0;
        int intIn = 0;
        do{
            intIn = istream.read();
            valueBuff = intIn & 0x7F;
            value += valueBuff * multiplier;
            multiplier *= 128;
            intIn = intIn >> 7;
        }while(intIn > 0);
        
        return value;
    }






    /**
     * Solves for how many 7bit blocks are needed to endcode this int
     *
     * @param n - the value (unsigned) to solve for 7 bit pattern
     * @return the count of 7 bit bytes needed to encode this int
     */
    public static int howMany7BitBytes(int n){
        int howmany = 1;    // how many 7 bit bytes to write

        if (n < 0) {
            throw new IllegalArgumentException("must be a positive number to " +
                    "DataTypeUnSignInteger.howMany7BitBytes [" + n +"]");
        }
        while((n = n >> 7) > 0)  {   howmany++;    }
        return howmany;
    }




    /**
     * Because a byte is in the range of -128 and 127, the most significant
     * bit of a byte cannot be 1 and still be a valid byte
     *
     * The workaround is to use int knowing the output stream will write
     * bytes from int, just have to control that it only writes out the first
     * of the 4 bytes of the int
     *
     * @param n the integer to convert to EXI unsignedInt
     * @return and array of bytes (as integers)
     */
    public static int[] getEncodedUsignedInt(int n){
        int howMany = howMany7BitBytes(n);
        int[] byteToEXI = new int[howMany];

        // 1607564 = 1100010    0001111    0001100
        // in EXI order: 10001100   10001111    01100010
        for(int i = 0; i < howMany; i++) {
            byteToEXI[i] = (int)(n & 127); //
            if(i != howMany - 1)
                byteToEXI[i] += 128;
            n = n >> 7;
       }
        return byteToEXI;
    }





    /**
     * @param bytes the sources of the output to format for pretty print
     * @return the pretty formatted binary as a stirng of this UsignedInt
     */
    public static String prettyPrintUnsignedInt(int[] bytes)
    {
        String hex = "";
        String buff = "";
        int len = bytes.length;
        int buffLen;

        for(int i = 0; i < len; i++) {
            buff = Integer.toBinaryString(bytes[i]);
            buffLen = buff.length();
            for(int j = 0; j < 8-buffLen; j++)// force display of 8 bits
                buff = "0" + buff;
            hex += buff + "  ";
        }
        return hex;
    }
}