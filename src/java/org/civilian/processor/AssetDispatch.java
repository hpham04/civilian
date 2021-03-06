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
package org.civilian.processor;


import org.civilian.Application;
import org.civilian.Processor;
import org.civilian.Request;
import org.civilian.Response;
import org.civilian.asset.AssetService;
import org.civilian.asset.Asset;
import org.civilian.resource.Path;
import org.civilian.util.Check;


/**
 * AssetDispatch serves static resources ({@link Asset Assets}).
 * It uses the {@link Application#getAssetService()} of the application to locate assets.
 * The AssetDispatch accepts the following request methods:
 * <ul>
 * <li>OPTIONS: returns the list of accepted methods
 * <li>GET, POST: returns the asset
 * <li>HEAD: returns the asset head information
 * </ul>
 */
public class AssetDispatch extends Processor
{
	/**
	 * Creates a new AssetDispatch.
	 * @param assetService the asset service used by the AssetDispatch.
	 */
	public AssetDispatch(AssetService assetService)
	{
		Check.notNull(assetService, "assetService");
		if (!assetService.hasAssets())
			throw new IllegalArgumentException("AssetService is empty");

		assetService_	= assetService;
		info_ 			= assetService.getInfo();
	}
	

	/**
	 * Tries to find the asset corresponding to the request path.
	 * If not found, it invokes the next processor in the processor chain.
	 * Else it send the asset to the client.  
	 */
	@Override public boolean process(Request request, ProcessorChain chain) throws Exception
	{
		Path relativePath = request.getRelativePath();
		
		// catch if someone tries to sneak into private folders (e.g. WEB-INF)
		if (request.getContext().isProhibitedPath(relativePath.toString()))
		{
			request.getResponse().sendError(Response.Status.SC404_NOT_FOUND);
			return true;
		}
		
		// if this request does not match a asset, run the processor in the chain 
		Asset asset = assetService_.getAsset(relativePath);
		if (asset == null)
			return chain.next(request); // not an asset

		// handle the asset request
		Response response = request.getResponse();
		String method = request.getMethod();
		if (!VALID_METHODS.contains(method))
			response.sendError(Response.Status.SC405_METHOD_NOT_ALLOWED);
		else if ("OPTIONS".equals(method))
			response.getHeaders().set("Allow", VALID_METHODS);
		else
			asset.write(response, !"HEAD".equals(method) /*write content*/);
		return true; // we handled this request
	}

	
	private AssetService assetService_;
	private static final String VALID_METHODS = "GET, HEAD, POST, OPTIONS";
}
