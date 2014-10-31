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


import java.io.File;
import java.net.URL;
import org.civilian.CivTest;
import org.civilian.context.test.TestContext;
import org.junit.Test;


public class TestContextTest extends CivTest
{
	@Test public void testResourceLoader() throws Exception
	{
		File tempFile = File.createTempFile("test", ".tmp");
		try
		{
			TestContext container = new TestContext(tempFile.getParentFile());
			URL url = container.getResourceLoader().getResourceUrl("/" + tempFile.getName());
			assertEquals(tempFile.toURI().toURL(), url);
		}
		finally
		{
			tempFile.delete();
		}
	}
}