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
 *
 * @author Sheldon L. Snyder
 */
public class NamespaceTables 
{
    // maps URI to its associated Tables
    private HashMap<String, Tables> URITables = new HashMap<String, Tables>(); 
    
    // maps a Prefix to a URI
    private HashMap<String, String> prefixTables = new HashMap<String, String>(); 
    // Prefix sting to id to string helper
    private StringTable prefixST = new StringTable();
    
    
    
    /**
     * Loads the default initial entries into the tables
     * @param schema - boolean indicating is schema informed
     * @param schemaPre - schema prefix used in docudement
     */
public NamespaceTables(boolean schema, String schemaPre)
{
    //Create the default namespace and prefix entries
    addPrefixAndUri("", "");
    addPrefixAndUri("xml", "http://www.w3.org/XML/1998/namespace");
    addPrefixAndUri("xsi", "http://www.w3.org/2001/XMLSchema-instance");   
    
    if(schema)
        addPrefixAndUri(schemaPre, "http://www.w3.org/2001/XMLSchema"); 
}
    
    
    
/**
 * Adds a new namespace to the NamespaceTable
 * @param prefix
 * @param uri
 */
 public int addPrefixAndUri(String prefix, String uri)
 {
     if(!prefixTables.containsKey(prefix))
     {
         prefixTables.put(prefix, uri);
         URITables.put(uri, new Tables());
     }   
     
     return prefixST.addString(prefix);
 }
 
 /**
  * Print out the Namespaces in pretty format
  */
 public void prettyPrint()
 {
    int size = prefixST.getSize();
    // for each namespace print its tables
    for(int i = 0; i < size; i++)
    {
        try
        {
            // get the next prefix
            String prefix = prefixST.getStringForIdentifier(i);
            // get the associated URI for this prefix
            String uri = prefixTables.get(prefix);
            
            String nsHeader = "Namespace Tables -> [Prefix = " + prefix + "], [URI = " + uri + "]";
            System.out.println("\n------------------------------------------");            
            System.out.println(nsHeader.toUpperCase());
            System.out.println("------------------------------------------");
            
            // pretty print the associated Tables for this URI            
            URITables.get(uri).prettyPrint();            
        }
        catch(Exception e)
        {
            System.out.println("Error in pretty print namespaces \n" + e);
        }
    }
 } 
 
 
 /**
  * Get the namespace table for this current addition (Element, Attribute, character)
  * @param qName - table key...table to find
  * @return -  the associated table
  * @throws org.apache.exi.datatypes.StringNotFoundException
  */
 public Tables getAssociatedTablesForNamespace(String qName) throws StringNotFoundException
 {
     // find the prefix marker ':' for this qname
     int colon = qName.indexOf(":");
     
     // no prefix marker then is the default namesapce
     if(colon == -1)
     {
         return URITables.get("");
     }
     
     // get just the uri for the namespace
     String prefixSub = qName.substring(0, colon);
     // get the Tables for this uri
     String uri = prefixTables.get(prefixSub);
              
     return URITables.get(uri);
 }
 
 
 //-----------------------------------------------------------------------------
 //-----------  DEFAULT DOCUMENT LEVEL EVENTS NOT NAMESPACE UNIQUE -------------
 //-----------------------------------------------------------------------------
 /**
  * Perform Comment additon operations if not pruned
  */ 
 public void setComment()
 {
     
 }
 
 /**
  * Perform Processing Instruction additon operations if not pruned
  */ 
 public void setPI()
 {
     
 }
 
 /**
  * Perform entity additon operations if not pruned
  */
 public void setEntity()
 {
     
 }
 
 
 /**
  * Perform DOCTYPE additon operations if not pruned
  */
 public void setDOCTYPE()
 {
     
 } 

 
 
 
 

     

}
