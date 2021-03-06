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
package org.civilian.processor;


import static org.mockito.Mockito.*;
import org.junit.Test;
import org.civilian.CivTest;
import org.civilian.Context;
import org.civilian.Request;
import org.civilian.Response;
import org.civilian.asset.Asset;
import org.civilian.asset.AssetService;
import org.civilian.asset.AssetServices;
import org.civilian.resource.Path;
import org.civilian.response.ResponseHeaders;


public class AssetDispatchTest extends CivTest
{
	@Test public void testInvalidService() throws Exception
	{
		AssetService service = AssetServices.combine(Path.ROOT);
		try
		{
			new AssetDispatch(service);
		}
		catch(IllegalArgumentException e)
		{
			assertEquals("AssetService is empty", e.getMessage());
		}
	}
	
	
	@SuppressWarnings("boxing")
	@Test public void testRequests() throws Exception
	{
		// setup mocks
		Request request 		= mock(Request.class);
		Response response 		= mock(Response.class);
		ResponseHeaders headers = mock(ResponseHeaders.class);
		Context container     = mock(Context.class);
		Asset asset 			= mock(Asset.class);
		AssetService service	= mock(AssetService.class);
		when(request.getResponse()).thenReturn(response);
		when(request.getContext()).thenReturn(container);
		when(response.getHeaders()).thenReturn(headers);
		when(service.hasAssets()).thenReturn(Boolean.TRUE);
		
		AssetDispatch dispatch 	= new AssetDispatch(service);
		
		// test dispatch.getInfo()
		assertEquals(service.getInfo(), dispatch.getInfo());

		// test non-asset requests
		when(request.getRelativePath()).thenReturn(new Path("/customers"));
		assertFalse(dispatch.process(request, ProcessorChain.EMPTY));
		
		// switch to a request which is processed by the AssetDispatch
		when(request.getRelativePath()).thenReturn(new Path("/test"));
		
		// test prohibited-asset requests
		when(container.isProhibitedPath(anyString())).thenReturn(Boolean.TRUE);
		assertTrue(dispatch.process(request, ProcessorChain.EMPTY));
		verify(response).sendError(Response.Status.SC404_NOT_FOUND);
		
		// switch to allowed requests
		when(container.isProhibitedPath(anyString())).thenReturn(Boolean.FALSE);
		
		// no asset found: dispatch to next
		assertFalse(dispatch.process(request, ProcessorChain.EMPTY));

		when(service.getAsset(any(Path.class))).thenReturn(asset);
		
		// test invalid request methods
		when(request.getMethod()).thenReturn("PUT");
		assertTrue(dispatch.process(request, ProcessorChain.EMPTY));
		verify(response).sendError(Response.Status.SC405_METHOD_NOT_ALLOWED);
		
		// test options request
		when(request.getMethod()).thenReturn("OPTIONS");
		assertTrue(dispatch.process(request, ProcessorChain.EMPTY));
		verify(headers).set("Allow", "GET, HEAD, POST, OPTIONS");
		
		// switch to an allowed method
		when(request.getMethod()).thenReturn("HEAD");
		
		// test successful request 
		when(request.getMethod()).thenReturn("GET");
		when(request.getRelativePath()).thenReturn(new Path("/test/some.css"));
		assertTrue(dispatch.process(request, ProcessorChain.EMPTY));
		verify(asset).write(response, true);
	}
}
