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
package org.civilian.context.test;


import javax.servlet.http.Cookie;
import org.civilian.CivTest;
import org.civilian.internal.admin.AdminApp;
import org.civilian.internal.admin.AdminPathParams;
import org.civilian.internal.admin.AdminResources;
import org.civilian.internal.admin.app.ResourcesController;
import org.civilian.request.CookieList;
import org.civilian.resource.Url;
import org.civilian.util.Settings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class TestRequestTest extends CivTest
{
	@BeforeClass public static void beforeClass()
	{
		context_ = new TestContext();
		app_	 = new AdminApp();
		context_.addApp(app_, "app", "/", new Settings());
	}

	
	@Before public void before()
	{
		request 	= new TestRequest(app_);
		response 	= request.getTestResponse();   
	}
	
	
	@Test public void testBasics() throws Exception
	{
		// method
		assertEquals("GET", request.getMethod());
		request.setMethod("POST");
		assertEquals("POST", request.getMethod());
		
		// attributes
		assertNull(request.getAttribute("x"));
		assertIterator(request.getAttributeNames());
		request.setAttribute("x", "1");
		request.setAttribute("x", "1");
		assertEquals("1", request.getAttribute("x"));
		assertEquals(null, request.getAttribute("y"));
		assertIterator(request.getAttributeNames(), "x");
		
		// cookies
		assertEquals(0, request.getCookies().size());
		request.setCookies((CookieList)null);
		request.setCookies(new Cookie("x", "1"));
		assertEquals(1, request.getCookies().size());

		// getRequest
		assertSame(request, request.getRequest());

		// setResponse
		request.setResponse(response); // ok
	}
	
	
	@Test public void testPath() throws Exception
	{
		// path
		assertEquals("", request.getRelativePath().toString());
		assertEquals("/", request.getOriginalPath());
		
		request.setPath("x");
		assertEquals("/x", request.getRelativePath().toString());
		
		Url url = new Url(response, "/y").addQueryParam("q", "p");
		request.setPath(url);
		assertEquals("p", request.getParameter("q"));

		request.setPath(AdminResources.root.$appId.settings, "crm");
		assertEquals("/crm/settings", request.getRelativePath().toString());
		assertEquals("crm", request.getPathParam(AdminPathParams.APPID));
		
		request.setPath(ResourcesController.class, "test");
		assertEquals("/test/resources", request.getRelativePath().toString());
		
		request.run();
	}
	
	
	@Test public void testCopy()
	{
		request.setMethod("POST");
		
		TestRequest request2 = new TestRequest(request);
		assertEquals("POST", request2.getMethod());
	}
	
	
	private static TestContext context_;
	private static AdminApp app_;
	private TestRequest request;
	private TestResponse response;
}
