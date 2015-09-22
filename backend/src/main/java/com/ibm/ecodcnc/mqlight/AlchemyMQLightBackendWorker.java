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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.ibm.mqlight.api.ClientException;

import com.ibm.mqlight.api.CompletionListener;
import com.ibm.mqlight.api.NonBlockingClient;
import com.ibm.mqlight.api.NonBlockingClientAdapter;
import com.ibm.mqlight.api.QOS;
import com.ibm.mqlight.api.SendOptions;
import com.ibm.mqlight.api.SubscribeOptions;
import com.ibm.mqlight.api.DestinationAdapter;
import com.ibm.mqlight.api.StringDelivery;
import com.ibm.mqlight.api.Delivery;
import com.alchemyapi.api.AlchemyAPI;
import com.google.gson.*;

/**
 * The java AlchemyMQLightBackendWorker worker for the MQ Light sample application.
 */
public class AlchemyMQLightBackendWorker {

	/** The topic we publish on to send data to the back-end */
	private static final String PUBLISH_TOPIC = "ecodcnc/mqlight/getauthor";

	/** The topic we subscribe on to receive notifications from the back-end */
	private static final String SUBSCRIBE_TOPIC = "ecodcnc/mqlight/posturl";

	private static final String SHARE_ID = "ecodcnc-alchemyauthor-workers";

	/** Simple logging */
	private final static Logger logger = Logger.getLogger(AlchemyMQLightBackendWorker.class.getName());

	private NonBlockingClient mqlightClient;

	public static void main(String[] args) throws Exception {
		AlchemyMQLightBackendWorker bw = new AlchemyMQLightBackendWorker();
	}

	/**
	 * Default Constructor
	 */
	public AlchemyMQLightBackendWorker() {
		logger.log(Level.INFO, "Initialising...");

		try {
			logger.log(Level.INFO,"Creating an MQ Light client...");
			System.out.println("***Backend MQ Client Creation***");

			mqlightClient = NonBlockingClient.create(null, new NonBlockingClientAdapter<Void>() {

				@Override
				public void onStarted(NonBlockingClient client, Void context) {
					System.out.printf("Connected to %s using client-id %s\n", client.getService(), client.getId());
					
					SubscribeOptions opts = SubscribeOptions.builder().setShare(SHARE_ID).build();
					System.out.println("***SUBS TOPIC BACKEND ***" + SUBSCRIBE_TOPIC);
					client.subscribe(SUBSCRIBE_TOPIC, opts, new DestinationAdapter<Void>() {
						public void onMessage(NonBlockingClient client, Void context, Delivery delivery) {
							logger.log(Level.INFO,"Received message of type: " + delivery.getType());
							StringDelivery sd = (StringDelivery)delivery;
							logger.log(Level.INFO,"Data: " + sd.getData());
							processMessage(sd.getData());
						}
					}, new CompletionListener<Void>() {
						@Override
						public void onSuccess(NonBlockingClient c, Void ctx) {
							logger.log(Level.INFO, "Subscribed!");
						}
						@Override
						public void onError(NonBlockingClient c, Void ctx, Exception exception) {
							logger.log(Level.SEVERE, "Exception while subscribing. ", exception);
						}
					}, null);
				}

				@Override
				public void onRetrying(NonBlockingClient client, Void context, ClientException throwable) {
					System.out.println("***BackEnd - In Retrying error ***");
					if (throwable != null) System.err.println(throwable.getMessage());
					client.stop(null, null);
				}

				@Override
				public void onStopped(NonBlockingClient client, Void context, ClientException throwable) {
					if (throwable != null) {
						System.err.println("*** BackEnd - In Stoppederror ***");
						System.err.println(throwable.getMessage());
					}
					logger.log(Level.INFO,"MQ Light client stopped.");
				}
			}, null);
			logger.log(Level.INFO,"MQ Light client created. Current state: " + mqlightClient.getState());
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Failed to initialise", e);
			throw new RuntimeException(e);
		}
		logger.log(Level.INFO, "Completed initialisation.");
	}

	
	public void processMessage(String message) {

		JsonParser parser = new JsonParser();
		JsonObject messageJSON = (JsonObject)parser.parse(message);

		String postedURL = messageJSON.get("postedURL").getAsString();

		AlchemyAPI alchemyObj = AlchemyAPI.GetInstanceFromString("5fc91e98eacfa5ebf83440e8c6a61d0f60fa380b");

		//String URLString = "http://www.huffingtonpost.com/2015/04/05/report-vegan-diet_n_7008156.html";
		System.out.println("URL sent to URLGetAuthor AlchemyAPI --> " + postedURL);
		Document doc = null;
		try {
			doc = alchemyObj.URLGetAuthor(postedURL);
		} catch (XPathExpressionException | IOException | SAXException
				| ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		String convertDocToString = getStringFromDocument(doc);
		//System.out.println(convertDocToString);
		String alchemyAPIResult = null;
		try {
			alchemyAPIResult = returnResultFromXML(convertDocToString);
		} catch (SAXException | IOException | ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println(convertDocToString);
		System.out.println("Output from URLGetAuthor AlchemyAPI: Author is --> " + alchemyAPIResult.toUpperCase());


		JsonObject reply = new JsonObject();
		reply.addProperty("Author", alchemyAPIResult.toUpperCase());
		reply.addProperty("AlchemyBackend", "JavaAPI: " + toString());

		SendOptions opts = SendOptions.builder().setQos(QOS.AT_LEAST_ONCE).build();
		mqlightClient.send(PUBLISH_TOPIC, reply.toString(), null, opts, new CompletionListener<Void>() {
			public void onSuccess(NonBlockingClient client, Void context) {
				logger.log(Level.INFO, "Sent reply!");
			}
			public void onError(NonBlockingClient client, Void context, Exception exception) {
				logger.log(Level.INFO,"Error!." + exception.toString());
			}
		}, null);
	}

	//XMLToStringConverter
	private static String returnResultFromXML(String convertDocToString ) throws SAXException, IOException, ParserConfigurationException {
		String alchemyResult = null;
		//XML Conversion 
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		// Load the input XML document, parse it and return an instance of the
		// Document class.
		InputSource is = new InputSource();
		is.setCharacterStream(new StringReader(convertDocToString));
		Document document = builder.parse(is);
		NodeList nodes = document.getElementsByTagName("results");
		for (int i = 0; i < nodes.getLength(); i++) {
			Element element = (Element) nodes.item(i);
			NodeList authorElement = element.getElementsByTagName("author");
			Element line = (Element) authorElement.item(i);
			//System.out.println("author is" + authorElement.toString());
			Node child = line.getFirstChild();
			if (child instanceof CharacterData) {
				CharacterData cd = (CharacterData) child;
				alchemyResult = cd.getData();
			}
		}

		return alchemyResult;
	}

	// utility method
	private static String getStringFromDocument(Document doc) {
		try {
			DOMSource domSource = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.transform(domSource, result);

			return writer.toString();
		} catch (TransformerException ex) {
			ex.printStackTrace();
			return null;
		}



	}   
}
