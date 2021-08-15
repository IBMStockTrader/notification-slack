/*
       Copyright 2017-2021 IBM Corp All Rights Reserved

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

package com.ibm.hybrid.cloud.sample.stocktrader.notification;

import com.ibm.hybrid.cloud.sample.stocktrader.notification.client.OpenWhiskClient;
import com.ibm.hybrid.cloud.sample.stocktrader.notification.json.LoyaltyChange;
import com.ibm.hybrid.cloud.sample.stocktrader.notification.json.NotificationResult;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Base64;

//CDI 2.0
import javax.inject.Inject;
import javax.enterprise.context.ApplicationScoped;

//Logging (JSR 47)
import java.util.logging.Level;
import java.util.logging.Logger;

//JAX-RS 2.0 (JSR 339)
import javax.ws.rs.core.Application;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;

//mpRestClient 2.0
import org.eclipse.microprofile.rest.client.inject.RestClient;


@ApplicationPath("/")
@Path("/")
@ApplicationScoped //CDI scope
/** Determine loyalty status based on total portfolio value.
 *  Also send a notification when status changes for a given user.
 */
public class NotificationSlack extends Application {
	private static Logger logger = Logger.getLogger(NotificationSlack.class.getName());

	private @Inject @RestClient OpenWhiskClient openWhiskClient;

	private String url = null;
	private String id = null;
	private String pwd = null;
	private String authorization = null;

	// Override OpenWhisk Client URL if config map is configured to provide URL
	static {
		String mpUrlPropName = OpenWhiskClient.class.getName() + "/mp-rest/url";
		String owURL = System.getenv("OW_URL");
		if ((owURL != null) && !owURL.isEmpty()) {
			logger.info("Using OpenWhisk URL from config map: " + owURL);
			System.setProperty(mpUrlPropName, owURL);
		} else {
			logger.info("OpenWhisk URL not found from env var from config map, so defaulting to value in jvm.options: " + System.getProperty(mpUrlPropName));
		}
	}

	/**
	 * Default constructor. 
	 */
	public NotificationSlack() {
		super();

		try {
			//The following variables should be set in a Kubernetes secret, and
			//made available to the app via a stanza in the deployment yaml

			//Example secret creation command: kubectl create secret generic openwhisk
			//--from-literal=url=https://openwhisk.ng.bluemix.net/api/v1/namespaces/jalcorn%40us.ibm.com_dev/actions/PostLoyaltyLevelToSlack
			//--from-literal=id=bc2b0a37-0554-4658-9ebe-ae068eb1aa22
			//--from-literal=pwd=<myPassword>

			/* Example deployment yaml stanza:
	           spec:
	             containers:
	             - name: notification-slack
	               image: ibmstocktrader/notification-slack:latest
	               env:
	                 - name: OW_URL
	                   valueFrom:
	                     secretKeyRef:
	                       name: openwhisk
	                       key: url
	                 - name: OW_ID
	                   valueFrom:
	                     secretKeyRef:
	                       name: openwhisk
	                       key: id
	                 - name: OW_PASSWORD
	                   valueFrom:
	                     secretKeyRef:
	                       name: openwhisk
	                       key: pwd
	               ports:
	                 - containerPort: 9080
	               imagePullPolicy: Always
	             imagePullSecrets:
	             - name: dockerhubsecret
			 */
			String id = System.getenv("OW_ID");
			String pwd = System.getenv("OW_PASSWORD");

			if (id == null) {
				logger.warning("The OW_ID environment variable was not set!");
			} else {
				String credentials = id + ":" + pwd;
				authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
				logger.fine("Initialization completed successfully!");
			}
		} catch (Throwable t) {
			logger.warning("Unexpected error occurred during initializiation");
			logException(t);
		}
	}

    @POST
    @Path("/")
	@Consumes("application/json")
	@Produces("application/json")
//	@RolesAllowed({"StockTrader", "StockViewer"}) //Couldn't get this to work; had to do it through the web.xml instead :(
	public NotificationResult notifyLoyaltyLevelChange(LoyaltyChange loyaltyChange) {
		NotificationResult result = new NotificationResult();
		String message = null;

		if (loyaltyChange != null) try {
			logger.fine("Notifying about change in loyalty level");

			openWhiskClient.sendSlackMessageViaOpenWhisk(authorization, loyaltyChange);

			message = loyaltyChange.getOwner()+" has changed status from "+loyaltyChange.getOld()+" to "+loyaltyChange.getNew()+".";
			logger.fine("Successfully delivered message to IBM Cloud Functions");
		} catch (Throwable t) { //in case OpenWhisk credentials are not configured, just log the exception and continue
			logger.warning("An unexpected error occurred.  Continuing without notification of change in loyalty level.");
			logException(t);
			message = t.getMessage();
		} else {
			message = "No http body provided in call to Notification microservice!";
		}

		result.setMessage(message);
		result.setLocation("Slack");

		return result;
	}

	private static void logException(Throwable t) {
		logger.warning(t.getClass().getName()+": "+t.getMessage());

		//only log the stack trace if the level has been set to at least FINE
		if (logger.isLoggable(Level.FINE)) {
			StringWriter writer = new StringWriter();
			t.printStackTrace(new PrintWriter(writer));
			logger.fine(writer.toString());
		}
	}
}
