package com.rackspace.cloud.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class DeployUtility {
	private static Logger log = Logger.getLogger(DeployUtility.class);  
	private static Properties propsFile;

	static{

		DeployUtility.propsFile=new Properties();
		//File theFile=new File("/home/docs/DeployWars/props.properties");
		InputStream innyStream=DeployUtility.class.getClassLoader().getResourceAsStream("props.properties");
		try {
			File afile=new File("/home/docs/DeployWars/rax-deploy-nexus-war.properties");
			//rax-deploy-nexus-war.properties should override prop.properties
			if(afile.exists()){
				innyStream=new FileInputStream(afile);
				DeployUtility.propsFile.load(innyStream);
			}
			DeployUtility.propsFile.load(innyStream);					
		} 
		catch (FileNotFoundException e) {
			e.printStackTrace();
			log.debug(": FileNotFoundException caught e.getMessage()="+e.getMessage());
			log.debug(e);
			e.printStackTrace(System.out);
		} 
		catch (IOException e) {
			e.printStackTrace();
			log.debug(": IOException caught e.getMessage()="+e.getMessage());
			log.debug(e);
			e.printStackTrace(System.out);
		}
		catch(Throwable e){
			e.printStackTrace();
			log.debug(": Throwable caught e.getMessage()="+e.getMessage());
			log.debug(e);
			e.printStackTrace(System.out);
		}
	}
	
	
	public static String jSchExec(String command, String prodServer, Map<String,List<String>>messages){
		String scpUser=(propsFile.getProperty("scpuser","docs")).trim();							
		String passwd=(propsFile.getProperty("docspasswd","Fanatical7")).trim();
		String known_hosts=(propsFile.getProperty("knownhosts","/home/docs/.ssh/known_hosts")).trim();
		String id_rsa=(propsFile.getProperty("idrsa","/home/docs/.ssh/id_rsa")).trim();
		return jSchExec(scpUser,passwd,known_hosts,id_rsa,prodServer,messages,command);
	}

	private static String jSchExec(String scpUser, String passwd, String known_hosts, String id_rsa, 
			String prodServer, Map<String,List<String>>messages, String command){
		String retVal="";
		String METHOD_NAME="jSchExec2()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: scpUser="+scpUser+" passwd="+passwd+" known_hosts="+known_hosts+" id_rsa="+id_rsa+
					" prodServer="+prodServer+ " command="+command);
		}
		JSch jsch=new JSch();
		Channel channel=null;
		com.jcraft.jsch.Session session=null;
		try {
			jsch.setKnownHosts(known_hosts);
			jsch.addIdentity(id_rsa);
			//Get a jsch session
			session=jsch.getSession(scpUser, prodServer, 22);
			session.setPassword(passwd);
			//Properties config = new Properties();
			//config.put("StrictHostKeyChecking","yes");
			//session.setConfig(config);
			session.setConfig("PreferredAuthentication", "publickey");
			session.setConfig("StrictHostKeyChecking","no");
			session.connect();

			channel=session.openChannel("exec");
			((ChannelExec)channel).setCommand(command);
			
			channel.setInputStream(null);
			((ChannelExec)channel).setErrStream(System.err);

			InputStream inny=channel.getInputStream();			
			channel.connect();
			
		      byte[] tmp=new byte[1024];
		      StringBuffer strBuff=new StringBuffer("");
		      while(true){
		    	  while(inny.available()>0){
		    		  int i=inny.read(tmp, 0, 1024);
		    		  if(i<0){
		    			  break;
		    		  }
		    		  strBuff.append(new String(tmp, 0, i));
		    	  }
		    	  if(channel.isClosed()){
		    		  strBuff.append("exit-status: "+channel.getExitStatus());
		    		  break;
		    	  }
		    	  try{
		    		  Thread.sleep(1000);
		    	  }
		    	  catch(InterruptedException e){
		    		  strBuff.append("InterruptException caught message: ");
		    		  strBuff.append(e.getMessage());
		    		  if(null!=messages){
		    			  List<String>messagesList=messages.get("error");
		    			  if(null==messagesList){
		    				  messagesList=new ArrayList<String>();
		    				  messages.put("error", messagesList);
		    			  }
		    			  messagesList.add("<span class='failuremessage'>InterruptedException Error sleeping and trying to run jSch command: "+
		    					  command+" with error message:"+ e.getMessage()+"</span>");
		    		  }
		    		  e.printStackTrace();
		    		  log.debug(e);
		    	  }
		    	  catch(Throwable e){
		    		  strBuff.append("Throwable Exception caught message: ");
		    		  strBuff.append(e.getMessage());
		    		  if(null!=messages){
		    			  List<String>messagesList=messages.get("error");
		    			  if(null==messagesList){
		    				  messagesList=new ArrayList<String>();
		    				  messages.put("error", messagesList);
		    			  }
		    			  messagesList.add("<span class='failuremessage'>Throwable Error while sleeping and trying to run jSch command: "+
		    					  command+" with error message:"+ e.getMessage()+"</span>");
		    		  }
		    		  e.printStackTrace();
		    		  log.debug(e);
		    	  }
		      }
		    channel.disconnect();
			session.disconnect();
		    retVal=strBuff.toString();
		}
		catch (JSchException e) {
			e.printStackTrace();
			log.debug(e);
			log.debug(METHOD_NAME+": END: JSchException caught e.getMessage()="+e.getMessage());
			if(null!=messages){
				List<String>messagesList=messages.get("error");
				if(null==messagesList){
					messagesList=new ArrayList<String>();
					messages.put("error", messagesList);
				}
				messagesList.add("<span class='failuremessage'>JSchException Error trying to run jSch command: "+command+
						" with error message:"+ e.getMessage()+"</span>");
			}
		}
		catch(Throwable e){
			log.debug(METHOD_NAME+": END: Throwable caught e.getMessage()="+e.getMessage());
			if(null!=messages){
				List<String>messagesList=messages.get("error");
				if(null==messagesList){
					messagesList=new ArrayList<String>();
					messages.put("error", messagesList);
				}
				messagesList.add("<span class='failuremessage'>Throwable Exception Error trying to run jSch command: "+command+
						" with error message:"+ e.getMessage()+"</span>");
			}
			e.printStackTrace();
			log.debug(e);
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END: retVal="+retVal);
		}
		return retVal;
	}

	//public static boolean scpFileToServer(String scpUser, String prodServer, File fileToTransfer, String finalFileName, 
	//		String prodWebAppsPath, String passwd,String known_hosts, String id_rsa, Map<String,List<String>> messages)
	
	public static boolean scpFileToServer(NexusDeploy nexusDeploy, String prodServer, File fileToTransfer, String finalFileName, 
			                              Map<String,List<String>> messages){
		String METHOD_NAME="scpFileToServer";
		boolean retVal=true;
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START prodServer="+prodServer+" fileToTransfer="+fileToTransfer+
					" finalFileName="+finalFileName+" nexusDeploy.getScpwebapps1()="+nexusDeploy.getScpwebapps1());

		}		
		JSch jsch=new JSch();
		try {
			jsch.setKnownHosts(nexusDeploy.getKnown_hosts());
			jsch.addIdentity(nexusDeploy.getId_rsa());
			//Get a jsch session
			com.jcraft.jsch.Session session=jsch.getSession(nexusDeploy.getScpUser(), prodServer, 22);
			session.setPassword(nexusDeploy.getPasswd());
			//Properties config = new Properties();
			//config.put("StrictHostKeyChecking","yes");
			//session.setConfig(config);
			session.setConfig("PreferredAuthentication", "publickey");
			session.setConfig("StrictHostKeyChecking","no");
			session.connect();

			Channel channel=session.openChannel("sftp");
			channel.connect();
			ChannelSftp channelSftp=(ChannelSftp)channel;
			channelSftp.cd(nexusDeploy.getScpwebapps1());

			FileInputStream innyStream=new FileInputStream(fileToTransfer);
			channelSftp.put(innyStream,finalFileName);

			channel.disconnect();
			session.disconnect();
			innyStream.close();	    		    
		} 
		catch (JSchException e) {
			e.printStackTrace();
			log.debug(e);
			retVal=false;
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": END: JSchException caught e.getMessage()="+e.getMessage());
			}

			DeployUtility.addABadMessage(finalFileName, messages,"<span class='failuremessage'>Could not upload "+finalFileName+" to "+prodServer+" JSch error: "+e.getMessage()+"</span>");
		}
		catch(SftpException e){
			e.printStackTrace();
			retVal=false;
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": END: SftpException caught e.getMessage()="+e.getMessage());
			}
			DeployUtility.addABadMessage(finalFileName, messages,"<span class='failuremessage'>Could not upload "+finalFileName+" to"+prodServer+" Sftp error: "+e.getMessage()+"</span>");
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
			retVal=false;
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": END: FileNotFoundException caught e.getMessage()="+e.getMessage());
			}
			DeployUtility.addABadMessage(finalFileName, messages,"<span class='failuremessage'>Could not upload "+finalFileName+" to "+prodServer+" File not found error: "+e.getMessage()+"</span>");

		}
		catch(IOException e){
			e.printStackTrace();
			retVal=false;
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": END: IOException caught e.getMessage()="+e.getMessage());
			}
			DeployUtility.addABadMessage(finalFileName, messages,"<span class='failuremessage'>Could not upload "+finalFileName+" to +"+prodServer+" Input/output error: "+e.getMessage()+"</span>");
		}
		catch(Throwable e){
			e.printStackTrace();
			retVal=false;
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": END: Thorwable caught e.getMessage()="+e.getMessage());
			}
			DeployUtility.addABadMessage(finalFileName, messages,"<span class='failuremessage'>Could not upload "+finalFileName+" to "+prodServer+" Unknown Throwable error: "+e.getMessage()+"</span>");
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END retVal="+retVal);
		}
		return retVal;
	}

	public static String doesWarHaveInternalFolderOnInternalDocsServer(String aSelectedApp)
			throws MalformedURLException, ProtocolException, IOException{
		String METHOD_NAME="doesWarHaveInternalFolderOnInternalDocsServer()";

		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:");
		}

		//We must make a rest call to staging internal to see if we should scp the .war to the internal server too
		String url=propsFile.getProperty("prodserverinternalraxinternalfolderquery", 
				"http://docs-internal/rax-doctools-services/rest/doctools/raxinternalfolder?webappfolder=");
		String retVal=null;
		InputStream inny=null;

		url+=aSelectedApp;
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": url="+url);
		}
		URL theURL=new URL(url);

		HttpURLConnection.setFollowRedirects(true);

		HttpURLConnection httpConn = (HttpURLConnection)theURL.openConnection();

		httpConn.setRequestMethod("GET");
		httpConn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");

		httpConn.setConnectTimeout(10000);
		httpConn.setReadTimeout(10000);

		inny=httpConn.getInputStream();		
		int readInt=-1;
		char readChar=' ';
		StringBuffer jsonResponse=new StringBuffer("");
		while(-1!=(readInt=inny.read())){
			readChar=(char)readInt;
			jsonResponse.append(readChar);
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+":~!@~!@~!@~!@~!@~@jsonResponse.toString()="+jsonResponse.toString());
		}
		JSONObject jsonObj=(JSONObject)JSONSerializer.toJSON(jsonResponse.toString());
		retVal=jsonObj.getString("hasinternalfolder");					
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+":~!@~!@~!@~!@~!@~@hasinternalfolder="+retVal);
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END: retVal="+retVal);
		}
		return retVal;
	}

	public static void addABadMessage(String finalFileName,Map<String,List<String>>messages, String message){
		if(null!=messages){
			String key=finalFileName+"-bad";
			List<String>badMessages=messages.get(key);
			if(null==badMessages){
				badMessages=new ArrayList<String>();
				messages.put(key, badMessages);
			}		
			badMessages.add(message);
		}
	}

	public static String getNodeVal(Element ele, String eleName){
		String retVal=null;
		NodeList nameNodes=ele.getElementsByTagName(eleName);
		if(null!=nameNodes){
			Node aNode=nameNodes.item(0);
			if(null!=aNode){
				NodeList children=aNode.getChildNodes();
				if(null!=children){
					Node node=children.item(0);
					if(null!=node){
						retVal=node.getNodeValue();
					}
				}
			}
		}
		return retVal;
	}

	public static String cleanMessage(String message){
		String retVal="";
		if(null!=message){
			String[] splitStr=message.split("<");
			StringBuffer strBuff=new StringBuffer("");
			for(String aStr:splitStr){
				if(!aStr.isEmpty()){
					int greaterThanCharIndex=aStr.indexOf(">");
					if(-1!=greaterThanCharIndex){
						strBuff.append(aStr.substring(greaterThanCharIndex+1));
					}
				}
			}
			retVal=strBuff.toString();			
		}
		return retVal;
	}

	
	public static void deleteFolderAndSubFolders(File dir){
		String METHOD_NAME="deleteFolderAndSubFolders()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+":START: ");
			log.debug(METHOD_NAME+": (null!=dir && dir.exists())="+(null!=dir && dir.exists()));
			if(null!=dir){
			    log.debug(METHOD_NAME+": dir="+dir.getAbsoluteFile());
			}
		}
		if(null!=dir && dir.exists()){
			if(dir.isDirectory()){
				File[] files=dir.listFiles();
				for(File aFile:files){
					deleteFolderAndSubFolders(aFile);
				}
				dir.delete();
			}
			else{
				dir.delete();
			}
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+":END:");
		}
	}

	public static void addSuccessMessages(HttpServletRequest request, String aSelectedAppWithWarSuffix, 
			Map<String,List<String>>messages, boolean deployment){
		String METHOD_NAME="buildSuccessMessages()";

		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:");
		}
		String serverName=request.getServerName();       	
		String prodServerurl="localhost:8080";       	

		if(serverName.contains(".rackspace.com")){
			if(serverName.contains("internal")){
				prodServerurl="docs-internal.rackspace.com";
			}
			else{
				prodServerurl="docs.rackspace.com";
			}
		}      	
		String autodeployUrl="http://"+prodServerurl;
		if(!autodeployUrl.endsWith("/")){
			autodeployUrl+="/";
		}
		autodeployUrl+=("index.jsp#"+aSelectedAppWithWarSuffix);
		String key=aSelectedAppWithWarSuffix+"-good";
		List<String>messagesList=messages.get(key);
		if(null==messagesList){
			messagesList=new ArrayList<String>();

			messages.put(key, messagesList);
			if(deployment){
				messagesList.add("<span class='successmessage'>Successfully deployed <a target='_blank' href='"+
						autodeployUrl+"'>"+aSelectedAppWithWarSuffix+"</a></span>");
			}
			else{
				messagesList.add("<span class='successmessage'>Successfully reverted to backup <a target='_blank' href='"+
						autodeployUrl+"'>"+aSelectedAppWithWarSuffix+"</a></span>");
			}
		}
		else{
			//We have already encountered a success message for this selected app, do not add another one.
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+"Added a success message for aSelectedAppWithWarSuffix"+aSelectedAppWithWarSuffix);
			log.debug(METHOD_NAME+": END:");
		}
	}

	public static void createWarFileInBackupWithTimeStamp(File warFileInBackupFolder, String webappsBackupFolderStr){
		String METHOD_NAME="createWarFileInBackupWithTimeStamp()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: warFileInBackupFolder.getAbsolutePath()="+warFileInBackupFolder.getAbsolutePath()+
					" webappsBackupFolderStr="+webappsBackupFolderStr);
		}
		String warFileName=warFileInBackupFolder.getName();
		String warFileNameWithoutDotWar=warFileName.substring(0,warFileName.lastIndexOf(".war"));

		try{
			String timestamp=DeployUtility.getTimeStamp(new ZipFile(warFileInBackupFolder));
			String warFileNameWithTimestamp=warFileNameWithoutDotWar+"-"+timestamp+".war";
			String warFileWithTimestampBackupFolder=webappsBackupFolderStr+warFileNameWithTimestamp;
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": warFileName="+warFileName);
				log.debug(METHOD_NAME+": warFileNameWithoutDotWar="+warFileNameWithoutDotWar);
				log.debug(METHOD_NAME+": warFileNameWithTimestamp="+warFileNameWithTimestamp);
				log.debug(METHOD_NAME+": warFileWithTimestampBackupFolder="+warFileWithTimestampBackupFolder);				
			}
			File warFileWithTimestamp=new File(warFileWithTimestampBackupFolder);
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+":warFileWithTimestamp.exists()="+warFileWithTimestamp.exists());
			}
			if(warFileWithTimestamp.exists()){
				warFileWithTimestamp.delete();
			}
			warFileWithTimestamp.createNewFile();
			DeployUtility.copyWarFileToNewFile(warFileInBackupFolder, warFileWithTimestamp);
		}
		catch(ZipException e){
			e.printStackTrace();
			log.debug(e);
		}
		catch(IOException e){
			e.printStackTrace();
			log.debug(e);
		}
		catch(Throwable e){
			e.printStackTrace();
			log.debug(e);
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}
	}

	public static String getTimeStampOld(ZipFile currentZipFile){
		String METHOD_NAME="getTimeStampOld()";
		String retVal="";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: retVal="+retVal);
		}
		Enumeration currentZipEntries=currentZipFile.entries();
		try{
			while(currentZipEntries.hasMoreElements()){
				ZipEntry aZipEntry=(ZipEntry)currentZipEntries.nextElement();
				String aZipEntryPathFileName=aZipEntry.getName();
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": aZipEntryPathFileName="+aZipEntryPathFileName);
					log.debug(METHOD_NAME+": aZipEntryPathFileName.endsWith(\"+warinfo.properties+\")="+
							aZipEntryPathFileName.endsWith("warinfo.properties"));
				}
				//We are only interested in the warinfo.properties 
				if(!aZipEntry.isDirectory() && aZipEntryPathFileName.endsWith("warinfo.properties")){
					Properties warinfoProps=new Properties();
					warinfoProps.load(currentZipFile.getInputStream(aZipEntry));
					String timestamp=warinfoProps.getProperty("timestamp");
					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": timestamp=" +timestamp);
					}
					if(null!=timestamp && !timestamp.isEmpty()){
						retVal=((timestamp.replaceAll(":", "-"))).trim().replaceAll(" ", "_");					
					}
					break;
				}
			}
		}
		catch(IOException e){
			e.printStackTrace();
			log.debug(e);
		}
		catch(Throwable e){
			e.printStackTrace();
			log.debug(e);
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END: retVal="+retVal);
		}
		return retVal;		
	}

	public static String getTimeStamp(ZipFile currentZipFile){
		String METHOD_NAME="getTimeStamp()";
		String retVal="";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: retVal="+retVal);
		}		
		try{
			ZipEntry aZipEntry=currentZipFile.getEntry("WEB-INF/warinfo.properties");
			if(null==aZipEntry){
				retVal=getTimeStampOld(currentZipFile);
			}
			else{
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": aZipEntry.getName()="+aZipEntry.getName());
				}
				Properties warinfoProps=new Properties();
				warinfoProps.load(currentZipFile.getInputStream(aZipEntry));
				String timestamp=warinfoProps.getProperty("timestamp");
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": timestamp=" +timestamp);
				}
				if(null!=timestamp && !timestamp.isEmpty()){
					retVal=((timestamp.replaceAll(":", "-"))).trim().replaceAll(" ", "_");					
				}
			}
		}
		catch(IOException e){
			e.printStackTrace();
			log.debug(e);
		}
		catch(Throwable e){
			e.printStackTrace();
			log.debug(e);
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END: retVal="+retVal);
		}		
		return retVal;		
	}

	public static void copyWarFileToNewFile(File selectedWarFile, File newTimeStampFile){
		String METHOD_NAME="copySelectedWarFileToBackupAsCurrent()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:");
		}
		if(newTimeStampFile.exists()){
			newTimeStampFile.delete();						
		}
		try{
			newTimeStampFile.createNewFile();
			FileOutputStream newTimeStampWarFile=new FileOutputStream(newTimeStampFile);
			ZipOutputStream zipFile=new ZipOutputStream(newTimeStampWarFile);

			FileInputStream fileInny=new FileInputStream(selectedWarFile);
			ZipInputStream zipInnyStream=new ZipInputStream(fileInny);

			ZipEntry anEntry=null;
			//Add all the entries 
			while(null!=(anEntry=zipInnyStream.getNextEntry())){
				String name=anEntry.getName();
				zipFile.putNextEntry(new ZipEntry(name));
				int len=-1;
				while(-1!=(len=zipInnyStream.read())){
					zipFile.write(len);
				}	
				zipFile.closeEntry();
			}
			zipInnyStream.close();
			fileInny.close();
			zipFile.close();
		}
		catch(FileNotFoundException e){
			e.printStackTrace();
			log.debug(e);
		}
		catch(IOException e){
			e.printStackTrace();
			log.debug(e);
		}
		catch(Throwable e){
			e.printStackTrace();
			log.debug(e);
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}
	}

	public static void touchRoot(String scpUser, String passwd, String known_hosts, String id_rsa, 
			String prodServer, String fullPathToRootWar){
		String METHOD_NAME="touchRoot()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: scpUser="+scpUser+" passwd="+passwd+" known_hosts="+known_hosts+" id_rsa="+id_rsa+
					" prodServer="+prodServer+" fullPathToRootWar="+fullPathToRootWar);
		}
		String command="touch "+fullPathToRootWar;
		jSchExec(command, prodServer, null);
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}
	}	

	public static void expandWars(boolean isExternal){
		String METHOD_NAME="expandWars()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:");
		}
	    String webappsFolderStr="";
	    if(isExternal){
	    	webappsFolderStr=propsFile.getProperty("webappfolder","/home/docs/Tomcat/latest/webapps");
	    }
	    else{
	    	webappsFolderStr=propsFile.getProperty("webappfolder","/home/docs/Tomcat/internal/latest/webapps");
	    }
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": webappsFolderStr="+webappsFolderStr);
		}
	    File webappsFolder=new File(webappsFolderStr);
	    if(webappsFolder.exists() && webappsFolder.isDirectory()){
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": webfolder exists!!!");
			}
	    	File[] webappsSubFolders=webappsFolder.listFiles();
	    	
	    	for(File aWebappSubFolder:webappsSubFolders){
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": aWebappSubFolder.getAbsolutePath()="+aWebappSubFolder.getAbsolutePath());
					log.debug(METHOD_NAME+": aWebappSubFolder.exists()="+aWebappSubFolder.exists());	
					log.debug(METHOD_NAME+": aWebappSubFolder.isDirectory()="+aWebappSubFolder.isDirectory());					
				}
	    		//Double check to make sure the directory exists and that it is a directory
	    		if(aWebappSubFolder.exists() && aWebappSubFolder.isDirectory()){
	    			File[] subFolderFilesAndFolders=aWebappSubFolder.listFiles();
	    			if(log.isDebugEnabled()){
	    				log.debug(METHOD_NAME+": aWebappSubFolder.getAbsolutePath()="+aWebappSubFolder.getAbsolutePath()+
	    				" subFolderFilesAndFolders.length="+subFolderFilesAndFolders.length);
	    			}
	    			//If there is 1 or less file/folder then we need to delete the entire folder
	    			if(subFolderFilesAndFolders.length<=1){
	    				deleteFolderAndSubFolders(aWebappSubFolder);
	    				expandWar(aWebappSubFolder);
	    			}
	    		}
	    	}
	    }
	    else{
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": webappsFolder: "+webappsFolder.getAbsolutePath()+ " does NOT exists");
			}
	    }
	    if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}	    
	}

	public static void expandWar(File aWebappFolder){
		String METHOD_NAME="expandWar()";
	    if(log.isDebugEnabled()){
	    	log.debug(METHOD_NAME+": START:");
	    }
	    if(null!=aWebappFolder){
	    	try {
	    		String theWarFileStr=(aWebappFolder.getAbsolutePath()+".war");
	    		File theWarFile=new File(theWarFileStr);
    			if(log.isDebugEnabled()){
    				log.debug(METHOD_NAME+": theWarFileStr="+theWarFileStr);
    				log.debug(METHOD_NAME+": aWebappFolder.getAbsolutePath()="+aWebappFolder.getAbsolutePath());
    				log.debug(METHOD_NAME+": theWarFile.exist()="+theWarFile.exists());
    			}
	    		if(theWarFile.exists()){
		    		String theWebAppFolder=aWebappFolder.getAbsolutePath();
		    		if(!theWebAppFolder.endsWith("/")){
		    			theWebAppFolder+="/";
		    		}
	    			if(log.isDebugEnabled()){
	    				log.debug(METHOD_NAME+": theWebAppFolder="+theWebAppFolder);
	    			}
	    			ZipFile zipFile=new ZipFile(theWarFileStr);

	    			Enumeration zipEnums=zipFile.entries();
	    			int count=0;

	    			while(zipEnums.hasMoreElements()){
	    				ZipEntry aZipEntry=(ZipEntry)zipEnums.nextElement();
	    				String aZipEntryName=aZipEntry.getName();
	    				String aZipEntryFolderName=aZipEntryName.substring(0,(aZipEntryName.lastIndexOf("/")+1));
	    				String newFolderName=theWebAppFolder+aZipEntryFolderName;

	    				File folder=new File(newFolderName);
	    				if(log.isDebugEnabled()){
	    					log.debug(METHOD_NAME+": aZipEntryName="+aZipEntryName);
	    					log.debug(METHOD_NAME+": aZipEntryFolderName="+aZipEntryFolderName);
	    					log.debug(METHOD_NAME+": newFolderName="+newFolderName);
	    					log.debug(METHOD_NAME+": folder.exists()="+folder.exists());
	    				}
	    				if(aZipEntry.isDirectory()){
	    					if(!folder.exists()){
	    						folder.mkdirs();
	    					}
	    				}
	    				String zipFileName=theWebAppFolder+aZipEntryName;

	    				if(log.isDebugEnabled()){
	    					log.debug(METHOD_NAME+": zipFileName="+zipFileName);
	    				}
	    				File aFile=new File(zipFileName);

	    				InputStream inny=zipFile.getInputStream(aZipEntry);
	    				FileOutputStream tempWarFileOutStream=null;
	    				try{
	    					tempWarFileOutStream=new FileOutputStream(aFile);
	    				}
	    				catch(FileNotFoundException e){
	    					//e.printStackTrace();
	    					log.debug(e);
	    					if(log.isDebugEnabled()){
	    						log.debug(METHOD_NAME+": could not find aFile: "+aFile.getAbsolutePath());
	    						log.debug(METHOD_NAME+": aFile.isFile()= "+aFile.isFile());
	    					}
	    					String parentFolderStr=aFile.getAbsolutePath();
	    					int lastSlashIndex=parentFolderStr.lastIndexOf("/");
	    					if(-1!=lastSlashIndex){
	    						parentFolderStr=parentFolderStr.substring(0,lastSlashIndex);

	    						File parentFolder=new File(parentFolderStr);
	    						if(log.isDebugEnabled()){
	    							log.debug(METHOD_NAME+": parentFolderStr="+parentFolderStr);
	    							log.debug(METHOD_NAME+": parentFolder.exists()="+parentFolder.exists());
	    						}
	    						if(!parentFolder.exists()){
	    							parentFolder.mkdirs();
	    							tempWarFileOutStream=new FileOutputStream(aFile);
	    						}
	    					}
	    				}
	    				int read=-1;
	    				//Write out the compress .war file to the tempdir
	    				while(-1!=(read=inny.read())){
	    					tempWarFileOutStream.write(read);
	    				}
	    				if(null!=tempWarFileOutStream){
	    					tempWarFileOutStream.close();
	    				}
	    				if(null!=inny){
	    					inny.close();
	    				}
	    				++count;
	    			}
	    			if(log.isDebugEnabled()){
	    				log.debug(METHOD_NAME+": %%%%%expanded "+count+" files/folders.");
	    			}
	    		}
	    	} 
	    	catch (ZipException e) {
	    		e.printStackTrace();
	    		log.debug(e);
	    	} 
	    	catch (IOException e) {
	    		e.printStackTrace();
	    		log.debug(e);
	    	}
	    	catch(Throwable e){
	    		e.printStackTrace();
	    		log.debug(e);
	    	}
	    }
	    if(log.isDebugEnabled()){
	    	log.debug(METHOD_NAME+": END:");
	    }
	}
}
