package com.rackspace.cloud.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;

public class JenkinsBuild {

	public static void main(String[] args){
		JenkinsBuild build=new JenkinsBuild();
		File propsFile=new File("jenkins.properties");
		Properties props=new Properties();
		
//		try{
//			props.load(new FileInputStream(propsFile));
//			String jenkinsUrl=props.getProperty("jenkinsurl");
//			String jobName=props.getProperty("jobname");
//			String username=props.getProperty("username");
//			String password=props.getProperty("password");
//			
//			System.out.println("~~~~~~~jenkinsUrl="+jenkinsUrl);
//			System.out.println("~~~~~~~jobName="+jobName);
//			System.out.println("~~~~~~~username="+username);
//			System.out.println("~~~~~~~password="+password);
//			HttpClient client=build.authenticate(jenkinsUrl,username,password);
//			build.launchBuildWithParameters(client, jenkinsUrl, jobName, "");
//		}
//		catch(HttpException e){
//			e.printStackTrace();
//		}
//		catch(IOException e){
//			e.printStackTrace();
//		}
		Date now= new Date();
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-ms/");
		System.out.println(sdf.format(now));
	}

	private HttpClient authenticate(String jenkinsUrl,String username, String password)throws IOException, HttpException{
		HttpClient client = new HttpClient();
		PostMethod postMethodAuth = new PostMethod(jenkinsUrl + "/j_acegi_security_check");
		NameValuePair[] postData = new NameValuePair[3];
		postData[0] = new NameValuePair("j_username", username);
		postData[1] = new NameValuePair("j_password", password);
		postData[2] = new NameValuePair("Login", "login");
		postMethodAuth.addParameters(postData);
		try {
			int status= client.executeMethod(postMethodAuth);
			System.out.println(status + "\n"+ postMethodAuth.getResponseBodyAsString());			
		} finally {
			postMethodAuth.releaseConnection();
		}
		return client;		
	}

	/**
	 * The Jenkins build must have "This build is parameterized" enabled
	 * Then in the Goals and options under the Build section we must have -Dsecurity=${security} enabled
	 */
	private void launchBuildWithParameters(HttpClient client, String jenkinsUrl, String jobName, String buildParams)throws IOException, HttpException{
		String url=jenkinsUrl+"/job/"+jobName;
		url+="/buildWithParameters?";
		if(null!=buildParams&&!buildParams.isEmpty()){			
			url+=buildParams;
		}
		else{
			url+="security=external";
		}
		GetMethod getMethod = new GetMethod(url);
		try{
			System.out.println("^^^^^^^^^^url="+url);
			int status=client.executeMethod(getMethod);			
			System.out.println("~~~~~~status="+status+"\n"+getMethod.getResponseBodyAsString());
		}
		finally{
			getMethod.releaseConnection();
		}
	}
}
