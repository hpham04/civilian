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
package org.civilian.template.mixin;


import org.junit.Test;
import org.civilian.CivTest;
import org.civilian.response.TestResponseWriter;
import org.civilian.util.Date;


public class LocaleMixinTest extends CivTest
{
	@Test public void testFormat()
	{
		TestResponseWriter out = TestResponseWriter.create("ISO-8859-1");
		LangMixin locale = new LangMixin(out);
		
		// dates
		Date date = new Date(2012, 01, 31);
		assertEquals("01/31/2012", locale.format(date));
		assertEquals("01/31/2012", locale.format(date, "a"));
		assertEquals("01/31/2012", locale.format(date.toCalendar()));
		assertEquals("01/31/2012", locale.format(date.toJavaDate()));
		assertEquals("a", locale.format((Date)null, "a"));
		
		// int
		assertEquals("1,234", locale.format(1234));
		assertEquals("2,345", locale.format(new Integer(2345), "a"));
		assertEquals("a", locale.format((Integer)null, "a"));

		// long
		assertEquals("1,234", locale.format(1234L));
		assertEquals("2,345", locale.format(new Long(2345), "a"));
		assertEquals("a", locale.format((Long)null, "a"));

		// double
		assertEquals("1,234.50", locale.format(1234.5));
		assertEquals("2,345.60", locale.format(new Double(2345.6), "a"));
		assertEquals("a", locale.format((Double)null, "a"));
	}
	


	@Test public void testAccessors()
	{
		TestResponseWriter out = TestResponseWriter.create("ISO-8859-1");
		LangMixin locale = new LangMixin(out);

		assertEquals("?key", locale.msg("key"));
		assertEquals("?key", locale.msg("key", "param"));
	}
}
