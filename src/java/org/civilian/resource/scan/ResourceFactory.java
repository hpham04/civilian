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
package org.civilian.resource.scan;


import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import org.civilian.Controller;
import org.civilian.controller.ControllerNaming;
import org.civilian.internal.controller.MethodAnnotations;
import org.civilian.resource.PathParam;
import org.civilian.resource.PathParamMap;
import org.civilian.response.UriEncoder;
import org.civilian.util.Check;
import org.civilian.util.ClassUtil;
import org.civilian.util.StringUtil;


class ResourceFactory
{
	public ResourceFactory(String rootPackage, 
		ControllerNaming naming,
		PathParamMap pathParams)
	{
		rootPackage_ 	= Check.notNull(rootPackage, "rootPackage");
		naming_		 	= Check.notNull(naming, "naming");
		pathParamMap_ 	= Check.notNull(pathParams, "pathParams");
		packages_.put(rootPackage_, new ControllerPackage(root_, rootPackage_));  
	}
	
	
	public ResourceInfo getRoot()
	{
		return root_;
	}


	public String getRootPackage()
	{
		return rootPackage_;
	}
	
	
	public ControllerNaming getNaming()
	{
		return naming_;
	}
	
	
	private ControllerPackage getPackage(String name)
	{
		if (!name.startsWith(rootPackage_))
			throw new IllegalArgumentException("invalid package " + name);
		
		ControllerPackage cp = packages_.get(name);
		if (cp == null)
			cp = mapPackage(name);
		return cp;
	}
	
	
	private ControllerPackage mapPackage(String packageName)
	{
		ControllerPackage parent 	= null;
		String segment		 		= null;
		if (!packageName.equals(rootPackage_))
		{
			int p = packageName.lastIndexOf('.');
			segment = naming_.packagePart2Segment(packageName.substring(p + 1));
			parent 	= getPackage(packageName.substring(0, p));
		}
		
		Class<?> infoClass		= getPackageInfoClass(packageName);
		ResourcePart part		= map(infoClass, segment, true);
		ControllerPackage cp	= new ControllerPackage(parent, packageName, part);
		packages_.put(packageName, cp);
		return cp;
	}

	
	private Class<?> getPackageInfoClass(String packageName)
	{
		try
		{
			return Class.forName(packageName + ".package-info");
		}
		catch(ClassNotFoundException e)
		{
			return null;
		}
	}

	
	/**
	 * Maps a controller class to a resource. By default the controller resource
	 * extends the package resource by the controller class name.
	 * This method also handles:
	 * - mapping of default controllers, as defined by the DefaultController annotation
	 * - mapping of classes with a PathParam annotation
	 * - mapping of classes with a Path annotation
	 * @param cp the controller package, containing the resource to which the package is mapped
	 * @param c the controller class
	 */
	public void mapController(Class<? extends Controller> c)
	{
		ControllerPackage cp 	= getPackage(ClassUtil.getPackageName(c));
		String segment 			= naming_.className2Segment(c.getSimpleName());
		
		ResourcePart part 		= map(c, segment, false);
		ResourceInfo ctrlRes	= part == null ? cp.resInfo : cp.resInfo.getChild(part);
		
		ctrlRes.setControllerInfo(c.getName(), null);
		
		for (ResourcePart methodPart : collectMethodParts(c))
		{
			ResourceInfo methodRes = ctrlRes.getChild(methodPart);
			methodRes.setControllerInfo(c.getName(), methodPart.pathAnnotation);
		}
	}
	
	
	private HashSet<ResourcePart> collectMethodParts(Class<? extends Controller> c)
	{
		methodParts_.clear();
		for (Method method : c.getDeclaredMethods())
		{
			String pathAnno = MethodAnnotations.getPath(method);
			String path = normPathAnnotation(pathAnno);
			if (path != null)
				methodParts_.add(new ResourcePart(encoder_.encode(path), pathAnno));
		}
		return methodParts_;
	}
	
	
	/**
	 * Reads @PathParam and @Path annotations for package-info and controller classes.
	 * @param segment the default segment derived from the last package part or 
	 * 		the simple name of the controller class. 
	 */
	private ResourcePart map(Class<?> c, String segment, boolean isPackage)
	{
		// check if @PathParam is annotated
		PathParam<?> pp = null;
		String pathAnno = null;
		
		if (c != null)
		{
			org.civilian.annotation.PathParam paramAnnotation = c.getAnnotation(org.civilian.annotation.PathParam.class);
			if (paramAnnotation != null)
			{
				pp = pathParamMap_.get(paramAnnotation.value());
				if (pp == null)
					throw new ScanException(c.getName() + ": annotation @PathParam specifies unknown path parameter '" + paramAnnotation.value() + "'");
			}
			
			// check if @Path is annotated
			org.civilian.annotation.Path pathAnnotation = c.getAnnotation(org.civilian.annotation.Path.class);
			if (pathAnnotation != null)
			{
				if (paramAnnotation != null)
					throw new ScanException(c.getName() + ": cannot specify both @PathParam and @Path annotations");
				
				segment = normPathAnnotation(pathAnnotation.value());
				if ((segment == null) && isPackage)
					throw new ScanException(c.getName() + ": @Path annotation '" + pathAnnotation.value() + "' results in an empy path");
			}
		}
		
		if (pp != null)
			return new ResourcePart(pp);
		else if (segment != null)
			return new ResourcePart(encoder_.encode(segment), pathAnno);
		else
			return null;
	}
	
	
	private String normPathAnnotation(String path)
	{
		if (path != null)
		{
			path = path.trim(); 
			path = StringUtil.cutLeft(path, "/");
			path = StringUtil.cutRight(path, "/").trim();
			if (path.length() == 0)
				path = null;
		}
		return path;
	}
	
	
	private ResourceInfo root_ = new ResourceInfo();
	private String rootPackage_;
	private PathParamMap pathParamMap_;
	private ControllerNaming naming_;
	private HashMap<String,ControllerPackage> packages_ = new HashMap<>(); 
	private HashSet<ResourcePart> methodParts_ = new HashSet<>();
	private UriEncoder encoder_ = new UriEncoder();
}
