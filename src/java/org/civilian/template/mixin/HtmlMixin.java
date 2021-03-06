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
package org.civilian.template.mixin;


import org.civilian.Controller;
import org.civilian.Resource;
import org.civilian.Response;
import org.civilian.provider.PathProvider;
import org.civilian.provider.ResponseProvider;
import org.civilian.resource.Path;
import org.civilian.resource.Url;
import org.civilian.response.ResponseWriter;
import org.civilian.template.HtmlUtil;
import org.civilian.util.Check;


/**
 * HtmlMixin is a template mixin with HTML related methods.
 */
public class HtmlMixin
{
	/**
	 * Creates a new HtmlMixin.
	 * @param out the ResponseWriter.
	 */
	public HtmlMixin(ResponseWriter out)
	{
		this.out = Check.notNull(out, "out");
	}

	
	/**
	 * Prints a link element whose href attribute is the given css path.
	 * The link element has its rel-attribute set to "stylesheet" and its type attribute
	 * set to "text/css".
	 * @param cssPath a path to the css file. The path is automatically
	 * 		prefixed by the application path.
	 */
	public void linkCss(String cssPath)
	{
		linkCss(cssPath, (String[])null);
	}

	
	/**
	 * Prints a link element like {@link #linkCss(String)} and additionally prints
	 * a list of attribute names and values given by the attrs parameter.
	 * @param cssPath a path to the css file. The path is automatically
	 * 		prefixed by the {@link #path()} stored in the mixin..
	 */
	public void linkCss(String cssPath, String... attrs)
	{
		out.print("<link");
		attr("rel", "stylesheet");
		attr("type", "text/css");
		printPathAttr("href", cssPath);
		if (attrs != null)
			HtmlUtil.attrs(out, attrs);
		out.println(">");
	}
	
	
	/**
	 * Prints a script element with a src-attribute
	 * @param src a path to the script file. The path is automatically prefixed by 
	 * 		the application path.
	 */
	public void script(String src)
	{
		script(src, (String[])null);
	}
	
	
	/**
	 * Same as {@link #script(String)} but additionally prints a 
	 * list of attribute names and values given by the attrs parameter.
	 */
	public void script(String src, String... attrs)
	{
		out.print("<script");
		printPathAttr("src", src);
		if (attrs != null)
			HtmlUtil.attrs(out, attrs);
		out.println("></script>");
	}


	/**
	 * Prints a img element with a src-attribute
	 * @param src a path to the img file. The path is automatically prefixed by 
	 * 		the {@link #path()} stored in the mixin.
	 */
	public void img(String src)
	{
		img(src, (String[])null);
	}
	
	
	/**
	 * Same as {@link #img(String)} but additionally prints a 
	 * list of attribute names and values given by the attrs parameter.
	 */
	public void img(String src, String... attrs)
	{
		out.print("<img");
		printPathAttr("src", src);
		if (attrs != null)
			HtmlUtil.attrs(out, attrs);
		out.println("></img>");
	}


	/**
	 * Prints a meta tag with a http-equiv and content attribute.
	 */
	public void metaHttpEquiv(String httpEquiv, String content)
	{
		out.print("<meta");
		attr("http-equiv", httpEquiv);
		attr("content", content);
		out.println(">");
	}

	
	/**
	 * Prints a meta tag for the content-type plus encoding of the response.
	 * @see #metaHttpEquiv(String, String)
	 * @see Response#getContentTypeAndEncoding()
	 * @throws IllegalStateException thrown if the ResponseWriter does not 
	 * 		have a Response as {@link ResponseWriter#getContext(java.lang.Class) context object}
	 */
	public void metaContentType()
	{
		String contentType = response().getContentTypeAndEncoding();
		if (contentType != null)
			metaHttpEquiv("Content-Type", contentType);
	}
	

	/**
	 * Prints a integer attribute.
	 * The output is: ' ' name '="' value '"'.
	 */
	public void attr(String name, int value)
	{
		attr(name, String.valueOf(value), false);
	}
	
	
	/**
	 * Prints a String attribute.
	 * The output is: ' ' name '="' escaped-value '"'.
	 */
	public void attr(String name, String value)
	{
		attr(name, value, true);
	}
	
	
	/**
	 * Prints an attribute.
	 * The output is: ' ' name '="'value '"'.
	 * @param name the attribute name
	 * @param value the attribute value
	 * @param escape should the attribute value be escaped. Only pass false, if you
	 * 		know that escaping is not needed.
	 */
	public void attr(String name, String value, boolean escape)
	{
		out.print(' ');
		out.print(name);
		out.print("=\"");
		if (value != null)
		{
			if (escape)
				attrValue(value);
			else
				out.print(value);
		}
		out.print('"');
	}
	
	
	/**
	 * Prints an escaped attribute value.
	 */
	public void attrValue(String value)
	{
		HtmlUtil.escape(out, value, true);
	}
	
	
	/**
	 * Prints an escaped text string.
	 */
	public void text(String text)
	{
		HtmlUtil.escape(out, text, false);
	}
	

	/**
	 * Prints the text with proper escaping if the text is not null and has
	 * a non-zero length, else print the defaultValue without escaping.
	 * Example: out.printText(name, "&nbsp;");
	 */
	public void text(String s, String defaultValue)
	{
		if ((s != null) && (s.length() > 0))
			text(s);
		else
			out.print(defaultValue);
	}
	
	
	private void printPathAttr(String attr, String path)
	{
		out.print(' ');
		out.print(attr);
		out.print("=\"");
		path(path);
		out.print('"');
	}
	
	
	/**
	 * Returns the path stored in the HtmlMixin.
	 * This is either the default path or a path previously set
	 * by #setPath(Path). 
	 * The default path equals the application path if the   
	 * ResponseWriter was initialized from a Civilian response.
	 * Else the default path is simple the root path.
	 */
	public Path path()
	{
		if (path_ == null)
			initPath();
		return path_;
	}


	/**
	 * Lazily init the path.
	 */
	private void initPath()
	{
		ResponseProvider rp = out.getContext(ResponseProvider.class);
		path_ = rp != null ? rp.getResponse().getApplication().getPath() : Path.ROOT;
	}
	
	
	/**
	 * Sets the path stored in the HtmlMixin.
	 * @return this
	 */
	public HtmlMixin setPath(Path path)
	{
		Check.notNull(path, "path");
		path_ = path;
		return this;
	}

	
	/**
	 * Prints the mixin {@link #path()} + the subpath.
	 */
	public void path(String subPath)
	{
		path().print(out, subPath);
	}

	
	/**
	 * Prints a JavaScript string which is embedded in a HTML page.
	 * It escapes \', \n, \r \t and \\ characters and converts
	 * character which are not printable in the current encoding to
	 * a HTML character reference. It prints null, if the text is null
	 * @param text the string
	 * @param addQuotes adds single quote character around the string if true. 
	 */
	public void jsString(String text, boolean addQuotes)
	{
		HtmlUtil.jsString(out, text, addQuotes);
	}

	
	//-----------------------------
	// Urls
	//-----------------------------
	

	/**
	 * Returns a Url object with the resource path.
	 * @throws IllegalStateException thrown if the ResponseWriter does not 
	 * 		have a Response as {@link ResponseWriter#getContext(java.lang.Class) context object}
	 */
	public Url url(Resource resource)
	{
		return prepUrl(new Url(response(), resource));
	}

	
	/**
	 * Returns a Url object with the path of the resource associated with the controller.
	 * @throws IllegalStateException thrown if the ResponseWriter does not 
	 * 		have a Response as {@link ResponseWriter#getContext(java.lang.Class) context object}
	 */
	public Url url(Class<? extends Controller> controllerClass)
	{
		return prepUrl(new Url(response(), controllerClass));
	}

	
	/**
	 * Returns a Url with the given value.  
	 * @throws IllegalStateException thrown if the ResponseWriter does not 
	 * 		have a Response as {@link ResponseWriter#getContext(java.lang.Class) context object}
	 */
	public Url url(String value)
	{
		return prepUrl(new Url(response(), value));
	}
	
	
	/**
	 * Returns a Url with the path of the given path provider.  
	 * @throws IllegalStateException thrown if the ResponseWriter does not 
	 * 		have a Response as {@link ResponseWriter#getContext(java.lang.Class) context object}
	 */
	public Url url(PathProvider pp)
	{
		return prepUrl(new Url(response(), pp));
	}
	
	
	/**
	 * Returns a Url with the given path.  
	 * @throws IllegalStateException thrown if the ResponseWriter does not 
	 * 		have a Response as {@link ResponseWriter#getContext(java.lang.Class) context object}
	 */
	public Url url(Path path)
	{
		return prepUrl(new Url(response(), path));
	}
	
	
	private Url prepUrl(Url url)
	{
		return url.setSerializer(out.getSerializer());
	}

	
	//-----------------------------
	// stacktrace
	//-----------------------------
	

	/**
	 * Prints a stacktrace of the Throwable.
	 * The stacktrace lines are wrapped in a paragraph element with css class "stacktrace".
	 */
	public void stackTrace(Throwable t)
	{
		stackTrace(t, false);
	}
	
	
	/**
	 * Prints a stacktrace of the Throwable.
	 * The stacktrace lines are wrapped in a paragraph element with css class "stacktrace".
	 */
	private void stackTrace(Throwable t, boolean printCausedBy)
	{
		if (printCausedBy)
			out.print("caused by: ");
		text(t.toString());
		int stLimit = t.getCause() != null ? 3 : 0;
		int count = 0;
		out.println("<p class=\"stacktrace\">");
		for (StackTraceElement ste : t.getStackTrace())
		{
			text(ste.toString());
			out.println("<br>");
			if (++count == stLimit)
			{
				out.println("...<br>");
				break;
			}
		}
		out.println("</p>");
		if (t.getCause() != null)
			stackTrace(t.getCause(), true);
	}


	//-----------------------------
	// helper
	//-----------------------------
	

	private Response response()
	{
		if (response_ == null)
			response_ = out.getSafeContext(ResponseProvider.class).getResponse();
		return response_;
	}
	

	private Response response_;
	private ResponseWriter out;
	private Path path_;
}
