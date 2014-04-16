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


/**
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 */
public class EventNamespace extends EventAbstract
{
    private String prefix;
   


    public EventNamespace(String pre, String u){
        super(pre, EventType.NS, u);
        this.prefix = pre;
    }

    public EventNamespace() {
        super("", EventType.NS, "");
    }


    public EventNamespace exiSetParamsNewObj(String pre, String u){
        return new EventNamespace(pre, u);
    }

    public String getPrefix() {
        return prefix;
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
        s += "URI=";
        if(getUriFound() == EXIConstants.VALUE_NOT_YET_SET){
            s+= "(miss)0:" + getUriNbits() + " \"" + this.getUri() + "\"";
        }
        else{
            s += "(hit)" + (getUriFound()+1) + ":" + getUriNbits();
        }
        s += ", prefix=\"" + getPrefix() + "\", nsFlag=";
        
        if(getUriFound() == EXIConstants.VALUE_NOT_YET_SET)
            s += "0";
        else
            s += "1";


        return s;
    }

    @Override
    public void writeEvent() {
        super.writeEvent();

        // URI hit/miss location
        getOutputstream().writeNbit(getUriFound()+1, getUriNbits());
        
        if(getUriFound() == EXIConstants.VALUE_NOT_YET_SET){
            getOutputstream().writeStringLiteral(getUri(), 0);
        }
        
        getOutputstream().writeStringLiteral(getPrefix(), 0);
        /*
         * 4. EXI Streams
         *
Like XML, the namespace of a particular element may be specified by a namespace
declaration preceeding the element or a local namespace declaration following
the element name. When the namespace is specified by a local namespace
declaration, the local-element-ns flag of the associated NS event is set to
true and the prefix of the element is set to the prefix of that NS event. When
the namespace is specified by a previous namespace declaration, the
localelement-ns flag of all local NS events is false and the prefix of the
element is set according to the prefix component of the element qname. The
series of NS events associated with a particular element may include at most
one NS event with its local-element-ns flag set to true. The uri of a NS event
with its local-element-ns flag set to true MUST match the uri of the associated
         SE event.
         *
         */
        if(getUriFound() == EXIConstants.VALUE_NOT_YET_SET){
            getOutputstream().writeNbit(0,1); // local-element-ns flag...not sure but its requried CH4
        }
        else
            getOutputstream().writeNbit(1,1); // local-element-ns flag...not sure but its requried CH4

    }

    /**
     * @param prefix the prefix to set
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void readEventContents(NamespaceTables nst, GrammarRuleSchemaLessAbstract curGram) throws Exception {
        System.out.println("reading NS");
        String readUri = "";
        String readPre = "";
        int strLen = 0;
        boolean flag = false;
        int elementFlag = 1;
        
        int n = nst.getCountOfURI();
        int uriHitmiss = getInputstream().readNbit(n)-1;
        if(uriHitmiss == -1){
            strLen = getInputstream().readUInt();
            readUri = getInputstream().readStringLiteral(strLen);
            nst.addURI(readUri);
            flag = true;
        }
        else
            readUri = nst.getUriForID(uriHitmiss);

        strLen = getInputstream().readUInt();
        readPre = getInputstream().readStringLiteral(strLen);
        nst.addPrefixToURI(readPre, readUri);

        // 0 = uri not found...just created this element
        // 1 = already known uri
        // not sure what to do with this
        elementFlag = getInputstream().readNbit(1);
    }



}
