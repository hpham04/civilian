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
package org.civilian.controller.classloader;


import org.junit.Test;
import org.civilian.CivTest;
import org.civilian.controller.classloader.NonDelegatingClassLoader;
import org.civilian.controller.classloader.ReloadConfig;


public class ClassLoaderTest extends CivTest
{
	public static class Included
	{
	}
	
	
	public static class Excluded
	{
	}
	
	
	private String getInnerClassName(String inner)
	{
		return getClass().getName() + "$" + inner;
	}
	
	
	@Test public void test() throws Exception
	{
		ReloadConfig config = new ReloadConfig();
		config.includes().addPackage(getClass());
		config.excludes().add(getInnerClassName("Excluded"));
		
		ClassLoader reloadCl = config.createClassLoader(); 
		
		assertTrue(reloadCl instanceof NonDelegatingClassLoader);
		
		Class<?> includedClass = reloadCl.loadClass(getInnerClassName("Included"));
		assertSame(reloadCl, includedClass.getClassLoader()); 

		Class<?> excludedClass = reloadCl.loadClass(getInnerClassName("Excluded"));
		assertSame(getClass().getClassLoader(), excludedClass.getClassLoader()); 
	}
}
