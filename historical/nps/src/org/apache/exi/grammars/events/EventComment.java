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
public class EventComment extends EventAbstract
{
    private String value;

    public String getValue() {
        return value;
    }

    public EventComment(String val){
        super("comment", EventType.CM, "");
        this.value = val;
    }

    public EventComment(){
        super("comment", EventType.CM, "");
    }

    public EventComment exiSetParamsNewObj(String val){
        return new EventComment(val);
    }

    @Override
    public void writeEvent() {
        super.writeEvent();
        getOutputstream().writeStringLiteral(getValue(), 0);
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

        s+= "\"" + getValue() + "\"";

        return s;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    public void readEventContents(NamespaceTables nst, GrammarRuleSchemaLessAbstract curGram) {
        //System.out.println("reading CM");
        int strLen = this.getInputstream().readUInt();
        String readValue = this.getInputstream().readStringLiteral(strLen);
        setValue(readValue);
    }


}
