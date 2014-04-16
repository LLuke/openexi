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
public class EventDTD extends EventAbstract
{
    private String nameDTD;
    private String publicId;
    private String systemId;


    public EventDTD(String nm, String pid, String sid){
        super(nm, EventType.DT, "");
        this.nameDTD = nm;
        this.publicId = pid;
        this.systemId = sid;
    }

    public EventDTD() {
        super("", EventType.DT, "");
    }

    public EventDTD exiSetParamsNewObj(String nm, String pid, String sid){
        return new EventDTD(nm, pid, sid);
    }

    public String getNameDTD() {
        return nameDTD;
    }
    public String getPublicId() {
        return publicId;
    }
    public String getSystemId() {
        return systemId;
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
        String s = super.toString() + " | ???";

        return s;
    }

    /**
     * @param nameDTD the nameDTD to set
     */
    public void setNameDTD(String nameDTD) {
        this.nameDTD = nameDTD;
    }

    /**
     * @param publicId the publicId to set
     */
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    /**
     * @param systemId the systemId to set
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public void readEventContents(NamespaceTables nst, GrammarRuleSchemaLessAbstract curGram) {
        //System.out.println("reading DT");
    }
}