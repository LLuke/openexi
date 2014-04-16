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
public class EventProcessingInstruction extends EventAbstract
{
    private String target;
    private String data;

    public String getData() {
        return data;
    }
    public String getTarget() {
        return target;
    }

    public EventProcessingInstruction(String target, String data){
        super(target, EventType.PI, "");
        this.target = target;
        this.data = data;
    }

    public EventProcessingInstruction(){
        super("", EventType.PI, "");
    }


    public EventProcessingInstruction exiSetParamsNewObj(String target, String data){
        return new EventProcessingInstruction(target, data);
    }


    public void writeEvent() {
        super.writeEvent();

        getOutputstream().writeStringLiteral(getTarget(), 0);
        getOutputstream().writeStringLiteral(getData(), 0);
    }

    /**
     * Prints the SD Event in the form of EventType<EventName>[n,m,c] | {Content:Nentries}
     * <P>
     * where (as applicable to the event):
     * <ul>
     *  <LI>=part1, m=part2, c=part3 </li>
     * <li>content is uri, localname, value...Nentires is the count of entries
     * for this content to solve for Nbit during bit aligned encoding
     * (disrgarded for Byte aligned)</li>
     * </ul>
     * @return detailed event string
     */
    @Override
    public String toString() {
        String s = super.toString() + " | ";
        
        s+= "\"" + getTarget() + "\"" + " " + "\"" + getData() + "\"";

        return s;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(String target) {
        this.target = target;
    }

    /**
     * @param data the data to set
     */
    public void setData(String data) {
        this.data = data;
    }

    public void readEventContents(NamespaceTables nst, GrammarRuleSchemaLessAbstract curGram) {
        //System.out.println("reading PI");
        int strLen = getInputstream().readUInt();
        String readTarget = this.getInputstream().readStringLiteral(strLen);
        strLen = getInputstream().readUInt();
        String readData = getInputstream().readStringLiteral(strLen);
        setData(readData);
        setTarget(readTarget);
    }


}
