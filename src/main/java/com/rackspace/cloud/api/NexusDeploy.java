package com.rackspace.cloud.api;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.rackspace.cloud.api.dao.ILatestwardeployDao;
import com.rackspace.cloud.api.entity.Latestwardeploy;

@Component
public class NexusDeploy {
	private static Properties propsFile=null;
	private static Logger log = null;//Logger.getLogger(NexusDeploy.class);
	private static Object lock=new Object();
	private long ADDED_SECONDS=(20*1000);
	
	private String scpUser;
	private String scpwebapps1;
	private String passwd;
	private String known_hosts;
	private String id_rsa;		
	private String serversToScp;
	private String externalOrInternal;
	private Map<String, File>webAppWarFilesAndFoldersMap;
	private List<String>serversToScpList;
	private List<String>requestUrls;
	private String tempFolder;
	private String installedWebAppDirStr;

	
	public static void main(String[]args){	
		
		ApplicationContext ctx=new ClassPathXmlApplicationContext("spring-config.xml");
		ILatestwardeployDao dao=(ILatestwardeployDao)ctx.getBean("latestDeployWarDao");
		
		
		Latestwardeploy aNewDeployment=new Latestwardeploy("newWar.war");
		aNewDeployment.setGroupid("com.groupid");
		aNewDeployment.setArtifactid("com.artifactid");
		SimpleDateFormat df=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date theDate=new Date();
		
		String theDateStr=df.format(theDate);

		System.out.println("After setting UTC timezone theDateStr="+theDateStr);

		try {
			df.setTimeZone(TimeZone.getTimeZone("UTC"));
			theDate=df.parse(theDateStr);
			
			aNewDeployment.setLatest(theDate);
		} catch (ParseException e) {
			
			e.printStackTrace();
		}
		aNewDeployment.setNexuslatestincludewawrsjar("http://docs-staging.rackspace.com:8081/nexus/service/local/repositories/docs-staging/content/com/rackspace/cloud/apidocs/loadbalancers-docs/1.0.0-SNAPSHOT/loadbalancers-docs-1.0.0-20140921.034350-131-includewars.jar");
	    aNewDeployment.setTimezone("UTC");
	    aNewDeployment.setServerenv("ext");
	    
	    
	    aNewDeployment=dao.findById(6);
	    Date theNewDate=aNewDeployment.getLatest();
	    df.setTimeZone(TimeZone.getTimeZone("UTC"));
	    
	    
	    
	    System.out.println("~~~~~~~~theNewDate="+df.format(theNewDate));
	    
	    
	    //dao.save(aNewDeployment);
		
		
		List<Latestwardeploy>deployedWars=dao.findAll();
		
		for(Latestwardeploy aDeployedWar:deployedWars){
			System.out.println("{warName="+aDeployedWar.getWarname()+", artifactId="+aDeployedWar.getArtifactid()+
					", groupId="+aDeployedWar.getGroupid()+", latestincludewawrsjar="+aDeployedWar.getNexuslatestincludewawrsjar()+
					", latest="+aDeployedWar.getLatest()+", timezone="+aDeployedWar.getTimezone()+"}");
		}
		
		SimpleDateFormat dateFormatter=new SimpleDateFormat("MM/dd/yyyy:HH:mm:ss");
		TimeZone cstTime = TimeZone.getTimeZone("America/Chicago");
		dateFormatter.setTimeZone(cstTime);
		
		log.debug("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%Started running at: "+dateFormatter.format(Calendar.getInstance().getTimeInMillis()));
		runDeploy(args);
		log.debug("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%Finished running at: "+dateFormatter.format(Calendar.getInstance().getTimeInMillis()));
	}
	
	
	private static void runDeploy(String[] args){
		String METHOD_NAME="runDeploy()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:");
		}
		NexusDeploy deploy=new NexusDeploy();
		try{
			if(log.isDebugEnabled()){
				log.debug("args.lenght="+args.length);
			}
			//If the props.properties file is indicated as part of the execution, use that instead of the one
			//found in the .jar
			if(args.length>0){
				if(log.isDebugEnabled()){
					log.debug("args.length is greater than 0 setting propFileName="+args[0]);
				}
				//propFileName will will be either rax-deploy-nexus-war.properties or rax-deploy-nexus-warInternal.properties
				String propFileName=args[0];
				deploy.loadProps(propFileName);				
				//although we can pull from eiter docs-staging or docs-deploy repos, currently we are 
				//only using the docs-staging repo:
				//http://docs-staging.rackspace.com:8081/nexus/index.html#view-repositories;docs-staging~browsestorage
				deploy.initialize(args,false);
				deploy.updateWars();
			}
			else{
				log.debug("ERROR: usage: java -cp $PATH_TO_RAX_DEPLOY_NEXUS_WAR_JAR/rax-deploy-nexuswar.jar:$PATH_TO_DEPLOYWARS_DIR/DeployWars com.rackspace.cloud.api/NexusDeploy $PATH_TO_PROPS_FILE/rax-deploy-nexus-war.properties");				
			}
		}
		catch(IOException e){
			e.printStackTrace();
			log.debug(METHOD_NAME+": IOException caught e.getMessage()="+e.getMessage());			
			log.debug(e);
		}	
		catch(Throwable e){
			log.debug(METHOD_NAME+": Throwable caught e.getMessage()="+e.getMessage());
			log.debug(e);
		}
		if(args.length>0){
			String propFileName=args[0];
			if(null!=propFileName){
				if(propFileName.contains("DeployWarsInternal")){
		            DeployUtility.expandWars(false);
				}
				else{
					DeployUtility.expandWars(true);
				}
			}
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}
	}
	
	public NexusDeploy(){
		super();
		String METHOD_NAME="NexusDeploy() Constructor";
		if(null==NexusDeploy.propsFile){
			synchronized(lock){
				if(null==NexusDeploy.propsFile){
					NexusDeploy.propsFile=new Properties();
					//File theFile=new File("/home/docs/DeployWars/props.properties");
					InputStream innyStream=NexusDeploy.class.getClassLoader().getResourceAsStream("props.properties");
					try {
						File afile=new File("/home/docs/DeployWars/rax-deploy-nexus-war.properties");
						//rax-deploy-nexus-war.properties should override prop.properties
						if(afile.exists()){
						    innyStream=new FileInputStream(afile);
						    NexusDeploy.propsFile.load(innyStream);
						}
						NexusDeploy.propsFile.load(innyStream);					
					} 
					catch (FileNotFoundException e) {
						e.printStackTrace();
						log.debug(METHOD_NAME+": FileNotFoundException caught e.getMessage()="+e.getMessage());
						log.debug(e);
					} 
					catch (IOException e) {
						e.printStackTrace();
						log.debug(METHOD_NAME+": IOException caught e.getMessage()="+e.getMessage());
						log.debug(e);
					}
					catch(Throwable e){
						e.printStackTrace();
						log.debug(METHOD_NAME+": Throwable caught e.getMessage()="+e.getMessage());
						log.debug(e);
					}
					
				}
			}
		}		
	}

	//We have two repositories, docs-staging and docs-deploy
	//When isProd is true we use the docs-deploy repo when false we use the docs-staging repo
	private void initialize(String[] args, boolean isProd)
			throws IOException{
		String METHOD_NAME="initialize()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:\n");
		}
		//Clear everything
		this.clearMembers();
		this.externalOrInternal=args[0];
		//Credentials for user
		this.scpUser=(propsFile.getProperty("scpuser","docs")).trim();							
		this.scpwebapps1=(propsFile.getProperty("scpwebapps1","/home/docs/Tomcat/latest/scpwebapps1/")).trim();
		this.passwd=(propsFile.getProperty("docspasswd","Fanatical7")).trim();
		this.known_hosts=(propsFile.getProperty("knownhosts","/home/docs/.ssh/known_hosts")).trim();
		this.id_rsa=(propsFile.getProperty("idrsa","/home/docs/.ssh/id_rsa")).trim();		
		this.serversToScp=(propsFile.getProperty("scpservers","docs-prod-2~docs-prod-3~docs-prod-4"));
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": this.externalOrInternal="+this.externalOrInternal);
			log.debug(METHOD_NAME+": this.scpwebapps1="+this.scpwebapps1);
			log.debug(METHOD_NAME+": this.known_hosts="+this.known_hosts);
			log.debug(METHOD_NAME+": this.id_rsa="+this.id_rsa);
			log.debug(METHOD_NAME+": this.serversToScp="+this.serversToScp);
		}
		//These are the production servers, note their names must be
		//reachable on the network, so make sure /etc/hosts has been edited as needed
		String[] serversToScpArray=serversToScp.split("~");
		
		//Although we are updating content on the staging servers, once we have finished
		//updating the staging server, we want to upload the latest war to all the production
		//node as a cache so that when deploy occurs the respective .war is ready on the 
		//production server to be copied to the webapps folder instead of having to uploaded
		//at the time of deployment
		this.serversToScpList=new ArrayList<String>();
		
		for(String aServer:serversToScpArray){
		    if(!aServer.trim().isEmpty()){
		    	serversToScpList.add(aServer);		    	
		    }
		}
		try{
			//This gives us all the latest snap-shot url's for all the books on our nexus repo
			//server
			this.requestUrls=this.getAllUrls(isProd);
			//webappfolder will be either the be internal or external depending on which command
			//the cron job calls
			this.installedWebAppDirStr=(NexusDeploy.propsFile.getProperty("webappfolder",
					"/home/docs/Tomcat/latest/webapps/")).trim();
			if(!this.installedWebAppDirStr.endsWith("/")){
				this.installedWebAppDirStr+="/";
			}
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": this.installedWebAppDirStr="+this.installedWebAppDirStr);
			}
			//Now that we have the webapps folder, we should cache all the .war files as keys and the corresponding folder name
			//as the value, if a folder does not have a corresponding .war then just use the folder name as the key and value
			this.webAppWarFilesAndFoldersMap=getWebAppWarFilesAndFolders(this.installedWebAppDirStr);			

			if(log.isDebugEnabled()){
				Set<String>keys=this.webAppWarFilesAndFoldersMap.keySet();
				for(String aKey:keys){
					log.debug(METHOD_NAME+": this.webAppWarFilesAndFoldersMap.get("+aKey+")="+this.webAppWarFilesAndFoldersMap.get(aKey).getAbsolutePath()+
					" webAppWarFilesAndFoldersMap.get("+aKey+").isDirectory="+this.webAppWarFilesAndFoldersMap.get(aKey).isDirectory());
				}
				log.debug(METHOD_NAME+": webAppWarFilesAndFoldersMap.size()="+this.webAppWarFilesAndFoldersMap.size());
			}
			
			this.tempFolder="";
			if(this.externalOrInternal.contains("DeployWarsInternal")){
				this.tempFolder=(NexusDeploy.propsFile.getProperty("tempfolder","/home/docs/DeployWarsInternal/Temp/")).trim();
			}
			else{
				this.tempFolder=(NexusDeploy.propsFile.getProperty("tempfolder","/home/docs/DeployWars/Temp/")).trim();
			}
			if(!this.tempFolder.endsWith("/")){
				this.tempFolder+="/";
			}
			File tempFolderFolder=new File(this.tempFolder);
			//Make sure the temp folder exists
			if(!tempFolderFolder.exists()){
			    tempFolderFolder.mkdirs();
			}
		}
		catch(ProtocolException e){
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": Authentication Failed"+e.getMessage());	
			}			
			e.printStackTrace();
			log.debug(METHOD_NAME+": ProtocolException caught e.getMessage()="+e.getMessage());
			log.debug(e);
		}
		catch(MalformedURLException e){
			log.debug(e);
			log.debug(METHOD_NAME+": MalformedURLException caught e.getMessage()="+e.getMessage());
			log.debug(e);
		}
		catch(Throwable e){
			e.printStackTrace();
			log.debug(METHOD_NAME+": Throwable caught e.getMessage()="+e.getMessage());
			log.debug(e);
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}
	}
	
	private void updateWars(){
		String METHOD_NAME="updateWars()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:");
		}
		String requestUrl="";
		//Each request url is of the form:
		//http://docs-staging.rackspace.com:8081/nexus/service/local/repositories/docs-staging/content/com/rackspace/cloud/api/auth-doc-1x/1.1.2-SNAPSHOT/~target/docbkx/webhelp:cloudfiles-v1.0-cf-devguide.war*cloudfiles-v1.0-cf-intro.war*cloudfiles-v1.0-cf-releasenotes.war
		//the ~ character delimits the repository url from the .jar meta data information
		//the : character delimits information of where the .war files are located in the compressed .jar file (on the lhs)
		//and the names of the individual .war file names (on the rhs)
		//the * character delimits each .war file name located in the compress .jar file
		for(String urlAndWar:this.requestUrls){

			//urlAndWar contains the url to the latest SNAPSHOT of a given repo and the .war's for that repo we should process, for example:
			//urlAndWar=http://docs-staging.rackspace.com:8081/nexus/service/local/repositories/docs-staging/content/com/rackspace/cloud/apidocs/autoscale/1.0.0-SNAPSHOT/~target/docbkx/webhelp:cas-v1.0-autoscale-devguide.war*cas-v1.0-autoscale-gettingstarted.war			
			String[] urlAndWarArr=urlAndWar.split("~");

			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": urlAndWar="+urlAndWar);
			}

			if(null!=urlAndWarArr){
				requestUrl=urlAndWarArr[0];

				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": requestUrl="+requestUrl);
					log.debug(METHOD_NAME+": urlAndWarArr.length="+urlAndWarArr.length);
				}

				if(urlAndWarArr.length==2){
					//jarMetaData should of the form:
					//target/docbkx/webhelp:cloudfiles-v1.0-cf-devguide.war*cloudfiles-v1.0-cf-intro.war*cloudfiles-v1.0-cf-releasenotes.war
					//or rax-headerservice.war
					String jarMetaData=urlAndWarArr[1];						

					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": jarMetaData="+jarMetaData);
					}
					try{
					    this.getWarsAndDeploy(requestUrl,jarMetaData);
					}
					catch(FileNotFoundException e){
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": Could not find resource from the URL: "+requestUrl);
						}
						e.printStackTrace();
						log.debug(METHOD_NAME+": FileNotFoundException caught e.getMessage()="+e.getMessage());
						log.debug(e);
					}
					catch(IOException e){
						e.printStackTrace();
						log.debug(METHOD_NAME+": IOException caught e.getMessage()="+e.getMessage());
						log.debug(e);
					}
				}
			}
		}	
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}
	}
	
	//The requestUrl always points to the root of all the snapshots on the nexus server, for example:
	//http://docs-staging.rackspace.com:8081/nexus/service/local/repositories/docs-staging/content/com/rackspace/cloud/api/service-registry/
	//From this point, we have to get the latest snapshot and build version for the given repo
	private void getWarsAndDeploy(String requestUrl, String jarMetaData)throws MalformedURLException, IOException{
		String METHOD_NAME="getWarsAndDeploy()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: requestUrl="+requestUrl+" jarMetaData="+jarMetaData);
		}		
		//String warFolderInJar="target/docbkx/webhelp/";
		String warFolderInJar=null;
		String[] jarMetaDataArr=jarMetaData.split(":");
		String warFileNames=jarMetaDataArr[0];
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": jarMetaDataArr.length="+jarMetaDataArr.length);
		}
		if(jarMetaDataArr.length==2){
			warFolderInJar=jarMetaDataArr[0];
			if(null!=warFolderInJar){
				warFolderInJar=warFolderInJar.trim();
			}
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": warFolderInJar="+warFolderInJar);
			}
			if(!warFolderInJar.isEmpty() && !warFolderInJar.endsWith("/")){
				warFolderInJar+="/";
			}
			warFileNames=jarMetaDataArr[1];
		}
		//There is no ":" in jarMeataData, this occurs when this is a header/feedback nexus deploy
		else{
			warFileNames=jarMetaData;
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": warFolderInJar="+warFolderInJar+" warFileNames="+warFileNames);
		}
		//We now have all the war's that are associated with this archive docbook on the Nexus server
		String[] warFileNamesArr=warFileNames.split("\\*");
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": warFileNamesArr.length="+warFileNamesArr.length);
			for(String aWar:warFileNamesArr){
				log.debug(METHOD_NAME+": aWar="+aWar);
			}
		}

		ZipFile latestNexusFileZip=null;

		try {
		
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": warFileNames.trim()="+warFileNames.trim());
			}			
			NexusArtifactSaxHandler nexusHandler=new NexusArtifactSaxHandler();
			//Look for the latest jar or war
			NexusContentItem latestJarContent=getLatestIncludeWarsJarFileUrlFromNexus(nexusHandler,requestUrl,warFileNames);

			if(null!=latestJarContent){
				if(null!=this.tempFolder && !this.tempFolder.isEmpty()){
					if(!this.tempFolder.endsWith("/")){
						this.tempFolder+="/";
					}
				}
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": latestContent="+latestJarContent);
					log.debug(METHOD_NAME+": tempFolder="+this.tempFolder);
				}				
				//The file always points to the latest *includewwars.jar file on the nexus server, for example:
				//http://docs-staging.rackspace.com:8081/nexus/service/local/repositories/docs-staging/content/com/rackspace/cloud/api/auth-doc-1x/1.1.2-SNAPSHOT/auth-doc-1x-1.1.2-20140822.215729-26-includewars.jar
				String theFile=latestJarContent.getResourceURI();
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": theFile="+theFile);
					log.debug(METHOD_NAME+": (theFile.toLowerCase().endsWith(\".jar\")||theFile.toLowerCase().endsWith(\".war\"))="+
							(theFile.toLowerCase().endsWith(".jar")||theFile.toLowerCase().endsWith(".war")));
				}
				//theFile must be a .jar file
				if(theFile.toLowerCase().endsWith(".jar")){

					List<File>appendedWarFiles=new ArrayList<File>();
					Date now=new Date();
					
					SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");					
					String folderNow=sdf.format(now);
					//Do it again just to try to ensure that the folder is unique
					now=new Date();
					
					Long timeNow=now.getTime();
					
					folderNow+=("-"+timeNow.toString());
					String tempFolderNow=this.tempFolder+folderNow;
					File tempFolderNowFolder=new File(tempFolderNow);
					if(tempFolderNowFolder.exists()){
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": deleting entire folder="+tempFolderNowFolder.getAbsolutePath());
						}
						DeployUtility.deleteFolderAndSubFolders(tempFolderNowFolder);
					}
					//Create a unique temp folder, that way we know from the logs what folder
					//was created for a respective deployment to staging
					tempFolderNowFolder.mkdirs();
					String jarFileFullPath=tempFolderNow+"/"+latestJarContent.getFileName();
					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": tempFolderNow="+tempFolderNow);
						log.debug(METHOD_NAME+": jarFileFullPath="+jarFileFullPath);
					}
					File jarFileFullPathFile=null;
					Map<String,ZipEntry>latestJarWars=null;														
					
					//Iterate through all the selected .war files
					for(String aWar:warFileNamesArr){
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": aWar="+aWar);
						}

						if(null!=latestJarContent && this.shouldDeployLatestWar(latestJarContent, aWar)){

							//The first time we should deploy a war, we need to get the latest *-include-wars.jar
							//from nexus
							if(null==jarFileFullPathFile){
								jarFileFullPathFile=new File(jarFileFullPath);
								
								jarFileFullPathFile=this.getLatestJarFileFromNexus(nexusHandler, tempFolderNow);
								
								if(log.isDebugEnabled()){
									log.debug(METHOD_NAME+": jarFileFullPathFile.exists()="+(jarFileFullPathFile.exists()));
								}
								latestNexusFileZip=new ZipFile(jarFileFullPathFile);
								if(theFile.endsWith(".jar")){
									latestJarWars=getAllWarEntries(latestNexusFileZip);
								}
								if(log.isDebugEnabled() && latestJarWars!=null){
									log.debug(METHOD_NAME+": ^^^^^^latestJarWars: START");
									for(String aKey:latestJarWars.keySet()){
										log.debug(METHOD_NAME+": latestJarWars.get("+aKey+").getName()="+latestJarWars.get(aKey).getName());
									}
									log.debug(METHOD_NAME+": ^^^^^^latestJarWars: STOP");
								}								
							}
							deployWar(latestNexusFileZip, latestJarWars, nexusHandler, tempFolderNow, 
									aWar, warFolderInJar, appendedWarFiles,jarFileFullPathFile);
							//private void deployWar(ZipFile latestNexusFileZip, Map<String, ZipEntry>latestJarWarsMap, NexusArtifactSaxHandler nexusHandler, 
									//String tempFolderNow, String webAppPath, String warName, String warFolderInJar,
									//List<File>appendedWarFiles, File latestNexusFile) 
							//At this point appendedWarFiles contains all the wars that we need to scp to
							//all the production servers
							if(log.isDebugEnabled()){
								log.debug(METHOD_NAME+": this.externalOrInternal="+this.externalOrInternal+" appendedWarFiles.size()="+appendedWarFiles.size());												
							}

							//Now we have to scp the files to each of the production nodes in preparation or a future deploy
							//to production
							for(File aFile:appendedWarFiles){
								String fileName=aFile.getName();

								for(String aServer:this.serversToScpList){														
									if(log.isDebugEnabled()){
										if(!this.externalOrInternal.contains("DeployWarsInternal")){
											log.debug(METHOD_NAME+":External: scping files to server:"+aServer+" fileName:"+fileName+" to folder:"+scpwebapps1+" scpUser: "+scpUser);
										}
										else{
											log.debug(METHOD_NAME+":Internal: scping files to server:"+aServer+" fileName:"+fileName+" to folder:"+scpwebapps1+" scpUser: "+scpUser);
										}
									}
									DeployUtility.scpFileToServer(this, aServer, aFile, fileName, null);										
								}
							}
						}
					}
					if(log.isDebugEnabled() && null!=tempFolderNowFolder){
						log.debug(METHOD_NAME+": deleting tempFolderNowFolder.getAbsolutePath()="+tempFolderNowFolder.getAbsolutePath());
					}
					DeployUtility.deleteFolderAndSubFolders(tempFolderNowFolder);
				}
			}
		}
		catch(ZipException e){
			e.printStackTrace();
			log.error(METHOD_NAME+": ZipException caught e.getMessage()="+e.getMessage());
			log.error(e);
		}
		catch (ParseException e) {
			e.printStackTrace();
			log.error(METHOD_NAME+": ParseException caught e.getMessage()="+e.getMessage());
			log.error(e);
		}
		catch (ParserConfigurationException e) {
			e.printStackTrace();
			log.error(METHOD_NAME+":  ParserConfigurationException caught e.getMessage()="+e.getMessage());
			log.error(e);
		} 
		catch (SAXException e) {
			e.printStackTrace();
			log.error(METHOD_NAME+": SAXException caught e.getMessage()="+e.getMessage());
			log.error(e);
		}
		catch(IOException e){
			e.printStackTrace();
			log.error(METHOD_NAME+": IOException caught e.getMessage()="+e.getMessage());
			log.error(e);
		}
		catch(Throwable e){
			e.printStackTrace();
			log.error(METHOD_NAME+": Throwable caught e.getMessage()="+e.getMessage());
			log.error(e);
		}
		finally{
			if(null!=latestNexusFileZip){
				latestNexusFileZip.close();
			}
		}		
	}
	
	private NexusContentItem getLatestIncludeWarsJarFileUrlFromNexus(NexusArtifactSaxHandler nexusHandler,String requestUrl, String warFileNames)
			throws MalformedURLException, IOException, SAXParseException, SAXException, ParserConfigurationException{

		String METHOD_NAME="getLatestIncludeWarsJarFileUrlFromNexus()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: requestUrl="+requestUrl);
		}
		NexusContentItem retVal=null;
		URL url = new URL(requestUrl);
		URLConnection conn = url.openConnection();

		conn.setReadTimeout(4000);
		conn.setConnectTimeout(5000);

		SAXParserFactory factory = SAXParserFactory.newInstance();
		InputStream innyStream=null;

		SAXParser saxParser = factory.newSAXParser();
		
		try{
			innyStream=conn.getInputStream();
			saxParser.parse(innyStream, nexusHandler);

			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": warFileNames.trim()="+warFileNames.trim());
			}			
			//Look for the latest jar or war
			retVal=nexusHandler.getLatestJarContent();		

		}
		finally{
			innyStream.close();
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}
		return retVal;
	}
	
	private Map<String,File>getWebAppWarFilesAndFolders(String installedWebAppDirStr){
		String METHOD_NAME="getWebAppWarFilesAndFolders()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: installedWebAppDirStr="+installedWebAppDirStr);
		}
		Map<String,File>retVal=new HashMap<String,File>();
		if(null!=installedWebAppDirStr && !installedWebAppDirStr.isEmpty()){
			File installedWebAppFolder=new File(installedWebAppDirStr);
			if(installedWebAppFolder.exists() && installedWebAppFolder.isDirectory()){
				File[]installedWebAppFilesAndFolders=installedWebAppFolder.listFiles();
				for(File aFileOrFolder:installedWebAppFilesAndFolders){
					String aFileOrFolderName=aFileOrFolder.getName();
					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": aFileOrFolderName="+aFileOrFolderName);
					}
					//We only care about .war files that are not jenkins.war
					if(null!=aFileOrFolderName && !aFileOrFolderName.trim().equalsIgnoreCase("jenkins.war") && 
					   !aFileOrFolderName.trim().equalsIgnoreCase("jenkins")){					    
						retVal.put(aFileOrFolderName, aFileOrFolder);					
					}
				}
			}
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END retVal.size()="+retVal.size());
		}
		return retVal;
	}

	//This checks to see i the current installed webapp needs to be replaced by a newer version on the nexus repo
	//First it reads the warinfo.properties, if the the time stamp cannot be found in the warinfo.properties,
	//we try to find the timestamp in the buildinfo.properties, lastly if no timestamp can be found there, then
	//we use the last modified time of the respective webapp folder
	private boolean shouldDeployLatestWar(NexusContentItem latestContent, String warFileName)throws ProtocolException,
	FileNotFoundException, MalformedURLException, IOException, ParseException{
		String METHOD_NAME="shouldDeployLatestWar()";
		boolean retVal=false;
		boolean useResourcesbuildinfo=false;
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: warFileName="+warFileName+" latestContent="+latestContent+"!!!!!!");
		}
		String timestamp=null;
		//Get the absolute path to the tomcat webapps folder 
		String installedWebAppDirStr=(NexusDeploy.propsFile.getProperty("webappfolder",
				"/home/docs/Tomcat/latest/webapps/")).trim();
		if(!installedWebAppDirStr.endsWith("/")){
			installedWebAppDirStr+="/";
		}
		//Get the absolute path to the warFileName
		String installedWebAppStr=installedWebAppDirStr+warFileName;
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": installedWebAppDirStr="+installedWebAppDirStr);
			log.debug(METHOD_NAME+": installedWebAppStr="+installedWebAppStr);
		}
		//Get a handle of the .war installed on the server
		File installedWar=new File(installedWebAppStr);

		//The deployed .war does exist, we first have to see if there is a matching deployed webApp folder,
		//then we have to check to see if it's the latest version
		if(installedWar.exists()){

			int indexOfDot=warFileName.lastIndexOf(".");
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": indexOfDot="+indexOfDot);
			}
			DateFormat df = null;
			String dateFormat="yyyy-MM-dd HH:mm:ss";			

			//The webapp folder name should match the .war name minus the .war extension
			if(-1!=indexOfDot){
				//Get the deployed webapp folder name
				String webAppFolderName=warFileName.substring(0,indexOfDot);
				webAppFolderName+="/";
				//if(log.isDebugEnabled()){
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": indexWarFolderName="+webAppFolderName);
				}
				//}
				//Get the path to the warinfo.properties relative to the deployed .war folder
				//The warinfo.properties is created and added to the respective deployed .war by this application.
				//Once a .war is pulled from Nexus, we get the last modified timestamp from Nexus for a respective .war
				//and add the value to the warinfo.properties file. All .war's are compressed in a .jar archive
				//on the Nexus server. We also add the sha value corresponding to the downloaded .jar
				String warInfoFilePath=(NexusDeploy.propsFile.getProperty("warinfoNexusDeploy.propsFile","WEB-INF/warinfo.properties")).trim();

				String warInfoPropsStr=installedWebAppDirStr+webAppFolderName+warInfoFilePath;
				//if(log.isDebugEnabled()){
				if(log.isDebugEnabled()){
					//warInfoPropsStr can be of the form:
					// /home/docs/Tomcat/latest/webapps/orchestration-v1-orchestration-devguide/WEB-INF/warinfo.properties
					log.debug(METHOD_NAME+": warInfoPropsStr="+warInfoPropsStr);
				}
				//}
				File warInfopropsFile=new File(warInfoPropsStr);				
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": !warInfopropsFile.exists()="+(!warInfopropsFile.exists()));
				}
				Properties buildInfoProps=new Properties();
				//The warinfo.properties file should have been created by a previous run of this
				//code, if it does not exist then we can look for the buildinfo.properties,
				//which would have been created as part of the .war build process. 
				//We should check the warinfo.properties first because it matches the .war creation time
				//on the nexus server, while the buildinfo.properties is an estimation
				if(!warInfopropsFile.exists()){

					String theFileStr=installedWebAppDirStr+webAppFolderName+"WEB-INF/classes/resourcesbuildinfo.properties";
					//First we try to get the src/main/resources/resourcesbuildinfo.properties file
					File theFile=new File(theFileStr);
					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": theFileStr="+theFileStr);
						log.debug(METHOD_NAME+": theFile.exists()="+(theFile.exists()));
					}
					if(theFile.exists()){
						useResourcesbuildinfo=true;
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": resourcesbuildinfo.properties exists!!!!");
						}
						buildInfoProps.load(new FileInputStream(theFile));
						timestamp=buildInfoProps.getProperty("buildtime");
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": timestamp="+timestamp);
						}						
					}
					else{						
						String buildInfoPath=NexusDeploy.propsFile.getProperty("bookinfoprops","webapp/WEB-INF/bookinfo.properties");
						String buildInfoStr=installedWebAppDirStr+webAppFolderName+"/"+buildInfoPath;
						File buildInfoFile= new File(buildInfoStr);
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": (null!=buildInfoFile && buildInfoFile.exists())="+
									(null!=buildInfoFile && buildInfoFile.exists()));
						}
						//We have found the buildinfo.properties, read the buildtime value
						if(null!=buildInfoFile && buildInfoFile.exists()){
							buildInfoProps.load(new FileInputStream(new File(buildInfoStr)));
							timestamp=buildInfoProps.getProperty("buildtime");
							if(log.isDebugEnabled()){
								log.debug(METHOD_NAME+": timestamp="+timestamp);
							}
						}
						//we use the folder time stamp as the last resort
						else{


							String webAppFolderPath=installedWebAppDirStr+webAppFolderName;
							File webAppFolderFile=new File(webAppFolderPath);
							if(log.isDebugEnabled()){
								log.debug(METHOD_NAME+": (webAppFolderFile.exists())="+
										(webAppFolderFile.exists()));
							}
							if(webAppFolderFile.exists()){
								df = new SimpleDateFormat(dateFormat);
								long lastmodified=webAppFolderFile.lastModified();
								Date dateFromWebAppFolder=new Date(lastmodified);
								timestamp=df.format(dateFromWebAppFolder);
								if(log.isDebugEnabled()){
									log.debug(METHOD_NAME+": timestamp="+timestamp);
								}
							}
							else{
								//This scenario should never occur or at least should be very unlikely
								//If the *.war file exists in the webapps folder, the corresponding
								//folder should also exist
							}
						}
						if(null!=timestamp){
							timestamp=timestamp.replaceAll("\\\\:",":");							   
						}
					}
				}
				//We have found the warinfo.properties file
				else{
					Properties warinfoProps=new Properties();
					warinfoProps.load(new FileInputStream(warInfoPropsStr));

					timestamp=warinfoProps.getProperty("timestamp");
				}
				if(null!=timestamp){
					df = new SimpleDateFormat(dateFormat);						
					Date dateFromWarBuildOrLastModified=df.parse(timestamp);
					
					//This is of the form: 2014-08-01 15:48:10.0 UTC
					long dateAsLongFromCurrentWar=dateFromWarBuildOrLastModified.getTime();
					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": useResourcesbuildinfo="+useResourcesbuildinfo);
					}
					//The resourceguildinfo.properties file timestamp is a little off, so we add 20 seconds
					//to help it out.
					if(useResourcesbuildinfo){
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": adding "+(ADDED_SECONDS)+" milliseconds");
						}
					    dateAsLongFromCurrentWar+=(ADDED_SECONDS);
					}
					if(log.isDebugEnabled()){
						Date dateDateasLongFromCurrentWar=new Date(dateAsLongFromCurrentWar);					
						log.debug(METHOD_NAME+": dateDateasLongFromCurrentWar.toString()=" +dateDateasLongFromCurrentWar.toString());
					}
					String lastModified=latestContent.getLastModified();
					long dateAsLongFromNexus=this.getLongFromDateStr(lastModified);
					
					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": dateFormat="+dateFormat);
						log.debug(METHOD_NAME+": timestamp="+timestamp);
						
						SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
						Calendar cal=new GregorianCalendar();
						TimeZone timezone=cal.getTimeZone();
						log.debug(METHOD_NAME+": timezone="+timezone.getDisplayName());
						formatter.setTimeZone(timezone);
						String theDate=formatter.format(new Date(dateAsLongFromNexus));
						log.debug(METHOD_NAME+": theDate="+theDate);
						log.debug(METHOD_NAME+": lastModified="+lastModified);
						log.debug(METHOD_NAME+": dateAsLongFromCurrentWar="+dateAsLongFromCurrentWar);
						log.debug(METHOD_NAME+": dateAsLongFromNexus="+dateAsLongFromNexus);
						log.debug(METHOD_NAME+": (dateAsLongFromNexus>dateAsLongFromCurrentWar)="+
						(dateAsLongFromNexus>dateAsLongFromCurrentWar));
					}
					if(dateAsLongFromNexus>dateAsLongFromCurrentWar){
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": #$%#$#$%#$%#$%War on nexus server is more recent, set retVal to true");
						}
						retVal=true;
					}
					else{
						retVal=false;
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": #$%#$#$%#$%#$%War on nexus server is NOT more recent");
						}
					}
					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": #$%#$#$%#$%#$%dateAsLongFromCurrentWar="+dateAsLongFromCurrentWar+
								" dateAsLongFromNexus="+dateAsLongFromNexus);
					}
				}
			}
			//There is something strange with the .war name skip this one
			else{
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": warFileName: "+warFileName+" is invalid, skip this .war file");
				}
			}
		}
		//The .war does not exist, this means the previous nexus deploy resulted in a pull of a .war
		//not specified in the nexus deploy properties file, for example, the .war specified in the 
		//properties file is book_name-internal.war, but .war on the server is book_name-reviewer.war
		else{
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": War does not exists on the server, set retVal to true");
			}
			retVal=true;
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END: timestamp="+timestamp+" retVal="+retVal);
		}
		return retVal;
	}


	//This method adds the server prefix to each of the url's and then calls another method to find the latest snapshot for each
	//respective url
	private List<String> getAllUrls(boolean isProd)throws IOException{
		String METHOD_NAME="getAllUrls()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:");
		}
		List<String> retVal=new ArrayList<String>();

		String serverUrl=(NexusDeploy.propsFile.getProperty("servername","http://docs-staging.rackspace.com:8081/nexus/service/local/repositories")).trim();
		if(!serverUrl.endsWith("/")){
			serverUrl+="/";
		}
		String repo=null;
		if(isProd){
			repo=(NexusDeploy.propsFile.getProperty("prodrepo","docs-deploy/content")).trim();		   
		}
		else{
			repo=(NexusDeploy.propsFile.getProperty("stagingrepo","docs-staging/content")).trim();
		}
		if(!repo.endsWith("/")){
			repo+="/";
		}
		String urlPrefix=serverUrl+repo;
		if(!urlPrefix.endsWith("/")){
			urlPrefix+="/";
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": urlPrefix="+urlPrefix);
			log.debug(METHOD_NAME+": repo="+repo);
			log.debug(METHOD_NAME+": serverUrl="+serverUrl);
		}
		String reqUrls=NexusDeploy.propsFile.getProperty("requesturls");
		if(null!=reqUrls){
			String[] urls=reqUrls.split("@");
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": urls.length="+urls.length);
			}
			if(null!=urls){
				for(String aUrl:urls){
					retVal.add((urlPrefix+aUrl));
					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": urlPrefix+aUrl="+urlPrefix+aUrl);
					}
				}
			}
		}
		//Now we need to figure out what is the latest snapshot for each
		retVal=getAllSnapShotUrls(retVal, urlPrefix);
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END: retVal.size()="+retVal.size());
		}
		return retVal;
	}

	//Get the latest snap shot for each url and append it to each respective url string
	private List<String> getAllSnapShotUrls(List<String> urls, String serverUrlPrefix)
			throws IOException{
		String METHOD_NAME="getAllSnapShotUrls()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: urls.size()="+urls.size());
		}

		List<String>retVal=new ArrayList<String>();

		for(String aUrl:urls){			
			String[] aUrlTwoParts=aUrl.split("~");
			if(null!=aUrlTwoParts){
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": aUrl="+aUrl+" aUrlTwoParts.length="+aUrlTwoParts.length);
				}
				//This comes from docTools configuration
				if(aUrlTwoParts.length==2 || aUrlTwoParts.length==1){
										
					String requestUrl=aUrlTwoParts[0];
					String secondPartOfUrl=null;
					if(aUrlTwoParts.length==2){
						secondPartOfUrl=aUrlTwoParts[1];
					}
					if(aUrlTwoParts.length==1){
						int lastIndex=aUrl.lastIndexOf(":");
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": lastIndex="+lastIndex);
							log.debug(METHOD_NAME+": requestUrl="+requestUrl);
							log.debug(METHOD_NAME+": secondPartOfUrl="+secondPartOfUrl);
						}
						if(-1!=lastIndex){
							requestUrl=aUrl.substring(0,lastIndex);
							secondPartOfUrl=aUrl.substring(lastIndex);
						}
					}
					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": requestUrl="+requestUrl);
						log.debug(METHOD_NAME+": secondPartOfUrl="+secondPartOfUrl);
					}
					
					URL url = null;
					URLConnection conn = null;

					//InputStreamReader innyReader=new InputStreamReader(conn.getInputStream());
					SAXParserFactory factory = SAXParserFactory.newInstance();
					InputStream innyStream=null;

					try {
						url = new URL(requestUrl);
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": requestUrl="+requestUrl);
						}
						conn = url.openConnection();
						conn.setReadTimeout(4000);
						conn.setConnectTimeout(5000);

						SAXParser saxParser = factory.newSAXParser();
						NexusArtifactSaxHandler nexusHandler=new NexusArtifactSaxHandler();
						innyStream=conn.getInputStream();

						saxParser.parse(innyStream, nexusHandler);
						//Look for the latest jar
						NexusContentItem latestSnapShot=nexusHandler.getLatestSnapShot();
						if(log.isDebugEnabled()){
							log.debug(METHOD_NAME+": latestSnapShot="+latestSnapShot);
						}
						if(null!=latestSnapShot){
							//The contentStr is of the form:
							//http://docs-staging.rackspace.com:8081/nexus/service/local/repositories/docs-staging/content/com/rackspace/cloud/apidocs/cloud-networks/1.0.0-SNAPSHOT/~target/docbkx/webhelp:servers-v2-cn-releasenotes.war*servers-v2-cn-gettingstarted.war*servers-v2-cn-devguide.war
							String contentStr=latestSnapShot.getResourceURI()+"~"+secondPartOfUrl;
							if(log.isDebugEnabled()){
								log.debug(METHOD_NAME+": contentStr="+contentStr);
							}
							retVal.add(contentStr);
						}
						if(null!=innyStream){
							innyStream.close();
						}

					} 
					catch (ParserConfigurationException e) {
						e.printStackTrace();
						log.debug(METHOD_NAME+": ParserConfigurationException caught e.getMessage()="+e.getMessage());					log.debug(e);
						log.debug(e);
					} 
					catch (SAXException e) {
						e.printStackTrace();
						log.debug(METHOD_NAME+": SAXException caught e.getMessage()="+e.getMessage());
						log.debug(e);

					}
					catch(IOException e){
						e.printStackTrace();
						log.debug(METHOD_NAME+": IOException caught e.getMessage()="+e.getMessage());
						log.debug(e);
					}
					catch(Throwable e){
						e.printStackTrace();
						log.debug(METHOD_NAME+": Throwable caught e.getMessage()="+e.getMessage());
						log.debug(e);
					}
				}
				else if(aUrlTwoParts.length==1){
					aUrlTwoParts=aUrl.split(":"); 
				}
			}
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END: retVal.size()="+retVal.size());
		}
		return retVal;
	}

	//aDate is of the form: 2014-08-01 15:48:10.0 UTC
	private long getLongFromDateStr(String aDate){
		String METHOD_NAME="getLongFromDateStr()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: aDate="+aDate);
		}		
		long retVal=-1;
		if(null!=aDate){
			String aDateStr=aDate.trim();
			SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			try {	
				Date d=sdf.parse(aDateStr);
				retVal=d.getTime();
			} catch (ParseException e) {
				e.printStackTrace();
				log.debug(METHOD_NAME+": ParseException caught e.getMessage()="+e.getMessage());
				log.debug(e);
			}
			catch(Throwable e){
				e.printStackTrace();
				log.debug(METHOD_NAME+": Throwable caught e.getMessage()="+e.getMessage());
				log.debug(e);
			}
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END: retVal="+retVal);
		}
		return retVal;
	}
	


	//This checks to see if the temp folder cotains the latest jar file if so delete it, it then deletes the war file and 
	//corresponding webapp folder as indicated by the props.properties file key: webappfolder if any these exists, then 
	//cleanupWorkSpace will delete it
	/**
	 * 
	 * @param webAppWarFilesAndFoldersMap map that has the .war file or war folder name as the key and
	 *                                    a handle to the file as the value
	 * @param nexusHandler                Object that holds the latest *-include-wars.jar information
	 * @param warFolderInJar              path to the war folder in the *-include-wars.jar file
	 * @param tempFolder				  temp folder path
	 * @param webAppPath                  webapp folder on tomcat server
	 * @param warName					  name of the .war file we are processing as indicated in the rax-deploy-nexus-war.properties
	 *                                    or rax-deploy-nexus-war.properties
	 * @throws IOException
	 */
	private void cleanupWebAppsFolder( NexusArtifactSaxHandler nexusHandler, String warFolderInJar, String warName)
			throws IOException{
		String METHOD_NAME="cleanupWebAppsFolder()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:");
		}
		if(warName!=null && !warName.endsWith(".war")){
			warName+=".war";
		}
		String warNameInZip=warName;
		
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": ENTER warFolderInJar="+warFolderInJar+
					" this.installedWebAppDirStr="+this.installedWebAppDirStr+" warName="+warName+" warNameInZip="+warNameInZip);
		}
		if(null!=warFolderInJar && !warFolderInJar.equals("") && !warFolderInJar.endsWith("/")){
			warFolderInJar+="/";
		}
		if(!this.installedWebAppDirStr.endsWith("/")){
			this.installedWebAppDirStr+="/";
		}
		
		
		//The warName originates from the nexus deploy properties file (rax-deploy-nexus-war.properties or 
		//rax-deploy-nexus-warInternal.properties), and should be either the base .war name or *-internal.war
		//We will never see *-reviewer.war
		//If from rax-deploy-nexus-war.properties, there can only be the base *.war file
		
		//Here are the precedence rules if the nexus deploy properties file specifies the base .war file
		//From the *-include-war.jar:
		//1. Always take the base .war file first if it exists
		//2. If there is no base .war but *-reviewer.war exists, take the *-reviewer.war 
		//3. If no base .war nor *-reviewer.war exists, do nothing
		
		//Here are the precedence rules if the nexus deploy properties file specifies the -internal*.war
		//From the *-include-war.jar:
		//1. Always take the *-internal.war first if it exists
		//2. If there is no *-internal.war but a *-reviewer.war exists, take the *-reviewer.war
		//3. If no *-internal.war nor *-reviewer.war exists, do nothing
		
		
		//We can only figure out from the context of the latestJar file whether we are dealing with a doc-name-reviewer.war file
		//If the latestJar contains no corresponding external (doc-name.war) *.war file, but does contain a *-reviewer.war file
		//then we know this is a deploy of a *-reviewer.war
		String externalName="";
		String warReviewerName="";
		String warInternalName="";
		
		String warInternalFolder="";
		String warReviewerFolder="";
		String warExternalFolder="";
		
		if(warName.endsWith("-internal.war")){
			externalName=warName.substring(0,warName.lastIndexOf("-internal.war"))+".war";
			warReviewerName=warName.substring(0,warName.lastIndexOf("-internal.war"))+"-reviewer.war";
			warInternalName=warName;
			
			warExternalFolder=warName.substring(0,warName.lastIndexOf("-internal.war"));
			warReviewerFolder=warName.substring(0,warName.lastIndexOf("-internal.war"))+"-reviewer";
			warInternalFolder=warName.substring(0,warName.lastIndexOf(".war"));
		}
		//We should never see this but in case it is in the nexus deploy war properties file, handle it
		else if(warName.endsWith("-reviewer.war")){
			externalName=warName.substring(0,warName.lastIndexOf("-reviewer.war"))+".war";	
			warReviewerName=warName;
			warInternalName=warName.substring(0,warName.lastIndexOf("-reviewer.war"))+"-internal.war";
			
			warExternalFolder=warName.substring(0,warName.lastIndexOf("-reviewer.war"));
			warReviewerFolder=warName.substring(0,warName.lastIndexOf(".war"));
			warInternalFolder=warName.substring(0,warName.lastIndexOf("-reviewer.war"))+"-internal";
		}
		else{
			externalName=warName;
			warReviewerName=warName.substring(0,warName.lastIndexOf(".war"))+"-reviewer.war";
			warInternalName=warName.substring(0,warName.lastIndexOf(".war"))+"-internal.war";
			
			warExternalFolder=warName.substring(0,warName.lastIndexOf(".war"));
			warReviewerFolder=warName.substring(0,warName.lastIndexOf(".war"))+"-reviewer";
			warInternalFolder=warName.substring(0,warName.lastIndexOf(".war"))+"-internal";
		}	
		String externalWarInZip=warFolderInJar+externalName;
		String reviewerWarInZip=warFolderInJar+warReviewerName;
		String internalWarInZip=warFolderInJar+warInternalName;

					
		if(log.isDebugEnabled()){			
			log.debug(METHOD_NAME+": warName="+warName);
			log.debug(METHOD_NAME+": warFolderInJar="+warFolderInJar);
			log.debug(METHOD_NAME+": this.installedWebAppDirStr="+this.installedWebAppDirStr);
			log.debug(METHOD_NAME+": externalName="+externalName);
			log.debug(METHOD_NAME+": externalWarInZip="+externalWarInZip);
			log.debug(METHOD_NAME+": warReviewerName="+warReviewerName);
			log.debug(METHOD_NAME+": reviewerWarInZip="+reviewerWarInZip);
			log.debug(METHOD_NAME+": warInternalName="+warInternalName);			
			log.debug(METHOD_NAME+": internalWarInZip="+internalWarInZip);	
			
			log.debug(METHOD_NAME+": warExternalFolder="+warExternalFolder);
			log.debug(METHOD_NAME+": warReviewerFolder="+warReviewerFolder);	
			log.debug(METHOD_NAME+": warInternalFolder="+warInternalFolder);
			
			log.debug(METHOD_NAME+": webAppWarFilesAndFoldersMap.containsKey("+warReviewerName+")="+webAppWarFilesAndFoldersMap.containsKey(warReviewerName));
			log.debug(METHOD_NAME+": webAppWarFilesAndFoldersMap.containsKey("+warInternalName+")="+webAppWarFilesAndFoldersMap.containsKey(warInternalName));
			log.debug(METHOD_NAME+": (!warName.endsWith(\"-internal.war\"))="+(!warName.endsWith("-internal.war")));
		}
		
		deleteWarAndWebAppFolder(webAppWarFilesAndFoldersMap,warReviewerName);
		deleteWarAndWebAppFolder(webAppWarFilesAndFoldersMap,warReviewerFolder);
		
		deleteWarAndWebAppFolder(webAppWarFilesAndFoldersMap,warInternalName);	
		deleteWarAndWebAppFolder(webAppWarFilesAndFoldersMap,warInternalFolder);

		deleteWarAndWebAppFolder(webAppWarFilesAndFoldersMap,externalName);	
		deleteWarAndWebAppFolder(webAppWarFilesAndFoldersMap,warExternalFolder);

		//first we need to delete the .war file
		String webAppFullPath=this.installedWebAppDirStr+warNameInZip;

		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": webAppFullPath="+webAppFullPath);
		}

		//We must now clean up the appserver webapp folder and .war file
		File webAppFullPathFile=new File(webAppFullPath);
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": webAppFullPathFile.exists()="+webAppFullPathFile.exists());
			log.debug(METHOD_NAME+": webAppFullPathFile.getAbsolutePath()="+webAppFullPathFile.getAbsolutePath());
		}
		//The .war does exist, delete it
		if(webAppFullPathFile.exists()){
			//just delete it
			webAppFullPathFile.delete();
		}
		int indexOfDot=warName.indexOf(".");
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": indexOfDot="+indexOfDot);
		}
		//Now delete the webapp project folder if it exists
		if(-1!=indexOfDot){
			String warFolderName=warName.substring(0,indexOfDot);
			String warAppFolderPath=this.installedWebAppDirStr+warFolderName;
			File webAppFolderFullPath=new File(warAppFolderPath);
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": webAppFolderFullPath.exists()="+webAppFolderFullPath.exists());
			}
			//the webapp folder exists
			if(webAppFolderFullPath.exists()){
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": webAppFolderFullPath.isDirectory()="+webAppFolderFullPath.isDirectory());
				}
				//delete it
				DeployUtility.deleteFolderAndSubFolders(webAppFolderFullPath);
			}
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+"END");
		}
	}
	
	
	/**
	 * This deletes the indicated .war warName and corresponding webapp folder if they exist in the webapps folder 
	 * @param webAppWarFilesAndFoldersMap is a Map<String,File> which is populated by al the .war file names in the webapps folder
	 * as the key and the File itself as the value
	 * @param warFileOrFolderName The .war file name that we want to delete
	 */
	private void deleteWarAndWebAppFolder(Map<String,File>webAppWarFilesAndFoldersMap,String warFileOrFolderName){
		String METHOD_NAME="deleteWarAndWebAppFolder()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: warFileOrFolderName="+warFileOrFolderName);
			log.debug(METHOD_NAME+": webAppWarFilesAndFoldersMap.containsKey("+warFileOrFolderName+")="+webAppWarFilesAndFoldersMap.containsKey(warFileOrFolderName));
			log.debug(METHOD_NAME+": webAppWarFilesAndFoldersMap.get("+warFileOrFolderName+")="+webAppWarFilesAndFoldersMap.get(warFileOrFolderName));
		}
		if(webAppWarFilesAndFoldersMap.containsKey(warFileOrFolderName)){
			File deleteFile=webAppWarFilesAndFoldersMap.get(warFileOrFolderName);
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": (deleteFile.exists())="+(deleteFile.exists()));
			}
			//String fullWarPath=deleteFile.getAbsolutePath();
			if(deleteFile.exists()){
				DeployUtility.deleteFolderAndSubFolders(deleteFile);
			}
			webAppWarFilesAndFoldersMap.remove(warFileOrFolderName);
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}
	}

	private Map<String,ZipEntry>getAllWarEntries(ZipFile latestNexusFileZip){
		String METHOD_NAME="getAllWarEntries()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:");
		}
		Map<String,ZipEntry>retVal=new HashMap<String, ZipEntry>();

		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+":(null!=latestNexusFileZip)="+(null!=latestNexusFileZip));
		}
		if(null!=latestNexusFileZip){
			String nexusFileName=latestNexusFileZip.getName();
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+":nexusFileName="+nexusFileName);
			}
			if(nexusFileName.toLowerCase().endsWith(".jar")){
				Enumeration zipEnums=latestNexusFileZip.entries();
				while(zipEnums.hasMoreElements()){
					ZipEntry aZipEntry=(ZipEntry)zipEnums.nextElement();
					if(!aZipEntry.isDirectory()){
						String anEntryName=aZipEntry.getName();
						retVal.put(anEntryName, new ZipEntry(aZipEntry));
					}			
				}
			}
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END: retVal.size()="+retVal.size());
		}
		return retVal;
	}
	
	/**
	 * 
	 * @param latestNexusFileZip latest zip jar file pulled from nexus that has the .war's bundled in a 
	 *                         *-include-wars.jar file
	 * @param latestJarWarsMap Map that has all the zip entries of the latestNexusFilesZip file
	 * @param nexusHandler Contains information on the .jar's on the nexus server
	 * @param tempFolderNow where to retrieve the *-include.jar
	 * @param webAppPath Absolute path to the tomcat webapp folder
	 * @param warName The nexus deploy war name that is specified in the nexus deploy properties file
	 * @param warFolderInJar path of a ZipEntry in the *-include.jar
	 * @param webAppWarFilesAndFoldersMap Map that is keyed the name of the file or folder and the value 
	 *                                    is the 
	 *                                    handle to the File
	 * @param appendedWarFiles List that contains all the .war files that will update the respective 
	 *                         webapps application
	 * @throws IOException
	 */
	private void deployWar(ZipFile latestNexusFileZip, Map<String, ZipEntry>latestJarWarsMap, NexusArtifactSaxHandler nexusHandler, 
			String tempFolderNow, String warName, String warFolderInJar,
			List<File>appendedWarFiles, File latestNexusFile) 
					throws IOException{
		String METHOD_NAME="deployWar()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: tempFolder="+tempFolderNow+ " this.installedWebAppDirStr="+this.installedWebAppDirStr+" warName="+warName+
					" warFolderInJar="+warFolderInJar+" this.webAppWarFilesAndFoldersMap.size()="+this.webAppWarFilesAndFoldersMap.size());

		}
		if(null!=warFolderInJar&&warFolderInJar.trim().equals("/")){
			warFolderInJar="";
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": warFolderInJar="+warFolderInJar);
		}
		if(!this.installedWebAppDirStr.endsWith("/")){
			this.installedWebAppDirStr+="/";
		}
		this.cleanupWebAppsFolder( nexusHandler, warFolderInJar, warName);

		if(!tempFolderNow.endsWith("/")){
			tempFolderNow+="/";
		}
		
		String warNameWithPathInJar=warFolderInJar+warName;

		File tempDir=new File(tempFolderNow);
		//Create the tempDir if it does not exist
		if(!tempDir.exists()){
			tempDir.mkdirs();
		}
		String tempWarFileFullPathName=tempFolderNow+warName;
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": tempWarFileFullPathName="+tempWarFileFullPathName);
		}
		File tempWarFile=new File(tempWarFileFullPathName);

		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": creating tempWarFile \n");			
		}

		//If the warName is found in the latestNexusFileZip file which means the possible scenarios:
		//going from external to external
		//going from internal to internal
		//going from reviewer to external
		//going from reviewer to internal
		if((null==latestJarWarsMap)||(latestJarWarsMap.containsKey(warNameWithPathInJar))){
			deployZippedWar(latestJarWarsMap, nexusHandler,warName, this.installedWebAppDirStr, tempWarFileFullPathName, 
			webAppWarFilesAndFoldersMap,  warNameWithPathInJar, tempWarFile,appendedWarFiles,latestNexusFile);
		}
		//There is not a match which means the possible scenarios:
		//going from external to reviewer
		//going from internal to reviewer
		else {
			String warNameWithPathInJarMinusDotWar=warNameWithPathInJar.substring(0,warNameWithPathInJar.lastIndexOf(".war"));		
			String warNameReviewerAppended=null;
			if(warNameWithPathInJarMinusDotWar.toLowerCase().endsWith("-internal")){
				int indexInternal=warNameWithPathInJarMinusDotWar.toLowerCase().lastIndexOf("-internal");
				if(-1!=indexInternal){
					warNameReviewerAppended=warNameWithPathInJarMinusDotWar.substring(0,indexInternal);
					warNameReviewerAppended+="-reviewer.war";
				}
			}
			else{
				warNameReviewerAppended=warNameWithPathInJarMinusDotWar+"-reviewer.war";
			}
						
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": warNameWithPathInJarMinusDotWar="+warNameWithPathInJarMinusDotWar);
				log.debug(METHOD_NAME+": warNameReviewerAppended="+warNameReviewerAppended);
				log.debug(METHOD_NAME+": latestJarWarsMap.containsKey("+warNameReviewerAppended+
						")="+latestJarWarsMap.containsKey(warNameReviewerAppended));
			}
			if(latestJarWarsMap.containsKey(warNameReviewerAppended)){
				deployReviewer(latestJarWarsMap,webAppWarFilesAndFoldersMap,appendedWarFiles,tempWarFile,
					       latestNexusFileZip,nexusHandler,warName,warNameReviewerAppended,
					       tempWarFileFullPathName,this.installedWebAppDirStr,latestNexusFile);
			}

		}
		
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}
	}
	
	private void deployReviewer(Map<String, ZipEntry>latestJarWarsMap, Map<String, File>webAppWarFilesAndFoldersMap,
			List<File>appendedWarFiles, File tempWarFile, ZipFile latestNexusFileZip, NexusArtifactSaxHandler nexusHandler, 
			String warName, String warNameReviewerAppended, String tempWarFileFullPathName, String webAppPath, File latestNexusFile)
	throws IOException{
		String METHOD_NAME="deployReviewer()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: latestJarWarsMap.containsKey("+warNameReviewerAppended+")="+
					  latestJarWarsMap.containsKey(warNameReviewerAppended));
		}		
		if(latestJarWarsMap.containsKey(warNameReviewerAppended)){
			if(tempWarFile.exists()){
				tempWarFile.delete();
			}
			if(tempWarFileFullPathName.endsWith("-internal.war")){
				tempWarFileFullPathName=tempWarFileFullPathName.substring(0,tempWarFileFullPathName.lastIndexOf("-internal.war"));
			}
			else{
				tempWarFileFullPathName=tempWarFileFullPathName.substring(0,tempWarFileFullPathName.lastIndexOf(".war"));
			}
			tempWarFileFullPathName+="-reviewer.war";
			String warNameReviewer=null;
			if(warName.endsWith("-internal.war")){
				warNameReviewer=warName.substring(0,warName.lastIndexOf("-internal.war"));
			}
			else{
				warNameReviewer=warName.substring(0,warName.lastIndexOf(".war"));
			}
			warNameReviewer+="-reviewer.war";
			
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": warNameReviewer="+warNameReviewer);
				log.debug(METHOD_NAME+": tempWarFileFullPathName="+tempWarFileFullPathName);
			}
			tempWarFile=new File(tempWarFileFullPathName);
			if(tempWarFile.exists()){
				tempWarFile.delete();
			}
			tempWarFile.createNewFile();
			deployZippedWar(latestJarWarsMap, nexusHandler,warNameReviewer, webAppPath, tempWarFileFullPathName, 
					webAppWarFilesAndFoldersMap,  warNameReviewerAppended, tempWarFile, appendedWarFiles,latestNexusFile);
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}
	}
	

	private void deployZippedWar(Map<String,ZipEntry>latestJarWarsMap, NexusArtifactSaxHandler nexusHandler, 
			String warName, String webAppPath, String tempWarFileFullPathName, Map<String,File> webAppsFilesAndFoldersMap, 
			String warNameWithPathInJar, File tempWarFile, List<File>appendedWarFiles,File latestNexusFile)
					throws IOException{
		String METHOD_NAME="deployZippedWar()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: warName="+warName+" webAppPath="+webAppPath+
					" tempWarFileFullPathName="+tempWarFileFullPathName+" warNameWithPathInJar="+warNameWithPathInJar);
		}	

		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": (null!=latestNexusFile && latestNexusFile.getName().trim().toLowerCase().endsWith(\".jar\"))="+
					(null!=latestNexusFile && latestNexusFile.getName().trim().toLowerCase().endsWith(".jar")));
		}

		//We are dealing with a .jar file
		if(null!=latestNexusFile && latestNexusFile.getName().trim().toLowerCase().endsWith(".jar")){
			ZipFile latestNexusFileZip=new ZipFile(latestNexusFile);
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": latestNexusFileZip.getName()="+latestNexusFileZip.getName());
				log.debug(METHOD_NAME+": (!latestNexusFileZip.getName().endsWith(\".war\"))="+
						(!latestNexusFileZip.getName().endsWith(".war")));
			}
			if(!latestNexusFileZip.getName().endsWith(".war")){
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": (null!=latestJarWarsMap)="+(null!=latestJarWarsMap));
				}	
				if(null!=latestJarWarsMap){
					ZipEntry aZipEntry=latestJarWarsMap.get(warNameWithPathInJar);
					String anEntryName=aZipEntry.getName();
					if(log.isDebugEnabled()){
						log.debug(METHOD_NAME+": anEntryName="+anEntryName);
					}		

					InputStream inny=latestNexusFileZip.getInputStream(aZipEntry);

					FileOutputStream tempWarFileOutStream=new FileOutputStream(tempWarFile);
					int read=-1;
					try{
						//Write out the compress .war file to the tempdir
						while(-1!=(read=inny.read())){
							tempWarFileOutStream.write(read);
						}
					}
					finally{
						if(null!=tempWarFileOutStream){
							tempWarFileOutStream.close();
						}
						if(null!=inny){
							inny.close();
						}
					}
					//We need to scp this file to all the production servers
					File appendedWarFile=this.addWarInfoToDeployedWar(nexusHandler, warName, webAppPath, tempWarFileFullPathName,webAppsFilesAndFoldersMap);
					if(appendedWarFiles!=null){
						appendedWarFiles.add(appendedWarFile);
					}
				}
			}
		}
		//This is a .war zip file
		else{

			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": tempWarFileFullPathName="+tempWarFileFullPathName);
				log.debug(METHOD_NAME+": (null!=latestNexusFile && latestNexusFile.exists())="+
						(null!=latestNexusFile && latestNexusFile.exists()));				
			}
			if(null!=latestNexusFile && latestNexusFile.exists()){
				if(!webAppPath.endsWith("/")){
					webAppPath=(webAppPath+="/");
				}				
				String webFile=webAppPath+warName;
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": webFile="+webFile);
					log.debug(METHOD_NAME+": latestNexusFile.getAbsolutePath()="+latestNexusFile.getAbsolutePath());
					log.debug(METHOD_NAME+": latestNexusFile.getTotalSpace()="+latestNexusFile.getTotalSpace());
				}
				FileOutputStream writeWarOut=new FileOutputStream(new File((webFile)));

				int readInt=-1;
				FileInputStream inny=new FileInputStream(latestNexusFile);
				//BufferedInputStream buffInny=new BufferedInputStream(inny);
				//BufferedOutputStream buffOutty=new BufferedOutputStream(writeWarOut);
				//byte[] readByte=new byte[512];
				try{
					while(-1!=(readInt=inny.read())){
						writeWarOut.write(readInt);
					}
				}
				finally{
					//buffInny.close();
					inny.close();
					//buffOutty.close();
					writeWarOut.close();
				}
			}		
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END");
		}
	}
	
	private File getLatestJarFileFromNexus(NexusArtifactSaxHandler nexusHandler, String tempFolder)
			throws FileNotFoundException, MalformedURLException, IOException{
		File retVal=null;
		String METHOD_NAME="getLatestJarFileFromNexus()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:");
		}
		if(!tempFolder.endsWith("/")){
			tempFolder+="/";
		}
		NexusContentItem latestContent=nexusHandler.getLatestJarContent();

		if(null!=latestContent){
			String fileName=latestContent.getFileName();
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": fileName="+fileName);
			}

			//At this point the .war and the respective webApp folder has been deleted
			//Create the war file in the temp dir
			String outputFileStr=tempFolder+fileName;
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": outputFileStr="+outputFileStr);
			}
			retVal = new File(outputFileStr);
			if(log.isDebugEnabled()){
				log.debug("retVal.getAbsolutePath()="+retVal.getAbsolutePath());
			}
			boolean createdFileSuccessfully=retVal.createNewFile();
			if(log.isDebugEnabled()){
				log.debug("createdFileSuccessfully="+createdFileSuccessfully);
			}			
			FileOutputStream outtyStream = new FileOutputStream(retVal);

			String artifactUrl=latestContent.getResourceURI();

			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": artifactUrl="+artifactUrl);
			}

			URL theURL=new URL(artifactUrl);

			HttpURLConnection.setFollowRedirects(true);

			HttpURLConnection httpConn = (HttpURLConnection)theURL.openConnection();

			httpConn.setRequestMethod("GET");
			httpConn.setRequestProperty("Accept:", "application/zip");

			httpConn.setConnectTimeout(10000);
			httpConn.setReadTimeout(10000);

			int readInt=-1;
			InputStream inny=httpConn.getInputStream();
			try{
				while(-1!=(readInt=inny.read())){	
					outtyStream.write(readInt);
				}	
			}
			finally{
				inny.close();
				outtyStream.close();
			}
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": retVal.getAbsolutePath()="+retVal.getAbsolutePath());
			log.debug(METHOD_NAME+": retVal.getTotalSpace()="+retVal.getTotalSpace());
			log.debug(METHOD_NAME+": END: ");
		}
		return retVal;
	}

	//This method adds the warinfo.properties file to the .war and writes the entire .war to the webapps folder
	//returns the .war file with the warinfo.properties in the .war
	private File addWarInfoToDeployedWar(NexusArtifactSaxHandler nexusHandler,String warName, String webAppPath, 
			String tempWarFilePathAndName, Map<String,File>webAppsFilesAndFoldersMap){
		String METHOD_NAME="addWarInfoToDeployedWar()";
		File outputFile=null;
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START: warName="+warName+" webAppPath="+webAppPath+
					" tempWarFilePathAndName="+tempWarFilePathAndName);
		}
		NexusContentItem latestContent=nexusHandler.getLatestJarContent();
		String zipFileTempFolder=NexusDeploy.propsFile.getProperty("tempfolder","~/Temp");
		File theTempFolder=new File(zipFileTempFolder);

		if(theTempFolder.exists()){
			//Get the .war file that was created in the temp folder by deployWar()
			File theZipFile=new File((tempWarFilePathAndName));
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+": theZipFile="+theZipFile+" theZipFile.exists()="+theZipFile.exists());
			}
			try{
				FileInputStream fileInny=new FileInputStream(theZipFile);
				ZipInputStream zipInnyStream=new ZipInputStream(fileInny);

				String webAppWarFileStr=webAppPath+warName;
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": webAppWarFileStr="+webAppWarFileStr);
				}
				outputFile=new File(webAppWarFileStr);

				FileOutputStream fileOutty=new FileOutputStream(outputFile);
				ZipOutputStream zipFile=new ZipOutputStream(fileOutty);

				ZipEntry anEntry=null;
				//Add all the entries 
				while(null!=(anEntry=zipInnyStream.getNextEntry())){
					String name=anEntry.getName();
					zipFile.putNextEntry(new ZipEntry(name));
					int len=-1;
					while(-1!=(len=zipInnyStream.read())){
						zipFile.write(len);
					}	
					if(null!=zipFile){
						zipFile.closeEntry();
					}
				}
				if(null!=zipInnyStream){
					zipInnyStream.close();
				}
				if(null!=fileInny){
					fileInny.close();
				}
				if(null!=theZipFile){
					theZipFile.delete();
				}

				String newFileStr=NexusDeploy.propsFile.getProperty("warinfopropsfile","WEB-INF/warinfo.properties");
				String sha1=this.getSha1FromLatestSha1(nexusHandler);

				StringBuffer newStrBuff = new StringBuffer("");
				newStrBuff.append("sha1=");
				newStrBuff.append(sha1);
				newStrBuff.append("\n");
				newStrBuff.append("filename=");
				newStrBuff.append(latestContent.getFileName());
				newStrBuff.append("\n");
				newStrBuff.append("timestamp=");
				String lastModifiedNoTZ=latestContent.getLastModified();
				if(null!=lastModifiedNoTZ){
					lastModifiedNoTZ=lastModifiedNoTZ.trim();
					if(lastModifiedNoTZ.toLowerCase().endsWith("utc")){
						lastModifiedNoTZ=lastModifiedNoTZ.substring(0,(lastModifiedNoTZ.length()-3));
					}
				}
				newStrBuff.append(lastModifiedNoTZ);

				//Now add an new Entry
				ZipEntry aNewEntry = new ZipEntry(newFileStr);

				zipFile.putNextEntry(aNewEntry);
				zipFile.write(newStrBuff.toString().getBytes());
				zipFile.closeEntry();
				zipFile.close();

				if(null!=fileOutty){
					fileOutty.close();
				}
			    if(log.isDebugEnabled()){
			    	log.debug(METHOD_NAME+":warName="+warName);
			    }				
                //Only touch the war if it's not the ROOT.war
			   // if(null!=warName && !warName.toLowerCase().contains("root.war")){
			    	Runtime run=Runtime.getRuntime();

			    	String command="touch "+webAppPath+"ROOT.war";
			    	String command2="touch "+webAppWarFileStr;
			    	Process proc=run.exec(command);
			    	BufferedReader reader=new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			    	String line=null;
			    	StringBuffer lines=new StringBuffer("");
			    	while((null!=(line=reader.readLine()))){
			    		lines.append(line);
			    		lines.append("\n");
			    	}
			    	if(log.isDebugEnabled()){
			    		log.debug("Just tried to running command: "+command);
			    		log.debug("lines.toString()="+lines.toString());
			    	}
			    	proc=run.exec(command);
			    	reader=new BufferedReader(new InputStreamReader(proc.getErrorStream()));
			    	StringBuffer lines2=new StringBuffer("");
			    	String line2=null;
			    	while((null!=(line2=reader.readLine()))){
			    		lines2.append(line2);
			    		lines2.append("\n");
			    	}
			    	if(log.isDebugEnabled()){
			    		log.debug("Just tried to running command2: "+command2);
			    		log.debug("lines2.toString()="+lines2.toString());
			    	}
			    //}
			}
			catch(FileNotFoundException e){

				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": FileNotFoundException caught: ");
				}
				log.debug(METHOD_NAME+": FileNotFoundException caught e.getMessage()="+e.getMessage());
				log.debug(e);
			}
			catch(IOException e){
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": IOException caught: ");
				}	
				log.debug(METHOD_NAME+": IOException caught e.getMessage()="+e.getMessage());
				log.debug(e);
			}
			catch(Throwable e){
				if(log.isDebugEnabled()){
					log.debug(METHOD_NAME+": Throwable caught: ");
				}		
				log.debug(METHOD_NAME+": Throwable caught e.getMessage()="+e.getMessage());
				log.debug(e);
			}
		}
		else{
			if(log.isDebugEnabled()){
				log.debug(METHOD_NAME+" Could not create temp folder: theTempFolder.getAbsolutePath()="+
						theTempFolder.getAbsolutePath()+" and the folder does not exist, aborting operation!!!!");
			}
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END:");
		}
		return outputFile;
	}
	


	private String getSha1FromLatestSha1(NexusArtifactSaxHandler nexusHandler)
			throws IOException
			{
		String retVal="";
		String METHOD_NAME="getSha1FromLatestSha1()";
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": START:");
		}
		NexusContentItem latestSha1Item=nexusHandler.getLatestSha1Content();
		String url=null;
		if(null!=latestSha1Item){
			url=latestSha1Item.getResourceURI();
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": url="+url);
		}
		BufferedReader buffReader=null;
		InputStreamReader reader=null;
		InputStream inny=null;
		try{
			URL theURL=new URL(url);

			HttpURLConnection.setFollowRedirects(true);

			HttpURLConnection httpConn = (HttpURLConnection)theURL.openConnection();

			httpConn.setRequestMethod("GET");
			httpConn.setRequestProperty("Accept:", "application/zip");

			httpConn.setConnectTimeout(10000);
			httpConn.setReadTimeout(10000);

			inny=httpConn.getInputStream();		
			int readInt=-1;
			char readChar=' ';
			StringBuffer sha1Buffer=new StringBuffer("");
			while(-1!=(readInt=inny.read())){
				readChar=(char)readInt;
				sha1Buffer.append(readChar);
			}
			retVal=sha1Buffer.toString();

		}
		catch(MalformedURLException e){
			e.printStackTrace();
			log.debug(METHOD_NAME+": MalformedURLException caught e.getMessage()="+e.getMessage());
			log.debug(e);
		}
		catch(IOException e){
			e.printStackTrace();
			log.debug(METHOD_NAME+": IOException caught e.getMessage()="+e.getMessage());
			log.debug(e);
		}
		catch(Throwable e){
			e.printStackTrace();
			log.debug(METHOD_NAME+": Throwable caught e.getMessage()="+e.getMessage());
			log.debug(e);
		}
		finally{
			if(null!=buffReader){
				try{
					buffReader.close();
				}
				catch(IOException e){
					e.printStackTrace();
					log.debug(e);
				}
			}
			if(null!=reader){
				try{
					reader.close();
				}
				catch(IOException e){
					e.printStackTrace();
					log.debug(METHOD_NAME+": IOException caught e.getMessage()="+e.getMessage());
					log.debug(e);
				}
				catch(Throwable e){
					e.printStackTrace();
					log.debug(METHOD_NAME+": Throwable caught e.getMessage()="+e.getMessage());
					log.debug(e);
				}
			}
			if(null!=inny){
				try{
					inny.close();
				}
				catch(IOException e){
					e.printStackTrace();
					log.debug(METHOD_NAME+": IOException caught e.getMessage()="+e.getMessage());
					log.debug(e);
				}
			}
		}
		if(log.isDebugEnabled()){
			log.debug(METHOD_NAME+": END: retVal="+retVal);
		}
		return retVal;
	}
	
	public String getScpUser(){
		return this.scpUser;
	}
	
	public String getScpwebapps1(){
		return this.scpwebapps1;
	}
	
	public String getKnown_hosts(){
		return this.known_hosts;
	}
	
	public String getId_rsa(){
		return this.id_rsa;
	}
	
	public String getExternalOrInternal(){
		return this.externalOrInternal;
	}
	
	public String getPasswd(){
		return this.passwd;
	}	
	
	private void loadProps(String filePathAndName)throws IOException{
		File theFile=new File(filePathAndName);
		if(theFile.exists()){
			synchronized(this){
				NexusDeploy.propsFile=new Properties();
				NexusDeploy.propsFile.load(new FileInputStream(theFile));
			}
		}
		else{
			throw new IOException("IOException, could not find file: "+theFile);
		}
	}


	public String getProperty(String key){
		String retVal=null;
		if(null!=NexusDeploy.propsFile && key!=null){
			retVal= NexusDeploy.propsFile.getProperty(key);
		}
		return retVal;
	}

	public String getProperty(String key, String defaultValue){
		String retVal=null;
		if(null!=NexusDeploy.propsFile && key!=null){
			retVal= NexusDeploy.propsFile.getProperty(key, defaultValue);
		}
		return retVal;
	}
	
	private void clearMembers(){
		this.scpUser="";
		this.scpwebapps1="";
		this.passwd="";
		this.known_hosts="";
		this.id_rsa="";		
		this.serversToScp="";
		this.externalOrInternal="";
		if(null!=this.webAppWarFilesAndFoldersMap){
		    this.webAppWarFilesAndFoldersMap.clear();
		}
		if(null!=this.serversToScpList){
			this.serversToScpList.clear();
		}
		if(null!=this.requestUrls){
			this.requestUrls.clear();
		}
		this.tempFolder="";
		this.installedWebAppDirStr="";
	}
}
