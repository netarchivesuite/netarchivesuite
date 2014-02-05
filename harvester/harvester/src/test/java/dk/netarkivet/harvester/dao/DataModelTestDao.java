/**
 * 
 */
package dk.netarkivet.harvester.dao;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;

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
