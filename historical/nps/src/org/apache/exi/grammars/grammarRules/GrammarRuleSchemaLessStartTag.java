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
package org.apache.exi.grammars.grammarRules;

import org.apache.exi.core.EXIConstants;
import org.apache.exi.core.headerOptions.HeaderPreserveRules;
import org.apache.exi.grammars.events.*;

/**
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 * 
 *  StartTagContent:    n = 0 at start and incr by 1 for each event
 *      EE                      n.0
 *      AT (*) StartTagContent  n.1 -> 0.1 -> 1.1 -> 2.1
 *      NS StartTagContent      n.2
 *      SC Fragment             n.3   -- SE contained in this element
 *      ChildContentItems       n.4 -- CH of this element
 *

 *
 *
 *
 *  ElementContent:
 * 		EE					0       always written out even though 0
 *      ChildContentItems   1.0
 * 
 *  ChildContentItems (n.m) :
 * 		SE (*) 	ElementContent	n.m             1.0
 * 		CH 		ElementContent	n.(m+1)         1.1
 * 		ER 		ElementContent	n.(m+2)         1.2
 * 		CM 		ElementContent	n.(m+3).0       1.3.0
 * 		PI	 	ElementContent	n.(m+3).1       1.3.1
 *
 * 
 *
 * [Definition:] EXI defines a built-in element grammar that is used in the 
 *  absence of additional information about the contents of an EXI element
 *  prior to its processing. A built-in element grammar shown below is prescibed
 *  by EXI to reflect the events that will occur in an element and the order
 *  amongst them in general without any further constraint about what is likely
 *  or not likely to occur inside elements.
 *
 * A single instance of built-in element grammar is shared by those elements in
 *  a stream that have the same qname and do not have additional a priori
 *  constraints as to their content. A separate instance of built-in element
 *  grammar is assigned to each qname upon the first occurrence of the elements
 *  of the same qname, thereafter the grammar continuously evolves by reflecting
 *  the knowledge learned while processing the content of those elements. The
 *  grammar shown below represents the initial set of productions that belong to
 *  a built-in element grammar at the time when a new instance is created, which
 *  is supplemented by the semantic description that explains the rules that are
 *  applied by the grammar onto itself to evolve and be better prepared for
 *  subsequent uses of the same grammar instance during the rest of the
 * processing of the stream.
 *
 *
 *
 * All productions in the built-in Element grammar of the form
 *  LeftHandSide: AT (*) RightHandSide are evaluated as follows:
 *      1. Let qname be the qname of the attribute matched by AT (*)
 *      2. If qname is not xsi:type or xsi:nil, create a production of the
 *          form LeftHandSide : AT (qname) StartTagContent with an event
 *          code 0 and increment the first part of the event code of each
 *          production in the current grammar with the non-terminal
 *          LeftHandSide on the left hand side. Add this production
 *          to the grammar.
 *      3. If qname is xsi:type, let type-qname be the value of the
 *          xsi:type attribute, and if a grammar exists for the typeqname
 *          type, evaluate the remainder of event sequence using the grammar
 *          for type-qname type instead of RightHandSide. Otherwise,
 *          evaluate the remainder of event sequence using RightHandSide.
 *
 *  All productions of the form LeftHandSide : SC Fragment are
 *      evaluated as follows:
 *      1. Save the string table, grammars, namespace prefixes and any
 *          implementation-specific state learned while processing this
 *          EXI Body.
 *      2. Initialize the string table, grammars, namespace prefixes and
 *          any implementation-specific state learned while processing this
 *          EXI Body to the state they held just prior to processing this
 *          EXI Body.
 *      3. Skip to the next byte-aligned boundary in the stream.
 *      4. Let qname be the qname of the SE event immediately preceding
 *          this SC event.
 *      5. Let content be the sequence of events following this SC event
 *          that match the grammar for element qname, up to and including
 *          the terminating EE event.
 *      6. Evaluate the sequence of events (SD, SE(qname), content, ED)
 *          according to the Fragment grammar.
 *      7. Restore the string table, grammars, namespace prefixes and
 *          implementation-specific state learned while processing this
 *          EXI Body to that saved in step 1 above.
 *
 * All productions in the built-in Element grammars of the form
 *   LeftHandSide : SE (*) RightHandSide are evaluated as follows:
 *      1. Let qname be the qname of the element matched by SE (*)
 *      2. If a grammar does not exist for element qname, create one based
 *          on the Built-in Element Grammar
 *      3. Evaluate the element contents using a built-in grammar for
 *          element qname
 *      4. Create a production of the form LeftHandSide : SE (qname)
 *          RightHandSide with an event code 0
 *      5. Increment the first part of the event code of each production
 *          in the current grammar with the non-terminal LeftHandSide on
 *          the left hand side.
 *      6. Add the production created in step 4 to the grammar
 *      7. Evaluate the remainder of event sequence using RightHandSide.
 *
 * All productions of the form LeftHandSide : SE (qname) RightHandSide that
 *  were previously added to the grammar upon the first occurrence of the
 *  element that has the qname qname are evaluated as follows when they
 *  are matched:
 *      1. Evaluate the element contents using a built-in grammar for
 *          element qname
 *      2. Evaluate the remainder of event sequence using RightHandSide.
 *
 *  All productions in the built-in Element grammar of the form
 *    LeftHandSide : CH RightHandSide are evaluated as follows:
 *      1. If a production of the form, LeftHandSide : CH RightHandSide with
 *          an event code of length 1 does not exist in the current element
 *          grammar, create one with event code 0 and increment the first
 *          part of the event code of each production in the current grammar
 *          with the non-terminal LeftHandSide on the left hand side.
 *      2. Add the production created in step 1 to the grammar
 *      3. Evaluate the remainder of event sequence using RightHandSide.
 *
 */
public class GrammarRuleSchemaLessStartTag extends GrammarRuleSchemaLessAbstract {

    String start = "StartTagContent";
    String element = "ElementContent";
    String gramType = start;

    public GrammarRuleSchemaLessStartTag(String name, String uri, String local, String Qname, int lev) {
        super(name);
        // add EE to grammar...always there by default
        setTransitioned(true);
        EventEndElement EE = new EventEndElement(uri, local, Qname, lev);
        addEvent(EE);
        setTransitioned(false);
    }

    /**
     * gets the Grammar type...name of grammar at its current state
     * @return string of name
     */
    public String getGramType() {
        return gramType;
    }

    /**
     *  Determin if this event triggers the grammar to transition and
     * if so sets the transtion flag
     * @param addE - event to check
     */
    public void transition(EventAbstract addE) {
        EventType type = addE.getEventType();

        if (type.equals(EventType.SE)) {
            setTransitioned(true);
        } else if (type.equals(EventType.CH)) {
            setTransitioned(true);
        } else if (type.equals(EventType.CM)) {
            setTransitioned(true);
        } else if (type.equals(EventType.EE)) {
            setTransitioned(false);
        }
        // update the name accroding to transiton
        if (isTransitioned()) {
            gramType = element;
        } else {
            gramType = start;
        }
    }

    /**
     *
     *  StartTagContent:    n = 0 at start and incr by 1 for each event
     *      EE                      n.0
     *      AT (*) StartTagContent  n.1     -> 0.1 -> 1.1 -> 2.1
     *      NS StartTagContent      n.2
     *      SC Fragment             n.3     -- SE contained in this element
     *      ChildContentItems       n.4     -- CH of this element
     *
     ***************************************************************************
     * 
     *  ElementContent:
     * 		EE					0       always written out even though 0
     *      ChildContentItems   1.0
     *
     *  ChildContentItems (n.m) :
     * 		SE (*) 	ElementContent	n.m             1.0
     * 		CH 		ElementContent	n.(m+1)         1.1
     * 		ER 		ElementContent	n.(m+2)         1.2
     * 		CM 		ElementContent	n.(m+3).0       1.3.0
     * 		PI	 	ElementContent	n.(m+3).1       1.3.1
     *
     * @param addE - event to set codes
     */
    public void setEventCode(EventAbstract addE) {

        if (addE.getEventType().equals(EventType.CM)) {
            this.setTransitioned(true);
        }

        if (containsEvent(addE)) 
        {
            String name = addE.getFullyQualifiedLongName();
            if (isTransitioned()) {
                addE.setEventCodePart1(eventListOrderedTransitioned.indexOf(name));
                addE.setNumberBitsPart1(EXIConstants.howManyBits(eventListOrderedTransitioned.size()+2));
            } else {
                addE.setEventCodePart1(eventListOrderedNotTransitioned.indexOf(name));
                addE.setNumberBitsPart1(EXIConstants.howManyBits(eventListOrderedNotTransitioned.size()+1));
            }
            addE.setEventCodePart2(EXIConstants.VALUE_NOT_YET_SET);
            addE.setNumberBitsPart2(0);
            addE.setEventCodePart3(EXIConstants.VALUE_NOT_YET_SET);
            addE.setNumberBitsPart3(0);
        } 
        else
        {
            setCurrentEventCodePart1(addE);
            setCurrentEventCodePart2(addE);
            setCurrentEventCodePart3(addE);
        }
    }

    /**
     * Event code part 1 rules
     *
     * @param evt - event to set
     * @return results for part 1
     */
    public void setCurrentEventCodePart1(EventAbstract evt) {
        EventType type = evt.getEventType();
        evt.setEventCodePart1(getSize());
        evt.setNumberBitsPart1(EXIConstants.howManyBits(getSize()+1));
    }



    public EventAbstract getEventForCode(int part1, int part2, int part3) {
        int size = getSize();
        int count = 0;
        boolean repeat = false;
        EventAbstract theEvent = null;

        if(part1 < size && part1 != -1)
            repeat = true;

        if(isTransitioned()){
            // part1 has to be <= size
            if(part2 == EXIConstants.VALUE_NOT_YET_SET && part3 == EXIConstants.VALUE_NOT_YET_SET){
                theEvent = new EventEndElement();
            }
            else if(part2 == 0){
                theEvent = new EventStartElement();
            }
            else if(part2 == 1){
                theEvent = new EventCharacters();
            }
            else{
                if(HeaderPreserveRules.PRESERVE_CM.isPreserved())
                    count++;
                if(HeaderPreserveRules.PRESERVE_PI.isPreserved())
                    count++;
                if(HeaderPreserveRules.PRESERVE_DTD_ENTITY.isPreserved())
                    count++;
                if(count == 3){
                    if(part2 == 2){
                        theEvent = new EventEntity();
                    }
                    else if(part1 <= size && part2 == 3 && part3 == 0){
                        theEvent = new EventComment();
                    }
                    else
                        theEvent = new EventProcessingInstruction();
                }
                else if(count == 2){
                    if(HeaderPreserveRules.PRESERVE_DTD_ENTITY.isPreserved()){
                        if(part2 == 2)
                            theEvent = new EventEntity();
                        if(HeaderPreserveRules.PRESERVE_CM.isPreserved() && part2 == 3)
                            theEvent = new EventComment();
                        else
                            theEvent = new EventProcessingInstruction();
                    }
                    else{// this implies CM and PI
                        if(part2 == 2)
                            theEvent = new EventComment();
                        else
                            theEvent = new EventProcessingInstruction();
                    }
                }
                else{// count == 1
                    if(HeaderPreserveRules.PRESERVE_CM.isPreserved())
                        theEvent = new EventComment();
                    if(HeaderPreserveRules.PRESERVE_PI.isPreserved())
                        theEvent = new EventProcessingInstruction();
                    if(HeaderPreserveRules.PRESERVE_DTD_ENTITY.isPreserved())
                        theEvent = new EventEntity();
                }
            }
        }
        else{
            if(part2 == 0)
                theEvent = new EventEndElement();
            else if(part2 == 1)
                theEvent = new EventAttribute();
            else if( HeaderPreserveRules.PRESERVE_NAMESPACE_PREFIX.isPreserved()){
                if(part2 == 2)
                    theEvent = new EventNamespace();
                else if(part2 == 3)
                    theEvent = new EventStartElement();
                else
                    theEvent = new EventCharacters();
            }
            else{
                if(part2 == 2)
                    theEvent = new EventStartElement();
                else
                    theEvent = new EventCharacters();
            }
        }

        theEvent.setRepeatFind(repeat);
        return theEvent;
    }

    

    /**
     * Event code part 2 rules
     *
     * @param evt - event to set
     * @return results for part 2
     */
    public void setCurrentEventCodePart2(EventAbstract evt) {
        EventType type = evt.getEventType();

        if (isTransitioned()) {
            int count = 0;

            if(HeaderPreserveRules.PRESERVE_CM.isPreserved())
                count++;
            if(HeaderPreserveRules.PRESERVE_PI.isPreserved())
                count++;
            if(HeaderPreserveRules.PRESERVE_DTD_ENTITY.isPreserved())
                count++;

            // how many bits to encode event based on number of preservation options used
            if(count > 0)
                evt.setNumberBitsPart2(2);
            else
                evt.setNumberBitsPart2(1);
            
            if (type.equals(EventType.EE)) {
                evt.setEventCodePart2(EXIConstants.VALUE_NOT_YET_SET);
                evt.setNumberBitsPart2(0);
            }
            else if (type.equals(EventType.SE)) {
                evt.setEventCodePart2(0);
            }
            else if (type.equals(EventType.CH)) {
                evt.setEventCodePart2(1);
            }            
            else if (type.equals(EventType.ER)) {
                evt.setEventCodePart2(2);
            }
            else if (type.equals(EventType.CM)) {
                if(HeaderPreserveRules.PRESERVE_DTD_ENTITY.isPreserved()){
                    evt.setEventCodePart2(3);
                }
                else{
                    evt.setEventCodePart2(2);
                }
            }
            else if (type.equals(EventType.PI)) {
                if(count == 1)// the only additon
                    evt.setEventCodePart2(2);
                else // defaults to 3
                    evt.setEventCodePart2(3);
            } 
        }
        else {
            boolean ns = HeaderPreserveRules.PRESERVE_NAMESPACE_PREFIX.isPreserved();
            
            // how many bits to encode event based on whether or not namespaces are preserved
            if(ns)
                evt.setNumberBitsPart2(3);
            else
                evt.setNumberBitsPart2(2);

            if (type.equals(EventType.EE)) {
                evt.setEventCodePart2(0);
            }
            else if (type.equals(EventType.AT)) {
                evt.setEventCodePart2(1);
            }
            else if (type.equals(EventType.NS)) {
                evt.setEventCodePart2(2);
            }            
            else if (type.equals(EventType.SE) && !ns) {
                evt.setEventCodePart2(2);
            }
            else if (type.equals(EventType.SE)) {
                evt.setEventCodePart2(3);
            }
            else if ((type.equals(EventType.CH)) && !ns) {
                evt.setEventCodePart2(3);
            }
            else if (type.equals(EventType.CH)) {
                evt.setEventCodePart2(4);
            }
        }
    }

    /**
     * Event code part 3 rules
     *
     * @param evt - event to set
     * @return results for part 3
     */
    public void setCurrentEventCodePart3(EventAbstract evt) {
        EventType type = evt.getEventType();

        if (isTransitioned()) {
            int count = 0;
            if (HeaderPreserveRules.PRESERVE_CM.isPreserved()) {
                count++;
            }
            if (HeaderPreserveRules.PRESERVE_PI.isPreserved()) {
                count++;
            }
            if (HeaderPreserveRules.PRESERVE_DTD_ENTITY.isPreserved()) {
                count++;
            }

            if(count == 3)
                evt.setNumberBitsPart3(1);
            else
                evt.setNumberBitsPart3(0);

            // default and most likely
            evt.setEventCodePart3(EXIConstants.VALUE_NOT_YET_SET);


            if (type.equals(EventType.CM) && count == 3) {
                evt.setEventCodePart3(0);
            }
            if (type.equals(EventType.PI) && count == 3) {
                evt.setEventCodePart3(1);
            }
        }
        else {
            evt.setEventCodePart3(EXIConstants.VALUE_NOT_YET_SET);
            evt.setNumberBitsPart3(0);
        }

    // return result;
    }

    public int getMaxEventCodePart1() {
        return getSize();
    }

    public int getMaxEventCodePart2() {
        int returnResult = 0;

        if (isTransitioned()) {
            int count = 0;

            if(HeaderPreserveRules.PRESERVE_CM.isPreserved())
                count++;
            if(HeaderPreserveRules.PRESERVE_PI.isPreserved())
                count++;
            if(HeaderPreserveRules.PRESERVE_DTD_ENTITY.isPreserved())
                count++;

            if(count == 0)
                returnResult = 1;
            if(count == 1)
                returnResult = 2;
            if(count > 1)
                returnResult = 3;
        }
        else{
            boolean ns = HeaderPreserveRules.PRESERVE_NAMESPACE_PREFIX.isPreserved();
            if(ns)
                returnResult = 4;
            else
                returnResult = 3;
        }
        
        return returnResult;
    }

    public int getMaxEventCodePart3() {
        int returnResult = 0;

        if (isTransitioned()) {
            int count = 0;

            if(HeaderPreserveRules.PRESERVE_CM.isPreserved())
                count++;
            if(HeaderPreserveRules.PRESERVE_PI.isPreserved())
                count++;
            if(HeaderPreserveRules.PRESERVE_DTD_ENTITY.isPreserved())
                count++;

            if(count < 3)
                returnResult = 0;
            else
                returnResult = 1;
        }
        else{
            returnResult = 0;
        }

        return returnResult;
    }


}