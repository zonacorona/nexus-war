package com.rackspace.cloud.api.dao.impl;

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.springframework.transaction.annotation.Transactional;

import com.rackspace.cloud.api.dao.ILatestwardeployDao;
import com.rackspace.cloud.api.entity.Latestwardeploy;

public class LatestwardeployDaoImpl extends AbstractDaoImpl<Latestwardeploy> implements ILatestwardeployDao{

	public LatestwardeployDaoImpl(){
		super(Latestwardeploy.class);
	}

	@Transactional(readOnly=true)
	public List<Latestwardeploy> findByGroupIdAndArtifactId(String groupId,String artifactId) {
		Session session=super.getCurrentSession();
		Query query=session.createQuery("from Latestwardeploy d where d.groupid = :groupId AND d.artifactid = :artifactId");
		query.setParameter("groupId", groupId);
		query.setParameter("artifactId", artifactId);
		List<Latestwardeploy>retVal= query.list();
		return retVal;
	}
	
	@Transactional(readOnly=true)
	public List<Latestwardeploy>findAll(){
		Session session=super.getCurrentSession();
		Query query=session.createQuery("from Latestwardeploy");
		List<Latestwardeploy>retVal= query.list();
		return retVal;		
	}
	
	@Transactional(readOnly=false)
	public void upSert(String warName){
		Session session=super.getCurrentSession();
		Query query=session.createQuery("select l.warname Latestwardeploy");
	}



}
