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

import org.apache.exi.core.EXIConstants;

/**
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 *
 * Block Size
 *      This defines the block size for compression.  The EXI compression
 *      technique works on blocks of data and this field defines the size of
 *      each block.  The EXI compression methodology is covered in more details
 *      in later sub sections of this chapter.
 *
 * Value Max Length
 *      This indicates the maximum size of a string that can be added to the
 *      value portion of the string.  If a string is larger (character length
 *      count) than this maximum length, it is not added to the string table,
 *      though it is written directly to the EXI stream as the string.  This
 *      ensures that one time long string values do not clog up a string table
 *      with one time occurrences such as a paragraph within an element.  This
 *      same paragraph will not likely be repeated exactly again within the XML
 *      document so adding it to the string table and indexing to it is would
 *      cost more (in terms of compactness) than writing it directly to the EXI
 *      stream.
 *
 * Value Partition Capacity
 *      Specifies the maximum total capacity (count) of the global and value
 *      contents of the string table.  If a new string is encountered in the
 *      XML document, it is only added to the string tables if there are less
 *      than Partition capacity items within the value and or global string
 *      portions existing.
 *
 */
public enum HeaderSizeLimitRules
{
    /**
     * The blocking size for EXI compression
     *      1,000,000 as default
     */
    BLOCK_SIZE(EXIConstants.DEFAULT_BLOCK_SIZE),
    /**
     * Largest string that can be added to the string table
     *      unbound as default
     */
    VALUE_MAX_LENG(EXIConstants.UNBOUNDED),
    /**
     * Maximum capacity of the VALUES portion in the string tables
     *      unbound as default
     */
    VALUE_PARTITION_CAP(EXIConstants.UNBOUNDED);


    int sizeLimit;

    public void setSizeLimit(int sizeLimit) {
        this.sizeLimit = sizeLimit;
    }
    public int getSizeLimit() {
        return sizeLimit;
    }


    HeaderSizeLimitRules(int szLimit)
    {
        sizeLimit = szLimit;
    }

    @Override
    public String toString() {
        String buff = "HeaderSizeLimitRules." + this.name() + " [";
        if(getSizeLimit() == -1)
            buff += "unbounded]";
        else
            buff += getSizeLimit() + "]";
        return buff;
    }
}
