// ******************************************************************
//
// Program name: mqlight_sample_frontend_web
//
// Description:
//
// A http servlet that demonstrates use of the IBM Bluemix MQ Light Service.
//
// <copyright
// notice="lm-source-program"
// pids=""
// years="2014"
// crc="659007836" >
// Licensed Materials - Property of IBM
//
//
// (C) Copyright IBM Corp. 2014 All Rights Reserved.
//
// US Government Users Restricted Rights - Use, duplication or
// disclosure restricted by GSA ADP Schedule Contract with
// IBM Corp.
// </copyright>
// *******************************************************************

package com.ibm.ecodcnc.mqlight;


import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.LinkedList;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ibm.mqlight.api.ClientException;
import com.ibm.mqlight.api.CompletionListener;
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.NonBlockingClientAdapter;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.SendOptions;
import com.ibm.mqlight.api.DestinationAdapter;
import com.ibm.mqlight.api.StringDelivery;
import com.ibm.mqlight.api.Delivery;

/**
 * This file forms part of the MQ Light Sample Messaging Application - Worker offload pattern sample
 * provided to demonstrate the use of the IBM Bluemix MQ Light Service.
 * 
 * It provides a simple JAX-RS REST service that:
 * - Publishes messages to a back-end worker upon a REST POST
 * - Consumes notifications from the back-end asynchronously upon a REST GET
 * 
 * The REST interface is very simple, using plain text.
 * The posted text is split into separate words, and each sent to the back-end in a separate message.
 * The responses are returned one at a time to REST GET requests. 
 */
/**
 * Servlet implementation class FrontEndServlet
 */
@ApplicationPath("/ecodcnc/mqlightrest/")
@Path("/")
public class FrontEndMQLightPubSubApp extends Application {
	
	/** The topic we publish on to send data to the back-end */
	private static final String PUBLISH_TOPIC = "ecodcnc/mqlight/posturl";
	
	/** The topic we subscribe on to receive notifications from the back-end */
	private static final String SUBSCRIBE_TOPIC = "ecodcnc/mqlight/getauthor";
	
  /** Simple logging */
	private final static Logger logger = Logger.getLogger(FrontEndMQLightPubSubApp.class.getName());
	
	/** JVM-wide initialisation of our subscription */
	private static boolean subInitialised = false;

  /** Client that will send and receive messages */
  private NonBlockingClient mqlightClient;

  /** Holds the messages we've recieved from the backend */
  private LinkedList<String> receivedMessages;
	
    /**
     * Default Constructor
     */
    public FrontEndMQLightPubSubApp() {
      logger.log(Level.INFO, "Initialising...");
      receivedMessages = new LinkedList<String>();
        
      try {

        logger.log(Level.INFO,"Creating an MQ Light client...");

        mqlightClient = NonBlockingClient.create(null, new NonBlockingClientAdapter<Void>() {

          @Override
          public void onStarted(NonBlockingClient client, Void context) {
            System.out.printf("Connected to %s using client-id %s\n", client.getService(), client.getId());

            client.subscribe(SUBSCRIBE_TOPIC, new DestinationAdapter<Void>() {
              public void onMessage(NonBlockingClient client, Void context, Delivery delivery) {
                logger.log(Level.INFO,"Received message of type: " + delivery.getType());
                StringDelivery sd = (StringDelivery)delivery;
                logger.log(Level.INFO,"Data: " + sd.getData());
                receivedMessages.add(sd.getData());
              }
            }, new CompletionListener<Void>() {
              @Override
              public void onSuccess(NonBlockingClient c, Void ctx) {
                logger.log(Level.INFO, "Subscribed!");
                subInitialised = true;
              }
              @Override
              public void onError(NonBlockingClient c, Void ctx, Exception exception) {
                logger.log(Level.SEVERE, "Exception while subscribing. ", exception);
              }
            }, null);
          }

          @Override
          public void onRetrying(NonBlockingClient client, Void context, ClientException throwable) {
              System.out.println("*** In Retrying error ***");
              if (throwable != null) System.err.println(throwable.getMessage());
              client.stop(null, null);
          }

          @Override
          public void onStopped(NonBlockingClient client, Void context, ClientException throwable) {
              if (throwable != null) {
                  System.err.println("*** In Stopped error ***");
                  System.err.println(throwable.getMessage());
              }
              logger.log(Level.INFO,"MQ Light client stopped.");
          }
        }, null);
        logger.log(Level.INFO,"MQ Light client created. Current state: " + mqlightClient.getState());
    	}
    	catch (Exception e) {
    		logger.log(Level.SEVERE, "Failed to initialise", e);
    		throw new RuntimeException(e);
    	}
      logger.log(Level.INFO,"Completed initialisation.");
    }

    /**
     * POST on the words resource publishes the content of the POST to our topic
     */
    @POST
    @Path("posturl")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response publishUrl(Map<String, String> jsonInput) {
        
      logger.log(Level.INFO,"Publishing to URL" + jsonInput);
    	
    	// Check the caller supplied some postedURL
    	String postedURL = jsonInput.get("authorurl");
    	if (postedURL == null) throw new RuntimeException("No URL sent");

      // Before we connect to publish, we need to ensure our subscription has been
      // initialised, otherwise we might miss responses.
      while (!subInitialised) {
        try {
          logger.log(Level.INFO,"Sleep for a sec while we wait for sub to complete");
          Thread.sleep(1000);
        } catch (InterruptedException ie) {
          ie.printStackTrace();
        }
      }

      SendOptions opts = SendOptions.builder().setQos(QOS.AT_LEAST_ONCE).build();

	    com.google.gson.JsonObject message = new com.google.gson.JsonObject();
	    message.addProperty("postedURL", postedURL);
	    message.addProperty("frontend", "JavaAPI: " + toString());

        mqlightClient.send(PUBLISH_TOPIC, message.toString(), null, opts, new CompletionListener<Void>() {
          public void onSuccess(NonBlockingClient client, Void context) {
            logger.log(Level.INFO, "Client id: " + client.getId() + " sent message!");
            System.out.println("Sent the Message from ClientId ---" + client.getId());
            
          }
          public void onError(NonBlockingClient client, Void context, Exception exception) {
            logger.log(Level.INFO,"Error!." + exception.toString());
            System.out.println("Error in Send Frontend");
          }
        }, null);
      HashMap<String, Integer> jsonOutput = new HashMap<>();
      jsonOutput.put("msgSent", 1);
      return Response.ok(jsonOutput).build();
    }
    
    /**
     * GET on the postedURL resource checks for any publications that have been
     * returned on our subscription. Replies with either a single word, or a 204 (No Content).
     * Call repeatedly to get all the responses
     */
    @GET
    @Path("getauthor")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkforAuthors() {
    	// Delegate to a static method synchronized across the JVM
    	return singleThreadedCheckForAuthors();
    }
    
	/**
	 * A synchronized method for consuming messages we have received.
	 */
    private synchronized Response singleThreadedCheckForAuthors() {
      Response response;
      if (receivedMessages == null || receivedMessages.isEmpty()) {
        response = Response.status(204).build();
        return response;
      }
      String msg = receivedMessages.remove();
      response = Response.ok(msg).build();
      return response;
    }
    
}
