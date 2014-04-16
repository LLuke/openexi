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
package org.apache.exi.core.headerOptions;

/**
 * Specifies whether or not to preserve (all false initially as defaults)
 * 
 * EXI enables the ability to prune (remove) certain items from an XML document
 * that for all intended purposes do not impact the content of the document,
 * but only support its processing.  Often these items can be removed without
 * impacting any aspect of the XML file and for compactness considerations can
 * be stripped from the EXI stream.
 *
 * Note, the preserve cannot be used if the strict option is also used.  Also,
 * any items that are discarded during the creation of the EXI document from an
 * XML are lost and cannot be reconstructed when decoding (lossy).  This option
 * can normally be employed, but domain case specifics must be taken into
 * consideration.
 * 
 * @author SheldonAcess
 */
public enum HeaderPreserveRules
{
    /**
     * Retains any comments from within the document
     */
    PRESERVE_CM(false),
    /**
     * Retains any processing instructions within the document
     */
    PRESERVE_PI(false),
    /**
     * Retains any DTD within the document
     */
    PRESERVE_DTD_ENTITY(false),
    /**
     * Retains any namespace prefixes within the document
     */
    PRESERVE_NAMESPACE_PREFIX(false),
    /**
     * Lexical form of elements and attribute values preserved
     */
    PRESERVE_LEX(false);

   
    boolean preserved;

    public void setPreserved(boolean preserved) {
        this.preserved = preserved;
    }

    public boolean isPreserved() {
        return preserved;
    }



    
    HeaderPreserveRules(boolean fidoOpt)
    {
        preserved = fidoOpt;
    }

    @Override
    public String toString() {
        return "HeaderPreserveRules." + this.name() + " [" + isPreserved() + "]";
    }
}
