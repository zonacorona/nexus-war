package com.rackspace.cloud.api;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


public class NexusArtifactSaxHandler extends DefaultHandler {

	private static Logger log = Logger.getLogger(NexusArtifactSaxHandler.class);
	private List<NexusContentItem>artifacts;

	private boolean lastModified=false;
	private boolean resourceURI=false;
	private boolean size=false;
	private boolean fileName=false;

	private NexusContentItem aContent;
	private NexusContentItem latestJarContent=null;
	private NexusContentItem latestSha1Content=null;
	private NexusContentItem latestSnapShot=null;
	

	public enum ContentType{JAR, WAR, SHA1, SNAPSHOT};

	public NexusContentItem getLatestJarContent(){
		if(null==latestJarContent){
			this.latestJarContent=this.getLatestContentItem(ContentType.JAR);
		}
		return this.latestJarContent;
	}
	
	public NexusContentItem getLatestWarContent(){
		if(null==latestJarContent){
			this.latestJarContent=this.getLatestContentItem(ContentType.WAR);
		}
		return this.latestJarContent;		
	}

	public NexusContentItem getLatestSha1Content(){
		if(null==latestSha1Content){
			this.latestSha1Content=this.getLatestContentItem(ContentType.SHA1);
		}
		return this.latestSha1Content;
	}

	public NexusContentItem getLatestSnapShot(){
		if(null==latestSnapShot){
			this.latestSnapShot=this.getLatestContentItem(ContentType.SNAPSHOT);
		}
		return this.latestSnapShot;
	}

	//Example url form that returns the xml that we are parsing is:
	//http://198.101.202.99:8081/nexus/service/local/repositories/rax-docs-repo/content/com/rackspace/cloud/files/api/files-docs/1.0.0-SNAPSHOT/
	public void startElement(String uri, String localName,String qName, 
			Attributes attributes) throws SAXException{
		if(qName.equalsIgnoreCase("content")){
			this.artifacts=new ArrayList<NexusContentItem>();
		}
		if(qName.equalsIgnoreCase("content-item")){
			this.aContent=new NexusContentItem();
		}
		if(qName.equalsIgnoreCase("resourceURI")){
			this.resourceURI=true;			
		}
		if(qName.equalsIgnoreCase("sizeOnDisk")){
			this.size=true;
		}
		if(qName.equalsIgnoreCase("lastModified")){
			this.lastModified=true;
		}
		if(qName.equalsIgnoreCase("text")){
			this.fileName=true;
		}
	}

	public void characters(char ch[], int start, int length)
			throws SAXException {

		if(this.resourceURI){
			String uri=new String(ch, start, length);
			//we only car about .jar, .war, and  resources and the sha1 info			
			if(uri.endsWith("includewars.jar") || uri.endsWith(".pom.sha1")||uri.endsWith("-SNAPSHOT/")||uri.endsWith(".war")){
				//				if(uri.endsWith("-SNAPSHOT/")&&null!=logWriter){
				//					logWriter.write("~~~~~~~~~~~uri="+uri);
				//				}
				this.aContent.setResourceURI(uri);
				this.artifacts.add(this.aContent);	
				this.resourceURI=false;
			}
		}
		if(this.lastModified){
			String modified=new String(ch,start,length);
			this.aContent.setLastModified(modified);
			this.lastModified=false;
		}
		if(this.size){
			String strSize=new String(ch,start,length);
			Integer bigISize=Integer.valueOf(strSize);
			this.aContent.setSize(bigISize);
			this.size=false;
		}
		if(this.fileName){
			String nameOfFile=new String(ch,start,length);
			this.aContent.setFileName(nameOfFile);
			this.fileName=false;	 
		}
	}

	public List<NexusContentItem>getArtfacts(){
		if(null==this.artifacts){
			this.artifacts=new ArrayList<NexusContentItem>();
		}
		return this.artifacts;
	}

	public String toString(){
		return "{lastModified="+this.lastModified+", resourceURI="+this.resourceURI+", size="+this.size+", fileName="+this.fileName+"}";
	}


	//Iterate through all the items and return the latest item, if isLookingForLatestJar is true then 
	//return the latest NexustContentItem for an item ending in .jar, if false we are returning an item
	//that ends in .pom.sha1
	private NexusContentItem getLatestContentItem(ContentType lookfor){
		NexusContentItem retVal=null;
		String METHOD_NAME="getLatestContentItem()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: lookfor="+lookfor);
		}
		if(this.artifacts!=null && artifacts.size()>0){
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": artifacts.size()="+artifacts.size());
			}
			//The list contains more than 1 item, iterate and compare each to find the largest
			if(this.artifacts.size()>0){
				//The Nexus repository will always contain the .war compressed in the .jar archive
				//even if the docbook project only has 1 .war
				//Check to see if the first item contains a reference to a .jar

				retVal=this.artifacts.get(0);
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": retVal="+retVal);
				}
				String retValResourceURI=retVal.getResourceURI();

				if(null!=retValResourceURI){
					retValResourceURI=retValResourceURI.trim();
				}
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": retValResourceURI="+retValResourceURI);
				}
				for(int i=1;i<this.artifacts.size();++i){

					NexusContentItem iItem=this.artifacts.get(i);
					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": iItem.getResourceURI()="+iItem.getResourceURI());
					}
					long iItemTime=-1L;
					long retValTime=-1L;
					String theResourceURI=iItem.getResourceURI();
					if(null!=theResourceURI){
						theResourceURI=theResourceURI.trim();
					}

					//We want the latest .jar
					if(lookfor==ContentType.JAR){						 
						if(iItem.getResourceURI().endsWith("includewars.jar")){
							String retResourceURI=retVal.getResourceURI();
							if(null!=retResourceURI){
								retResourceURI=retResourceURI.trim();
							}
							if(!retResourceURI.endsWith("includewars.jar")){
								retVal=iItem;
							}
							else{
								iItemTime=getLongFromDateStr(iItem.getLastModified());

								retValTime=getLongFromDateStr(retVal.getLastModified());
								if(log.isDebugEnabled()){
									log.debug(METHOD_NAME+": iItemTime="+iItemTime);
									log.debug(METHOD_NAME+": retValTime="+retValTime);
								}
								if(iItemTime!=-1L && retValTime!=-1L && iItemTime>retValTime){
									retVal=iItem;						 
								}
							}
						}
					}
					else if(lookfor==ContentType.SHA1){
						if(iItem.getResourceURI().endsWith(".pom.sha1")){
							String retResourceURI=retVal.getResourceURI();
							if(null!=retResourceURI){
								retResourceURI=retResourceURI.trim();
							}
							if(!retResourceURI.endsWith(".pom.sha1")){
								retVal=iItem;
							}
							else{
								iItemTime=getLongFromDateStr(iItem.getLastModified());
								retValTime=getLongFromDateStr(retVal.getLastModified());
								if(iItemTime!=-1L && retValTime!=-1L && iItemTime>retValTime){
									retVal=iItem;						 
								}
							}
						}
					}
					else if(lookfor==ContentType.WAR){
						if(iItem.getResourceURI().endsWith(".war")){
							String retResourceURI=retVal.getResourceURI();
							if(log.isDebugEnabled()){
								log.debug(METHOD_NAME+": ~~~iItem.getResourceURI()="+iItem.getResourceURI());
								log.debug(METHOD_NAME+": ~~~retResourceURI="+retResourceURI);
								log.debug(METHOD_NAME+": ~~~(null!=retResourceURI && !retResourceURI.trim().endsWith(\".war\"))="+
								(null!=retResourceURI && !retResourceURI.trim().endsWith(".war")));
							}
							if(null!=retResourceURI && !retResourceURI.trim().endsWith(".war")){
								retVal=iItem;
							}
							else{
								iItemTime=getLongFromDateStr(iItem.getLastModified());
								retValTime=getLongFromDateStr(retVal.getLastModified());
								if(iItemTime!=-1L && retValTime!=-1L && iItemTime>retValTime){
									retVal=iItem;						 
								}
							}
						}						
					}
					//For finding the latest snapshot value, we must look at the snap shot version
					//value and not the date, although a later snapshot value should have a later
					//latest modified value
					else{

						if(iItem.getResourceURI().endsWith("-SNAPSHOT/")){
							String retValSnapshotVal=retVal.getFileName();
							String iItemSnapShotVal=iItem.getFileName();

							int comparedVal=retValSnapshotVal.compareTo(iItemSnapShotVal);
							if(log.isDebugEnabled()){
								log.debug(METHOD_NAME+": retValSnapshotVal="+retValSnapshotVal);
								log.debug(METHOD_NAME+": iItemSnapShotVal="+iItemSnapShotVal);
								log.debug(METHOD_NAME+": (retValSnapshotVal.compareTo(iItemSnapShotVal))="+comparedVal);
							}
							//The retVal has a value "less than" the current iItem value
							if(comparedVal<0){
								retVal=iItem;
							}
						}
					}
				}				 

			}

		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END: retVal="+retVal);
		}
		return retVal;
	}


	public static long getLongFromDateStr(String aDate){
		long retVal=-1;
		if(null!=aDate){
			String aDateStr=aDate.trim();
			if((aDateStr.toLowerCase()).endsWith("utc")){
				aDateStr=aDateStr.substring(0,((aDateStr.length())-3));
			}
			SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			try {
				Date d=sdf.parse(aDateStr);
				retVal=d.getTime();

			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return retVal;
	}

}
