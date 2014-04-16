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
package org.apache.exi.core;

import java.io.*;
import java.text.DecimalFormat;

/**
 * a pretty print of the output byte aligned exi file<P>
 *
 * list the contents by by number of the exi file, allows for manual debugging
 * of the output exi file and simple view of the resulting output of the
 * exi compression
 * <p>
 *
 *
 * for example: {byte number = decimal _ [hex] _ (binary) - ASCII}
 * <pre>
0001 = 144 _ [90] _ (1001 0000) - ?
0002 = 000 _ [00] _ (0000 0000) -
0003 = 001 _ [01] _ (0000 0001) - 
0004 = 009 _ [09] _ (0000 1001) -
0005 = 110 _ [6E] _ (0110 1110) - n
0006 = 111 _ [6F] _ (0110 1111) - o
0007 = 116 _ [74] _ (0111 0100) - t
0008 = 101 _ [65] _ (0110 0101) - e
0009 = 098 _ [62] _ (0110 0010) - b
0010 = 111 _ [6F] _ (0110 1111) - o
0011 = 111 _ [6F] _ (0110 1111) - o
0012 = 107 _ [6B] _ (0110 1011) - k
0013 = 001 _ [01] _ (0000 0001) - 
0014 = 001 _ [01] _ (0000 0001) - 
0015 = 005 _ [05] _ (0000 0101) - 
0016 = 100 _ [64] _ (0110 0100) - d
0017 = 097 _ [61] _ (0110 0001) - a
0018 = 116 _ [74] _ (0111 0100) - t
0019 = 101 _ [65] _ (0110 0101) - e
0020 = 012 _ [0C] _ (0000 1100) - 
0021 = 050 _ [32] _ (0011 0010) - 2
0022 = 048 _ [30] _ (0011 0000) - 0
0023 = 048 _ [30] _ (0011 0000) - 0
0024 = 055 _ [37] _ (0011 0111) - 7
0025 = 045 _ [2D] _ (0010 1101) - -
0026 = 048 _ [30] _ (0011 0000) - 0
0027 = 057 _ [39] _ (0011 1001) - 9
0028 = 045 _ [2D] _ (0010 1101) - -
0029 = 049 _ [31] _ (0011 0001) - 1
0030 = 050 _ [32] _ (0011 0010) - 2
0031 = 001 _ [01] _ (0000 0001) - 
0032 = 003 _ [03] _ (0000 0011) - 
0033 = 001 _ [01] _ (0000 0001) - 
0034 = 005 _ [05] _ (0000 0101) - 
0035 = 110 _ [6E] _ (0110 1110) - n
0036 = 111 _ [6F] _ (0110 1111) - o
0037 = 116 _ [74] _ (0111 0100) - t
0038 = 101 _ [65] _ (0110 0101) - e
0039 = 001 _ [01] _ (0000 0001) - 
 * </pre>
 *
 *
 *
 *
 *
 *
 *
 * @author SheldonAcess
 */
public class byteToHex
{

    public static void makeByteToHex(String inFile)
    {
        try
        {
            File file = new File(inFile);
            String path = file.getPath();

            // reads in 1 byte at a time...8 bits
            FileInputStream fin = new FileInputStream(file);
            // output file
            PrintWriter pw = new PrintWriter(path + "_HEX.txt");

            int read;
            String binary;
            String hex;
            String outFormat;

            DecimalFormat byteNumberFormat = new DecimalFormat("0000");
            DecimalFormat decimalNumberFormat = new DecimalFormat("000");

            int ct = 1;
            while((read = fin.read()) != -1)
            {
                binary = formatBinary(read);
                hex = formatHEX(read);
                char c = (char)read;

                outFormat = byteNumberFormat.format(ct) + " = " +
                        decimalNumberFormat.format(read) +
                        " _ [" + hex + "] _ (" + binary + ") - " + c;

                pw.println(outFormat);
                //System.out.println(outFormat);

                ct++;
            }

            pw.close();
            fin.close();
        }// try
        catch (Exception e)
        {
            System.out.print(e);
        }
    }// byteToHex()



    // format HEX to "AA"
    private static String formatHEX(int b)
    {
        String hex = Integer.toHexString(b).toUpperCase();
        if(hex.length() != 2)
            hex = "0"+hex;
        return hex;
    }




    // format binary to "0000 0000"
    private static String formatBinary(int b)
    {
        String binary = Integer.toBinaryString(b);
        int len = binary.length();
        String buff = "";

        // ensure padded with enough 0
        for(int i = 0; i < 8-len; i++)
            buff += "0";
        binary = buff + binary;

        // put the space inbetween each 4 group
        String left = binary.substring(0, 4);
        String right = binary.substring(4);
        binary = left + " " + right;

        return binary;
    }
}