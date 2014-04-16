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
 *
 * NOT IMPLEMENTED YET
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 */
public class GrammarRuleSchemaLessFragment extends GrammarRuleSchemaLessAbstract
{
    String gramType = "Fragment";

    public String getGramType() {
        return gramType;
    }

    public GrammarRuleSchemaLessFragment(String name){
        super(name);
    }

    public void addEvent(EventAbstract addE) {//throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setEventCode(EventAbstract addE) {//throws UnknownEvent {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void transition(EventAbstract addE) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCurrentEventCodePart1(EventAbstract evt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCurrentEventCodePart2(EventAbstract evt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setCurrentEventCodePart3(EventAbstract evt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getMaxEventCodePart1() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getMaxEventCodePart2() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getMaxEventCodePart3() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public EventAbstract getDefaultEvent() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public EventAbstract getEventForCode(int part1, int part2, int part3) {
        throw new UnsupportedOperationException("Not supported yet.");
    }









}
