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
 package org.civilian.samples.upload;


import org.civilian.Controller;
import org.civilian.annotation.Get;
import org.civilian.annotation.Post;
import org.civilian.annotation.Produces;


public class IndexController extends Controller
{
	@Get @Post @Produces("text/html")
	public void process() throws Exception
	{
		IndexForm form = new IndexForm(this);
		try
		{
			if (form.isSubmitted())
				form.read();
			getResponse().writeTemplate(new IndexTemplate(form));
		}
		finally
		{
			if (form.file.getUpload() != null)
				form.file.getUpload().delete();
		}
	}
}
