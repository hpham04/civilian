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
package org.civilian.template;


import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import org.civilian.CivTest;
import org.civilian.Response;
import org.civilian.Template;
import org.civilian.content.ContentType;
import org.civilian.internal.AbstractResponse;
import org.civilian.response.ResponseWriter;
import org.civilian.template.TextTemplate;
import org.junit.Test;


public class TextTemplateTest extends CivTest
{
	@Test public void test() throws Exception
	{
		ResponseWriter out 	= mock(ResponseWriter.class);
		Response response	= mock(AbstractResponse.class);
		when(response.getContentWriter()).thenReturn(out);
		doCallRealMethod().when(response).writeContent(anyObject(), any(ContentType.class));
		doCallRealMethod().when(response).writeTemplate(any(Template.class));
		
		TextTemplate template = new TextTemplate("hallo");
		response.writeContent(template, null);
		
		verify(out).print("hallo");
	}
}
