package dk.netarkivet.wayback.indexer;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;

/**
 * An implementation of Generic DAO which is specialised for hibernate object
 * stores.
 * @param <T> The type of the persistent entity.
 * @param <PK> The type of the primary key for the entity.
 */
@SuppressWarnings({ "unchecked"})
public class GenericHibernateDAO<T, PK extends Serializable> implements GenericDAO<T, PK> {

    private Class<T> type;

    /**
     * Constructor for the class.
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
        T result =  (T) sess.get(type, id);
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
     * @return the session.
     */
    protected Session getSession() {
        return HibernateUtil.getSession();
    }



    /**
     * Use this inside subclasses as a convenience method to find objects
     * matching a given criterion.
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
