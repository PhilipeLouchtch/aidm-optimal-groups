package nl.tudelft.aidm.optimalgroups.dataset.bepsys;

import nl.tudelft.aidm.optimalgroups.dataset.bepsys.project.ProjectsInDb;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.agent.SimpleAgent;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import org.sql2o.GenericDatasource;
import org.sql2o.Query;
import org.sql2o.ResultSetHandler;
import org.sql2o.Sql2o;
import plouchtch.lang.Lazy;
import plouchtch.util.ComputerName;

import javax.sql.DataSource;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class CourseEditionFromDb extends CourseEdition
{
	private final static Map<Integer, CourseEditionFromDb> cachedCourseEditions = new HashMap<>();

	protected final DataSource dataSource;

	protected final int courseEditionId;
	
	protected final Lazy<Agents> agents;
	protected final Lazy<Projects> projects;
	protected final Lazy<GroupSizeConstraint> groupSizeConstraint;
	
	protected CourseEditionFromDb(DataSource dataSource, int courseEditionId)
	{
		super(courseEditionId);
		this.dataSource = dataSource;
		this.courseEditionId = courseEditionId;

		this.agents = new Lazy<>(() -> fetchAgents(dataSource, this));
		this.projects = new Lazy<>(() -> fetchProjects(dataSource, this));
		
		this.groupSizeConstraint = new Lazy<>(() -> new GroupSizeConstraintBepSys(dataSource, this));
	}

	public static CourseEditionFromDb fromLocalBepSysDbSnapshot(int courseEditionId)
	{
		return CourseEditionFromDb.fromBepSysDatabase(dataSourceToLocalDb(), courseEditionId);
	}

	public static CourseEditionFromDb fromBepSysDatabase(DataSource dataSource, int courseEditionId)
	{
		CourseEditionFromDb courseEdition = cachedCourseEditions.get(courseEditionId);
		if (courseEdition == null)
		{
			courseEdition = new CourseEditionFromDb(dataSource, courseEditionId);
			cachedCourseEditions.put(courseEditionId, courseEdition);
		}

		return courseEdition;
	}

	public Integer bepSysId()
	{
		return courseEditionId;
	}

	@Override
	public String identifier()
	{
		return String.format("CourseEdition[%s]_[s%s_p%s]_%s", courseEditionId, allAgents().count(), allProjects().count(), groupSizeConstraint());
	}

	@Override
	public Projects allProjects()
	{
		return projects.get();
	}

	@Override
	public Agents allAgents()
	{
		return agents.get();
	}
	
	@Override
	public String toString()
	{
		return identifier();
	}
	
	
	
	public Optional<Agent> findAgentByUserId(Integer bepSysUserId)
	{
		return this.allAgents().asCollection().stream()
				.map(agent -> (SimpleAgent.AgentInBepSysSchemaDb) agent)
				.filter(agent -> agent.bepSysUserId.equals(bepSysUserId))
				.map(agent -> (Agent) agent)
				.findAny();
	}
	
	public Optional<Project> findProjectByProjectId(Integer bepSysProjectId)
	{
		return this.allProjects().asCollection().stream()
				.map(project -> (Project.BepSysProject) project)
				.filter(project -> bepSysProjectId.equals(project.bepsysId))
				.map(project -> (Project) project)
				.findAny();
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CourseEditionFromDb that = (CourseEditionFromDb) o;
		return identifier().equals(that.identifier());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(identifier());
	}

	@Override
	public GroupSizeConstraint groupSizeConstraint()
	{
		return groupSizeConstraint.get();
	}


	private static Agents fetchAgents(DataSource dataSource, CourseEditionFromDb courseEdition)
	{
		var sql2o = new Sql2o(dataSource);
		try (var connection = sql2o.open())
		{
			var query = connection.createQuery(
					"SELECT distinct user_id as user_id " +
					"FROM course_participations " +
					"WHERE course_edition_id = :courseEditionId AND status = 1"
				)
				.addParameter("courseEditionId", courseEdition.bepSysId());
			
			List<Integer> userIds = query.executeAndFetch((ResultSetHandler<Integer>) resultSet -> resultSet.getInt("user_id"));
			
			var agentSequenceNumber = new AtomicInteger(1);
			List<Agent> agents = userIds.stream()
				.map(id -> new SimpleAgent.AgentInBepSysSchemaDb(dataSource, agentSequenceNumber.getAndIncrement(), id, courseEdition))
				.collect(Collectors.toList());

			return Agents.from(agents);
		}
	}

	private static Projects fetchProjects(DataSource dataSource, CourseEditionFromDb courseEdition)
	{
		// Use the project_preferences table as proxy for the projects table
		// to work around an issue in the datadumps
		var sqlProjects = """
				WITH p AS (
	                SELECT  	project_id, course_edition_id
	                FROM		project_preferences
	                GROUP BY 	project_id, course_edition_id
                )
                
                SELECT      p.project_id as id, cc.max_number_of_groups as numSlots
                FROM        p
                INNER JOIN  course_configurations as cc
                            ON p.course_edition_id = cc.course_edition_id
                WHERE       p.course_edition_id = :courseEditionId
				""";
		try (var connection = new Sql2o(dataSource).open())
		{
			Query query = connection.createQuery(sqlProjects);
			query.addParameter("courseEditionId", courseEdition.courseEditionId);
			
			var sequenceNumber = new AtomicInteger(1);

			List<Project> projectsAsList = query.executeAndFetch(
				(ResultSetHandler<Project>) rs ->
					new Project.BepSysProject(sequenceNumber.getAndIncrement(), rs.getInt("id"), rs.getInt("numSlots"))
			);

			return new ProjectsInDb(projectsAsList, courseEdition);
		}
	}

	private static DataSource dataSourceToLocalDb()
	{
		switch (ComputerName.ofThisMachine().toString())
		{
			case "COOLICER-DESK":
			case "PHILIPE-DESK":
			case "BAP-PF3XFB0B":
				return new GenericDatasource("jdbc:mysql://localhost:3306/adim_new", "henk", "henk");
			case "PHILIPE-LAPTOP":
				return new GenericDatasource("jdbc:mysql://localhost:3306/test", "henk", "henk");
			default:
				throw new RuntimeException("Unknown machine, don't know connection string to DB");
		}
	}
}
