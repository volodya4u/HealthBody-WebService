package edu.softserveinc.healthbody.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import edu.softserveinc.healthbody.constants.DaoConstants;
import edu.softserveinc.healthbody.constants.DaoStatementsConstant.CompetitionDBQueries;
import edu.softserveinc.healthbody.db.ConnectionManager;
import edu.softserveinc.healthbody.entity.Competition;
import edu.softserveinc.healthbody.exceptions.DataBaseReadingException;
import edu.softserveinc.healthbody.exceptions.JDBCDriverException;
import edu.softserveinc.healthbody.exceptions.QueryNotFoundException;;

public final class CompetitionDao extends AbstractDao<Competition> {
	
	private static volatile CompetitionDao instance;

	private CompetitionDao() {
		init();
	}

	public static CompetitionDao getInstance() {
		if (instance == null) {
			synchronized (CompetitionDao.class) {
				if (instance == null) {
					instance = new CompetitionDao();
				}
			}
		}
		return instance;
	}

	@Override
	protected void init() {
		for (CompetitionDBQueries competitionDBQueries : CompetitionDBQueries.values()) {
			sqlQueries.put(competitionDBQueries.getDaoQuery(), competitionDBQueries);
		}
	}
	
	@Override
	public Competition createInstance(String[] args) {
		return new Competition(
			args[0] == null ? UUID.randomUUID().toString() : args[0],
			args[1] == null ? new String() : args[1],
			args[2] == null ? new String() : args[2],
			Date.valueOf(args[3] == null ? new Date(System.currentTimeMillis()).toString() : args[3]),
			Date.valueOf(args[4] == null ? new Date(System.currentTimeMillis()).toString() : args[4]),
			args[5] == null ? UUID.randomUUID().toString() : args[5]);
	}
	
	@Override
	protected String[] getFields(final Competition entity) {
		List<String> fields = new ArrayList<>();
		fields.add(entity.getIdCompetition());
		fields.add(entity.getName());
		fields.add(entity.getDescription());
		fields.add(entity.getStart().toString());
		fields.add(entity.getFinish().toString());
		fields.add(entity.getIdCriteria().toString());
		return (String[]) fields.toArray();
	}
	
	public boolean createCompetition(final Competition competition)
			throws JDBCDriverException, QueryNotFoundException, DataBaseReadingException {
		boolean result = false;
		String query = sqlQueries.get(DaoQueries.INSERT).toString();
		if (query == null) {
			throw new QueryNotFoundException(String.format(DaoConstants.QUERY_NOT_FOUND, DaoQueries.INSERT.name()));
		}
		try (PreparedStatement pst = ConnectionManager.getInstance().getConnection().prepareStatement(query)) {
			int i = 1;
			pst.setString(i++, competition.getId());
			pst.setString(i++, competition.getName());
			pst.setString(i++, competition.getDescription());
			pst.setDate(i++, competition.getStart());
			pst.setDate(i++, competition.getFinish());
			pst.setString(i++, competition.getIdCriteria());
			result = pst.execute();
		} catch (SQLException e) {
			throw new DataBaseReadingException(DaoConstants.DATABASE_READING_ERROR, e);
		}
		return result;
	}
	
	public boolean editCompetition(final Competition competition, final String idCompetition,
			final String name, final String description, final String start,
			final String finish, final String idCriteria)
					throws QueryNotFoundException, JDBCDriverException, DataBaseReadingException {
		String[] fields = getFields(competition);	
		boolean result = false;
		int i = 1;
		updateByField(fields[0], idCompetition, fields[i++], name);
		updateByField(fields[0], idCompetition, fields[i++], description);
		updateByField(fields[0], idCompetition, fields[i++], start);
		updateByField(fields[0], idCompetition, fields[i++], finish);
		updateByField(fields[0], idCompetition, fields[i++], idCriteria);
		if (fields[3] != null) {
			result = false;
		} else if (fields[1] == name && fields[2] == description && fields[3] == start && fields[4] == finish && fields[5] == idCriteria){
			result = true;			
		}
		return result;
	}
	
	public List<Competition> view() throws JDBCDriverException, DataBaseReadingException {
		return getAll();
	}

	@Override
	public boolean deleteById(String id) throws QueryNotFoundException, JDBCDriverException, DataBaseReadingException {
		return false;
	}


}
