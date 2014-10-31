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
package org.civilian.text;


import java.util.Locale;
import org.civilian.Application;
import org.civilian.Request;
import org.civilian.Response;
import org.civilian.text.msg.MsgBundle;
import org.civilian.type.TypeSerializer;
import org.civilian.type.lib.LocaleSerializer;
import org.civilian.util.Check;


/**
 * LocaleData provides localization support for a certain locale.<p>
 * It contains a MsgBundle for that locale and a serializer 
 * which can be used to format/parse objects like numbers and dates into/from 
 * locale dependent string representations.<p>
 * LocaleData objects are created and can be obtained from the {@link LocaleService}.<p>
 * {@link Request} and {@link Response} are both associated with a locale
 * and therefore provide LocaleData objects, initialized
 * from the request preferences.
 * @see Application#getLocaleService()
 * @see Request#getLocaleData()
 * @see Response#getLocaleData()
 */
public class LocaleData
{
	/**
	 * Creates a new LocaleData object which uses a {@link LocaleSerializer},
	 * created for the locale
	 * @param locale a locale 
	 * @param messages a MsgBundle
	 * @param cached argument passed to the {@link LocaleSerializer#LocaleSerializer(Locale, boolean) LocaleSerializer ctor}.
	 */
	public LocaleData(Locale locale, MsgBundle messages, boolean cached)
	{
		this(locale, messages, new LocaleSerializer(locale, cached));
	}


	/**
	 * Creates a new LocaleData object which uses a {@link LocaleSerializer},
	 * created for the locale
	 * @param locale a locale 
	 * @param cached argument passed to the {@link LocaleSerializer#LocaleSerializer(Locale, boolean) LocaleSerializer ctor}.
	 */
	public LocaleData(Locale locale, boolean cached)
	{
		this(locale, null, new LocaleSerializer(locale, cached));
	}
	

	/**
	 * Creates a new LocaleData object.
	 * @param locale a locale 
	 * @param messages a MsgBundle. Will be converted into an empty bundle if null
	 * @param serializer a TypeSerializer suitable for the locale. 
	 */
	public LocaleData(Locale locale, MsgBundle messages, TypeSerializer serializer)
	{
		locale_ 		= Check.notNull(locale, 	"locale");
		serializer_		= Check.notNull(serializer, "serializer");
		messages_		= messages != null ? messages : MsgBundle.empty(locale);
		localeString_	= locale.toString();
	}
	
	
	/**
	 * Returns the locale.
	 */
	public Locale getLocale()
	{
		return locale_;
	}
	
	
	/**
	 * Returns the TypeSerializer for the locale.
	 */
	public TypeSerializer getTypeSerializer()
	{
		return serializer_;
	}

	
	/**
	 * Returns the MsgBundle.
	 */
	public MsgBundle getMsgBundle()
	{
		return messages_;
	}

	
	/**
	 * Returns the data previously set by setData().
	 * @see #setData
	 */
	public Object getData()
	{
		return data_;
	}
	
	
	/**
	 * Associates arbitrary data with the LocaleData object.
	 */
	public void setData(Object data)
	{
		data_ = data;
	}

	
	/**
	 * Returns true iif the other object is a LocaleData for
	 * the same locale.
	 */
	@Override public boolean equals(Object other)
	{
		return (other instanceof LocaleData) && 
			localeString_.equals(((LocaleData)other).localeString_); 
	}

	
	/**
	 * Returns a hash code.
	 */
	@Override public int hashCode()
	{
		return localeString_.hashCode();
	}
	
	
	/**
	 * Returns the string representation of the Locale.
	 */
	@Override public String toString()
	{
		return localeString_;
	}

	
	private MsgBundle messages_;
	private Locale locale_;
	private TypeSerializer serializer_;
	private Object data_;
	private String localeString_;
}