package com.rackspace.cloud.api.dao;

import java.util.List;

import com.rackspace.cloud.api.entity.Latestwardeploy;

public interface ILatestwardeployDao extends IAbstractDao<Latestwardeploy>{
	
	public List<Latestwardeploy>findAll();
	public List<Latestwardeploy>findByGroupIdAndArtifactId(String groupId, String artifactId);
	public void upSert(String warname);
}
