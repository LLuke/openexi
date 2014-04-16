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
package org.apache.exi.grammars.events;

import org.apache.exi.core.EXIConstants;
import org.apache.exi.datatypes.DataTypeUnSignInteger;
import org.apache.exi.grammars.grammarRules.GrammarRuleSchemaLessAbstract;
import org.apache.exi.stringTables.NamespaceTables;
import org.apache.exi.stringTables.Tables;

/**
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 *
 * The Grammar will set the event codes for this event based on the grammar
 * rules in place at time of event firing
 */
public class EventStartElement extends EventAbstract
{
    private String localName;
    private String qName;

    private boolean startDocGrammar = false;
    private int level;

    public String getLocalName() {
        return localName;
    }

    public String getQName() {
        return qName;
    }

    
    public EventStartElement(String uri, String localName, String Qn, int lev) {
        super(Qn, EventType.SE, uri);
        this.localName = localName;
        this.qName = Qn;
        this.setLevel(lev);
    }

    public EventStartElement() {
        super("", EventType.SE, "");
    }

    public EventStartElement exiSetParamsNewObj(String uri, String localName, String Qn){
        return new EventStartElement(uri, localName, Qn,-1);
    }



    /**
     * @return the startDocGrammar
     */
    public boolean isStartDocGrammar() {
        return startDocGrammar;
    }

    /**
     * @param startDocGrammar the startDocGrammar to set
     */
    public void setStartDocGrammar(boolean startDocGrammar) {
        this.startDocGrammar = startDocGrammar;
    }

    

    /**
     * Prints the SD Event in the form of EventType<EventName>[n,m,c] | {Content:Nbits}
     * <P>
     * where (as applicable to the event):
     * <ul>
     *  <LI>=part1, m=part2, c=part3 </li>
     * <li>content is uri, localname, value...Nbits is the number of bits to
     * encode the hit location (disrgarded for Byte aligned)</li>
     * </ul>
     * @return detailed event string
     */
    @Override
    public String toString() {
        String s = super.toString() + " | ";

        if(!isRepeatFind()){
            s += "URI=";
            if(getUriFound() == EXIConstants.VALUE_NOT_YET_SET)
                s+= "0";
            else
                s += (getUriFound()+1);
            s += ":" + this.getUriNbits();


            // uri miss, write the uri...namespace event will write prefix
            if(getUriFound()==EXIConstants.VALUE_NOT_YET_SET){
                s+= ", uriStr=\"" + getUri() + "\"";
            }

            
            s += ", localName=";
                if(this.getLocalFound() == EXIConstants.VALUE_NOT_YET_SET){// miss
                    s+= "(miss)" + "\"" + getLocalName() + "\"+1";
                }
                else{  //hit
                    s+= "(hit)0"+ "@" + getLocalFound() + ":" + this.getLocalNbits();//EXIConstants.howManyBits(getLocalCount());
                }
        }
        else{
            s += ", localName=" + getLocalName();
        }


        return s;
    }



    @Override
    public void writeEvent() {
        super.writeEvent();

        // URI hit/miss
        if(!isRepeatFind())
        {
            // URI hit/miss
            if(getUriFound() == EXIConstants.VALUE_NOT_YET_SET)
                getOutputstream().writeNbit(0, getUriNbits());
            else
                getOutputstream().writeNbit(getUriFound()+1, getUriNbits());

            // uri miss, write the uri...namespace event will write prefix
            if(getUriFound()==EXIConstants.VALUE_NOT_YET_SET){
                getOutputstream().writeStringLiteral(this.getUri(), 0);
            }

            // local(Qname) miss
            if(this.getLocalFound() == EXIConstants.VALUE_NOT_YET_SET){
                getOutputstream().writeStringLiteral(getLocalName(), 1);
            }
            else{  //hit
                getOutputstream().writeUInt(0);
                getOutputstream().writeNbit(getLocalFound(), this.getLocalNbits());
            }
        }
    }


    public void readEventContents(NamespaceTables nst, GrammarRuleSchemaLessAbstract curGram) throws Exception {
        int uriIndex = 0;
        String readURI = "";
        String readLocalName = "";
        int localHit = 0;
        String prefix = "";
        
        try{
            if(!isRepeatFind()){
                int maxNumNS = nst.getCountOfURI();
                int thisNS = getInputstream().readNbit(EXIConstants.howManyBits(maxNumNS));
                uriIndex = thisNS - 1;
                if(uriIndex == -1){
                    int strLen = DataTypeUnSignInteger.readUnsignedInt(getInputstream());
                    readURI = getInputstream().readStringLiteral(strLen);
                }// if(thisNS == 0)
                else{
                    readURI = nst.getUriForID(uriIndex);
                }

                this.setUri(readURI);
                prefix = nst.getPrefixForURI(readURI);

                localHit = this.getInputstream().readUInt();

                if(localHit == 0){ // local hit
                    Tables uriTables = nst.getAssociatedTablesForNamespace(readURI);
                    int index = getInputstream().readNbit(EXIConstants.howManyBits(uriTables.getLocalNameCount()));
                    readLocalName = uriTables.getLocalNameForIndex(index);
                }
                else{ // miss
                    readLocalName = this.getInputstream().readStringLiteral(localHit-1);
                }
            }
            else{
                // get the grammar entry for this event local and qname
                readURI = this.getUri();
                readLocalName = this.getLocalName();
            }


            setLocalName(readLocalName);
            setFullyQualifiedLongName(readLocalName + readURI);
            if(!prefix.equalsIgnoreCase(""))
                setQName(prefix + ":" + readLocalName);
            else
                setQName(readLocalName);
    
        }// try
        catch(Exception e){
            throw new Exception ("ERROR in readEventContent (SE)\n" + e);
        }
    }




    /**
     * @param localName the localName to set
     */
    public void setLocalName(String localName) {
        this.localName = localName;
    }

    /**
     * @param qName the qName to set
     */
    public void setQName(String qName) {
        this.qName = qName;
    }


    /**
     * @return the level
     */
    public int getLevel() {
        return level;
    }

    /**
     * @param level the level to set
     */
    public void setLevel(int level) {
        this.level = level;
    }
}