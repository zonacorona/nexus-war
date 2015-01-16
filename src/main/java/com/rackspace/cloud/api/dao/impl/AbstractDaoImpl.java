package com.rackspace.cloud.api.dao.impl;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.rackspace.cloud.api.dao.IAbstractDao;

@SuppressWarnings("unchecked")
public abstract class AbstractDaoImpl<E> implements IAbstractDao<E> {
	
	private Class<E> entityClass;
	
	@Autowired
	private SessionFactory sessFactory;
	
	
	protected AbstractDaoImpl(Class<E> entityClass) {
		this.entityClass = entityClass;
	}
	
	public Session getCurrentSession() {
		return sessFactory.getCurrentSession();
	}
	
	public void setSessionFactory(SessionFactory sessionFact){
		this.sessFactory=sessionFact;
	}
	
	public Criteria getCriteria() {		
		return getCurrentSession().createCriteria(entityClass);
	}
	
	@Transactional(readOnly = false)
	public void save(E entity) {
		getCurrentSession().saveOrUpdate(entity);
	}
	
	@Transactional(readOnly = false)
	public void save(List<E>entities){
		try{
			Session sess=this.getCurrentSession();
			for(E anE:entities){
				sess.saveOrUpdate(anE);
			}	
		}
		catch(Throwable e){
			e.printStackTrace();
		}
	}
	
	@Transactional(readOnly = false)
	public void update(List<E>entities){
		try{
			Session sess=getCurrentSession();
			//Transaction trans=sess.beginTransaction();
			//trans.begin();
			for(E anE:entities){
				sess.update(anE);
			}
			//trans.commit();			
		}
		catch(Throwable e){
			e.printStackTrace();
		}		
	}
	
	@Transactional(readOnly = false)
	public void update(E entity) {
		getCurrentSession().update(entity);
	}
	
	@Transactional(readOnly = false)
	public void delete(E entity) {
		getCurrentSession().delete(entity);
	}	
	
	@Transactional(readOnly = true)
	public E findById(String id) {
		return (E) getCurrentSession().get(entityClass, id);
	}
	
	@Transactional(readOnly = true)
	public E findById(Serializable id) {
		return (E) getCurrentSession().get(entityClass, id);
	}
}
