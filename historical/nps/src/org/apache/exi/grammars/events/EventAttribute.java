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
 */
public class EventAttribute extends EventAbstract
{
    private String localName;
    private String qName;
    private String value;
    private String elementParent;

    public String getElementParent() {
        return elementParent;
    }
    public String getValue() {
        return value;
    }
    public String getLocalName() {
        return localName;
    }
    public String getQName() {
        return qName;
    }
    public EventAttribute(String uri, String localName, String qName, 
            String value, String eleName) {
        super(qName, EventType.AT, uri);
        this.localName = localName;
        this.qName = qName;
        this.value = value;
        this.elementParent = eleName;
    }

    public EventAttribute(){
        super("", EventType.AT, "");
    }

    public EventAttribute exiSetParamsNewObj(String uri, String localName,
            String qName, String value, String eleName){
        return new EventAttribute(uri, localName, qName, value, eleName);
    }


    public void readEventContents(NamespaceTables nst, GrammarRuleSchemaLessAbstract curGram) throws Exception {
        int uriIndex = 0;
        String readURI = "";
        String readLocalName = "";
        int localHit = 0;
        String prefix = "";
        String readValue = "";
        int hitMiss =  0;
        int strLen = 0;
        int index = 0;
        int howmany = 0;


        if(!isRepeatFind()){
            int maxNumNS = nst.getCountOfURI();
            int thisNS = getInputstream().readNbit(EXIConstants.howManyBits(maxNumNS));
            uriIndex = thisNS - 1;
            if(uriIndex == -1)
            {
                strLen = DataTypeUnSignInteger.readUnsignedInt(getInputstream());
                readURI = getInputstream().readStringLiteral(strLen);
            }// if(thisNS == 0)
            else
            {
                readURI = nst.getUriForID(uriIndex);
            }
            setUri(readURI);
            prefix = nst.getPrefixForURI(readURI);
            localHit = getInputstream().readUInt();
            if(localHit == 0)
            { // local hit
                Tables uriTables = nst.getAssociatedTablesForNamespace(readURI);
                index = getInputstream().readNbit(EXIConstants.howManyBits(uriTables.getLocalNameCount()));
                readLocalName = uriTables.getLocalNameForIndex(index);
            }
            else{ // miss
                readLocalName = this.getInputstream().readStringLiteral(localHit-1);
            }
            this.setLocalName(readLocalName);
            if(!prefix.equalsIgnoreCase(""))
            {
                setQName(prefix + ":" + readLocalName);
            }
            else
            {
                setQName(readLocalName);
            }
            setLocalName(readLocalName);
            setFullyQualifiedLongName(readLocalName);
        }
        else{
            readURI = this.getUri();
            readLocalName = this.getLocalName();
        }

        //////////////////////
        // VALUE PORTION
        /////////////////////
        hitMiss = this.getInputstream().readUInt();
        if(hitMiss == 0){// local hit
            // this uri table space
            Tables tables = nst.getAssociatedTablesForNamespace(readURI);
            // how many values are in this local name local values list n-bit
            howmany = tables.getValueCount(localName);
            // get the index to this attributes local value from the exi stream
            index = getInputstream().readNbit(EXIConstants.howManyBits(howmany));
            // set the value
            readValue = tables.getValueForLocal(localName, index);
//            readValue = "valLocalHit";
        }
        else if(hitMiss == 1){// global hit
            Tables tables = nst.getAssociatedTablesForNamespace(readURI);
            howmany = tables.getGlobalCount();
            index = getInputstream().readNbit(EXIConstants.howManyBits(howmany));
            readValue = tables.getGlobalValueForID(index);
//            readValue = "valGlobalHit";
        }
        else{// local and global miss
            readValue = getInputstream().readStringLiteral(hitMiss-2);
//            readValue = "valLocal&Globalmiss";
        }

        setValue(readValue);
    }







    
    @Override
    public void writeEvent() {       
        super.writeEvent();

        if(!isRepeatFind()){
            // URI hit/miss
            if(getUriFound() == EXIConstants.VALUE_NOT_YET_SET)
                getOutputstream().writeNbit(0, getUriNbits());
            else
                getOutputstream().writeNbit(getUriFound()+1, getUriNbits());

            // local(Qname) miss
            if(this.getLocalFound() == EXIConstants.VALUE_NOT_YET_SET){//
                getOutputstream().writeStringLiteral(getLocalName(), 1);
            }
            else{  //hit
                getOutputstream().writeUInt(0);
                getOutputstream().writeNbit(getLocalFound(), getLocalNbits());
            }
        }
  

        // value hit/miss
        // local and global miss
        if(getValueFound() == EXIConstants.VALUE_NOT_YET_SET && getGlobalFound() == EXIConstants.VALUE_NOT_YET_SET){
            //getOutputstream().writeUInt(getValue().length()+2);
            getOutputstream().writeStringLiteral(getValue(), 2);
        }
        // local miss global hit
        else if(getValueFound() == EXIConstants.VALUE_NOT_YET_SET && getGlobalFound() != EXIConstants.VALUE_NOT_YET_SET){
            getOutputstream().writeUInt(1);
            getOutputstream().writeNbit(getGlobalFound(), getGlobalNbits());
        }
        // local hit
        else{
            getOutputstream().writeUInt(0);
            getOutputstream().writeNbit(getValueFound(), getValueNbits());
        }        
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
            s += ":" + this.getUriNbits();//EXIConstants.howManyBits(getUriCount()+1);


            s += ", localName=";
                if(this.getLocalFound() == EXIConstants.VALUE_NOT_YET_SET){// miss
                    s+= "(miss)" + "\"" + getLocalName() + "\"+1";
                }
                else{  //hit
                    s+= "(hit)0"+ "@" + getLocalFound() + ":" + this.getLocalNbits();//EXIConstants.howManyBits(getLocalCount());
                }

            s += ", value=";
        }
        else{
            s += getLocalName() + " value=";
        }
        
        // value hit/miss
        // local and global miss
        if(getValueFound() == EXIConstants.VALUE_NOT_YET_SET && getGlobalFound() == EXIConstants.VALUE_NOT_YET_SET){
            s += "(missLocal&Global)" + "\"" + getValue() + "\"+2";
        }
        // local miss global hit
        else if(getValueFound() == EXIConstants.VALUE_NOT_YET_SET && getGlobalFound() != EXIConstants.VALUE_NOT_YET_SET){
            s += "(hitGlobal)1"+ "@" + getGlobalFound()+ ":" + this.getGlobalNbits();//EXIConstants.howManyBits(getGlobalCount());
        }
        // local hit
        else{
            s += "(hitLocal)0"+ "@" + getValueFound() + ":" + this.getValueNbits();//EXIConstants.howManyBits(getValueCount());
        }

        return s;
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
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @param elementParent the elementParent to set
     */
    public void setElementParent(String elementParent) {
        this.elementParent = elementParent;
    }

}