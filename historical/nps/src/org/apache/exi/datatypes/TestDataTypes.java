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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Test file to verify Unsigned Integer code work
 * 
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 */
public class TestDataTypes 
{
    public static void main(String[] args)
    {
        // TEST INTEGER 50 (less than 128), 128, and large number
            // 1607564 = 1 1000 1000 0111 1000 1100
            // 1420160756 = 101 0100 1010 0101 1110 1110 1111 0100
            // 128 = 1000 0000
        int check = 300;

        // HOW MANY BYTES NEEDED TO ENDOCDE THIS INT AS AN UNSIGNEDINT
        int howMany = DataTypeUnSignInteger.howMany7BitBytes(check);        
        System.out.println("To encode UnSignedInt " + check + " it takes " +
                howMany + " bytes \n\t" +
                "normal binary fomat = " + Integer.toBinaryString(check));

        // ENCODE THE INT TO UNSIGNED EXI
        int[] outByte = DataTypeUnSignInteger.getEncodedUsignedInt(check);
        System.out.println("EXI UnSigned Binary format = " +
                DataTypeUnSignInteger.prettyPrintUnsignedInt(outByte) +
                "\n\n");
        
        try// EXECUTE ROUND TRIP
        {
            // WRITE TO FILE
            FileOutputStream out = new FileOutputStream("dataTypeTest");
            DataTypeUnSignInteger.writeUnsignedInt(out, check);
            out.flush();
            out.close();

            // READ FROM FILE
            FileInputStream in = new FileInputStream("dataTypeTest");
            int input = DataTypeUnSignInteger.readUnsignedInt(in);
            System.out.println("Value read in = " + input);
        } 
        catch (IOException ex) 
        {
            System.out.println("ERROR\n" + ex);
        }
    }
}