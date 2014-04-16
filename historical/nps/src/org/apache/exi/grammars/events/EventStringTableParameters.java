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

/**
 *  Defines the parameters of the events hit within the string table
 *      index to uri, local, global and value tables
 *      size (count of entries) in each table
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 */
public class EventStringTableParameters 
{
    // Global only found = 1as a byte, index n-bit of hit
    // if no global hit (= no value hit) length+2 "litteral"
    private int uriCount = EXIConstants.VALUE_NOT_YET_SET;
    // 0 = not found uri
    // if uri is found then its ID = string index + 1
    private int uriFound = EXIConstants.VALUE_NOT_YET_SET;
    private int uriNbits = EXIConstants.VALUE_NOT_YET_SET;
    
    private int localCount = EXIConstants.VALUE_NOT_YET_SET;
    private int localFound = EXIConstants.VALUE_NOT_YET_SET;
    private int localNbits = EXIConstants.VALUE_NOT_YET_SET;

    private int valueCount = EXIConstants.VALUE_NOT_YET_SET;
    private int valueFound = EXIConstants.VALUE_NOT_YET_SET;
    private int valueNbits = EXIConstants.VALUE_NOT_YET_SET;


    // total number of entries in
    private int globalCount = EXIConstants.VALUE_NOT_YET_SET;
    // Global only found = 1 as a byte, index n-bit of hit
    // if no global hit (= no value hit) length+2 "litteral"
    private int globalFound = EXIConstants.VALUE_NOT_YET_SET;
    private int globalNbits = EXIConstants.VALUE_NOT_YET_SET;


    @Override
    public String toString(){
        String s = "Event StringTable Parameters:";
        s += "\n\tURI";
        if (getUriFound() > 0)
            s += " found ";
        else
            s += " NOT found ";
        s += " [uriSize = " + getUriCount() + "]";

        s += "\n\tLocal";
        if (getLocalFound() > EXIConstants.VALUE_NOT_YET_SET)
            s += " found ";
        else
            s += " NOT found ";
        s += " [localSize = " + getLocalCount() + "]";

        s += "\n\tvalue";
        if (getValueFound() > EXIConstants.VALUE_NOT_YET_SET)
            s += " found ";
        else
            s += " NOT found ";
        s += " [valueSize = " + getValueCount() + "]";

        s += "\n\tglobal";
        if (getGlobalFound() > EXIConstants.VALUE_NOT_YET_SET)
            s += " found ";
        else
            s += " NOT found ";
        s += " [globalSize = " + getGlobalCount() + "]";

        return s;
    }

    /**
     * @return the uriCount
     */
    public int getUriCount() {
        return uriCount;
    }

    /**
     * @param uriCount the uriCount to set
     */
    public void setUriCount(int uriCount) {
        this.uriCount = uriCount;
    }

    /**
     * @return the uriFound
     */
    public int getUriFound() {
        return uriFound;
    }

    /**
     * @param uriFound the uriFound to set
     */
    public void setUriFound(int uriFound) {
        this.uriFound = uriFound;
    }

    /**
     * @return the localCound
     */
    public int getLocalCount() {
        return localCount;
    }

    /**
     * @param localCound the localCound to set
     */
    public void setLocalCount(int localCount) {
        this.localCount = localCount;
    }

    /**
     * @return the localFound
     */
    public int getLocalFound() {
        return localFound;
    }

    /**
     * @param localFound the localFound to set
     */
    public void setLocalFound(int localFound) {
        this.localFound = localFound;
    }

    /**
     * @return the valueCount
     */
    public int getValueCount() {
        return valueCount;
    }

    /**
     * @param valueCount the valueCount to set
     */
    public void setValueCount(int valueCount) {
        this.valueCount = valueCount;
    }

    /**
     * @return the valueFound
     */
    public int getValueFound() {
        return valueFound;
    }

    /**
     * @param valueFound the valueFound to set
     */
    public void setValueFound(int valueFound) {
        this.valueFound = valueFound;
    }

    /**
     * @return the globalCount
     */
    public int getGlobalCount() {
        return globalCount;
    }

    /**
     * @param globalCount the globalCount to set
     */
    public void setGlobalCount(int globalCount) {
        this.globalCount = globalCount;
    }

    /**
     * @return the globalFound
     */
    public int getGlobalFound() {
        return globalFound;
    }

    /**
     * @param globalFound the globalFound to set
     */
    public void setGlobalFound(int globalFound) {
        this.globalFound = globalFound;
    }

    /**
     * @return the uriNbits
     */
    public int getUriNbits() {
        return uriNbits;
    }

    /**
     * @param uriNbits the uriNbits to set
     */
    public void setUriNbits(int uriNbits) {
        this.uriNbits = uriNbits;
    }

    /**
     * @return the localNbits
     */
    public int getLocalNbits() {
        return localNbits;
    }

    /**
     * @param localNbits the localNbits to set
     */
    public void setLocalNbits(int localNbits) {
        this.localNbits = localNbits;
    }

    /**
     * @return the valueNbits
     */
    public int getValueNbits() {
        return valueNbits;
    }

    /**
     * @param valueNbits the valueNbits to set
     */
    public void setValueNbits(int valueNbits) {
        this.valueNbits = valueNbits;
    }

    /**
     * @return the globalNbits
     */
    public int getGlobalNbits() {
        return globalNbits;
    }

    /**
     * @param globalNbits the globalNbits to set
     */
    public void setGlobalNbits(int globalNbits) {
        this.globalNbits = globalNbits;
    }
}