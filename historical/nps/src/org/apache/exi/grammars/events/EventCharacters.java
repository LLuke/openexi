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
import org.apache.exi.grammars.grammarRules.GrammarRuleSchemaLessAbstract;
import org.apache.exi.stringTables.NamespaceTables;
import org.apache.exi.stringTables.Tables;


/**
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 */
public class EventCharacters extends EventAbstract
{
    private String eleQName;
    private String value;
    

    public String getEleQName() {
        return eleQName;
    }
    public String getValue() {
        return value;
    }


    
    public EventCharacters(String val, String eleQName, String uri) {
        super("characters", EventType.CH, uri);
        this.eleQName = eleQName;
        this.value = val;
    }

    public EventCharacters(){
        super("characters", EventType.CH, "");
    }

    public EventCharacters exiSetParamsNewObj(String val, String eleQName, String uri){
        return new EventCharacters(val, eleQName, uri);
    }

    public void readEventContents(NamespaceTables nst, GrammarRuleSchemaLessAbstract curGram) throws Exception {
//        System.out.println("reading CH");

        int hitMiss = getInputstream().readUInt();
        String readValue = "";       

        if(hitMiss == 0){// local hit
            Tables tables = nst.getAssociatedTablesForNamespace(getUri());
            hitMiss = getInputstream().readNbit(EXIConstants.howManyBits(tables.getLocalNameCount()));
            readValue = tables.getValueForLocal(getEleQName(), hitMiss);
        }
        else if(hitMiss == 1){ // global hit
            Tables tables = nst.getAssociatedTablesForNamespace(getUri());
            hitMiss = getInputstream().readNbit(EXIConstants.howManyBits(tables.getGlobalCount()));            
            readValue = tables.getGlobalValueForID(hitMiss);
        }
        else{// global and local miss
           readValue = getInputstream().readStringLiteral(hitMiss-2);
        }

        setValue(readValue);        
    }



    @Override
    public void writeEvent() {
        super.writeEvent();

        if(getValueFound() == EXIConstants.VALUE_NOT_YET_SET && getGlobalFound() == EXIConstants.VALUE_NOT_YET_SET){
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
        String s = super.toString() +" (" + this.getEleQName() + ")" + " | ";
        
        s += " value=";
            // value hit/miss
            // local and global miss
            if(getValueFound() == EXIConstants.VALUE_NOT_YET_SET && getGlobalFound() == EXIConstants.VALUE_NOT_YET_SET){
                s += "(missLocal&Global)" + "\"" + getValue() + "\"+2";
            }
            // local miss global hit
            else if(getValueFound() == EXIConstants.VALUE_NOT_YET_SET && getGlobalFound() != EXIConstants.VALUE_NOT_YET_SET){
                s += "(hitGlobal)1"+ "@" + getGlobalFound()+ ":" + this.getGlobalNbits();
            }
            // local hit
            else{
                s += "(hitLocal)0"+ "@" + getValueFound() + ":" + this.getValueNbits();
            }

        return s;
    }

    /**
     * @param eleQName the eleQName to set
     */
    public void setEleQName(String eleQName) {
        this.eleQName = eleQName;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }



}
