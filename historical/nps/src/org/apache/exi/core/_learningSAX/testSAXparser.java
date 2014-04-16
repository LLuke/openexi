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
package org.apache.exi.core._learningSAX;


/**
 *  This is a test app of the string tables and Table classes...
 * 
 * loads the EXI notbook example in the order it would be encoutered by SAX
 * It then prints out the values in the string tables...
 * @author Sheldon L. Snyder
 */
public class testSAXparser
{    
    public static void main(String s[])
    {
        System.out.println("TESTING SAX PARSING CLASS(org.apache.exi.core._learningSAX.testSAXparser)");
        //encodeHandler encodeDoc = new encodeHandler("notebook.xml");
        //encodeHandler encodeDoc = new encodeHandler("dtd.dtd");
        //encodeHandler encodeDoc = new encodeHandler("entity.dtd");
//        encodeHandler encodeDoc = new encodeHandler("sampleXML/customers.xml");
//        encodeHandler encodeDoc = new encodeHandler("sampleXML/HelloWorld.x3d");
        encodeHandler encodeDoc = new encodeHandler("sampleXML/orderNSs.xml");
    }
}