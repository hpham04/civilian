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
package org.civilian.response.std;


import static org.mockito.Mockito.*;
import java.util.ArrayList;
import org.junit.BeforeClass;
import org.junit.Test;
import org.civilian.CivTest;
import org.civilian.Request;
import org.civilian.Response;
import org.civilian.content.ContentType;
import org.civilian.content.ContentTypeList;
import org.civilian.request.RequestHeaders;
import org.civilian.resource.Path;
import org.civilian.resource.Url;
import org.civilian.response.TestResponseWriter;


public class StdTest extends CivTest
{
	@BeforeClass public static void beforeClass()
	{
		request = mock(Request.class);
		headers = mock(RequestHeaders.class);
		out = TestResponseWriter.create();
		when(out.response.getRequest()).thenReturn(request);
		when(out.response.getResponse()).thenReturn(out.response);
		when(request.getResponse()).thenReturn(out.response);
		when(request.getHeaders()).thenReturn(headers);
		when(request.getApplication()).thenReturn(out.app);
	}
	
	
	@SuppressWarnings("boxing")
	private static void develop(boolean on)
	{
		when(out.app.develop()).thenReturn(on);
	}
	
	
	@Test public void testNotFound() throws Exception
	{
		develop(false);
		assertNull(new NotFoundResponse().send(request.getResponse()));
		verify(out.response).setStatus(Response.Status.NOT_FOUND);

		ArrayList<String> list = new ArrayList<>();
		list.add("x");
		when(request.getAcceptedContentTypes()).thenReturn(new ContentTypeList(ContentType.TEXT_HTML));
		develop(true);
		assertNull(new NotFoundResponse().send(request.getResponse()));
	}
	
	
	// right now just for coverage
	@Test public void testError() throws Exception
	{
		when(request.getAcceptedContentTypes()).thenReturn(new ContentTypeList());
		develop(false);
		assertNull(sendErrorResponse());

		develop(true);
		assertNull(sendErrorResponse());

		when(request.getAcceptedContentTypes()).thenReturn(new ContentTypeList(ContentType.TEXT_HTML));
		assertNull(sendErrorResponse());
	}
	
	
	private Exception sendErrorResponse()
	{
		ErrorResponse errResponse = new ErrorResponse();
		return errResponse.send(request.getResponse(), 
			Response.Status.INTERNAL_SERVER_ERROR, 
			"some error", 
			new RuntimeException("unexpected"));
	}
	
	
	/**
	 * Runs the template for coverage.
	 */
	@Test public void testTemplates() throws Exception
	{
		TestResponseWriter out = TestResponseWriter.create();
		
		Request request  = mock(Request.class);
		when(out.response.getRequest()).thenReturn(request);
		when(request.getPath()).thenReturn(Path.ROOT);
		when(request.getUrl(false, true)).thenReturn(mock(Url.class));
		
		ErrorTemplate errTemplate = new ErrorTemplate(request, 400, "test", new IllegalArgumentException());
		errTemplate.print(out);
		
		NotFoundTemplate nfTemplate = new NotFoundTemplate(request);
		nfTemplate.print(out);
	}

	
	private static TestResponseWriter out;
	private static Request request;
	private static RequestHeaders headers;
}
