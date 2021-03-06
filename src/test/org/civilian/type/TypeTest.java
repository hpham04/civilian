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
package org.civilian.type;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.civilian.CivTest;
import org.civilian.type.lib.ArrayType;
import org.civilian.type.lib.DiscreteType;
import org.civilian.type.lib.EnumType;
import org.civilian.type.lib.InvalidType;
import org.civilian.type.lib.StandardSerializer;
import org.junit.Test;


public class TypeTest extends CivTest
{
	@Test public void testBasics()
	{
		TypeLib lib = new TypeLib();
		for (Type<?> type : lib)
			testType(type);
	}
	
	
	private <T> void testType(Type<T> type)
	{
		Class<T> ptype = type.getJavaPrimitiveType();

		if (ptype != null)
			assertSame(TypeLib.getObjectClassForPrimitiveType(ptype), type.getJavaType());
		
		assertTrue(type.isSimpleType());
		
		assertEquals("", type.format(StandardSerializer.INSTANCE, null));
	}


	@Test public void testArrayType() throws Exception
	{
		String[] empty = {};
		
		ArrayType<String> type = new ArrayType<>(TypeLib.STRING);
		
		assertFalse(type.isSimpleType());
		assertSame(TypeLib.STRING, type.getElementType());
		assertArrayEquals(new String[0][], type.createArray(0));
		assertNull(type.getJavaPrimitiveType());
		assertEquals(empty.getClass(), type.getJavaType());
		assertNull(type.parseList(null, (String[])null));
		
		ArrayType<Integer> intArrayType = new ArrayType<>(TypeLib.INTEGER);
		Integer p[] = intArrayType.parseList(StandardSerializer.INSTANCE,  "1", "2");
		assertEquals(2, p.length);
		assertEquals(1, p[0].intValue());
		assertEquals(2, p[1].intValue());
		
		assertNull(type.format(StandardSerializer.INSTANCE, null));
		assertEquals("a", type.format(StandardSerializer.INSTANCE, new String[] { "a" }));
		assertEquals("a,b", type.format(StandardSerializer.INSTANCE, new String[] { "a", "b" }));

		assertNull(type.parse(StandardSerializer.INSTANCE, null));
		assertArrayEquals2(type.parse(StandardSerializer.INSTANCE, "a"), "a");
		assertArrayEquals2(type.parse(StandardSerializer.INSTANCE, "a,b"), "a", "b");
	}
	
	
	@Test public void testDiscreteType() throws Exception
	{
		DiscreteType<String> type = new DiscreteType<>(TypeLib.STRING, "a", "b", "c");
		
		assertEquals("", type.format(StandardSerializer.INSTANCE, null));
		assertEquals("a", type.format(StandardSerializer.INSTANCE, "a"));
		
		assertEquals(String.class, type.getJavaType()); 
		
		assertNull(type.parse(StandardSerializer.INSTANCE, null));
		assertEquals("a", type.parse(StandardSerializer.INSTANCE, "a"));
		
		try
		{
			assertNull(type.parse(StandardSerializer.INSTANCE, "d"));
			fail();
		}
		catch(IllegalArgumentException e)
		{
		}
	}

	
	private enum TestEnum
	{
		alpha,
		beta
	}
	

	@Test public void testEnumType() throws Exception
	{
		EnumType<TestEnum> type = new EnumType<>(TestEnum.class);
		
		assertEquals("", type.format(StandardSerializer.INSTANCE, null));
		assertEquals("alpha", type.format(StandardSerializer.INSTANCE, TestEnum.alpha));
		
		assertEquals(null, type.parse(StandardSerializer.INSTANCE, null));
		assertEquals(TestEnum.beta, type.parse(StandardSerializer.INSTANCE, "beta"));
		try
		{
			assertNull(type.parse(StandardSerializer.INSTANCE, "gamma"));
			fail();
		}
		catch(IllegalArgumentException e)
		{
		}
		
		assertSame(TestEnum.class, type.getJavaType());
	}


	@Test public void testInvalid() throws Exception
	{
		InvalidType<String> type = InvalidType.<String>instance();
		
		for (Method m : type.getClass().getDeclaredMethods())
		{
			if (!Modifier.isStatic(m.getModifiers()))
			{
				Object[] dummyArgs = new Object[m.getParameterTypes().length];
				try
				{
					m.invoke(type, dummyArgs);
					fail();
				}
				catch(InvocationTargetException e)
				{
					assertTrue(e.getCause() instanceof UnsupportedOperationException);
				}
			}
		}
	}
}
