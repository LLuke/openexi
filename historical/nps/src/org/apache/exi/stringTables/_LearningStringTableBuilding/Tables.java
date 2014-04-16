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

import java.util.*;

/**
 * This is a instance of a namespace's string tables...
 *      local and values and Global for this namespace * 
 * 
 * This class then delegates the managment of the individual tables to the
 * class StringTable.java
 * 
 * Represents the string data of the EXI standard. This consists of
 * several sub-tables, such as the string table for URIs, the 
 * local string table, the global string table, etc.
 * @author DMcG
 */
public class Tables
{ 
    /**
     * The qnames string table, holds local names of elements, attributes and 
     * type declarations when schema is used 7.3.1 specification
     */
    private StringTable qnamesStringTable = new StringTable();    
    
    /**
     * Local value tables, one per qname (prefix + localname), so every element
     * and attribute has its own StringTable.
     */
    private HashMap<String, StringTable> localValueTables = new HashMap<String, StringTable>();    
    
    /**
     * The global string table, holds attributes and xml content. All the data
     * strings should be entered in this table. (ONLY VALUES...NOT QNAMES)
     */
    private StringTable globalStringTable = new StringTable();

 
 /**
  * Returns the compact identifier for a global string. Returns
  * -1 if the string is not found.
  * 
  * @param aString
  * @return
  */
 public int getGlobalCompactIdentifierForString(String aString) throws StringNotFoundException
 {
     return globalStringTable.getIdentifierForString(aString);
 }
 
 public void addStringToGlobalStringTable(String aString)
 {
     globalStringTable.addString(aString);
 }
 
 private String getStringFromGlobalStringTable(String aVal)
 {
     return globalStringTable.getExistingStringObject(aVal);
 }
  
 
 public void prettyPrint()
 {    
     System.out.println("QNAMES (for this URI)");
     qnamesStringTable.prettyPrint();       
     
     System.out.println("\nVALUES (for this QNAME of this URI)");
     
     // FOR EACH QNMAME PRINT ITS 
     int sizeQname = qnamesStringTable.getSize();
     for(int i = 0; i < sizeQname; i++)
     {
         try
         {
            String qname = qnamesStringTable.getStringForIdentifier(i);
            StringTable aTable = localValueTables.get(qname);
            System.out.println("   " + qname);
            aTable.prettyPrint();
         }
         catch(Exception e)
         {
             System.out.println("Error in pretty print values \n" + e);
         }    
     }     
     
     System.out.println("\nGLOBALS");
     globalStringTable.prettyPrint();      
 } 
 
 
 
 /** Given a qname and a string value, return the compact identifer for
  * that value. this operates on the local value tables for that qname.
  * @param qname qualified name
  * @param value the value we want the compact idientifer for
  * @return the compact identifier 
  */
 public int getIdentifierForLocalValue(String qname, String value) throws StringNotFoundException
 {     
     return localValueTables.get(qname).getIdentifierForString(value);    
 }
 
 /**
  * Given a qname, gets its compact identifier 
  *     (Elements names, Attributes names and type Declarations in Schema)
  * @param qname
  * @return
  * @throws org.apache.exi.datatypes.StringNotFoundException
  */
 public int getIdentifierForQname(String qname) throws StringNotFoundException //, String aString) throws StringNotFoundException
 {   
     return qnamesStringTable.getIdentifierForString(qname);
 } 
 
 /**
  * Adds a value to the qname table
  * 
  * Get the string table associated with qname then add aString to the table.
  * @param qname
  * @param aString
  */
 public int addStringToLocalValue(String qname, String value) throws StringNotFoundException
 {
     StringTable aTable = localValueTables.get(qname);
     
     // verify the qname exist, if not throw exception
     // should get evens of add qname then add value in that order
     //   therefore, if no qname exist when adding to value, this is error
     if(aTable == null)
         throw new StringNotFoundException("qname String " + qname + " not found in string table");
       
     // add to qname vlaues and global...if already exist no change to table
     
     // exit in the local table...no more processing
     if(aTable.stringExists(value))
         return aTable.getIdentifierForString(value);
     
     // not in local but in global...retrun golbal and add to the local
     if(globalStringTable.stringExists(value))
     {
         aTable.addString(value);
         return globalStringTable.getIdentifierForString(value);
     }
     
     aTable.addString(value);
     return globalStringTable.addString(value); 
 } 

    /**
    * Adds a element or attribute name to the quname table and makes mirror entry
    * for the localValueTable
    * 
    * if the qname already exist, then do nothing
    * 
    * @param qname
    */
    public void addStringToQname(String qname)
    {
        if(!qnamesStringTable.stringExists(qname))
        {
            qnamesStringTable.addString(qname);
            localValueTables.put(qname, new StringTable());
        }
    }  
    
}