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

import org.apache.exi.datatypes.*;
import java.util.*;

/**
 * Represents a string table, a mapping between a string and a compact
 * identifier. EXI replaces strings with compact identifiers in many
 * situations, and this provides the lookup table between these. 
 * 
 * This has two lookup methods, since we often want to look up a value by either 
 * string name value or compact identifier. The general maps are 
 * String->compact identifier, and compactIdentifer->String.
 * 
 * Arguably we should return a compact identifier object with ore data,
 * such as the number of bits to encode the value.
 * 
 * @author DMcG
 */
public class StringTable 
{
    /**
     * Lookup table, string is the key, comnpact identifier is the value. Use
     * when you have a string and want to find the corresponding CI.
     */
    private HashMap<String, Integer> stringToCompactIdentifier;
    
    /**
     * Lookup table, compact identifer is the key, value is the string. Use when
     * you have the CI and want to find the corresponding string.
     */
    private HashMap<Integer, String> compactIdentifierToString;
    
    /**
     * Constructor
     */
    public StringTable()
    {
        stringToCompactIdentifier = new HashMap<String, Integer>();
        compactIdentifierToString = new HashMap<Integer, String>();
    }
    
    /**
     * Gets the number of entries in this string table
     * @return
     */
    public int getSize()
    {
        return stringToCompactIdentifier.size();
    }
    
    /** 
     * Add a string to the table. The compact identifier is automatically
     * assigned and returned.
     * @param string
     */
    public int addString(String name)
    {

        // This string may already have a compact identifier assigned
        // to it. If that's the case, we simply return the existing
        // value.
       if(stringToCompactIdentifier.containsKey(name))
       {
           return (stringToCompactIdentifier.get(name)).intValue();
       }

       // Otherwise, add it to both hashtables
       int nextIdentifer = stringToCompactIdentifier.size();
       stringToCompactIdentifier.put(name, nextIdentifer);
       compactIdentifierToString.put(nextIdentifer, name);

       return nextIdentifer;
    }
    
    /**
     * Returns the ID for the given local name, eg the element
     * name. If the element name is not found, -1 is returned.
     * Look into returning a more complex object as well here--
     * for example, the number of bits with which to encode the 
     * compact identifier.
     * @param name
     * @return compact identifier for that string. 
     * (throw exception for not found instead?)
     */
    public int getIdentifierForString(String name) throws StringNotFoundException
    {
        Integer compactIdentifier = stringToCompactIdentifier.get(name);
        
        if(compactIdentifier == null)
        {
            throw new StringNotFoundException("String " + name + " not found in string table");
        }
        
        return compactIdentifier.intValue();
    }
    
    /**
     * Returns the string associated with the compact identifier for this
     * string table. Returns null if the compact identifier is not in this
     * table.
     * 
     * @param identifier
     * @return string for the compact identifer, or null if not found.
     */
    public String getStringForIdentifier(int identifier) throws IdentifierNotFoundException
    {
        String aString = compactIdentifierToString.get(new Integer(identifier));
        if(aString == null)
        {
            throw new IdentifierNotFoundException("Compact Identifier " + identifier + " not found in string table");
        }
        return aString;
    }

    
    public boolean stringExists(String aString)
    {
       return stringToCompactIdentifier.containsKey(aString);
    }

    
    public String getExistingStringObject(String aString)
    {
        if(this.stringExists(aString))
        {
            Integer id = stringToCompactIdentifier.get(aString);
            return compactIdentifierToString.get(id);
        }
        return null;
    }
    
    
    public void prettyPrint()
    {
        int size = compactIdentifierToString.size();       
        for(int i = 0; i < size; i++)
            System.out.println("      " + i + " " + compactIdentifierToString.get(i));
    }
}
