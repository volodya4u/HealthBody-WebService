package edu.softserveinc.healthbody.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.softserveinc.healthbody.exceptions.JDBCDriverException;
import edu.softserveinc.healthbody.log.Log4jWrapper;

public class ConnectionManager {
	private static final String FAILED_REGISTRATE_DRIVER = "Failed to Registrate JDBC Driver";
	private static final String ERROR_CONNECTION = "Error while getting connection";	
	private static final int MAX_POOL_SIZE = 15;

	private static volatile ConnectionManager instance;

	private DataSource dataSource;
	private final List<Connection> connections;

	private ConnectionManager() {
		this.connections = new ArrayList<>();
	}

	public static ConnectionManager getInstance() throws JDBCDriverException {
		return getInstance(null);
	}

	public static ConnectionManager getInstance(final DataSource dataSource) throws JDBCDriverException {
		if (instance == null) {
			synchronized (ConnectionManager.class) {
				if (instance == null) {
					instance = new ConnectionManager();
				}
			}
		}
		instance.checkStatus(dataSource);
		return instance;
	}

	/*-		dataSource		this.dataSource		Action
	 * 			null			null				create default
	 * 			null			not null			nothing
	 * 			not null		null				save dataSource
	 * 			not null		not null			if equals then nothing 
	 */
	private void checkStatus(final DataSource dataSource) throws JDBCDriverException {
		if (dataSource == null) {
			if (getDataSource() == null) {
				setDataSource(DataSourceRepository.getInstance().getPostgresDatabase());
			}
		} else if ((getDataSource() == null) || (!getDataSource().equals(dataSource))) {
			setDataSource(dataSource);
		}
	}

	private DataSource getDataSource() {
		return this.dataSource;
	}

	private void setDataSource(final DataSource dataSource) throws JDBCDriverException {
		synchronized (ConnectionManager.class) {
			this.dataSource = dataSource;
			registerDriver();
			closeAllConnections();
		}
	}

	private void registerDriver() throws JDBCDriverException {
		try {
			DriverManager.registerDriver(getDataSource().getJdbcDriver());
		} catch (SQLException e) {
			throw new JDBCDriverException(FAILED_REGISTRATE_DRIVER, e);
		}
	}

	private void populateConnectionPool() {
		while(connections.size() < MAX_POOL_SIZE) {		  
			connections.add(createNewConnection());		
		}
	}
	
	private synchronized Connection getConnectionFromPool() {
	  Connection connection;
	  if(connections.isEmpty()) {
		  populateConnectionPool();
	  } 
	  int lastElement = connections.size() - 1;
	  connection = connections.get(lastElement);
	  connections.remove(lastElement);	 
	  return connection;
	}
	
	public synchronized Connection getConnection() {
		return getConnectionFromPool();
	}
	
	private synchronized void returnConnectionToPool(Connection con) {
		if ((con != null) && (connections.size() < MAX_POOL_SIZE)){
			connections.add(con);
		} else {
			closeConnection(con);
		}
	}

	private void closeConnection(Connection con) {
			try {
				con.close();
			} catch (SQLException e) {
				Log4jWrapper.get().error(FAILED_REGISTRATE_DRIVER, e);
			}
	}

	private final Connection createNewConnection() {
		Connection connection = null;		
		try {
			connection = DriverManager.getConnection(getDataSource().getConnectionUrl(), getDataSource().getUser(),
					getDataSource().getPasswrd());
		} catch (SQLException e) {
			Log4jWrapper.get().error(ERROR_CONNECTION, e);
			Log4jWrapper.get().error(FAILED_REGISTRATE_DRIVER, e);
		}		
		return connection;
	}
		
	public final Connection beginTransaction() throws SQLException, JDBCDriverException {
		Connection con = getConnection();
		con.setAutoCommit(false);
		return con;
	}

	public final void commitTransaction(Connection con) throws SQLException, JDBCDriverException {
		con.commit();
		con.setAutoCommit(true);
		returnConnectionToPool(con);
	}

	public final void rollbackTransaction(Connection con) throws SQLException, JDBCDriverException {
		con.rollback();
		con.setAutoCommit(true);
		returnConnectionToPool(con);
	}

	private List<Connection> getAllConections() {
		return this.connections;
	}
	
	private void closeAllConnections() throws JDBCDriverException {
		if (instance != null) {
			for (Iterator<Connection> iterator = instance.getAllConections().iterator(); iterator.hasNext();) {
			    Connection conn = iterator.next();
				if (conn != null) {
					try {
						conn.close();
					} catch (SQLException e) {
						throw new JDBCDriverException(FAILED_REGISTRATE_DRIVER, e);
					}
					iterator.remove();
				}
			}
		}
	}
}
