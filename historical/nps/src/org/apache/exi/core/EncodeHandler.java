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
package org.apache.exi.core;

import org.xml.sax.ext.DefaultHandler2;
import java.util.Stack;
import java.io.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.apache.exi.grammars.events.*;
import org.apache.exi.core.headerOptions.HeaderPreserveRules;
import org.apache.exi.grammars.GrammarsCoder;
import org.apache.exi.io.EXI_OutputStreamIface;
   
/**
 * Parses the XML file using SAX
 * <P>
 * This extends the DefaultHnadler class in org.xml.sax.helpers;
 * we need only override the methods we are concerned with. Everything else
 * no-ops.
 * <P>
 * C:\Program Files\Java\JDK1.6_DOCS\api\org\xml\sax\ext\DefaultHandler2.html
 */
public class EncodeHandler extends DefaultHandler2
{
    // stack for all the elements of the xml document...CH needs this so
    //   we know which element this CH came from
    private Stack <String> eventStack = new Stack<String>();
    private Stack <String> eventStackURI = new Stack<String>();

    private GrammarsCoder grammar;
    private EXI_OutputStreamIface os;
    private int curLevel = -1;

    /**
     * loads xml file into string tables
     * @param inputPath - the xml file and path
     * @param tables - the table to load
     */
    public EncodeHandler(String inputPath, GrammarsCoder gram, EXI_OutputStreamIface eventOS)
    {      
        try
        {
            grammar = gram;
            os = eventOS;
            
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
     * Start Document event
     */
    @Override
    public void startDocument()
    {
        //System.out.println("Start Document");
        EventStartDoc sd = new EventStartDoc();
        sd.setOS(os);

        //System.out.println(sd);
        try {
            grammar.newXMLinputEvent(sd);
        } catch (Exception ex) {
            System.out.println("SD EXCEPTION in parser " + ex);
        }
    }


    /**
     * End Document Event
     */
    @Override
    public void endDocument()
    {
        //System.out.println("End Document");
        EventEndDoc ed = new EventEndDoc();
        ed.setOS(os);

        //System.out.println(sd);
        try {
            grammar.newXMLinputEvent(ed);
        } catch (Exception ex) {
            System.out.println("ED EXCEPTION in parser " + ex);
        }
    }        


    /**
     * Start element event
     *
     * @param uri - namespace URI of this element
     * @param localName - local name (not fully qualifed) of this element
     * @param qName - fully qualified name of this element
     * @param attributes - attributes associated to this element
     */
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    {
        curLevel++;

        try
        {
            // not preserving prefixes and namespaces
            if(!HeaderPreserveRules.PRESERVE_NAMESPACE_PREFIX.isPreserved())
                qName = EXIConstants.stripNameSpacePrefix(qName);

            EventStartElement se = new EventStartElement(uri, localName, qName, curLevel);
            se.setOS(os);// set output stream

            try {
                grammar.newXMLinputEvent(se);
            }
            catch (Exception ex) {
                System.out.println("SE EXCEPTION in parser \t\n" + se + "\n" + ex);
            }
            for(int idx = 0; idx < attributes.getLength(); idx++)
            {
                try
                {
                    String atQname = attributes.getQName(idx);
                    // not preserving prefixes and namespaces
                    if(!HeaderPreserveRules.PRESERVE_NAMESPACE_PREFIX.isPreserved())
                        atQname = EXIConstants.stripNameSpacePrefix(atQname);

                    EventAttribute att = new EventAttribute(
                            attributes.getURI(idx),
                            attributes.getLocalName(idx),
                            atQname,
                            attributes.getValue(idx),
                            localName);
                    att.setOS(os);
                    try {
                        grammar.newXMLinputEvent(att);
                    }
                    catch (Exception ex) {
                        System.out.println("AT EXCEPTION in parser \n\t" + att + "\n" + ex);
                    }
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

        eventStack.push(localName);
        eventStackURI.push(uri);
    }


    /**
     * End element event
     *
     * @param uri - namespace URI of this element
     * @param localName - local name (not fully qualifed) of this element
     * @param qName - fully qualified name of this element
     */
    @Override
    public void endElement(String uri, String localName, String qName)
    {        
        // not preserving prefixes and namespaces
        if(!HeaderPreserveRules.PRESERVE_NAMESPACE_PREFIX.isPreserved())
            qName = EXIConstants.stripNameSpacePrefix(qName);

        EventEndElement ee = new EventEndElement(uri, localName, qName, curLevel);
        ee.setOS(os);
        curLevel--;

        //System.out.println(ee);
        try {
            grammar.newXMLinputEvent(ee);
        } catch (Exception ex) {
            System.out.println("EE EXCEPTION in parser \n\t" + ee + "\n" + ex);
        }

        eventStack.pop();
        eventStackURI.pop();
    }        


    /**
     * Character event
     * <P>
     * Associated to the last start element event
     *
     * @param ch - array of characters
     * @param start - the starting point in array to first letter
     * @param length - length of the array
     */
    @Override
    public void characters(char[] ch, int start, int length)
    {
        // current element in the document being parsed by SAX
        String currElement = eventStack.peek();
        String currURI = eventStackURI.peek();
        // the characters of the element...every element will have this
        // mehtod fire because "" is a character as is "something"
        // only look at cases when not ""
        String words = new String(ch, start, length).trim();

        if(!words.equalsIgnoreCase(""))      
        {
            try
            {
                //EventCharacters(EventType et,String eleQName, String val) {
                //System.out.println("Character data for -" + currElement + "- " + words);
                EventCharacters chars = new EventCharacters(words, currElement, currURI);
                chars.setOS(os);
                //System.out.println(chars);
                try {
                    grammar.newXMLinputEvent(chars);
                } catch (Exception ex) {
                    System.out.println("CH EXCEPTION in parser " + ex);
                }
            }
            catch(Exception e)
            {
                System.out.println("ERROR in characters " + currElement + "  " + words +"\n" + e);
            }
        }
    }        


    /**
     * A namespace event
     *
     * @param prefix - prefix of this namespace
     * @param uri - uri of this namespace
     */
    @Override
    public void startPrefixMapping(String prefix, String uri)
    {
        EventNamespace ns = new EventNamespace(prefix, uri);
        ns.setOS(os);
        try {
            grammar.newXMLinputEvent(ns);
        }
        catch (Exception ex) {
            System.out.println("NS EXCEPTION in parser " + ex);
        }
    }        


    /**
     * Comment event
     *
     * @param ch - array of characters
     * @param start - the starting point in array to first letter
     * @param length - length of the array
     */
    @Override
    public void comment(char[] ch, int start, int length) 
    {
        //System.out.println("Comment" + new String(ch, start, length));
        EventComment cm = new EventComment(new String(ch, start, length));
        cm.setOS(os);
        //System.out.println(cm);
        try {
            grammar.newXMLinputEvent(cm);
        }
        catch (Exception ex) {
            System.out.println("CM EXCEPTION in parser " + ex);
        }
    }        


    /**
     * Processing instruction event
     * <P>
     * <?xml-stylesheet type="text/css" href="bike.css" ?>
     * <BR>
     * Processing Instruction target=(xml-stylesheet) data=(type="text/css" href="bike.css" )
     *
     * @param target - the command
     * @param data - the data support the command
     */
    @Override
    public void processingInstruction(String target, String data)
    {
        //System.out.println("Processing Instruction " + target);
        EventProcessingInstruction pi = new EventProcessingInstruction(target, data);
        pi.setOS(os);
        //System.out.println(pi);
        try {
            grammar.newXMLinputEvent(pi);
        } catch (Exception ex) {
            System.out.println("PI EXCEPTION in parser \n\t" + pi + "\n" + ex);
        }
    }        


    /**
     * DTD event
     *
     * @param name
     * @param publicId
     * @param systemId
     */
    @Override
    public void startDTD(String name, String publicId, String systemId)
    {
        //System.out.println("DTD " + name);
        EventDTD dt = new EventDTD(name, publicId, systemId);
        dt.setOS(os);
        //System.out.println(dt);
        try {
            grammar.newXMLinputEvent(dt);
        } catch (Exception ex) {
            System.out.println("DTD EXCEPTION in parser " + ex);
        }
    }   


    /**
     * Entity event
     *
     * @param name
     */
    @Override
    public void startEntity(String name) 
    {
        //System.out.println("Entity " + name);
        EventEntity er = new EventEntity(name);
        er.setOS(os);
        //System.out.println(er);
        try {
            grammar.newXMLinputEvent(er);
        } catch (Exception ex) {
            System.out.println("ER EXCEPTION in parser " + ex);
        }
    }
    

    /**
     * End DTD event
     */
   @Override
   public void endDTD()
   {
       //System.out.println("End DTD");
   }



   /**
    * End Entity event
    *
    * @param name
    */
   @Override
   public void endEntity(String name)
   {
       //System.out.println("End Entity " + name);
   }

    //-----------------------------------------------------
    //  DONT KNOW IF THESE ARE NEEDED....
   @Override
   public void startCDATA()
   {
       //System.out.println("CDATA ");
   }


   @Override
   public void endCDATA()
   {
       //System.out.println("End CDATA ");
   }
}