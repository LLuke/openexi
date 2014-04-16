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
package org.apache.exi.grammars;

import org.apache.exi.core.headerOptions.HeaderPreserveRules;
import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;
import org.apache.exi.core.EXIConstants;
import org.apache.exi.core.EncodeHandler;
import org.apache.exi.grammars.events.*;
import org.apache.exi.grammars.grammarRules.*;
import org.apache.exi.io.EXI_OutputStreamIface;
import org.apache.exi.io.EXI_abstractInput;
import org.apache.exi.io.XML_DocumentWriter;
import org.apache.exi.io.XML_OutputStreamIface;
import org.apache.exi.stringTables.*;

/**
 * Grammar structure for Schemaless encoding of XML to EXI to XML
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 *
 *
 * Container for all the grammars DocContent, StartTag, Element, Fragment
 */
public class GrammarRunner implements GrammarsCoder
{
    //current rule in effect
    Stack <GrammarRuleSchemaLessAbstract> grammarStack;

    // list of all grammars in this document
    HashMap<String, GrammarRuleSchemaLessAbstract> listOfGrammars;

    Vector<EventAbstract> listNS = new Vector<EventAbstract>();
    boolean nameSpace = false;

    // current grammar in use
    GrammarRuleSchemaLessAbstract curGrammar = null;
    GrammarRuleSchemaLessAbstract nextGrammar = null;

    NamespaceTables NStables;
    EncodeHandler encodeDoc;
    private int currentDocumentLevel = -1;

    // TROUBLE SHOOTING PRINT OUTS... FOR EACH EVENT
        // EVENT THAT JUST FIRED
            // GRAMMAR STATE
    boolean verbose = false;

    EXI_abstractInput exiInputStream = null;
    XML_OutputStreamIface xmlOut;

    /**
     *  ENCODE XML to EXI Grammar Constructor
     *
     * @param xml - path and name of the xml file to encode
     * @param os - the output file to write EXI
     */
    public GrammarRunner(String xmlInput, EXI_OutputStreamIface os, boolean details){
        try
        {
            verbose = details;
            init();
            encodeDoc = new EncodeHandler(xmlInput, this, os);
        }
        catch(Exception e) {
            System.out.println("Error in GRAMMAR constructor XML->EXI");
        }
    }

    /**
     *  DECODE to EXI to XML Grammar Constructor
     *
     * @param xml - path and name of the xml file to encode
     * @param os - the output file to write EXI
     */
    public GrammarRunner(String xmlOutput, EXI_abstractInput is, boolean details){
        try
        {            
            verbose = details;
            exiInputStream = is;
            xmlOut = new XML_DocumentWriter(xmlOutput); 
            exiInputStream.defaultHeader90();
            init();            
            nextEXIinputEvent();
        }
        catch(Exception e) {
            System.out.println("Error in GRAMMAR constructor EXI->XML\n" + e + "\nError in GRAMMAR constructor EXI->XML");
        }
    }

    /**
     * Initialize the Grammar IO to a new grammar structure ready for XML events
     * or EXI events
     *
     * @throws java.lang.Exception
     */
    private void init() throws Exception{
        listOfGrammars = new HashMap<String, GrammarRuleSchemaLessAbstract>();
        grammarStack = new Stack<GrammarRuleSchemaLessAbstract>();
        NStables = new NamespaceTables(false, null);

        GrammarRuleSchemaLessDocContent docContent = new GrammarRuleSchemaLessDocContent();
        curGrammar = docContent;
    }

    /**
     * Gets the number of entries in this grammar
     * 
     * @return
     */
    public int getCurrentGrammarSize(){
        return curGrammar.getSize();
    }

    public boolean doesCurrentGrammarContain(EventAbstract evt){
        return curGrammar.containsEvent(evt);
    }

    
    /**
     * Does this event mean the grammar needs to transition to a new grammar?
     * If so, make the new grammar and push the old onto the grammar stack
     * <P>
     * transition to a new grammar if this is a start Element event
     *  its added to the current grammar before the new grammar is made
     * @param evt - event to check if it forces a new grammar
     */
    private void needNewGrammar(EventAbstract evt) {
        EventType type = evt.getEventType();

        if(type.equals(EventType.SE))
        {
            EventStartElement SE = (EventStartElement)evt;

            //***************************
            curGrammar.setTransitioned(true);
            
            grammarStack.push(curGrammar);
            nextGrammar = listOfGrammars.get(evt.getFullyQualifiedLongName());  // maybe null


            // a previous grammar does not exist
            if(nextGrammar == null)// make a new grammar
            {                
                GrammarRuleSchemaLessStartTag rst = new GrammarRuleSchemaLessStartTag(
                        evt.getFullyQualifiedLongName(),
                        SE.getUri(), SE.getLocalName(), SE.getQName(), SE.getLevel());
                curGrammar = rst;
                listOfGrammars.put(evt.getFullyQualifiedLongName(), rst);
            }
            else // a previous grammar does exist
            {
                curGrammar = nextGrammar;
            }

            //********************************
            curGrammar.setTransitioned(false);

            // process name space events if any
            if(nameSpace){
                processNameSpaces();
            }                
        }//if(type.equals(EventType.SE))
        else if(type.equals(EventType.EE))
        {
            curGrammar.setTransitioned(false);
            curGrammar = grammarStack.pop();
        }
    }



    /**
     * Namespaces are fired before the elements are fired so PI need to be
     * processed after the element event...Namespaces are not docContent items
     * <P>
     * EXI processes elements then Namespaces
     * <P>
     * Events then Namespaces IAW EXI documentation
     */
    private void processNameSpaces(){
        nameSpace = false;
        for(int i = 0; i < listNS.size(); i++){
            processXMLinputEvent(listNS.elementAt(i));
        }

        listNS.clear();
    }



    /**
     * based on the preserve settings determine if this event to be ignored or not
     *
     * @return
     */
    private boolean isPruned(EventAbstract evt){
        EventType et = evt.getEventType();

        if(et.equals(EventType.CM) && !HeaderPreserveRules.PRESERVE_CM.isPreserved())
            return true;
        else if(et.equals(EventType.PI) && !HeaderPreserveRules.PRESERVE_PI.isPreserved())
            return true;
        else if(et.equals(EventType.DT) && !HeaderPreserveRules.PRESERVE_DTD_ENTITY.isPreserved())
            return true;
        else if(et.equals(EventType.ER) && !HeaderPreserveRules.PRESERVE_DTD_ENTITY.isPreserved())
            return true;
        else if(et.equals(EventType.NS) && !HeaderPreserveRules.PRESERVE_NAMESPACE_PREFIX.isPreserved())
            return true;
        else
            return false;
    }

    /**
     * RECEIVED FORM THE XML PARSER
     *
     * If not pruned, then process and then check if a new grammar is needed
     *
     * @param evt
     */
    public void newXMLinputEvent(EventAbstract evt)
    {           
        if(!isPruned(evt)){            
            if(evt.getEventType().equals(EventType.NS))
            {
                listNS.add(evt);
                nameSpace = true;                
            }
            else
            {
                processXMLinputEvent(evt);
            }

            needNewGrammar(evt);            
        }//if(!isPruned(evt))
    }

    /**
     * reading EXI to make XML next EXI event
     *
     * 1) determine the max code an event at this time could have
     *  a) this is the n-bit solution
     * 2) get the value of the event code of the next event from the exi file
     *  a) if max code = 0 then nothing
     * 3) Determine based on the current grammar what type of event this
     *    event code equates to
     * 4) based on the event type, get the follow on event values and content
     * 5) add event to both grammar and string table
     * 6) write the event to exi
     * 7) repeat until EOF or ED
     */
    public void nextEXIinputEvent() throws Exception {        
        EventAbstract evt = new EventStartElement();
        EventType evtType = evt.getEventType();
        int ct = 0;
        Stack <String> curElement = new Stack<String>();
        Stack <String> curUri = new Stack<String>();

        try{
            while(!evtType.equals(EventType.ED) && ct < 25){
                int max1 = curGrammar.getMaxEventCodePart1();
                int max2 = curGrammar.getMaxEventCodePart2();
                int max3 = curGrammar.getMaxEventCodePart3();
                
                int evtCode1 = EXIConstants.VALUE_NOT_YET_SET;
                int evtCode2 = EXIConstants.VALUE_NOT_YET_SET;
                int evtCode3 = EXIConstants.VALUE_NOT_YET_SET;

                int howMany = 0;

                if(max1 > 0){
                    howMany = EXIConstants.howManyBits(max1);
                    if(howMany == 0) howMany = 1;
                    evtCode1 = exiInputStream.readNbit(howMany);
                }
                // if the first event code is less than size of the grammar, then it is
                // a repeated event...contained within the grammar
                if((evtCode1 == curGrammar.getSize()) || evtCode1 == EXIConstants.VALUE_NOT_YET_SET){ // if it is a repeat only 1 code...dont read other 2
                    if(max2 > 0){
                        howMany = EXIConstants.howManyBits(max2);
                        if(howMany == 0) howMany = 1;
                        evtCode2 = exiInputStream.readNbit(howMany);
                    }
                    if(max3 > 0){
                        howMany = EXIConstants.howManyBits(max3);
                        if(howMany == 0) howMany = 1;
                        evtCode3 = exiInputStream.readNbit(howMany);
                    }
                }

                // is this a duplicate event?
                if(evtCode1 != EXIConstants.VALUE_NOT_YET_SET && evtCode3 == EXIConstants.VALUE_NOT_YET_SET && evtCode2 == EXIConstants.VALUE_NOT_YET_SET){
                    // duplicate event, get from the grammar
                    evt = curGrammar.getEvent(evtCode1);
                    evt.setRepeatFind(true);
                }
                else{
                    // determin the event type based on current grammar for this event code
                    evt = curGrammar.getEventForCode(evtCode1, evtCode2, evtCode3);
                }

                evt.setEventCodePart1(evtCode1);
                evt.setEventCodePart2(evtCode2);
                evt.setEventCodePart3(evtCode3);                
                evt.setInputstream(exiInputStream);

// print the read in event code
// System.out.print(curGrammar.getCurGrammarName() + " (size=" + curGrammar.getSize() + ")  evt = [" + evtCode1 + ", " + evtCode2 +
//                        ", " + evtCode3 + "] ");


                // get the events contents from the exi stream
                evt.readEventContents(NStables, this.curGrammar);



                

                evtType = evt.getEventType();
                if(evtType.equals(EventType.CH)){
                    ((EventCharacters)evt).setEleQName(curElement.peek());
                    ((EventCharacters)evt).setUri(curUri.peek());                    
                }
                else if(evtType.equals(EventType.SE)){
                    curElement.add(((EventStartElement)evt).getQName());
                    curUri.add(evt.getUri());
                }
                else if(evtType.equals(EventType.EE)){
                    String endElement = curElement.pop();
                    ((EventEndElement)evt).setQName(endElement);
                    ((EventEndElement)evt).setName(endElement);
                    curUri.pop();
                    evt.setGramTrans(curGrammar.isTransitioned());
                }

                this.doEncodePrints(evt);
//System.out.println(evt);
//System.out.println("\t" + curGrammar);



                xmlOut.writeEvent(evt);

                addToTables(evt);
                if(canAddToGrammar(evt))
                    curGrammar.addEvent(evt);
                curGrammar.transition(evt);

//                System.out.println("\t" + curGrammar);


                needNewGrammar(evt);


                
                ct++;               
            }
        }
        catch(Exception e){
            throw new Exception(e);
        }
        
        //NStables.prettyPrint();
    }

    /**
     * Processes the event:
     * 1) set event codes
     * 2) add to string table
     * 3) add to grammar
     * 4) verify not a inner grammar transistion
     * 5) write event to file
     *
     * @param evt - event to process
     */
    private void processXMLinputEvent(EventAbstract evt) {
        evt.setTotalEventsInThisGrammar(curGrammar.getSize());

        curGrammar.setEventCode(evt);

        addToTables(evt);            

        if(canAddToGrammar(evt))
            curGrammar.addEvent(evt);

        /**
         * Do prints now before adding to the grammar/or after????
         *  not sure which version makes more intuitive sense
         * 
         * Event codes are determined based on the current grammar state
         * before the event is added to the grammar....
         *
         * But showing the grammar after event is added sort of helps see
         * the growing process...
         */
        doEncodePrints(evt);
        
        curGrammar.transition(evt);

        evt.writeEvent();
    }

    /**
     * prints the grammar and Event
     * @param evt
     */
    private void doEncodePrints(EventAbstract evt){
        if(verbose){
            System.out.println(evt);
            System.out.println("\t" + curGrammar.toString());
        }
    }

        /**
         * dont add prunable events to grammar they are 1 time events
         */
    private boolean canAddToGrammar(EventAbstract evt){
        EventType evtType = evt.getEventType();
        if (evtType.equals(EventType.PI))
            return false;
        if(evtType.equals(EventType.NS))
            return false;
        if (evtType.equals(EventType.CM))
            return false;
        if(evtType.equals(EventType.DT))
            return false;
        if(evtType.equals(EventType.ER))
            return false;
        if(evtType.equals(EventType.SD))
            return false;
        if(evtType.equals(EventType.ED))
            return false;
        return true;
    }



    /**
     * Add this event's contents to the string tables and set the events
     * string table paramaters
     * 
     * @param evt - event to add to tables
     */
    private void addToTables(EventAbstract evt) {
        if(evt.getEventType().equals(EventType.SE)){
            setCurrentDocumentLevel(getCurrentDocumentLevel() + 1);
            // cast to element event type
            EventStartElement element = (EventStartElement)evt;

             // get the qname of this elemen
            String localname = element.getLocalName();
            // get the correct table ... else it makes a new table (NS)
            Tables tables = NStables.getAssociatedTablesForNamespace(element.getUri());

            // for n-bit to determin how many bits
            element.setUriCount(NStables.getCountOfURI());
            // int id to be converted to n-bit EXI Uint
            element.setUriFound(NStables.getIDforURI(element.getUri()));
            element.setUriNbits(EXIConstants.howManyBits(NStables.getCountOfURI()+1));
            

            /*
             * if tables == null add the namespace
             */
            if(tables == null){
                //NStables.addPrefixAndUri("", element.getUri());
                NStables.addURI(element.getUri());
                tables = NStables.getAssociatedTablesForNamespace(element.getUri());
            }
            
            // for n-bit to determin how many bits
            element.setLocalCount(tables.getLocalNameCount());            
            // int id to be converted to n-bit EXI Uint
            element.setLocalFound(tables.getIdentifierForLocalName(localname));
            element.setLocalNbits(EXIConstants.howManyBits(tables.getLocalNameCount()));

            // add this element Qname to the list if not already there
            tables.addStringToLocalNameTable(localname, getCurrentDocumentLevel());
        }
        else if(evt.getEventType().equals(EventType.AT)){
            // cast to correct event type
            EventAttribute att = (EventAttribute)evt;
            // get the parent element of this attribute
            //String qName = att.getElementParent();
            // get the namespace table for this event
            Tables tables = NStables.getAssociatedTablesForNamespace(att.getUri());


            // for n-bit to determin how many bits
            att.setUriCount(NStables.getCountOfURI());
            // int id to be converted to n-bit EXI Uint
            att.setUriFound(NStables.getIDforURI(att.getUri()));
            att.setUriNbits(EXIConstants.howManyBits(NStables.getCountOfURI()+1));

            // for n-bit to determin how many bits
            att.setLocalCount(tables.getLocalNameCount());
            // int id to be converted to n-bit EXI Uint
            att.setLocalFound(tables.getIdentifierForLocalName(att.getLocalName()));
            att.setLocalNbits(EXIConstants.howManyBits(tables.getLocalNameCount()));

            att.setValueCount(tables.getValueCount(att.getLocalName()));
            att.setValueFound(tables.getIdentifierForLocalValue(att.getLocalName(), att.getValue()));
            att.setValueNbits(EXIConstants.howManyBits(tables.getValueCount(att.getLocalName())));

            att.setGlobalCount(tables.getGlobalCount());
            att.setGlobalFound(tables.getGlobalCompactIdentifierForString(att.getValue()));
            att.setGlobalNbits(EXIConstants.howManyBits(tables.getGlobalCount()));

            // add the quname to this table
            tables.addStringToLocalNameTable(att.getLocalName(), getCurrentDocumentLevel());
            // add the the values of this attribute to this table
            tables.addStringToLocalValue(att.getLocalName(), att.getValue());
        }
        else if(evt.getEventType().equals(EventType.CH)){
            EventCharacters chars = (EventCharacters)evt;
            String localname = chars.getEleQName();
            String eleURI = chars.getUri();

            Tables tables = NStables.getAssociatedTablesForNamespace(eleURI);

            int count = tables.getValueCount(localname);
            chars.setValueCount(count);
            chars.setValueFound(tables.getIdentifierForLocalValue(localname, chars.getValue()));
            chars.setValueNbits(EXIConstants.howManyBits(count));

            //System.out.println(localname + "  " + chars.getValue() + " @ " + chars.getValueFound() + " count=" + count);

            count = tables.getGlobalCount();
            chars.setGlobalCount(count);
            chars.setGlobalFound(tables.getGlobalCompactIdentifierForString(chars.getValue()));
            chars.setGlobalNbits(EXIConstants.howManyBits(count));
            
            tables.addStringToLocalValue(localname, chars.getValue());
        }
        else if(evt.getEventType().equals(EventType.NS)){
            EventNamespace ns = (EventNamespace)evt;

            // for n-bit to determin how many bits
            ns.setUriCount(NStables.getCountOfURI());
            // int id to be converted to n-bit EXI Uint
            ns.setUriFound(NStables.getIDforURI(ns.getUri()));
            ns.setUriNbits(EXIConstants.howManyBits(NStables.getCountOfURI()+1));

           // NStables.addPrefixToURI(ns.getPrefix(), ns.getUri());
            NStables.addPrefixAndUri(ns.getPrefix(), ns.getUri());
        }
        else if(evt.getEventType().equals(EventType.EE)){
            EventEndElement element = (EventEndElement)evt;
            // get the correct table ... else it makes a new table (NS)
            Tables tables = NStables.getAssociatedTablesForNamespace(element.getUri());
            tables.deleteLocalValues(getCurrentDocumentLevel());

            setCurrentDocumentLevel(getCurrentDocumentLevel() - 1);
        }
    }//addToTables(EventAbstract evt)

    /**
     * @return the currentDocumentLevel
     */
    public int getCurrentDocumentLevel() {
        return currentDocumentLevel;
    }

    /**
     * @param currentDocumentLevel the currentDocumentLevel to set
     */
    public void setCurrentDocumentLevel(int currentDocumentLevel) {
        this.currentDocumentLevel = currentDocumentLevel;
    }


}