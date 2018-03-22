/*
       Copyright 2017 IBM Corp All Rights Reserved

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

package com.ibm.hybrid.cloud.sample.portfolio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

//Logging (JSR 47)
import java.util.logging.Level;
import java.util.logging.Logger;

//JSON-P (JSR 353).  The replaces my old usage of IBM's JSON4J (com.ibm.json.java.JSONObject)
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

//JAX-RS 2.0 (JSR 339)
import javax.ws.rs.core.Application;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;


@ApplicationPath("/")
@Path("/")
/** Determine loyalty status based on total portfolio value.
 *  Also send a notification when status changes for a given user.
 */
public class NotificationSlack extends Application {
	private static Logger logger = Logger.getLogger(NotificationSlack.class.getName());

	private String url = null;
	private String id = null;
	private String pwd = null;

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
			url = System.getenv("OW_URL");
			id = System.getenv("OW_ID");
			pwd = System.getenv("OW_PASSWORD");

			if (url == null) {
				logger.warning("The OW_URL environment variable was not set!");
			} else {
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
	public JsonObject notifyLoyaltyLevelChange(JsonObject input) {
		JsonObjectBuilder result = Json.createObjectBuilder();
		String message = null;

		if (input != null) try {
			logger.fine("Notifying about change in loyalty level");
			invokeREST("POST", url, input.toString(), id, pwd);
			message = input.getString("owner")+" has changed status from "+input.getString("old")+" to "+input.getString("new")+".";
			logger.fine("Successfully delivered message to IBM Cloud Functions");
		} catch (Throwable t) { //in case OpenWhisk credentials are not configured, just log the exception and continue
			logger.warning("An unexpected error occurred.  Continuing without notification of change in loyalty level.");
			logException(t);
			message = t.getMessage();
		} else {
			message = "No http body provided in call to Notification microservice!";
		}

		result.add("message", message);
		result.add("location", "Slack");

		return result.build();
	}

	private static JsonObject invokeREST(String verb, String uri, String input, String user, String password) throws IOException {
		logger.fine("Preparing to invoke "+verb+" on "+uri);
		URL url = new URL(uri);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod(verb);
		conn.setRequestProperty("Content-Type", "application/json");
		conn.setDoOutput(true);

		if ((user != null) && (password != null)) { //send via basic-auth
			logger.fine("Setting credentials on REST call");
			String credentials = user + ":" + password;
			String authorization = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
			conn.setRequestProperty("Authorization", authorization);
			logger.fine("Successfully set credentials");
		}

		if (input != null) {
			logger.fine("Writing JSON to body of REST call");
			OutputStream body = conn.getOutputStream();
			body.write(input.getBytes());
			body.flush();
			body.close();
			logger.fine("Successfully wrote JSON");
		}

		logger.fine("Driving call to REST API");
		InputStream stream = conn.getInputStream();

		logger.fine("Parsing results as JSON");
//		JSONObject json = JSONObject.parse(stream); //JSON4J
		JsonObject json = Json.createReader(stream).readObject();

		stream.close();

		logger.fine("Returning JSON to caller of REST API");
		return json;
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
