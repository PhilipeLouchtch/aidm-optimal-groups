package nl.tudelft.aidm.optimalgroups.dataset.bepsys.pref;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.model.agent.SimpleAgent;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;
import org.sql2o.Query;
import org.sql2o.ResultSetHandler;
import org.sql2o.Sql2o;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GroupPreferenceInDb implements GroupPreference
{
	private DataSource dataSource;
	private Integer bepSysUserId;
	private CourseEdition courseEdition;

	private List<Agent> asList = null;
	private Agent[] asArray = null;
	
	public GroupPreferenceInDb(DataSource dataSource, Integer bepSysUserId, CourseEdition courseEdition)
	{
		this.dataSource = dataSource;
		this.bepSysUserId = bepSysUserId;
		this.courseEdition = courseEdition;
	}

	@Override
	public Agent[] asArray()
	{
		if (asArray == null)
		{
			asArray = asListOfAgents().toArray(Agent[]::new);
		}

		return asArray;
	}

	@Override
	public List<Agent> asListOfAgents()
	{
		// The friend identifiers are bepsys/PF user_id's so we need to look up the agents in our courseEdition datasetcontext
		// furthermore, some friends may not be part of the dataset (never signed up for example)
		Function<Integer, Agent> findAgentByBepSysId = friendAgentId ->
				courseEdition.allAgents().asCollection().stream()
						// not pretty (i.e. quite hacky)
						.map(agent -> (SimpleAgent.AgentInBepSysSchemaDb) agent)
						.filter(agent -> agent.bepSysUserId.equals(friendAgentId))
						.findAny().orElseGet(() -> {
//								System.out.printf("Warning, friend not found: %s of %s peer pref in CE %s\n", friendAgentId, bepSysUserId, courseEdition.bepSysId());
								return null;
						});
		
		if (asList == null) {
			asList = fetchFromDb().stream()
					.map(findAgentByBepSysId)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
		}

		return Collections.unmodifiableList(asList);
	}

	@Override
	public Integer count()
	{
		return asArray().length;
	}
	
	@Override
	public String toString()
	{
		return "group pref: " + Arrays.toString(asArray());
	}


	private List<Integer> fetchFromDb()
	{
		final var sql = "SELECT student_id " +
			"FROM student_preferences " +
			"WHERE user_id = :userId AND course_edition_id = :courseEditionId";

		var sql2o = new Sql2o(dataSource);
		try (var conn = sql2o.open())
		{
			Query query = conn.createQuery(sql)
				.addParameter("userId", bepSysUserId)
				.addParameter("courseEditionId", courseEdition.bepSysId());

			List<Integer> preferredStudents = query.executeAndFetch((ResultSetHandler<Integer>) resultSet -> resultSet.getInt("student_id"));
			return preferredStudents;
		}
	}
}
