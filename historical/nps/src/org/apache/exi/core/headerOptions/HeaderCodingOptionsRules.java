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
 *
 *
 * @author SheldonAcess
 */
public enum HeaderCodingOptionsRules
{
    /**
 *      As the title of the option suggest, this option controls the alignment
 *      of the body of the EXI stream.  The available values are: bit-packed,
 *      byte-aligned or pre-compressed, with bit-packed as the default.
 *      Byte-aligned is required for many domain cases, and is also a good
 *      option for troubleshooting.
     *
     *  false = bit packed, true = byte packed
     *
     * DEFAULT is false, but have it set to true for trouble shooting
     */
	BIT_BYTE_PACKED(true),
    /**
 *      Normally false, but when true, this option strips Namespace, Comments,
 *      processing instructions and self contained events from the stream
 *      relying strictly on the supplied schema format.  Through the schema,
 *      all striped (but not comments) can be reconstructed based on the schema
 *      and as such do not need to be preserved in the EXI stream.  This
 *      provides for a more compact stream.
     *
     * Default is false, requires a schema to be set to true
     */
    STRICT(false),
    /**
 *      Indicates whether or not the stream is a document or a fragment. A
 *      fragment is a sequence of elements or processing instructions that
 *      exist separate form an XML document.  Likely not to have an XML header
 *      <?xml version="1.0" encoding="UTF-8"?> for example.
     *
     * Default is false
     */
    FRAGMENT(false),
    /**
     * If this option is exercised, the event codes and content of the body of
     * the EXI stream are compressed using EXI compression technique.
     *
     * Default is false
     */
	COMPRESSION(false),
    /**
     * This option enables faster indexing though elements that are read
     * independently of the rest of the EXI body.  The self contained cannot
     * be used if compression, or pre-compression options are used.
     *
     * default is false
     */
    SELF_CONTAINED(false),
    /**
     * Was a schema used to generate this exi stream
     *
     * default is false
     */
    SCHEMA_INFORMED(false);



	public boolean codingOptionValue;
    

    public void setCodingOptionValue(boolean codingOptionValue) {
        this.codingOptionValue = codingOptionValue;
    }
    public boolean isCodingOptionValue() {
        return codingOptionValue;
    }

	HeaderCodingOptionsRules(boolean isSet)
	{
		codingOptionValue = isSet;
	}

    @Override
    public String toString() {
        return "HeaderCodingOptionsRules."+this.name() + " [" + isCodingOptionValue() + "]";
    }

}
