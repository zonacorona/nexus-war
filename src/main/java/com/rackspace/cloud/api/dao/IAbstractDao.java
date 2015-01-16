package com.rackspace.cloud.api.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Session;

public interface IAbstractDao<E> {
	
	public Session getCurrentSession();
	
	public void save(E entity);
	public void save(List<E>entities);
	public void update(List<E>entities);
	
	public void update(E entity);
	
	public void delete(E entity);
	
	public E findById(String id);
	
	public E findById(Serializable id);
}