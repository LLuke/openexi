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
package org.apache.exi.io;

/**
 *
 * @author SheldonAcess
 */
public interface EXI_OutputStreamIface {
    public void writeStringLiteral(String s, int plus);
    public void writeUInt(int i);
    public void writeNbit(int value, int n);
    public void cleanAndClose();
    public void defaultHeader90();
    /**
     * Add the other datat types here and implment in concrete class
     */
}
