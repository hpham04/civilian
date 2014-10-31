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
 package org.civilian.samples.crm.web.root;


import org.civilian.Response;
import org.civilian.annotation.Get;
import org.civilian.annotation.Parameter;
import org.civilian.annotation.Post;
import org.civilian.annotation.Produces;
import org.civilian.content.ContentType;
import org.civilian.request.Session;
import org.civilian.samples.crm.db.entity.User;
import org.civilian.samples.crm.text.Message;
import org.civilian.samples.crm.web.CrmConstants;
import org.civilian.samples.crm.web.SessionUser;
import org.civilian.text.LocaleData;


public class LoginController extends CrmController
{
	//-------------------------
	// login via HTML form 
	//-------------------------
	
	
	/**
	 * Initial GET request to the login page: show login form
	 * or perform autologin during development.
	 */
	@Get @Produces(ContentType.Strings.TEXT_HTML)
	public void render() throws Exception
	{
		LoginForm form = new LoginForm(this);
		if (getCrmApp().doAutoLogin() && login("admin", "!admin", null, "en"))
		{
			// skip login during development
			form.path.read();
			redirect(form);
		}
		else
			render(form, null);
	}


	/**
	 * POST request from the login form.
	 */
	@Post @Produces(ContentType.Strings.TEXT_HTML)
	public void formLogin() throws Exception
	{
		LoginForm form = new LoginForm(this);
		if (form.read())
		{
			if (login(form.name.getValue(), form.password.getValue(), form.language.getValue(), null))
			{
				redirect(form);
				return;
			}
		}
		render(form, msg(Message.LoginInvalid));
	}
	
	
	/**
	 * POST request from the ajax login popup.
	 */
	@Post @Produces(ContentType.Strings.APPLICATION_JSON)
	public void ajaxLogin(
		@Parameter("name") String name, 
		@Parameter("password") String password, 
		@Parameter("language") String language) throws Exception
	{
		boolean ok = login(name, password, null, language);
		getResponse().setStatus(ok ? Response.Status.OK : Response.Status.BAD_REQUEST);
	}
	
	
	//-------------------------
	// helper 
	//-------------------------

	
	private void render(LoginForm form, String errorMessage) throws Exception
	{
		getResponse().writeTemplate(new LoginTemplate(form, errorMessage));
	}

	
	private void redirect(LoginForm form) throws Exception
	{
		if (form.path.hasValue())
			getResponse().sendRedirect(form.path.getValue());
		else
			getResponse().sendRedirect(root);
	}
	
	
	private boolean login(String name, String password, LocaleData localeData, String locale) throws Exception
	{
		if (localeData == null)
		{
			if (locale == null)
				return false;
			localeData = getApplication().getLocaleService().getLocaleData(locale);
		}

		User user = getCrmApp().getUserService().authenticate(name, password);
		if (user != null)
		{
			Session session = getRequest().getSession(true /*create*/);
			SessionUser sessionUser = new SessionUser(user, localeData); 
			session.setAttribute(CrmConstants.ATTR_USER, sessionUser);
			return true;
		}
		else
			return false;
	}
}