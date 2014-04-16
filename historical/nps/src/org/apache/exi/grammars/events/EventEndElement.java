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

import org.apache.exi.grammars.grammarRules.GrammarRuleSchemaLessAbstract;
import org.apache.exi.stringTables.NamespaceTables;

/**
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 */
public class EventEndElement extends EventAbstract
{
    private String localName;
    private String qName;

    public String getLocalName() {
        return localName;
    }

    public String getQName() {
        return qName;
    }



    public EventEndElement(String uri, String localName, String qName, int lev)  {
        super(qName+"EE", EventType.EE, uri);
        this.localName = localName;
        this.qName = qName;        
    }

    public EventEndElement()  {
        super("", EventType.EE, "");
    }

    public EventEndElement exiSetParamsNewObj(String uri, String localName, String qName){
        return new EventEndElement(uri, localName, qName, -1);
    }


    @Override
    public void writeEvent() {
        super.writeEvent();
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
        String s = super.toString() + " | --";

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

    public void readEventContents(NamespaceTables nst, GrammarRuleSchemaLessAbstract curGram) {
        //System.out.println("reading EE");
        // do nothing based on grammar
    }


}
