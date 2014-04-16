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

package org.apache.exi.core;

/**
 *
 * @author Sheldon L. Snyder<br>
 * @version 1.0.0<br>
 */
public class EXIConstants
{
    /** Value not set yet in event code, string table checks*/
    public static final int VALUE_NOT_YET_SET = -1;


    /**No limit to value.length or max entries into value table */
    public static final int UNBOUNDED = -1;

    /** Default block Size for compression*/
    public static final int DEFAULT_BLOCK_SIZE = 1000000;
    
    /** This string is too large value.length to be added to string table
    There are already the max entries possible in the string table*/
    public static final int STRING_VALUE_ENTER_TOO_MANY = -1;


	private static final char	WS_SPACE	= ' ';
	private static final char	WS_NL		= '\n';
	private static final char	WS_CR		= '\r';
	private static final char	WS_TAB		= '\t';

        /**
     * Strip off the prefix before loading into the localname and values of
     * the namespace table....it is implied  by the associated namespace table it
     * is loaded into
     * @param qName - the element or attribute to strip the namespace from
     * @return namespace prefix free string of the the input argument
     */
    public static String stripNameSpacePrefix(String qName)
    {
        int colon = qName.indexOf(":");
        if(colon == -1)
            return qName;
        return qName.substring(colon+1);
    }

	  /**
	   * Returns the local part of the given raw qname.
	   *
	   * @param qname raw qname input
	   *
	   * @return Local part of the name if prefixed, or the given name if not
	   */
	  public static String getLocalPart( String qname )
	  {
	    int index = qname.indexOf( ':' );

	    return (index < 0) ? qname : qname.substring(index + 1);
	  }

	  /**
	   * Returns the prefix part of the given raw qname.
	   *
	   * @param qname raw qname input
	   *
	   * @return Prefix of name or empty string if none there
	   */
	  public static String getPrefixPart(String qname)
	  {
	    int index = qname.indexOf(':');

	    return ( index >= 0 ) ? qname.substring(0, index) : "";
	  }

	public static int getLeadingWhitespaces( final char[] ch, int start, final int length )
	{
		final int end = start + length;
		int leadingWS = 0;

		while ( start < end && isWhiteSpace ( ch[start] ) )
		{
			start++;
			leadingWS++;
		}

		return leadingWS;
	}


	public static int getTrailingWhitespaces( final char[] ch, final int start, final int length )
	{
		int pos = start + length - 1;
		int trailingWS = 0;

		while ( pos >= start && isWhiteSpace ( ch[pos] ) )
		{
			pos--;
			trailingWS++;
		}

		return trailingWS;
	}

	public static boolean isWhiteSpaceOnly ( final char[] ch, int start, final int length )
	{
		if ( !isWhiteSpace ( ch[start] ) )
			return false;

		final int end = start + length;
		while ( ++start < end && isWhiteSpace ( ch[start] ) )
		{
		}

		return start == end;
	}

	public static boolean isWhiteSpaceOnly ( String chars )
	{
		if ( !isWhiteSpace ( chars.charAt ( 0 ) ) )
		{
			return false;
		}

		final int end = chars.length ( );
		int start = 1;
		while ( start < end && isWhiteSpace ( chars.charAt ( start++ ) ) )
		{
		}

		return start == end;
	}

	public static boolean isWhiteSpace ( char c )
	{
		return ( c == WS_SPACE || c == WS_NL || c == WS_CR || c == WS_TAB );
	}


    public static int howManyBits(int n){
        int howmany = 1;    

        if(n == 0)
            return 0;

        if (n < 0) {
            throw new IllegalArgumentException("must be a positive number to " +
                    "EXIConstants.howMany7BitBytes [" + n +"]");
        }

        return (int)Math.ceil(Math.log10(n)/Math.log10(2.0));
    }
	  
}
