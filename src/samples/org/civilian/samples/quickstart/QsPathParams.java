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
 package org.civilian.samples.quickstart;


import org.civilian.resource.PathParam;
import org.civilian.resource.PathParams;
import org.civilian.resource.PathParamMap;


/**
 * Contains the path parameters of application QsApp.
 */ 
public interface QsPathParams
{
	public static final PathParamMap PARAMS             = new PathParamMap(QsPathParams.class);
	
	// define your path params here and seal the map when adding the last 
	public static final PathParam<Integer> CUSTOMERID	= PARAMS.add(PathParams.forIntSegment("customerId"));
	public static final PathParam<Integer> USERID       = PARAMS.addAndSeal(PathParams.forIntSegment("userId"));
}
