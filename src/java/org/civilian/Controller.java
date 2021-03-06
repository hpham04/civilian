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
package org.civilian;


import org.civilian.annotation.Consumes;
import org.civilian.annotation.Get;
import org.civilian.annotation.Post;
import org.civilian.annotation.Produces;
import org.civilian.annotation.RequestMethod;
import org.civilian.controller.ControllerMethod;
import org.civilian.controller.ControllerType;
import org.civilian.controller.NegotiatedMethod;
import org.civilian.internal.Logs;
import org.civilian.provider.ApplicationProvider;
import org.civilian.provider.ContextProvider;
import org.civilian.provider.MessageProvider;
import org.civilian.provider.RequestProvider;
import org.civilian.provider.ResponseProvider;
import org.civilian.text.LocaleService;
import org.civilian.text.msg.MsgBundle;
import org.civilian.util.Check;


/**
 * Controller handles requests for a {@link Resource}. 
 * All controller classes of an application must be derived from Controller. 
 * A Controller object is instantiated to process a single request and is then
 * discarded. Therefore controllers don't need to be threadsafe 
 * and may declare own properties and use them to
 * process the request. The Controller class itself has several properties, notably 
 * for the {@link #getRequest() request} and {@link #getResponse() response}.
 * <p>
 * Controllers define action methods which represent different ways to handle a request,
 * depending on request properties. 
 * An action method is a 
 * <ul>
 * <li>non-static method
 * <li>with return type void 
 * <li>annotated with a {@link RequestMethod} annotation or one or more of the abbreviated 
 * 	   method annotations like {@link Get}, {@link Post} etc.
 * </ul>
 * <p>
 * Additionally an action method can be annotated with {@link Consumes} and {@link Produces}
 * annotations, to signal which request content can be processed and/or which response
 * content can be produced.
 * <p>
 * Given an incoming request, the best matching action method is selected by an algorithm
 * called content negotiation. If no action method matches the request, an error 
 * is sent (e.g. {@link Response.Status#SC405_METHOD_NOT_ALLOWED},
 * {@link Response.Status#SC406_NOT_ACCEPTABLE},
 * {@link Response.Status#SC415_UNSUPPORTED_MEDIA_TYPE} as response. 
 * Else the matching method is invoked.<br>
 * An action method can have annotated parameters into which context values, like 
 * request parameters, matrix parameters,
 * headers, etc. are injected when the method is invoked.
 * <p> 
 * Before and after the invocation of the negotiated action method several 
 * predefined controller methods are called, 
 * to allow the controller to properly initialize and shutdown before and after request processing.  
 * The exact invocation order is as follows:
 * <pre><code>
 * {@link #checkAccess() checkAccess}
 * try
 *     if request is not committed
 *         [actionMethod,error] = find matching action method
 *         if actionMethod is null
 *              {@link #reject(int) reject(error)}
 *         else
 *              {@link #init()}
 *              {@link #setCaching()}
 *              if request is not committed
 *                   call action method
 * catch exception
 *     {@link #onError(Exception) onError(exception)}
 * finally
 *     {@link #exit()}
 * </code></pre> 			
 * All of the above methods except exit() allow to throw any exception. 
 * If you decide not to handle an exception in your controller
 * implementation, then the exception is passed to {@link Application#onError(Request, Throwable)}
 * for application-wide error handling.
 * <p>
 */
public class Controller implements 
	MessageProvider, RequestProvider, ResponseProvider, ApplicationProvider, ContextProvider
{
	//------------------------------------
	// accessors
	//------------------------------------

	
	/**
	 * Returns the develop flag of the application.
	 * @see Application#develop
	 */
	public boolean develop()
	{
		return getApplication().develop();
	}


	/**
	 * Returns the context to which the application belongs.
	 * @see Application#getContext()
	 */
	@Override public Context getContext()
	{
		return getApplication().getContext();
	}
	
	
	/**
	 * Returns the application to which the controller belongs.
	 */
	@Override public Application getApplication()
	{
		return getRequest().getApplication();
	}

	
	/**
	 * Returns the application, casted to the given application class.
	 */
	public <A extends Application> A getApplication(Class<A> appClass)
	{
		return appClass.cast(getApplication());
	}

	
	/**
	 * Returns the request.
	 */
	@Override public Request getRequest()
	{
		checkProcessing();
		return request_;
	}
	

	private void setRequest(Request request)
	{
		Check.notNull(request, "request");
		if (request_ != null)
			throw new IllegalStateException("already processing");
		request_ = request;
	}
	

	/**
	 * Returns the response.
	 */
	@Override public Response getResponse()
	{
		return getRequest().getResponse();
	}

	
	/**
	 * Returns the type of the controller.
	 */
	public ControllerType getControllerType()
	{
		return type_;
	}
	
	
	/**
	 * Sets the ControllerType. This is automatically done
	 * when a Controller is created during resource dispatch.
	 */
	public void setControllerType(ControllerType type)
	{
		Check.notNull(type, "type");
		if (type.getControllerClass() != getClass())
			throw new IllegalArgumentException();
		type_ = type;
	}
	
	
	/**
	 * Returns the LocaleService of the response. 
	 * Shortcut for getResponse().getLocaleService().
	 */
	public LocaleService getLocaleService()
	{
		return getResponse().getLocaleService();
	}

	
	/**
	 * Returns the MsgBundle object of the LocaleService in the response.
	 * Shortcut for getResponse().getLocaleService().getMsgBundle()
	 */
	public MsgBundle getMsgBundle()
	{
		return getLocaleService().getMsgBundle();
	}
	
	
	/**
	 * Translates a text key into a message, using
	 * the MsgBundle object of the response.
	 * @see #getMsgBundle()
	 */
	@Override public String msg(Object key)
	{
		return getMsgBundle().msg(key);
	}
	
	
	/**
	 * Translates a text key into a message, using
	 * the MsgBundle object of the response.
	 * @param params parameters which are inserted into the message
	 * 		at placeholder strings.
	 * @see #getMsgBundle()
	 */
	@Override public String msg(Object key, Object... params)
	{
		return getMsgBundle().msg(key, params);
	}
	
	
	/**
	 * Returns any uncatched exception thrown during request processing. 
	 * This is the same exception passed to {@link #onError(Exception)}.
	 */
	public Exception getException()
	{
		return exception_;
	}
	
	

	//------------------------------------
	// processing
	//------------------------------------

	
	/**
	 * Processes a request. This method is called by the resource dispatch
	 * on the controller defined for the resource which matches the request.
	 * The ControllerType must have been {@link #setControllerType(ControllerType) initialized}
	 * before this method can be called.
	 * This method 
	 * <ul>
	 * <li>calls the various init-methods
	 * <li>selects the action method based on request properties
	 * <li>invokes the action method
	 * <li>calls the various exit-methods
	 * </ul>
	 */
	public void process(Request request) throws Exception
	{
		process(request, null);
	}
	

	/**
	 * Processes a request.
	 * @param negMethod if not null, then directly use this action instead of negotiating the method
	 */
	public void process(Request request, NegotiatedMethod negMethod) throws Exception
	{
		setRequest(request);
		
		Response response = request.getResponse();
		try
		{
			boolean debug = Logs.CONTROLLER.isDebugEnabled();
			if (debug)
				Logs.CONTROLLER.debug(getClass().getName());
			
			checkAccess();
			if (!response.isCommitted())
			{
				negMethod = negotiate(request, negMethod);
				if (negMethod.positive())
				{
					response.setContentType(negMethod.getContentType());
					init();
					setCaching();
					if (!response.isCommitted())
					{
						ControllerMethod method = negMethod.getMethod();
						if (debug)
							Logs.CONTROLLER.debug("#{}", method);
						method.invoke(this);
					}
				}
				else
					reject(negMethod.getError());
			}
		}
		catch(Exception e)
		{
			exception_ = e;
			onError(e);
		}
		finally
		{
			exit();
			request_ = null;
		}
	}


	private NegotiatedMethod negotiate(Request request, NegotiatedMethod method)
	{
		if (method == null)
		{
			if (type_ == null)
				throw new IllegalArgumentException("ControllerType not initialized");
			method = type_.getMethod(request);
		}
		return method;
	}
	

	/**
	 * Returns if the controller is processing a request.
	 */
	public boolean isProcessing()
	{
		return request_ != null;
	}
	
	
	private void checkProcessing()
	{
		if (request_ == null)
			throw new IllegalStateException("may not be called outside of process(Request)");
	}

	
	/**
	 * Called at the beginning of request processing.
	 * The controller should check if access to the resource is allowed.<br>
	 * Example: Controllers for resources who lie behind a login-wall 
	 * could allow access depending on whether there exists a valid session.<br>
	 * If the resource does not allow access, it should either set
	 * an appropriate response status code, or redirect the response.<br>
	 * The controller can use the request or response object, but should not 
	 * rely on any other initializations. 
	 */
	protected void checkAccess() throws Exception
	{
	}
	

	/**
	 * Called when no action method matches the request.
	 * The default implementation sends an response error.
	 * @param error the suggested response error code (405, 406, 415)
	 */
	protected void reject(int error) throws Exception
	{
		getResponse().sendError(error);
	}

	
	/**
	 * Initialize the controller. Called after {@link #checkAccess()} was called. 
	 * Derived classes can put common initialization code here.
	 * The default implementation is empty.
	 */
	protected void init() throws Exception
	{
	}
	
	
	/**
	 * Configures the caching behavior. 
	 * Called after {@link #init()} was called.
	 * The default implementation sets the "Cache-Control" header to "no-cache".  
	 */
	protected void setCaching()
	{
		getResponse().getHeaders().set("Cache-Control", "no-cache");
	}

	
	/**
	 * Called when an exception is thrown during request processing.
	 * The default implementation just rethrows the exception which will then be delivered
	 * to {@link Application#onError(Request, Throwable)}.
	 * Derived controller implementations which want to handle errors should override this method.
	 */
	protected void onError(Exception e) throws Exception
	{
		throw e;
	}
	
	
	/**
	 * Called at the end of request processing.
	 */
	protected void exit()
	{
	}
	
	
	private ControllerType type_;
	private Request request_;
	private Exception exception_;
}
