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

import java.io.IOException;
import java.io.InputStream;
import org.apache.exi.core.EXIConstants;
import org.apache.exi.datatypes.DataTypeUnSignInteger;

/**
 *  input of the EXI file
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 */
public class EXI_ByteAlignedInput extends EXI_abstractInput
{
    private InputStream	istream;
    String fileName = null;

    /**
     *
     * @param file - input file stream of the EXI file
     * @param fileName - raw string name of the input exi file
     */
	public EXI_ByteAlignedInput (InputStream file, String fileName)
	{
        this.istream = file;
        this.fileName = fileName;

	}

    /**
     * REads in raw bytes...header for exampel is 2 bytes and dont care about
     * it right now so just read 2 bytes
     * 
     * @return
     * @throws java.io.IOException
     */
    @Override
    public int read() throws IOException {
        int in = istream.read();
        return in;
    }

    /**
     * read in a string literal
     * 
     * @param n
     * @return
     */
    public String readStringLiteral(int n) {
        String s = "";
        try{
            for(int i = 0; i < n; i++){
                int nextChar = read();
                s += (char)nextChar;
            }
        }
        catch(Exception e){
            System.out.println(e + "\nERROR reading String literal for XML output (readStringLiteral)");
        }
        return s;
    }

    /**
     * read an unsigned integer
     *
     * @return
     */
    public int readUInt() {
        int uint = EXIConstants.VALUE_NOT_YET_SET;
        try {
            uint = DataTypeUnSignInteger.readUnsignedInt(this);
        } catch (Exception e) {
            System.out.println(e + "\nERROR reading UInt for XML output (readUInt)");
        }
        return uint;
    }

    /**
     * read n bits from the stream...only working byte aligned in this class
     * so just do an uint
     *
     * @param n
     * @return
     */
    public int readNbit(int n) {
        int uint = EXIConstants.VALUE_NOT_YET_SET;

        if(n > 0)
            uint = readUInt();
        return uint;
    }

    /**
     * close the input stream
     */
    public void cleanAndClose() {
        try{
            this.close();
        }
        catch(Exception e){
            System.out.println(e + "\nERROR in XML decode close (cleanAndClose)");
        }
    }

    /**
     * read in the default header 0x90
     */
    public int defaultHeader90() {
        int hex90 = -1;
        try{
            hex90 = (int)read();
        }
        catch(Exception e){
            System.out.println(e + "\nERROR in XML decode read EXI header (defaultHeader90)");
        }
        return hex90;
    }

}
