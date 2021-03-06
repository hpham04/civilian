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
package org.civilian.internal.controller.arg.conv;


import org.civilian.Request;
import org.civilian.controller.MethodArg;
import org.civilian.request.BadRequestException;
import org.civilian.util.Value;


/**
 * ValueArg returns a Value object containing the value or a parse exception.
 */
public class ValueArg extends MethodArg
{
	public ValueArg(MethodArg arg)
	{
		arg_ = arg;
	}
	
	
	@Override public Object getValue(Request request) throws Exception
	{
		Value<Object> value = new Value<>();
		try
		{
			value.setValue(arg_.getValue(request));
		}
		catch(BadRequestException e)
		{
			value.setError(e.getCause(), e.getErrorValue());
		}
		catch(Exception e)
		{
			value.setError(e);
		}
		
		return value;
	}


	@Override public String toString()
	{
		return arg_.toString();
	}
	
	
	private MethodArg arg_;
}
