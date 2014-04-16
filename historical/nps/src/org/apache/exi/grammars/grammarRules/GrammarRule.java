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

import org.apache.exi.grammars.events.*;


/**
 *  General interface for all Grammar types
 * @author SheldonAcess
 */
public interface GrammarRule {
    public void addEvent(EventAbstract addE);
    public EventAbstract getEvent(int index);

    public boolean containsEvent(EventAbstract evt);
    public void setEventCode(EventAbstract addE) ;//throws UnknownEvent;
    public String getGramType();
    public void transition(EventAbstract addE);

    public void setCurrentEventCodePart1(EventAbstract evt);
    public void setCurrentEventCodePart2(EventAbstract evt);
    public void setCurrentEventCodePart3(EventAbstract evt);

    public int getMaxEventCodePart1();
    public int getMaxEventCodePart2();
    public int getMaxEventCodePart3();
    //public EventAbstract getDefaultEvent();
    public EventAbstract getEventForCode(int part1, int part2, int part3);
}
