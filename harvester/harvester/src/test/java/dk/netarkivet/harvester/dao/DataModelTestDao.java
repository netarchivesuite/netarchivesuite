/**
 * 
 */
package dk.netarkivet.harvester.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

import dk.netarkivet.harvester.dao.HarvestDatabaseDAO;
import dk.netarkivet.harvester.dao.HarvesterDatabaseTables;
import dk.netarkivet.harvester.dao.ParameterMap;

/**
 * @author ngiraud
 *
 */
public class DataModelTestDao extends HarvestDatabaseDAO implements ResultSetExtractor<Boolean> {
	
	private static DataModelTestDao instance;
	
	public static final DataModelTestDao getInstance() {
		if (instance == null) {
			instance = new DataModelTestDao();
		}
		return instance;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Collection<HarvesterDatabaseTables> getRequiredTables() {
		return Collections.EMPTY_LIST;
	}
	
	public void testQuery(String sql) {
		testQuery(sql, new ParameterMap());
	}
	
	public void testQuery(String sql, ParameterMap params) {
		query(sql, params, this);
	}
	
	// VOID extractor
	@Override
	public Boolean extractData(ResultSet rs) throws SQLException, DataAccessException {
		return true;		
	}

}
