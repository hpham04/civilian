/*
 * Copyright (C) 2014 Civilian Framework.
 *
 * Licensed under the Civilian License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.civilian-framework.org/license.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.civilian.content;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.civilian.internal.HeaderParser;
import org.civilian.util.Check;
import org.civilian.util.Iterators;


/**
 * ContentTypeList is a list of ContentTypes.
 */
public class ContentTypeList implements Iterable<ContentType>
{
	/**
	 * An empty ContentTypeList.
	 */
	public static final ContentTypeList EMPTY = new ContentTypeList(new ContentType[0]);

	
	/**
	 * A ContentTypeList containing {@link ContentType#ANY ContentType.ANY}.
	 */
	public static final ContentTypeList ANY = new ContentTypeList(ContentType.ANY);
	
	
	/**
	 * Parse a list of ContentTypes from a list of string definitions.
	 * http://www.w3.org/Protocols/HTTP/HTRQ_Headers.html#z3
	 * <pre><code>
	 * field  = Accept: entry *[ , entry ]
     * entry  = &lt;content type> *[ ; param ]
	 * param  = attr = float
	 * attr   = q | other ignored params
     * float  = &lt;ANSI-C floating point text representation>
     * </code></pre>
	 * @param comparator if not null, then the content types are sorted with the comparator
	 * @param definitions string representation of the content types. Each string may 
	 * 		contain a comma separated list of content type definitions.
	 * 		(Think multiple accept-headers). 
	 * @return the list. Any exceptions during parsing are catched. The first exception is stored
	 * 		within the ContentTypeList.
	 * @see #getParseException() 
 	 */
	public static ContentTypeList parse(Comparator<ContentType> comparator, String... definitions)
	{
		ArrayList<ContentType> types = new ArrayList<>();
		RuntimeException exception = null;
		if (definitions != null)
		{
			HeaderParser parser = new HeaderParser();
			for (String definition : definitions) 
			{
				if (definition != null)
				{
					parser.init(definition);
					exception = parse(parser, types);
				}
			}
		}
		if ((comparator != null) && (types.size() > 1))
			Collections.sort(types, comparator);

		return new ContentTypeList(exception, types);
	}

	
	public static ContentTypeList parse(String... definitions)
	{
		return parse(null, definitions);
	}
	
	
	private static RuntimeException parse(HeaderParser parser, ArrayList<ContentType> types)
	{
		try
		{
			parser.next();
			while(parser.getToken() == HeaderParser.Token.ITEM)
			{
				ContentType contentType = ContentType.getContentType(parser.item);
				while(parser.next() == HeaderParser.Token.PARAM)
					contentType = parseParam(contentType, parser.paramName, parser.paramValue); 
				types.add(contentType);
			}
			return null;
		}
		catch(RuntimeException e)
		{
			return e;
		}
	}

	
	private static ContentType parseParam(ContentType contentType, String param, String value)
	{
		if (param.equals("q"))
		{
			double quality = Double.parseDouble(value);
			if (quality <= 0.0)
				quality = 0.0;
			contentType = contentType.withQuality(quality);
		}
		return contentType;
	}
	
	
	/**
	 * Creates a ContentTypeList.
	 */
	public ContentTypeList(List<ContentType> types)
	{
		this(null, types);
	}

	
	/**
	 * Creates a ContentTypeList.
	 */
	private ContentTypeList(Exception parseException, List<ContentType> types)
	{
		this(parseException, types.toArray(new ContentType[types.size()]));
	}
	
	
	/**
	 * Creates a ContentTypeList.
	 */
	public ContentTypeList(ContentType... types)
	{
		this(null, types);
	}
	
	
	/**
	 * Creates a ContentTypeList.
	 */
	private ContentTypeList(Exception parseException, ContentType... types)
	{
		types_ = Check.notNull(types, "types");
		parseException_ = parseException;
	}

	
	/**
	 * Returns the size of the list.
	 */
	public int size()
	{
		return types_.length;
	}
	
	
	/**
	 * Returns if the content type is contained in the list.
	 */
	public boolean contains(ContentType contentType)
	{
		Check.notNull(contentType, "contentType");
		for (int i=0; i<types_.length; i++)
		{
			if (types_[i].equals(contentType, false))
				return true;
		}
		return false;
	}

	
	/**
	 * Returns the i-th content type.
	 */
	public ContentType get(int i)
	{
		return types_[i];
	}
	
	
	/**
	 * Returns if the given content type matches at least
	 * one content type in the list.
	 */
	public boolean matchesSome(ContentType type)
	{
		for (int i=0; i<types_.length; i++)
		{
			if (types_[i].matches(type))
				return true;
		}
		return false;
	}
	

	/**
	 * Returns if any content type in the given list matches at least
	 * one content type in this list.
	 */
	public boolean matchesSome(ContentTypeList list)
	{
		for (int i=0; i<list.types_.length; i++)
		{
			if (matchesSome(list.get(i)))
				return true;
		}
		return true;
	}
	
	
	/**
	 * Returns an iterator for the content types.
	 */
	@Override public Iterator<ContentType> iterator()
	{
		return Iterators.forValues(types_);
	}

	
	/**
	 * Returns a parse exception, if the list was created by {@link #parse(String...)}
	 * and a syntax error was detected.
	 */
	public Exception getParseException()
	{
		return parseException_;
	}
			
	
	/**
	 * Returns a string representation of the ContentTypes.
	 * It has the format of an Accept-Header.
	 */
	@Override public String toString()
	{
		StringBuilder s = new StringBuilder();
		for (int i=0; i<types_.length; i++)
		{
			if (i > 0)
				s.append(",");
			s.append(types_[i].toString());
		}
			
		return s.toString();
	}
	
	
	private final ContentType[] types_;
	private final Exception parseException_;
}
