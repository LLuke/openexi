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
import org.apache.exi.io.EXI_OutputStreamIface;
import org.apache.exi.io.EXI_abstractInput;


/**
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 *
 * Each grammar production has an event code, which is represented by a sequence
 * of one to three parts separated by periods ("."). Each part is an unsigned
 * integer. The following are examples of grammar productions with event codes
 * as they appear in this specification.
 */
public abstract class EventAbstract extends EventStringTableParameters implements Events
{
    /**
     * Event code part 1 for this event
     */
    private int eventCodePart1 = EXIConstants.VALUE_NOT_YET_SET;

    /**
     * The number of bits needed to code part 1 of the event code
     */
    private int numberBitsPart1 = EXIConstants.VALUE_NOT_YET_SET;
    /**
     * Event code part 2 for this event
     */
    private int eventCodePart2 = EXIConstants.VALUE_NOT_YET_SET;

    /**
     * The number of bits needed to code part 2 of the event code
     */
    private int numberBitsPart2 = EXIConstants.VALUE_NOT_YET_SET;

    /**
     * Event code part 3 for this event
     */
    private int eventCodePart3 = EXIConstants.VALUE_NOT_YET_SET;
    /**
     * how many events are in the grammar at the time of this event
     */

    /**
     * The number of bits needed to code part 3 of the event code
     */
    private int numberBitsPart3 = EXIConstants.VALUE_NOT_YET_SET;

    private int totalEventsInThisGrammar = 0; // for n-bit...this will be the count of events up to this event
    /**
     * output steam to write this event
     */
    private EXI_OutputStreamIface outputstream;

    /**
     * output steam to write this event
     */
    private EXI_abstractInput inputstream;
    /**
     * Event name
     */
    private String name;

    /**
     * Event type
     */
    private EventType eventType;

    /**
     * uri of this event
     */
    private String uri;
    /**
     * Event has occured within this grammar already
     */
    private boolean repeatFind = false;

    private String fullyQualifiedLongName;

    /**
     * if this is an EE and grammar has transition or not
     *  do you write just 
     *      />  not transitioned
     * or full
     *      </name> transistioned
     */
    private boolean gramTrans = false;


    public EventAbstract(String nm, EventType et, String u){
        name = nm;
        eventType = et;
        uri = u;
        fullyQualifiedLongName = name+uri;
    }


    public void setOS(EXI_OutputStreamIface os){setOutputstream(os);}
    
    public EventType getEventType() {        return eventType;    }
    public void setEventType(EventType eventType) {this.eventType = eventType; }

    public int getEventCodePart1() {        return eventCodePart1;    }
    public void setEventCodePart1(int level1) {        eventCodePart1 = level1;    }

    public int getEventCodePart2() {        return eventCodePart2;    }
    public void setEventCodePart2(int level2) {        eventCodePart2 = level2;    }

    public int getEventCodePart3() {        return eventCodePart3;    }
    public void setEventCodePart3(int level3) {        eventCodePart3 = level3;    }

    public String getName() {        return name;    }
    public void setName(String name) {        this.name = name;    }

    /**
     * Are these two events equal...not the same event, just equal
     *  same event cannot occur, but equal events can and will
     * notebook example has several notes, both are equal, but not the same note
     *
     * Covers the case
     *  <elem at1="v1" at2="v2">
     *      <elem/>
     *  </elem>
     *
     * the first elem is a startTag with (SE(name) and the second is SE(*)
     * both will have the their name value as elem, but will be of differnet
     * event types
     *
     * the name of the event is unique for each event type in each grammar
     *
     * @param e
     * @return
     */
    public boolean equals(Events e) {
        String eName = ((EventAbstract)e).getName();
        EventType eType = ((EventAbstract)e).getEventType();

        // same if the event name and type are the same
        if(this.getName().equalsIgnoreCase(eName) && this.getEventType().equals(eType))
            return true;
        return false;
    }

    /*
     *  EventName Notebook (EventType) [EventCode]
     */
    @Override
    public String toString(){
        String buff = getEventType() + "<" + getName() + "> [";        
        // event codes
        buff += getEventCodePart1() + ":" + getNumberBitsPart1();                        
        if(getNumberBitsPart2() > 0)
            buff+= "." + getEventCodePart2()  + ":" + getNumberBitsPart2();
        if(getNumberBitsPart3() > 0)
            buff+= "." + getEventCodePart3()  + ":" + getNumberBitsPart3();
        buff += "]";        
        return buff;
    }



    /**
     * A detailed string of the event with event code and content codes
     * as will be written to file...like the examples from the W3C
     * @return
     */
    //public abstract String toStringDetailed();

    /**
     * @return the outputstream
     */
    public // for n-bit...this will be the count of events up to this event
    EXI_OutputStreamIface getOutputstream() {
        return outputstream;
    }

    /**
     * @param outputstream the outputstream to set
     */
    public void setOutputstream(EXI_OutputStreamIface outputstream) {
        this.outputstream = outputstream;
    }

    public void writeEvent(){
        // event codes
        if(getNumberBitsPart1() > 0)
            getOutputstream().writeUInt(getEventCodePart1());
        if(getNumberBitsPart2() > 0)
            getOutputstream().writeUInt(getEventCodePart2());
        if(getNumberBitsPart3() > 0)
            getOutputstream().writeUInt(getEventCodePart3());
    }

    /**
     * @return the totalEventsInThisGrammar
     */
    public int getTotalEventsInThisGrammar() {
        return totalEventsInThisGrammar;
    }

    /**
     * @param totalEventsInThisGrammar the totalEventsInThisGrammar to set
     */
    public void setTotalEventsInThisGrammar(int totalEventsInThisGrammar) {
        this.totalEventsInThisGrammar = totalEventsInThisGrammar;
    }

    /**
     * @return the repeatFind
     */
    public boolean isRepeatFind() {
        return repeatFind;
    }

    /**
     * @param repeatFind the repeatFind to set
     */
    public void setRepeatFind(boolean repeatFind) {
        this.repeatFind = repeatFind;
    }

    /**
     * @return the numberBitsPart3
     */
    public int getNumberBitsPart3() {
        return numberBitsPart3;
    }

    /**
     * @param numberBitsPart3 the numberBitsPart3 to set
     */
    public void setNumberBitsPart3(int numberBitsPart3) {
        this.numberBitsPart3 = numberBitsPart3;
    }

    /**
     * @return the numberBitsPart2
     */
    public int getNumberBitsPart2() {
        return numberBitsPart2;
    }

    /**
     * @param numberBitsPart2 the numberBitsPart2 to set
     */
    public void setNumberBitsPart2(int numberBitsPart2) {
        this.numberBitsPart2 = numberBitsPart2;
    }

    /**
     * @return the numberBitsPart1
     */
    public int getNumberBitsPart1() {
        return numberBitsPart1;
    }

    /**
     * @param numberBitsPart1 the numberBitsPart1 to set
     */
    public void setNumberBitsPart1(int numberBitsPart1) {
        this.numberBitsPart1 = numberBitsPart1;
    }

    /**
     * @return the uri
     */
    public String getUri() {
        return uri;
    }

    /**
     * @param uri the uri to set
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * @return the fullyQualifiedName
     */
    public String getFullyQualifiedLongName() {
        return fullyQualifiedLongName;
    }

    /**
     * @param fullyQualifiedName the fullyQualifiedName to set
     */
    public void setFullyQualifiedLongName(String fullyQualifiedLongName) {
        this.fullyQualifiedLongName = fullyQualifiedLongName;
    }

    /**
     * @return the inputstream
     */
    public EXI_abstractInput getInputstream() {
        return inputstream;
    }

    /**
     * @param inputstream the inputstream to set
     */
    public void setInputstream(EXI_abstractInput inputstream) {
        this.inputstream = inputstream;
    }

    /**
     * @return the gramTrans
     */
    public boolean isGramTrans() {
        return gramTrans;
    }

    /**
     * @param gramTrans the gramTrans to set
     */
    public void setGramTrans(boolean gramTrans) {
        this.gramTrans = gramTrans;
    }
}


