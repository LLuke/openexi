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
package org.apache.exi.stringTables;

import java.util.*;


/**
 *
 * @author Sheldon L. Snyder
 */
public class NamespaceTables {
    // maps URI to its associated Tables

    private HashMap<String, Tables> URITables = new HashMap<String, Tables>();
    // maps a Prefix to a URI
    private HashMap<String, String> prefixTables = new HashMap<String, String>();
    // Prefix sting to id to string helper
    private StringTable prefixST = new StringTable();
    private StringTable uriST = new StringTable();

    /**
     * Loads the default initial entries into the tables
     * @param schema - boolean indicating is schema informed
     * @param schemaPre - schema prefix used in docudement
     */
    public NamespaceTables(boolean schema, String schemaPre) {
        //Create the default namespace and prefix entries
        addPrefixAndUri("", "");
        addPrefixAndUri("xml", "http://www.w3.org/XML/1998/namespace");
        addPrefixAndUri("xsi", "http://www.w3.org/2001/XMLSchema-instance");

        if (schema) {
            addPrefixAndUri(schemaPre, "http://www.w3.org/2001/XMLSchema");
        }
    }

    /**
     * how many URI are in the table
     * @return how many uri are in the XML document so far
     */
    public int getCountOfURI() {
        //return uriST.getSize();
        return URITables.size();
    }
    /**
     * get the ID (index) to this uri, if does not exist, then -1
     * @param uri
     * @return index of this uri
     */
    public int getIDforURI(String uri) {
        return uriST.getIdentifierForString(uri);
    }
    /**
     * how many Prefixes are in the table
     * @return how many prefixes are in table
     */
    public int getCountOfPrefix() {
        return prefixST.getSize();
    }
    /**
     * get the ID (index) to this prefix, if does not exist, then -1
     * @param prefix
     * @return index of this prefix
     */
    public int getIDforPrefix(String prefix) {
        return prefixST.getIdentifierForString(prefix);
    }

    /**
     * Adds a new namespace to the NamespaceTable
     * @param prefix
     * @param uri
     * @return index of the added uri/prefix
     */
    public int addPrefixAndUri(String prefix, String uri) {
        if (!URITables.containsKey(uri)) {
            prefixTables.put(prefix, uri);
            URITables.put(uri, new Tables());
        }
        uriST.addString(uri);
        return prefixST.addString(prefix);
    }
    
    public String getUriForID(int id) throws Exception{
        return uriST.getStringForIdentifier(id);
    }

    public String getPrefixForURI(String uri){
        String rst = "";

        rst = prefixTables.get(uri);

        return rst;
    }

    //prefixTables

//    private HashMap<String, Tables> URITables = new HashMap<String, Tables>();
//    // maps a Prefix to a URI
//    private HashMap<String, String> prefixTables = new HashMap<String, String>();
//    // Prefix sting to id to string helper
//    private StringTable prefixST = new StringTable();
//    private StringTable uriST = new StringTable();

    public int addURI(String uri){
        URITables.put(uri, new Tables());
        return uriST.addString(uri);
    }

    /**
     * Namespace event to add prefix to uri
     * 
     * @param prefix
     * @param uri
     * @return
     */
    public int addPrefixToURI(String prefix, String uri) {
        prefixTables.put(prefix, uri);
        return prefixST.addString(prefix);
    }

    public void printLevels(){
int size = uriST.getSize();
        // for each namespace print its tables
        for (int i = 0; i < size; i++) {
            try{
                String uri = uriST.getStringForIdentifier(i);
                System.out.println("@@@@@@@@@@\t" + uri + "\t@@@@@@@@@@");
                URITables.get(uri).printTableLevels();
            }
            catch(Exception e) {}            
        }
    }

    /**
     * Print out the Namespaces in pretty format
     */
    public void prettyPrint() {
        int size = uriST.getSize();
        // for each namespace print its tables
        for (int i = 0; i < size; i++) {
            try {
                // get the next prefix and uri
                String prefix="";
                String uri = uriST.getStringForIdentifier(i);
                try{
                    prefix = prefixST.getStringForIdentifier(i);                    
                }
                catch(Exception e){/**case prefixes pruned...no id for index i*/}
                
                String nsHeader = "Namespace Tables -> [Prefix = " + prefix + "], [URI = " + uri + "]";
                System.out.println("\n------------------------------------------");
                System.out.println(nsHeader.toUpperCase());
                System.out.println("------------------------------------------");

                // pretty print the associated Tables for this URI
                URITables.get(uri).prettyPrint();
            } catch (Exception e) {
                System.out.println("Error in pretty print namespaces \n" + e);
            }
        }
    }

    /**
     * Get the namespace table for this current addition (Element, Attribute, character)
     * @param uri - uri table to find
     * @return -  the associated table
     */
    public Tables getAssociatedTablesForNamespace(String uri) {
        return URITables.get(uri);
    }
}