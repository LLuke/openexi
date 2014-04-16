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


import org.xml.sax.ext.DefaultHandler2;
import java.util.Stack;
import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.*;


/**
 * SAX XML FILE PARSING EXAMPLE....
 * 
 * EXI SPECIFIC EVENTS FIRINGS
 */
    
/**
 * This extends the DefaultHnadler class in org.xml.sax.helpers;
 * we need only override the methods we are concerned with. Everything else
 * no-ops.
 * 
 * C:\Program Files\Java\JDK1.6_DOCS\api\org\xml\sax\ext\DefaultHandler2.html
 */
public class encodeHandler extends DefaultHandler2
{
    // stack for all the elements of the xml document...CH needs this so
    //   we know which element this CH came from
    Stack <String> eventStack = new Stack<String>();

    // the master table for this exi instance
    NamespaceTables NStables;

    /**
     * loads xml file into string tables
     * @param inputPath - the xml file and path
     * @param tables - the table to load
     */
    public encodeHandler(String inputPath, NamespaceTables tables)
    {      
        NStables = tables;        
        
        try
        {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            SAXParser parser = factory.newSAXParser();
            XMLReader xmlReader = parser.getXMLReader();
            xmlReader.setProperty(
               "http://xml.org/sax/properties/lexical-handler",
               this);
            parser.parse(new FileInputStream(inputPath), this);
        }
        catch(Exception e)
        {
            System.out.println("Error in encodeHandler Constructor\n"+e);
        }        
    }
    
    /**
     * Strip off the prefix before loading into the localname and values of 
     * the namespace table....it is implied  by the associated namespace table it
     * is loaded into
     * @param qName - the element or attribute to strip the namespace from
     * @return namespace prefix free string of the the input argument
     */
    private String stripNameSpacePrefix(String qName)
    {
        int colon = qName.indexOf(":");
        if(colon == -1)
            return qName;
        
        return qName.substring(colon+1);
    }
    
    
    @Override
    public void startDocument()
    {
        //System.out.println("Start Document Found");
    }


    @Override
    public void endDocument()
    {
        //System.out.println("End Document Found");            
    }        


    // uri = the uri register to this namespace prefix
    // localname is only the name without the prefix
    // qName is the prefix:name format of the attribute or element
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    {
        try
        {
            Tables tables = NStables.getAssociatedTablesForNamespace(qName);

            tables.addStringToQname(stripNameSpacePrefix(qName));

            for(int idx = 0; idx < attributes.getLength(); idx++)
            {
                try
                {
                    tables.addStringToQname(attributes.getQName(idx));
                    int id = tables.addStringToLocalValue(stripNameSpacePrefix(attributes.getQName(idx)), attributes.getValue(idx));
                }
                catch(Exception e)
                {
                    System.out.println("ERROR in start element Attributes " + attributes.getQName(idx) + "\n" + e);
                }
            }
        }
        catch(Exception e)
        {
            System.out.println("ERROR in start element theElement Name\n" + e);
        }

        eventStack.push(qName);
    }


    @Override
    public void endElement(String uri, String localName, String qName)
    {
        eventStack.pop();
    }        


    @Override
    public void characters(char[] ch, int start, int length)
    {
        // current element in the document being parsed by SAX
        String currElement = eventStack.peek();
        // the characters of the element...every element will have this
        // mehtod fire because "" is a character as is "something"
        // only look at cases when not ""
        String words = new String(ch, start, length).trim();

        if(!words.equalsIgnoreCase(""))      
        {
            try
            {
                Tables tables = NStables.getAssociatedTablesForNamespace(currElement);
                
                currElement = stripNameSpacePrefix(currElement);
                
                tables.addStringToLocalValue(currElement, words);
                //System.out.println("Characters found = " + words);   
            }
            catch(Exception e)
            {
                System.out.println("ERROR in characters " + currElement + "  " + words +"\n" + e);
            }
        }
    }        


    @Override
    public void startPrefixMapping(String prefix, String uri)
    {
        NStables.addPrefixAndUri(prefix, uri);
    }        


    @Override
    public void comment(char[] ch, int start, int length) 
    {
        NStables.setComment();
    }        

    //-----------------------------------------------------------
    //  NOT SURE WHERE THESE ARE PUT...WHICH NAMESPACE OR ANY...
    @Override
    public void processingInstruction(String target, String data)
    {
        NStables.setPI();
    }        


    @Override
    public void startDTD(String name, String publicId, String systemId)
    {
        NStables.setDOCTYPE();
    }   


    @Override
    public void endDTD()
    {

    }


    @Override
    public void startEntity(String name) 
    {
        NStables.setEntity();
    }


    @Override
    public void endEntity(String name)
    {

    }            

    //-----------------------------------------------------
    //  DONT KNOW IF THESE ARE NEEDED....
    @Override
    public void startCDATA()
    {
        // probably would need to then get characters...
    }


    @Override
    public void endCDATA()
    {

    }
}