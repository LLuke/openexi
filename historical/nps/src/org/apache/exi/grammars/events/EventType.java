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

/**
 * EventCode codes in EXI. The event notation matches up with that
 * of the EXI standard, eg AT rather than ATTRIBUTE. The long name
 * is kept as an attribute.
 * 
 * @author DMcG
 */
public enum EventType
{
    //----FUNDAMENTAL TYPES----//
    /** Start Docuement type*/
    SD("Start Document"),
    /** End Docuement type*/
    ED("End Document"),
    /** Start Element type*/
    SE("Start Element"),
    /** End Element type*/
    EE("End Element"),
    /** Attribute type*/
    AT("Attribute"),
    /** Characters (element content) type*/
    CH("Characters"),
    
    //----PRUNABLE TYPES----//
    /** Namespace type*/
    NS("Namespace Declaration"),
    /** Comment type*/
    CM("Comment"),
    /** Processing Instruction type*/
    PI("Processing Instruction"),
    /** DTD type*/
    DT("DOCTYPE"),
    /** Entity type*/
    ER("Entity Reference");
    
    String eventDescription;

    
    EventType(String eventDescription)
    {
        this.eventDescription = eventDescription;
    }

    public String getEventDescription()
    {
        return eventDescription;
    }

    @Override
    public String toString() {
        return this.getEventDescription();
    }
}
