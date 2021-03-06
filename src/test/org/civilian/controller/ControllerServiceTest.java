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
package org.civilian.controller;


import org.civilian.CivTest;
import org.civilian.Controller;
import org.civilian.annotation.Get;
import org.civilian.annotation.Path;
import org.civilian.controller.classloader.ReloadConfig;
import org.civilian.resource.PathParamMap;
import org.civilian.testcase1.Test1PathParams;
import org.junit.Test;


public class ControllerServiceTest extends CivTest
{
	public static abstract class Base extends Controller
	{
		@Get public void inheritable()
		{
		}
	}
	
	
	public static class Derived extends Base
	{
		@Get @Path("path") public void somePath()
		{
		}

		@Get @Path("other") public void otherPath()
		{
		}

		@Path("path") public void pseudo()
		{
		}
	}

	
	@Test public void testCtor()
	{
		ControllerService service;
		
        service = new ControllerService(PathParamMap.EMPTY, TYPELIB, null, new ReloadConfig());
		assertTrue(service.isReloading());
		assertEquals("ControllerService", service.toString());
	}


	@Test public void testGetType()
	{
		ReloadConfig reloadConfig = new ReloadConfig();
		reloadConfig.includes().addPackage(getClass());
		
		ControllerService service = new ControllerService(Test1PathParams.MAP, TYPELIB, null, reloadConfig);
		assertTrue(service.isReloading());
		
		assertNull(service.getControllerType(Base.class.getName()));
		assertNull(service.getControllerType((String)null));
		
		service.getControllerType(Derived.class.getName());

		service.getControllerType(ControllerSignature.build(Derived.class.getName(), "path"));
	}
}
