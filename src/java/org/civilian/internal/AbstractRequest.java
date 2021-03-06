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
package org.civilian.internal;


import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.civilian.Application;
import org.civilian.Context;
import org.civilian.Request;
import org.civilian.Resource;
import org.civilian.Response;
import org.civilian.content.ContentSerializer;
import org.civilian.content.ContentType;
import org.civilian.content.ContentTypeList;
import org.civilian.internal.intercept.ReqReaderInterceptorChain;
import org.civilian.internal.intercept.ReqStreamInterceptorChain;
import org.civilian.request.AsyncContext;
import org.civilian.request.BadRequestException;
import org.civilian.request.RequestHeaders;
import org.civilian.request.RequestReaderInterceptor;
import org.civilian.request.RequestStreamInterceptor;
import org.civilian.resource.ExtensionMapping;
import org.civilian.resource.Path;
import org.civilian.resource.PathParam;
import org.civilian.resource.Url;
import org.civilian.text.LocaleService;
import org.civilian.type.Type;
import org.civilian.type.lib.LocaleSerializer;
import org.civilian.type.lib.StandardSerializer;
import org.civilian.util.Check;
import org.civilian.util.Value;


/**
 * AbstractRequest is a partial request implementation
 * with useful defaults.
 */
public abstract class AbstractRequest implements Request
{
	/**
	 * Creates a new AbstractRequest.
	 * @param application the associated application 
	 * @param relativePath the relative path of the request with respect to the application path.
	 */
	public AbstractRequest(Application application, String relativePath)
	{
		application_ 	= Check.notNull(application, "application");
		relativePath_	= new Path(relativePath);
	}
	
	
	protected AbstractRequest(AbstractRequest other)
	{
		application_ 			= other.getApplication();
		relativePath_			= other.getRelativePath();
		acceptedContentTypes_ 	= other.acceptedContentTypes_;
		resource_				= other.resource_;
		localeService_			= other.localeService_;

		if (other.pathParams_ != null)
			pathParams_ = new HashMap<>(other.pathParams_);
		if (other.extension_ != null)
			extension_ = new Extension(other.extension_);
	}


	//-----------------------------
	// general accessors
	//-----------------------------
	
	
	/**
	 * Implements RequestProvider and returns this.
	 */
	@Override public Request getRequest()
	{
		return this;
	}
	
	
	@Override public Context getContext()
	{
		return application_.getContext();
	}

	
	@Override public Application getApplication()
	{
		return application_;
	}

	
	@Override public Response getResponse()
	{
		return response_;
	}


	@Override public void setResponse(Response response)
	{
		Check.notNull(response, "response");
		if (response.getRequest() != this)
			throw new IllegalArgumentException("not my response");
		response_ = response;
	}

	
	/**
	 * Returns if the request has the given method.
	 */
	@Override public boolean hasMethod(String method)
	{
		return getMethod().equals(method);
	}
	
	
	//-----------------------------
	// path related methods
	//-----------------------------

	
	protected void setRelativePath(String relativePath)
	{
		relativePath_	= new Path(relativePath);
		path_ 			= null; // create lazy
		if (extension_ != null)
			extension_.matrixParams = null;
	}
	
	
	@Override public Path getPath()
	{
		if (path_ == null)
			path_ = application_.getPath().add(relativePath_);
		return path_;
	}

	
	@Override public Path getRelativePath()
	{
		return relativePath_;
	}

	
	@Override public String getRealPath()
	{
		Context context 	= getApplication().getContext();
		Path subContextPath = getPath().cutStart(context.getPath()); 
		return subContextPath != null ? context.getRealPath(subContextPath) : null;
	}
	
	
	@Override public Url getUrl(boolean includeServer, boolean includeParams)
	{
		Url url;
		
		if (getResource() != null)
			url = new Url(this, getResource()); // also copies the path params
		else
			url = new Url(this, getPath());
		
		if (includeServer)
			url.prepend(getServerInfo().toString());
		
		if (includeParams)
		{
			for (Iterator<String> pnames = getParameterNames(); pnames.hasNext(); )
			{
				String pname = pnames.next();
				url.addQueryParams(pname, getParameters(pname));
			}
		}
		
		return url;
	}


	//----------------------------
	// resource
	//----------------------------

	
	@Override public Resource getResource()
	{
		return resource_;
	}


	@Override public void setResource(Resource resource)
	{
		resource_ = resource;
	}
	
	
	//----------------------------
	// parameters
	//----------------------------
	
	
	@Override public <T> Value<T> getParameter(String name, Type<T> type)
	{
		return new Value<>(type, getParameter(name), StandardSerializer.INSTANCE);
	}
	

	//----------------------------
	// path params
	//----------------------------


	@Override public <T> void setPathParam(PathParam<T> pathParam, T value)
	{
		Check.notNull(pathParam, "pathParam");
		Check.notNull(value, "value");
		if (pathParams_ == null)
			pathParams_ = new HashMap<>();
		pathParams_.put(pathParam, value);
	}

	
	@Override public void setPathParams(Map<PathParam<?>,Object> pathParams)
	{
		pathParams_ = pathParams;
	}

	
	@SuppressWarnings("unchecked")
	@Override public <T> T getPathParam(PathParam<T> pathParam)
	{
		return pathParams_ != null ? (T)pathParams_.get(pathParam) : null;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override public Iterator<PathParam<?>> getPathParams()
	{
		return pathParams_ != null ? pathParams_.keySet().iterator() : Collections.EMPTY_LIST.iterator();
	}
	
	
	protected void clearPathParams()
	{
		pathParams_ = null;
	}
	

	//----------------------------
	// matrix params
	//----------------------------


	@Override public String getMatrixParam(String name)
	{
		return getMatrixParams().get(name);
	}
	
	
	@Override public String[] getMatrixParams(String name)
	{
		return getMatrixParams().getAll(name);
	}
	
	
	@Override public <T> Value<T> getMatrixParam(String name, Type<T> type)
	{
		return new Value<>(type, getMatrixParam(name), StandardSerializer.INSTANCE);
	}

	
	@Override public Iterator<String> getMatrixParamNames()
	{
		return getMatrixParams().iterator();
	}

	
	private ParamList getMatrixParams()
	{
		if (readExt().matrixParams == null)
			parseMatrixParams();
		return readExt().matrixParams;
	}

	
	private void parseMatrixParams()
	{
		String uri = getOriginalPath();
		
		
		
		int slash = uri.lastIndexOf('/');
		int colon = uri.indexOf(';', slash + 1);
		if (colon < 0)
			writeExt().matrixParams = ParamList.EMPTY;
		else
		{
			ParamList matrixParams = writeExt().matrixParams = new ParamList();
			
			int start = colon + 1;
			do
			{
				int end = uri.indexOf(';', start);
				if (end < 0)
					end = uri.length();
				int eq  = uri.indexOf('=', start);
				if ((eq < 0) || (eq > end))
					matrixParams.add(uri.substring(start, end), "");
				else if (eq > start)
					matrixParams.add(uri.substring(start, eq), uri.substring(eq + 1, end));
				start = end + 1;
			}
			while(start < uri.length());
		}
	}
	

	//----------------------------
	// preferences
	//----------------------------

	
	@Override public ContentTypeList getAcceptedContentTypes()
	{
		if (acceptedContentTypes_ == null)
			initAcceptedContentTypes();
		return acceptedContentTypes_;
	}
	
	
	// lazy construct the list of accepted content types
	// we sort by quality, higher quality coming first, to speed up content negotiation
	// if there are no accept headers, we want a list containing */*
	private void initAcceptedContentTypes()
	{
		acceptedContentTypes_ = ContentTypeList.parse(ContentType.Compare.BY_QUALITY, getHeaders().getAll("Accept"));
		if (acceptedContentTypes_.size() == 0)
		{
			ContentType contentType = getExtensionMapping().extractContentType(getRelativePath().getExtension());
			acceptedContentTypes_ 	= contentType != null ?
				new ContentTypeList(contentType) :
				ContentTypeList.ANY;
		}
	}


	protected ExtensionMapping getExtensionMapping()
	{
		return getApplication().getResourceConfig().getExtensionMapping();
	}

	
	//------------------------------
	// locale
	//------------------------------
	
	
	/**
	 * Returns the locale data associated with the request.
	 * The locale data can be set explicitly by {@link #setLocaleService(LocaleService)}.
	 * If not explicitly set it is derived from the requested preferred locale ({@link #getAcceptedLocale()}).
	 * If the preferred locale is not contained in the list of supported locales (see {@link Application#getLocaleServices()})
	 * then the default application locale will be used.  
	 */
	@Override public LocaleService getLocaleService()
	{
		// lazy init of LocaleService
		// if during processing the locale-item is initialized by a call
		// to #setLocaleItem (e.g. from a user profile), then we avoid to call getPreferences().getLocale())
		if (localeService_ == null)
			localeService_ = getApplication().getLocaleServices().getService(getAcceptedLocale());
		return localeService_;
	}
	

	/**
	 * Sets the localeService associated with the request.
	 * This overrides the default locale data, as defined by the requested preferred language.
	 */
	@Override public void setLocaleService(LocaleService localeService)
	{
		localeService_ = Check.notNull(localeService, "localeService");
	}

	
	/**
	 * Sets the localeService associated with the request.
	 */
	@Override public void setLocaleService(Locale locale)
	{
		Check.notNull(locale, "locale");
		localeService_ = getApplication().getLocaleServices().getService(locale);
	}


	/**
	 * Shortcut for getLocaleServce().getSerializer().
	 */
	@Override public LocaleSerializer getLocaleSerializer()
	{
		return getLocaleService().getSerializer();
	}

	
	//------------------------------
	// async
	//------------------------------

	
	/**
	 * Returns the AsyncContext that was created by the most recent call to {@link #startAsync()}
	 * @return the AsnycContext
	 * @throws IllegalStateException if startAsync() has not been called.
	 */
	@Override public AsyncContext getAsyncContext()
	{
		AsyncContext content = readExt().asyncContext; 
		if (content == null)
			throw new IllegalStateException("async not started");
		return content;
	}
	
	
	/**
	 * Returns if this request has been put into asynchronous mode by a call to {@link #startAsync()}. 
	 */
	@Override public boolean isAsyncStarted()
	{
		return readExt().asyncContext != null;
	}
	
	
	/**
	 * Returns if this request supports asynchronous processing. 
	 */
	@Override public abstract boolean isAsyncSupported();
	
	
	/**
	 * Puts this request into asynchronous mode and initializes its AsyncContext.
	 * @throws IllegalStateException if this request does not support asynchronous operations or if called again
	 * 		in a state where the AsyncContext intervenes, or when the response has been closed.
	 */
	@Override public AsyncContext startAsync()
	{
		try
		{
			return writeExt().asyncContext = createAsyncContext();
		}
		catch(Exception e)
		{
			String message = "can't start async mode";
			if (!isAsyncSupported())
				message += ", not supported or enabled";
			throw new IllegalArgumentException(message, e);
		}
	}
	
	
	protected abstract AsyncContext createAsyncContext() throws Exception;
	
	
	protected void resetAsyncContext()
	{
		if (extension_ != null)
			extension_.asyncContext = null;
	}
	
	
	//------------------------------
	// content
	//------------------------------
	

	/**
	 * Initializes the encoding to the application encoding
	 * if the encoding is not set and content-type is
	 * application/x-www-form-urlencoded.
	 * Derived implementation should call this method once
	 * they are initialized and allow access to their
	 * getContentType() implementation.
	 * Since the request implementation (e.g. HttpServletRequest)
	 * will read the from-urlencoded content to parse parameters)
	 * this initialization should be done before a parameter is accessed.
	 */
	protected void initEncoding()
	{
		if ((getContentEncoding() == null) && 
			ContentType.APPLICATION_X_WWW_FORM_URLENCODED.equals(getContentType()))
		{
			setDefaultContentEncoding();
		}
	}
	
	
	private void setDefaultContentEncoding()
	{
		String encoding = getApplication().getEncoding();
		try
		{
			setContentEncoding(encoding);
		}
		catch (UnsupportedEncodingException e)
		{
			throw new IllegalStateException("application encoding '" + encoding + "' not supported", e); 
		}
	}

	
	@Override public ContentAccess getContentAccess()
	{
		if (contentInput_ == null)
			return ContentAccess.NONE;
		else if (contentInput_ instanceof Reader)
			return ContentAccess.READER;
		else
			return ContentAccess.INPUTSTREAM;
	}

	
	@Override public ContentType getContentType()
	{
		ContentType contentType = readExt().contentType;
		return contentType != null ? contentType : getContentTypeImpl(); 
	}
	
	
	protected abstract ContentType getContentTypeImpl();

	
	@Override public void setContentType(ContentType contentType)
	{
		writeExt().contentType = contentType;
		initEncoding();
	}
	

	@Override public InputStream getContentStream() throws IOException
	{
		if (!(contentInput_ instanceof InputStream))
			initContentStream();
		return (InputStream)contentInput_;
	}
	
	  
	private void initContentStream() throws IOException
	{
		checkNoContentReader();
		InputStream in = getContentStreamImpl();
		
		// always save in contentInput_ before we try to apply the interceptors
		// if an interceptor fails, we still want to have an assigned imputstream 
		contentInput_ = in; 
		
		RequestStreamInterceptor interceptor = readExt().streamInterceptor;
		if (interceptor != null) 
			contentInput_ = ReqStreamInterceptorChain.intercept(this, in, interceptor);
	}
	
	
	protected abstract InputStream getContentStreamImpl() throws IOException;
	
	
	/**
	 * Returns a Reader for the request content, using the character
	 * encoding of the content.
	 */
	@Override public Reader getContentReader() throws IOException
	{
		if (!(contentInput_ instanceof Reader))
			initContentReader();
		return (Reader)contentInput_;
	}
	
	
	private void initContentReader() throws IOException
	{
		checkNoContentStream();
		
		if (getContentEncoding() == null)
			setDefaultContentEncoding();
		
		Reader reader;
		
		Extension ext = readExt();
		if (ext.streamInterceptor != null)   
		{
			initContentStream();
			reader = getContentReaderImpl((InputStream)contentInput_);  
		}
		else
			reader = getContentReaderImpl();
		
		// always save in contentInput_ before we try to apply the interceptors
		// if an interceptor fails, we still want to have an assigned reader 
		contentInput_ = reader;
		if (ext.readerInterceptor != null)
			contentInput_ = ReqReaderInterceptorChain.intercept(this, reader, ext.readerInterceptor);
	}
	
	
	protected abstract Reader getContentReaderImpl() throws IOException; 

	
	protected Reader getContentReaderImpl(InputStream out) throws IOException
	{
		return new InputStreamReader(out, getContentEncoding()); 
	}

	
	@Override public <T> T readContent(Class<T> type) throws Exception
	{
		return readContent(type, type); 
	}
	
	
	@Override public <T> T readContent(Class<T> type, java.lang.reflect.Type genericType) 
		throws Exception
	{
		Check.notNull(type, "type");
		if (genericType == null)
			genericType = type;

		ContentType contentType	= getContentType();
		if ((contentType == null) && (type == String.class))
			contentType = ContentType.TEXT_PLAIN;
			
		ContentSerializer reader = getApplication().getContentSerializer(contentType);
		if (reader == null)
			throw new IllegalStateException("don't know how to read content with content type '" + contentType + "'");
		
		try
		{
			return reader.read(type, genericType, getContentReader());
		}
		catch(Exception e)
		{
			String message = reader.describeReadError(e);
			if (message != null)
				throw new BadRequestException("RequestContent: " + message, e);
			else
				throw e;
		}
	}
	
	
	private void checkNoContentStream()
	{
		if (contentInput_ instanceof InputStream)
			throw new IllegalStateException("Request.getContentStream() has already been called"); 
	}
	
	
	private void checkNoContentReader()
	{
		if (contentInput_ instanceof Reader)
			throw new IllegalStateException("Request.getContentReader() has already been called"); 
	}
	
	
	protected void resetContentInput()
	{
		contentInput_ = null;
	}
	
	
	@Override public void addInterceptor(RequestStreamInterceptor interceptor)
	{
		checkAddInterceptor(interceptor);
		Extension ext 			= writeExt();
		ext.streamInterceptor 	= new ReqStreamInterceptorChain(ext.streamInterceptor, interceptor); 
	}
	

	@Override public void addInterceptor(RequestReaderInterceptor interceptor)
	{
		checkAddInterceptor(interceptor);
		Extension ext 			= writeExt();
		ext.readerInterceptor 	= new ReqReaderInterceptorChain(ext.readerInterceptor, interceptor); 
	}
	
	
	protected void clearInterceptors()
	{
		Extension ext = readExt();
		if (ext.streamInterceptor != null)
			ext.streamInterceptor = null;
		if (ext.readerInterceptor != null)
			ext.readerInterceptor = null;
	}
	

	private void checkAddInterceptor(Object interceptor)
	{
		Check.notNull(interceptor, "interceptor");
		
		// getContentStream() or getContentWriter() may not be called yet
		if (contentInput_ != null)
		{
			checkNoContentStream(); // fails or 	
			checkNoContentReader(); // fails
		}
	}
	
	
	//------------------------------
	// ext
	//------------------------------

	
	/**
	 * Bundles properties which are null most of the times
	 */
	private static class Extension
	{
		public Extension()
		{
		}
		
		
		public Extension(Extension other)
		{
			asyncContext		= other.asyncContext;
			matrixParams		= other.matrixParams;
			contentType			= other.contentType;
			streamInterceptor	= other.streamInterceptor;
			readerInterceptor	= other.readerInterceptor;
		}

		public AsyncContext asyncContext;
		public ParamList matrixParams;
		public ContentType contentType;
		public RequestStreamInterceptor streamInterceptor;
		public RequestReaderInterceptor readerInterceptor;
	}
	
	
	private Extension readExt()
	{
		return extension_ != null ? extension_ : READ_EXTENSION;
	}
	
	
	private Extension writeExt()
	{
		if (extension_ == null)
			extension_ = new Extension();
		return extension_;
	}

	
	//------------------------------
	// print
	//------------------------------

	
	/**
	 * Prints request info to the PrintStream.
	 */
	@Override public void print(PrintStream out)
	{
		Check.notNull(out, "out");
		print(new PrintWriter(out, true));
	}

	
	/**
	 * Prints request info to the PrintWriter.
	 */
	@Override public void print(PrintWriter out)
	{
		Check.notNull(out, "out");
		out.print(getMethod());
		out.print(" ");
		out.println(getUrl(false /*server*/, true /*params*/));
		RequestHeaders headers = getHeaders(); 
		for (String name : headers)
		{
			String[] values = headers.getAll(name);
			for (String value : values)
			{
				out.print(name);
				out.print(' ');
				out.println(value);
			}
		}
	}
	
	
	@Override public String toString()
	{
		return "request:" + getPath(); 
	}
	

	private Response response_;
	private Path path_;
	private Path relativePath_;
	private Map<PathParam<?>, Object> pathParams_;
	private LocaleService localeService_;
	private Application application_;
	private Object contentInput_;
	private Resource resource_;
	private Extension extension_;
	protected ContentTypeList acceptedContentTypes_;
	private static final Extension READ_EXTENSION = new Extension();
}
