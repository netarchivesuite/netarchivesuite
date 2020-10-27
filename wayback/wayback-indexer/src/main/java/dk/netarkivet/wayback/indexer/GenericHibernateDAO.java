/*
 * #%L
 * Netarchivesuite - wayback
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.wayback.indexer;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;

/**
 * An implementation of Generic DAO which is specialised for hibernate object stores.
 *
 * @param <T> The type of the persistent entity.
 * @param <PK> The type of the primary key for the entity.
 */
@SuppressWarnings({"unchecked"})
public class GenericHibernateDAO<T, PK extends Serializable> implements GenericDAO<T, PK> {

    private Class<T> type;

    /**
     * Constructor for the class.
     *
     * @param type the type of the persistent entity managed by this class.
     */
    public GenericHibernateDAO(Class<T> type) {
        this.type = type;
    }

    @Override
    public PK create(T o) {
        Session sess = getSession();
        sess.beginTransaction();
        PK key = (PK) sess.save(o);
        sess.getTransaction().commit();
        sess.close();
        return key;
    }

    @Override
    public T read(PK id) {
        Session sess = getSession();
        T result = (T) sess.get(type, id);
        sess.close();
        return result;
    }

    @Override
    public void update(T o) {
        Session sess = getSession();
        sess.beginTransaction();
        sess.update(o);
        sess.getTransaction().commit();
        sess.close();
    }

    @Override
    public void delete(T o) {
        Session sess = getSession();
        sess.beginTransaction();
        sess.delete(o);
        sess.getTransaction().commit();
        sess.close();
    }

    /**
     * Return a session object for managing instances of this class.
     *
     * @return the session.
     */
    protected Session getSession() {
        return HibernateUtil.getSession();
    }

    /**
     * Use this inside subclasses as a convenience method to find objects matching a given criterion.
     *
     * @param criterion the criteria to be matched.
     * @return a list of objects matching the criterion.
     */
    protected List<T> findByCriteria(Criterion... criterion) {
        Criteria crit = getSession().createCriteria(type);
        for (Criterion c : criterion) {
            crit.add(c);
        }
        return crit.list();
    }
}
