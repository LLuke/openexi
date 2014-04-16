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

import java.util.Vector;
import org.apache.exi.grammars.events.*;

/**
 *Abstract definitions shared by all grammar types
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 *
 * The general scoped set of rules for all grammar objects
 */
public abstract class GrammarRuleSchemaLessAbstract implements GrammarRule
{
    String curGrammarName;
    boolean transitioned = false;

    /**
     * Container of grammar events that are not in transitioned grammar state
     */
    Vector<String>  eventListOrderedNotTransitioned;
    Vector<EventAbstract> eventEVTListOrderedNotTransitioned;
    /**
     * Container of grammar events that have transitioned inner grammar state
     */
    Vector<String>  eventListOrderedTransitioned;
    Vector<EventAbstract> eventEVTListOrderedTransitioned;


    /**
     * The basics for all grammars
     *
     * @param name - grammar name
     */
    public GrammarRuleSchemaLessAbstract(String name){
        curGrammarName = name;
        eventListOrderedNotTransitioned = new Vector<String>();
        eventListOrderedTransitioned = new Vector<String>();
        eventListOrderedNotTransitioned.clear();
        eventListOrderedTransitioned.clear();

        eventEVTListOrderedNotTransitioned = new Vector<EventAbstract>();
        eventEVTListOrderedTransitioned = new Vector<EventAbstract>();
        eventEVTListOrderedNotTransitioned.clear();
        eventEVTListOrderedTransitioned.clear();
    }


    /**
     * Did the grammar transition yet
     * @return transition boolean
     */
    public boolean isTransitioned() {
        return transitioned;
    }
    /**
     * Set by the child class to determin if transition has occured for that
     * particular grammar rule set
     * @param transitioned - state of transtion
     */
    public void setTransitioned(boolean transitioned) {
        this.transitioned = transitioned;
    }


    /**
     * The count of events in the grammar
     * @return int count
     */
    public int getSize(){
        if(isTransitioned())
            return eventListOrderedTransitioned.size();
        return eventListOrderedNotTransitioned.size();
    }


    /**
     * The name of the current grammar in use
     * @return name
     */
    public String getCurGrammarName() {
        return "Grammar_" + getGramType() + ":" + curGrammarName;
    }

    /**
     * the basic name of the grammar...that it is actualy named without
     * added formatting as is in getCurGrammarName()
     * @return
     */
    public String getNameGrammar(){
        return curGrammarName;
    }





    /**
     * Is this event already been procesed...is this a repeat event in this
     * grammar
     *
     * If the Grammar already has this event in its list, then its is
     * encoded with this events current location within the list and not
     * an event code n.m.c
     *
     * @param evt event to check
     * @return boolean if this event is contained
     */
    public boolean containsEvent(EventAbstract evt) {
        String name = evt.getFullyQualifiedLongName();
        boolean results = false;

        if(!isTransitioned())
            results = eventListOrderedNotTransitioned.contains(name);
        else
            results = eventListOrderedTransitioned.contains(name);
                
        if(results)
            evt.setRepeatFind(true);
        else
            evt.setRepeatFind(false);

        return results;
    }





    /**
     * Add this event to this grammars event lists
     *  insert at head of list, not the tail...?
     *
     * not sure why they head, but that is what the spec shows
     *
     * 
     * @param addE - event to add
     */
    @Override
    public void addEvent(EventAbstract addE){
        String toAdd = addE.getFullyQualifiedLongName();
        if(!containsEvent(addE)  )
        {
            if(!transitioned){
                eventListOrderedNotTransitioned.insertElementAt(toAdd, 0);
                eventEVTListOrderedNotTransitioned.insertElementAt(addE, 0);
            }
            else{
                eventListOrderedTransitioned.insertElementAt(toAdd, 0);
                eventEVTListOrderedTransitioned.insertElementAt(addE, 0);
            }
        }// if(!contained)
    }

    /**
     * Gets teh event in the grammar at this index
     * @param index
     * @return
     */
    public EventAbstract getEvent(int index){
        EventAbstract result = null;
        if(!transitioned)
            result = eventEVTListOrderedNotTransitioned.get(index);
        else
            result = eventEVTListOrderedTransitioned.get(index);
        return result;
    }



    /**
     * Pretty print this grammar
     *  Grammar_GramarState:GrammarName [state1 content | state2 content]
     *
     * Grammar_ElementContent:notebook [note, date  |  note]
     * 
     * @return string of grammar
     */
    @Override
    public String toString(){
        String buff = getCurGrammarName();
        buff += "(trans=" + transitioned + ") [";

        int startSize = eventListOrderedNotTransitioned.size();
        int elementSize = eventListOrderedTransitioned.size();

        if(startSize < 0)
            startSize = 0;
        if(elementSize < 0)
            elementSize = 0;

        for(int i = 0; i < startSize-1; i++)
            buff += eventListOrderedNotTransitioned.elementAt(i).toString() + ", ";
        if(startSize > 0)
            buff += eventListOrderedNotTransitioned.elementAt(startSize-1);

        buff += "  |  ";

        for(int i = 0; i < elementSize-1; i++) 
            buff += eventListOrderedTransitioned.elementAt(i) + ", ";

        if(elementSize > 0)
            buff += eventListOrderedTransitioned.elementAt(elementSize-1);
        
        buff += "]";
        
        return buff;
    }
    
}
