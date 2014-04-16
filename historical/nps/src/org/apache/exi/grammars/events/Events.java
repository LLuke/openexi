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
 * @author SheldonAcess
 */
public interface Events {


    public boolean equals(Events e);

    
    /**
     * hit or no hit litteral+n     UINT
     *
     * Index to hit                 Nbit
     *      URI
     *      LocalName
     *      Value
     *
     * Event Codes                  Nbit
     */
    public void writeEvent();
    public void readEventContents(NamespaceTables nst, GrammarRuleSchemaLessAbstract curGram) throws Exception;
}
