package com.rackspace.cloud.api;

public class NexusContentItem{
	
	private String lastModified;
    private String resourceURI;
    private String fileName;
    private int    size;
	
	public NexusContentItem(){
	    this.resourceURI=null;

	}
	
	public void setFileName(String fileName){
		this.fileName=fileName;
	}
	
	public String getFileName(){
		return this.fileName;
	}
	
	public NexusContentItem(String resourceURI){
		this.resourceURI=resourceURI;
	}
	
	public void setResourceURI(String resourceURI){
		this.resourceURI=resourceURI;
	}
	
	public String getResourceURI(){
		return this.resourceURI;
	}

	public String getLastModified() {
		return lastModified;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String toString(){
		StringBuffer retVal=new StringBuffer("{ ");
		
		retVal.append("resourceURI=");
		retVal.append(this.getResourceURI());
		retVal.append(", last modified=");
		retVal.append(this.getLastModified());
		retVal.append(", fileName=");
		retVal.append(this.fileName);
		retVal.append(", size=");
		retVal.append(this.getSize());
		retVal.append("}");
		return retVal.toString();
	}

//	public int compareTo(NexusContentItem art){
		
//		int retVal=1;		
//		if(null!=art.getVersion()){	    
//		    if(this.getVersion()!=null){
		    	//we just want to compare the version before the -SNAPSHOT
//		    	String artStrArr[]=art.getVersion().split("-");
//		    	String thisStrArr[]=this.getVersion().split("-");
//		    	if(null!=artStrArr){
//		    		if(thisStrArr!=null){
//		    			retVal=thisStrArr[0].compareTo(artStrArr[0]);
//		    		}
//		    		else{
//		    			retVal=-1;
//		    		}
//		    	}
//		    	else{
//		    		//both have null version make it equal
//		    		if(null==thisStrArr){
//		    			retVal=0;
//		    		}
//		    	}
//		        retVal=this.getVersion().compareTo(art.getVersion());
//		    }
//		}	
//		return retVal;
//	}

}
