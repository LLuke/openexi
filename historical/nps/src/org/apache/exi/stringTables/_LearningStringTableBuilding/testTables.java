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
package org.apache.exi.stringTables._LearningStringTableBuilding;


/**
 *  This is a test app of the string tables and Table classes...
 * 
 * loads the EXI notbook example in the order it would be encoutered by SAX
 * It then prints out the values in the string tables...
 * @author Sheldon L. Snyder
 */
public class testTables 
{    
    public static void main(String s[])
    {
        System.out.println("TEST OF STRING TABLE FROM CLASS(org.apache.exi.stringTables._LearningStringTableBuilding.testTables)");

        NamespaceTables NStables = new NamespaceTables(false, "");
        encodeHandler encodeDoc = new encodeHandler("sampleXML/notebook.xml", NStables);
        //encodeHandler encodeDoc = new encodeHandler("order.xml", NStables);
        NStables.prettyPrint();
    }
}