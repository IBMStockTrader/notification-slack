/*
       Copyright 2021 IBM Corp All Rights Reserved

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.ibm.hybrid.cloud.sample.stocktrader.notification.client;

import com.ibm.hybrid.cloud.sample.stocktrader.notification.json.LoyaltyChange;
import com.ibm.hybrid.cloud.sample.stocktrader.notification.json.NotificationResult;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationPath("/")
@Path("/")
@ApplicationScoped
@RegisterRestClient
/** mpRestClient "remote" interface for the API Connect facade for the IEX stock quote service */
public interface OpenWhiskClient {
	@POST
	@Consumes("application/json")
	@Produces("application/json")
	public NotificationResult sendSlackMessageViaOpenWhisk(@HeaderParam("Authorization") String basicAuth, LoyaltyChange loyaltyChange);
}
