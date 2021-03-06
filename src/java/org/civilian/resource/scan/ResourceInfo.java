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


import java.util.ArrayList;
import java.util.Collections;
import org.civilian.Resource;
import org.civilian.controller.ControllerSignature;
import org.civilian.resource.PathParam;
import org.civilian.util.StringUtil;


/**
 * ResourceInfo holds information about a Resource.
 * It is created during resource scan and can be used 
 * to create a runtime resource tree or to generate 
 * a resources class.
 */
public class ResourceInfo implements Comparable<ResourceInfo>
{
	/**
	 * Creates a info object for the root resource.
	 */
	public ResourceInfo() 
	{
		parent_ 	= null;
		segment_	= null;
		pathParam_	= null;
		path_		= "/";
	}
	
	
	/**
	 * Creates a info object for a child resource which
	 * is mapped to the part.
	 */
	private ResourceInfo(ResourceInfo parent, ResourcePart part)
	{
		parent_ 	= parent;
		segment_	= part.segment;
		pathParam_	= part.pathParam;
		path_		= parent.appendPath(segment_ != null ? '/' + segment_ : pathParam_.toString());  
	}

	
	public ResourceInfo getParent()
	{
		return parent_;
	}
	
	
	public boolean isRoot()
	{
		return parent_ == null;
	}

	
	private String appendPath(String s)
	{
		return parent_ != null ? path_ + s : s;  
	}
	
	
	public String getSegment()
	{
		return segment_;
	}
	
	
	public PathParam<?> getPathParam()
	{
		return pathParam_;
	}
	

	/**
	 * Returns the number of child Path objects.
	 */
	public int getChildCount()
	{
		return children_ != null ? children_.size() : 0;
	}

	
	/**
	 * Returns the i-th child.
	 */
	public ResourceInfo getChild(int i)
	{
		return children_.get(i);
	}

	
	public ResourceInfo getChild(ResourcePart part)
	{
		for (int i=0; i<getChildCount(); i++)
		{
			ResourceInfo child = getChild(i);
			if ((part.segment != null) ? part.segment.equals(child.segment_) : (part.pathParam == child.pathParam_))
				return child;
		}
		return addChild(new ResourceInfo(this, part));
	}
	
	
	private ResourceInfo addChild(ResourceInfo path)
	{
		if (children_ == null)
			children_ = new ArrayList<>();
		children_.add(path);
		return path;
	}


	public void setPackage(ControllerPackage cp)
	{
		if (package_ != null)
			throw new ScanException("path '" + path_ + "' is mapped to package '" + package_ + "' and '" + cp + "'");
		package_ = cp;
	}
	
	
	public ControllerPackage getPackage()
	{
		return package_;
	}

	
	public void setControllerInfo(String className, String methodPath)
	{
		if (controllerSignature_ != null)
			throw new ScanException("resource '" + path_ + "' is mapped to class '" + className + "' and '" + controllerSignature_ + "'");
			
		controllerSignature_ = ControllerSignature.build(className, methodPath);
	}
	
	
	public String getControllerSignature()
	{
		return controllerSignature_;
	}
	
	
	/**
	 * Returns a Java (inner) class name for this resource, 
	 * used by the generator of the Java resources class.
	 */
	public String getJavaClass()
	{
		if (isRoot())
			return "Root";
		else if (segment_ != null)
			return StringUtil.startUpperCase(segment_);
		else
			return '$' + StringUtil.startUpperCase(pathParam_.getName());
	}
	
	
	/**
	 * Returns a Java field name for this resource object, 
	 * used by the generator of the Java resources class.
	 */
	public String getJavaField()
	{
		if (isRoot())
			return "root";
		else if (segment_ != null)
			return StringUtil.startLowerCase(segment_);
		else
			return '$' + StringUtil.startLowerCase(pathParam_.getName());
	}
	

	/**
	 * Recursively sorts the resource children.
	 */
	public void sortChildren()
	{
		if (children_ != null)
		{
			Collections.sort(children_);
			for (ResourceInfo child : children_)
				child.sortChildren();
		}
	}

	
	/**
	 * Sorts the children lexicographically by their path string.
	 * (Mostly to have a nice ordering in the generated resource class,
	 * or in Civilian Admin). 
 	 * At runtime the resources will be sorted again,
	 * prioritizing nodes with a segment!
	 * (see ResourceNode.compareTo)
	 */
	@Override public int compareTo(ResourceInfo other)
	{
		return path_.compareTo(other.path_);
	}

	
	@Override public int hashCode()
	{
		return path_.hashCode();
	}
	
	
	@Override public boolean equals(Object other)
	{
		return (other instanceof ResourceInfo) && (((ResourceInfo)other).path_.equals(path_)); 
	}

	
	@Override public String toString()
	{
		return path_;
	}

	
	/**
	 * Creates a resource tree out of this info.
	 */
	public Resource toResource()
	{
		if (!isRoot())
			throw new IllegalStateException("not root");
		return new RtResource(this); 
	}
	
	
	/**
	 * Runtime class for conversion of a ResourceInfo tree into a Resource tree.
	 */
	private static class RtResource extends Resource
	{
		public RtResource(ResourceInfo resInfo)
		{
			setControllerSignature(resInfo.controllerSignature_);
			addChildren(resInfo);
		}
		
		
		public RtResource(Resource parent, ResourceInfo resInfo)
		{
			super(parent, resInfo.segment_, resInfo.pathParam_);
			setControllerSignature(resInfo.controllerSignature_);
			addChildren(resInfo);
		}
		
		private void addChildren(ResourceInfo resInfo)
		{
			int n = resInfo.getChildCount();
			for (int i=0; i<n; i++)
				new RtResource(this, resInfo.getChild(i));
		}
	}


	private final ResourceInfo parent_;
	private final PathParam<?> pathParam_;
	private final String segment_;
	private final String path_;
	private ControllerPackage package_;
	private String controllerSignature_;
	private ArrayList<ResourceInfo> children_;
}
