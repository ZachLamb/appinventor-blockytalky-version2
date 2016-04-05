// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;

import com.google.appinventor.components.annotations.PropertyCategory;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.app.Activity;
import android.os.AsyncTask;
import android.net.wifi.WifiManager;
import android.net.DhcpInfo;
import android.content.Context;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.LogRecord;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Date;

import org.apache.http.conn.util.InetAddressUtils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_10;
import org.java_websocket.handshake.ServerHandshake;

import org.json.JSONObject;
import org.json.JSONException;

import java.awt.dnd.DragGestureEvent;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Exception;
import java.lang.String;
import java.util.ArrayList;
import java.util.List;

import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import com.google.appinventor.components.runtime.collect.Lists;
import com.google.appinventor.components.runtime.RandomNameGenerator;
import com.google.appinventor.components.runtime.AnnouncementBroadcaster;

@DesignerComponent(version = 1,
   description = "This is version 2 of BlockyTalky.",
   category = ComponentCategory.EXTENSION,
   nonVisible = true,
   iconName = "images/extension.png")
@SimpleObject(external = true)
@UsesLibraries(libraries = "java_websocket.jar")
@UsesPermissions(permissionNames = "android.permission.INTERNET, " +
                                    "android.permission.CHANGE_WIFI_MULTICAST_STATE, " +
                                    "android.permission.ACCESS_NETWORK_STATE, " +
                                    "android.permission.ACCESS_WIFI_STATE")
public class BlockyTalky extends AndroidNonvisibleComponent implements  Component {
	private static String LOG_TAG = "BLOCKYTALKY";
	private final ComponentContainer container;
	private String nodeName = "null";

	private int itemTextColor;
	private int itemBackgroundColor;
	public final static int DEFAULT_ITEM_TEXT_COLOR = Component.COLOR_GREEN;
	public final static int DEFAULT_ITEM_BACKGROUND_COLOR = Component.COLOR_BLACK;
	private NewClient client = null;
	/* Used to identify the call to startActivityForResult. Will be passed back
	into the resultReturned() callback method. */


	// sendMessage();
	//blockyTalky Component
	private int requestCode;
	public BlockyTalky(ComponentContainer container) 
	{
		super(container.$form());
		this.container = container;
			// property 
	}
		@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
		        defaultValue = "")
		@SimpleProperty(description = "Name of message sender")
		public void NodeName(String name) 
		{
		  if(name == "null")
		  {
		    RandomNameGenerator random = new RandomNameGenerator();
		    name = random.GenerateRandomName();
		  }
		  else
		  {
		  	Log.i("BlockyTalky", "There's already a name");
		  }
		}
		@SimpleFunction(description = "Sends a message to a BlockyTalky")
		public void SendMessage(String message, String destination) 
		{	
			BlockyTalkyMessage btMessage = new BlockyTalkyMessage(this.nodeName,destination,message);
		    Log.i("BlockyTalky", "I'm sending a message to" + destination);
		    //check network connectivity
		    if(nearbyBlockyTalkies().contains(destination)){
		    	//send message
		    	client.send(btMessage.toJson());
		    }
		    else{
		    	//reopen client
		    	connectToMessagingRouter();
	                client.send(btMessage.toJson()); //@fixme: message will likely be dropped on the floor, since registration will not be complete when this statement executes.
		    }

		}

		// return nearby BlockyTalky's
		// @SimpleProperty(description = "Will return BlockyTalkys nearby.")
		@SimpleFunction
		public List<String> nearbyBlockyTalkies() 
		{
			List<String> nearbyBTs = new ArrayList<String>();
			//   nearbyBTs.add("Everything");
			// for(String key : nearbyBTs.keySet()){
			//         nearbyBTs.add(key);
			//     }
			return nearbyBTs;
		}

		public class BlockyTalkyMessage {
	        private String source;
	        private String destination;
	        private String content;
	        private String jsonFormatString =
	                "{\"py/object\": \"__main__.Message\", \"channel\": \"Message\", \"content\": \"%s\", \"destination\": \"%s\", \"source\": \"%s\"}";

	        public BlockyTalkyMessage(String source, String destination, String content) {
	            this.source = source;
	            this.destination = destination;
	            this.content = content;
	        }

	        public BlockyTalkyMessage(String json) {
	            JSONObject message;
	            try {
	                message = new JSONObject(json);
	                this.source = message.getString("source");
	                this.destination = message.getString("destination");
	                this.content = message.getString("content");
	            } catch (Exception e) {
	                Log.d(LOG_TAG, "Exception while parsing json");
	                e.printStackTrace();
	            }
	        }

	        public String toJson() {
	            return String.format(
	                    this.jsonFormatString,
	                    this.content,
	                    this.destination,
	                    this.source);
	        }
    	}

		// Note from Web Socket Github README: Important events onOpen, onClose, onMessage and onIOError 
		//get fired throughout the life of the WebSocketClient, and must be implemented in your subclass.
		private class NewClient extends WebSocketClient {
			private boolean isReadyForUse = false;
			private String nodeName = null;

			public NewClient(String nodeName, URI serverUri, Draft draft, HashMap<String, String > protocol, int timeout) 
			{
	          super(serverUri, draft, protocol, timeout);
	          this.nodeName = nodeName;
		    }
		    public NewClient(String nodeName, URI serverUri, Draft draft) 
		    {
		      super(serverUri, draft);
		      this.nodeName = nodeName;
		    }
		    public NewClient(String nodeName, URI serverURI) 
		    {
		      super(serverURI);
		      this.nodeName = nodeName;
		    }

		    public boolean readyToCommunicate(){
		        boolean ret = false;
		        synchronized(this){
		          ret = this.isReadyForUse;
		        }
		        return ret;
		    }
		    
		    @Override 
		    public void onOpen(ServerHandshake handshakedata){
		    	Log.d(LOG_TAG, "BlockyTalky message router connection opened... attempting registration");
		        BlockyTalkyMessage registerMessage = new BlockyTalkyMessage(this.nodeName, "dax", "");
		        super.send(registerMessage.toJson());
		        Log.d(LOG_TAG, "Registered with BlockyTalky message router as " + this.nodeName);
		        synchronized(this){
		          isReadyForUse = true;
		        }		    
	      	}
		    @Override 
		    public void onClose(int code, String reason, boolean remote){
		    	Log.d(LOG_TAG, "closed with exit code " + code + " additional info: " + reason);		    	
		    	client = null;
		    }
		    @Override 
		    public void onMessage(String json){
		    	BlockyTalkyMessage message = new BlockyTalkyMessage(json);
	            receivedMessage = message.content;
	            receivedMessageFrom = message.source;
	            Log.d(LOG_TAG, "*****received message: " + message.toJson());
	            handler.post(new Runnable() {
	                public void run() {
	                    OnMessageReceived();
	                }
         		 });
		    }
		    @Override
		    public void onError(Exception ex){
		    	Log.d(LOG_TAG,"an IO Error occured: " + ex);
		    }
		}
		private void connectToMessagingRouter(){
        try {
          Log.d(LOG_TAG, "Opening connection to BlockyTalky messaging router");
          client = new EmptyClient(this.nodeName, new URI(blockyTalkyMessageRouter), new Draft_10(), headers, 10000);
          client.connect();
        } catch (Exception e) {
          Log.d(LOG_TAG, "Exception Caught while trying to connect to messasge router: " + e);
        }
    }
}