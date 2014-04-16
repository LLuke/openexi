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
import java.io.OutputStream;
import org.apache.exi.datatypes.DataTypeUnSignInteger;


public class EXI_ByteAlignedOutput  extends OutputStream implements EXI_OutputStreamIface
{
    private OutputStream	ostream;
    String fileName = null;

    /**
     * Byte Aligned output...no nbit...everything is coded as UINT or Litteral
     *
     * Uint
     *      String literal length,
     *      GlobalValue hit flag (1)
     *      LocalValue hit flag (0)
     *      LocalName hit flag (0)
     *
     * 
     * Nbit
     *      Event codes
     *
     *      URI hit index (+1)
     *      Value hit index
     *      Global hit index
     *
     * Litterals
     *      String misses of LovalName, Values, Namespaces
     *      
     *
     */
  
	public EXI_ByteAlignedOutput (OutputStream file, String fileName)
	{
        this.ostream = file;
        this.fileName = fileName;
	}


    public void writeStringLiteral(String s, int plus) {
        try {
            writeUInt(s.length()+plus);
            ostream.write(s.getBytes());
        } catch (IOException ex) {
            System.out.println("****STRING IO ERROR****");
        }
    }

    public void writeUInt(int i) {
        int[] outBytes = DataTypeUnSignInteger.getEncodedUsignedInt(i);
        for(int idx = 0; idx < outBytes.length; idx++)  {
            try {
                ostream.write(outBytes[idx]);
            } catch (IOException ex) {
                System.out.println("****UINT IO ERROR****");
            }
        }
    }

    public void cleanAndClose() {
        try {
            ostream.flush();
            ostream.close();
        } catch (IOException ex) {
            System.out.println("Close Errors");
        }
    }

    /**
     * Writes a rawInteger
     * 
     * @param b
     */
    @Override
    public void write(int b)  {
        try{
            ostream.write(b);
        }
        catch(Exception e){}
    }

    /**
     * The default no options header....will need to work on this later
     */
    public void defaultHeader90() {        
        try{
            /**do not change...for now ...this is how */
            ostream.write(144);
        }
        catch(Exception e){}
    }



    /**
     * an UInt but only of length n
     *
     * Not implemented in Byte aligned...this class...calls Uint
     *
     * @param value
     * @param count
     */
    public void writeNbit(int value, int n) {
        if(n > 0){
            writeUInt(value);
        }
    }

    

    
 
}