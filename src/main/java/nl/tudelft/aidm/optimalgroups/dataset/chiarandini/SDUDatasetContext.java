package nl.tudelft.aidm.optimalgroups.dataset.chiarandini;

import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.pref.CombinedPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.base.MapBasedProjectPreferences;
import nl.tudelft.aidm.optimalgroups.model.pref.rank.PresentRankInPref;
import nl.tudelft.aidm.optimalgroups.model.pref.rank.RankInPref;
import nl.tudelft.aidm.optimalgroups.model.pref.rank.UnacceptableAlternativeRank;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import org.apache.commons.lang3.StringUtils;
import plouchtch.assertion.Assert;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class SDUDatasetContext implements DatasetContext
{
	public final int year;
	
	private final TypeCompatibility typeCompatibility;
	private final ProjectRecords projectRecords;
	private final Students students;
	private final ProjectPreferences projectPreferences;
	
	private Projects allProjects;
	private Agents allAgents;
	
	public static SDUDatasetContext instanceOfYear(int year)
	{
		Assert.that(2008 <= year && year <= 2016).orThrowMessage("SDU dataset for given year (%s) does not exist".formatted(year));
		
		var typeCompatibility = TypeCompatibility.inInstance(year);
		var projectRecords = ProjectRecords.inInstance(year);
		var students = Students.inInstance(year);
		var projectPreferences = ProjectPreferences.inInstance(year);
		
		var data = new SDUDatasetContext(year, typeCompatibility, projectRecords, students, projectPreferences);
		
		return data;
	}
	
	public SDUDatasetContext(int year, TypeCompatibility typeCompatibility, ProjectRecords projectRecords, Students students, ProjectPreferences projectPreferences)
	{
		this.year = year;
		
		this.typeCompatibility = typeCompatibility;
		this.projectRecords = projectRecords;
		this.students = students;
		this.projectPreferences = projectPreferences;
	}
	
	@Override
	public String identifier()
	{
		return "SDU_" + year;
	}
	
	@Override
	public String toString()
	{
		return identifier();
	}
	
	@Override
	public Projects allProjects()
	{
		if (allProjects == null)
		{
			this.allProjects = projectRecords.asProjects();
		}
		
		return allProjects;
	}
	
	@Override
	public Agents allAgents()
	{
		if (allAgents == null)
		{
			var allProjects = allProjects();
			var desiredGroups = students.asDesiredGroups();
			var counter = new AtomicInteger(1);
			
			this.allAgents = this.students.asSet().stream()
					.map(studentRecord -> {
						var rawGroup = desiredGroups.get(studentRecord);
						
						return new SDUAgent(this, counter.getAndIncrement(), projectRecords, studentRecord, projectPreferences, rawGroup, typeCompatibility);
					})
					.collect(Agents.collector);
		}
		
		return allAgents;
	}
	
	@Override
	public GroupSizeConstraint groupSizeConstraint()
	{
		// I hate doing this, but reworking the whole dataset model to cleanly support TUD and SDU datasets requires a lot of additional effort, without a reasonable payoff
		throw new RuntimeException("SDU does not have a single, common group size bound. Use SDU datasets only with methods that explicitly can handle them");
	}
	
	public SDUAgent findAgentByStudentId(StudentId studentId)
	{
		var agent = this.allAgents.asCollection().stream()
				.map(a -> (SDUAgent) a)
				.filter(SDUAgent -> SDUAgent.studentRecord.studentId().equals(studentId))
				.findAny();
		
		Assert.that(agent.isPresent()).orThrowMessage("Agent with studentId %s could not be found".formatted(studentId.asString()));
		return agent.get();
	}
	
	public GroupSizeConstraint groupSizeBoundsOf(Project project)
	{
		Assert.that(project instanceof SDUProject).orThrowMessage("Must be an SDU Project");
		Assert.that(allProjects().asCollection().contains(project)).orThrowMessage("Project must be part of the dataset");
		
		return ((SDUProject) project).groupSizeConstraint();
	}
	
	public static class SDUAgent implements Agent
	{
		private final SDUDatasetContext SDUDatasetContext;
		private final int sequenceNum;
		
		private final ProjectPreference projectPreference;
		private final GroupPreference groupPreference;
		
		private CombinedPreference combinedPreference;
		private boolean usingCombinedPreference;
		
		private final StudentRecord studentRecord;
		
		private final Set<StudentRecord> rawGroup;
		
		public SDUAgent(SDUDatasetContext SDUDatasetContext,
		                int sequenceNum,
		                ProjectRecords projectRecords,
		                SDUDatasetContext.StudentRecord studentRecord,
		                ProjectPreferences projectPreferences,
		                Set<SDUDatasetContext.StudentRecord> rawGroup,
		                SDUDatasetContext.TypeCompatibility typeCompatibility)
		{
			this.SDUDatasetContext = SDUDatasetContext;
			this.sequenceNum = sequenceNum;
			
			this.studentRecord = studentRecord;
			this.rawGroup = rawGroup;
			
			this.projectPreference = SDUProjectPreference.from(studentRecord, typeCompatibility, projectRecords, projectPreferences, this);
			
			// Must have students!
			this.groupPreference = SDUGroupPreference.from(rawGroup, SDUDatasetContext);
		}
		
		@Override
		public void replaceProjectPreferenceWithCombined(Agents agents)
		{
			this.combinedPreference = new CombinedPreference(this.groupPreference(), this.projectPreference(), agents);
			this.usingCombinedPreference = true;
		}
	
		@Override
		public void useDatabaseProjectPreferences()
		{
			this.usingCombinedPreference = false;
		}
	
		@Override
		public int groupPreferenceLength()
		{
			return this.groupPreference().asArray().length;
		}
		
		@Override
		public GroupPreference groupPreference()
		{
			return groupPreference;
		}
		
		@Override
		public ProjectPreference projectPreference()
		{
			return (usingCombinedPreference) ? this.combinedPreference : this.projectPreference;
		}
		
		@Override
		public Integer sequenceNumber()
		{
			return sequenceNum;
		}
		
		@Override
		public DatasetContext datasetContext()
		{
			return SDUDatasetContext;
		}
		
		@Override
		public String toString()
		{
			return "SDUAgent_"+sequenceNum;
		}
	}
	
	private static class SDUProjectPreference extends MapBasedProjectPreferences
	{
		public SDUProjectPreference(Object owner, Map<Project, RankInPref> asMap)
		{
			super(owner, asMap);
		}
		
		public static ProjectPreference from(StudentRecord student, TypeCompatibility compatibility, ProjectRecords projectRecords, ProjectPreferences projectPreferences, Object owner)
		{
			var projects = projectRecords.asProjects();
			
			var rawPriorities = projectPreferences.asMap().get(student.studentId);
			
			var prefsAsMap = rawPriorities.stream()
					.filter(projectPriority -> {
						var project = projects.findBy(projectPriority.projectId()).orElseThrow();
						var isCompatible = compatibility.isCompatible(student, project);
						return isCompatible;
					})
					.collect(Collectors.toMap(
							projectPriority -> (Project) projects.findBy(projectPriority.projectId()).orElseThrow(),
							projectPriority -> (RankInPref) new PresentRankInPref(projectPriority.rank())));
			
			// all projects that are not present are considered unacceptible to agent
			projects.forEach(project -> prefsAsMap.computeIfAbsent(project, p -> new UnacceptableAlternativeRank()));
			
			return new SDUProjectPreference(owner, prefsAsMap);
		}
	}
	
	private static class SDUGroupPreference implements GroupPreference
	{
		private final Set<StudentRecord> rawGroup;
		private final SDUDatasetContext SDUDatasetContext;
		private SDUAgent[] asArray;
		
		public SDUGroupPreference(Set<StudentRecord> rawGroup, SDUDatasetContext SDUDatasetContext)
		{
			this.rawGroup = rawGroup;
			this.SDUDatasetContext = SDUDatasetContext;
		}
		
		public static GroupPreference from(Set<StudentRecord> rawGroup, SDUDatasetContext SDUDatasetContext)
		{
			return new SDUGroupPreference(rawGroup, SDUDatasetContext);
		}
		
		@Override
		public Agent[] asArray()
		{
			if (this.asArray == null) {
				this.asArray = rawGroup.stream()
						.map(studentRecord -> SDUDatasetContext.findAgentByStudentId(studentRecord.studentId()))
						.toArray(SDUAgent[]::new);
			}
			
			return this.asArray;
		}
		
		@Override
		public List<Agent> asListOfAgents()
		{
			return List.of(asArray());
		}
		
		@Override
		public Integer count()
		{
			return asArray().length;
		}
	}
	
	private record TypeCompatibility(Map<StudentType, Set<ProjectType>> asMap)
	{
		static TypeCompatibility inInstance(int year)
		{
			var asMap = DataFile.types(year)
					.readIntoObjects(TypeRecord::fromColumnsInFile)
					.stream()
					.flatMap(Collection::stream)
					.collect(groupingBy(TypeRecord::studentType, mapping(TypeRecord::acceptableProjectType, toSet())));
			
			return new TypeCompatibility(Collections.unmodifiableMap(asMap));
		}
		
		public boolean isCompatible(StudentRecord studentRecord, SDUProject SDUProject)
		{
			var compatibleProjectTypesForStudent = this.asMap().getOrDefault(studentRecord.studentType, Set.of());
			return compatibleProjectTypesForStudent.contains(SDUProject.projectType());
		}
	}
	
	private record ProjectRecords(Map<ProjectId, SDUProject> asMap)
	{
		static ProjectRecords inInstance(int year)
		{
			var sequenceNum = new AtomicInteger(1);
			var slotsAsList = DataFile.projects(year).readIntoObjects(ProjectSlotRecord::fromColumnsInFile);
			var slotsAsSet = new HashSet<>(slotsAsList);
			var groupedByProjectId = new HashSet<>(slotsAsSet).stream().collect(groupingBy(ProjectSlotRecord::projectId, toSet()));
			
//			var slotsByTypeAndGsc = slotsAsSet.stream()
//					.collect(groupingBy(slot -> slot.projectType, groupingBy(ProjectSlotRecord::groupSizeConstraint)));

//			var gscs = slotsAsSet.stream()
//					.map(projectSlotRecord -> Pair.of(projectSlotRecord.gsc_lb(), projectSlotRecord.gsc_ub()))
//					.map(gscAsPair -> GroupSizeConstraint.manual(gscAsPair.getKey().asInt(), gscAsPair.getValue().asInt()))
//					.collect(collectingAndThen(toSet(), ArrayList::new));
			
//			Assert.that(gscs.size() == 1).orThrowMessage("Different group sizes per project not yet supported");
			
			var asMap = groupedByProjectId.entrySet().stream()
					.map(project -> {
						
						var projectRecord = new SDUProject(
								sequenceNum.getAndIncrement(),
								project.getKey(),
								project.getValue()
						);
						
						return Map.entry(project.getKey(), projectRecord);
					})
					.collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
			
			return new ProjectRecords(asMap);
		}
		
		public SDUProjects asProjects()
		{
			var henk = this;
			return new SDUProjects(henk);
		}
	}
	
	private record SDUProjects(ProjectRecords projectRecords) implements Projects
	{
		public Optional<SDUProject> findBy(ProjectId projectId)
		{
			return Optional.ofNullable(
					projectRecords.asMap().get(projectId)
			);
		}
		
		@Override
		public Collection<Project> asCollection()
		{
			return Collections.unmodifiableCollection(projectRecords.asMap().values());
		}
	}
	
	private record Students(Set<StudentRecord> asSet)
	{
		static Students inInstance(int year)
		{
			var students = DataFile.students(year).readIntoObjects(StudentRecord::fromColumnsInFile);
			
			return new Students(new HashSet<>(students));
		}
		
		public Map<StudentRecord, Set<StudentRecord>> asDesiredGroups()
		{
			var groupedByGroupId = this.asSet.stream()
					.collect(groupingBy(StudentRecord::groupId));
			
			var studentGroupMapping = new HashMap<StudentRecord, Set<StudentRecord>>(asSet.size());
			for (StudentRecord studentRecord : asSet)
			{
				var peers = groupedByGroupId.get(studentRecord.groupId());
				
				var peersAsSet = new HashSet<>(peers);
				peersAsSet.remove(studentRecord); // remove self
				
				studentGroupMapping.put(studentRecord, peersAsSet);
			}
			
			return studentGroupMapping;
		}
	}
	
	private record ProjectPreferences(Map<StudentId, PriorityQueue<ProjectPriority>> asMap)
	{
		static ProjectPreferences inInstance(int year)
		{
			var allPrioritiesAsList = DataFile.priorities(year).readIntoObjects(ProjectPriority::fromColumsInFile);
			
			var asMap = allPrioritiesAsList.stream()
					.collect(groupingBy(ProjectPriority::studentId, Collectors.toCollection(() -> new PriorityQueue<>(Comparator.comparing(ProjectPriority::rank)))));
			
			return new ProjectPreferences(asMap);
		}
	}
	
	private record StudentId(String asString) {}
	
	private record StudentType(String asString) {}
	
	private record ProjectType(String asString) {}
	
	private record ProjectId(Integer asInt)
	{
		static ProjectId fromString(String asString)
		{
			return new ProjectId(Integer.parseInt(asString));
		}
	}
	
	private record ProjectSlotIndex(Integer asInt)
	{
		public ProjectSlotIndex {
			Assert.that(0 <= asInt && asInt <= 9).orThrowMessage("Unsupported index number, bug?: parsing issues?");
		}
		
		public static ProjectSlotIndex fromString(String asString)
		{
			// Proably wiser to simply use an atomicInteger and use whatever "index" the dataset contains as a slot name
			// and not do all this parsing logic
			
			Assert.that(asString.length() <= 1).orThrowMessage("ProjectSlotIndex must be empty or a single letter");
			
			// when project only has one slot
			if (asString.isBlank()) {
				asString = "a";
			}
			
			// apprantly indexes may also be numbers...
			var asIntTmp = ((int) asString.charAt(0));
			if ( ((int) '0') <= asIntTmp && asIntTmp <= ((int) '9') ) {
				asString = "" + (char) ('a' + asIntTmp - '0');
			}
			
			var asInt = ((int) asString.toLowerCase().charAt(0)) - (int) 'a';
			return new ProjectSlotIndex(asInt);
		}
	}
	
	private record GroupsizeLowerbound(Integer asInt) {}
	
	private record GroupsizeUpperbound(Integer asInt) {}
	
	private record GroupId(Integer asInt)
	{
		static GroupId fromString(String asString)
		{
			return new GroupId(Integer.parseInt(asString));
		}
	}
	
	private record TypeRecord(StudentType studentType, ProjectType acceptableProjectType)
	{
		static Set<TypeRecord> fromColumnsInFile(String[] cols)
		{
			Function<String,String> stripQuotes = (String string) -> string.replaceAll("\"", "");
			var studentType = new StudentType(stripQuotes.apply(cols[0]));
			
			return Arrays.stream(cols)
					.skip(1) // first element is the student
					.map(stripQuotes)
					.map(acceptibleProjectType -> new TypeRecord(studentType, new ProjectType(acceptibleProjectType)))
					.collect(toSet());
		}
	}
	
	private static class SDUProject implements Project
	{
		private final int sequenceNum;
		private final ProjectId projectId;
		private final ProjectType projectType;
		private final List<Project.ProjectSlot> slots;
		private final GroupSizeConstraint groupSizeConstraint;
		
		public SDUProject(int sequenceNum, ProjectId projectId, Set<ProjectSlotRecord> slotRecords)
		{
			this.sequenceNum = sequenceNum;
			this.projectId = projectId;
			
			var projectType = slotRecords.stream().map(ProjectSlotRecord::projectType).distinct().collect(toList());
			Assert.that(projectType.size() == 1).orThrowMessage("Projects are expected to have only one single type");
			this.projectType = projectType.get(0);
			
			this.slots = slotRecords.stream().map(slotRecord -> OwnedProjectSlot.from(slotRecord, this)).collect(toList());
			
			var gscs = slotRecords.stream().map(ProjectSlotRecord::groupSizeConstraint).distinct().collect(toList());
			Assert.that(gscs.size() == 1).orThrowMessage("A Project is expected to have homogenous group size bounds over all their slots");
			this.groupSizeConstraint = gscs.get(0);
		}
		
		@Override
		public String name()
		{
			return String.format("SDU_proj_%s(seq_%s)", projectId.asInt(), sequenceNum());
		}
		
		@Override
		public String toString()
		{
			return name();
		}
		
		@Override
		public int sequenceNum()
		{
			return sequenceNum;
		}
		
		@Override
		public List<? extends ProjectSlot> slots()
		{
			return slots;
		}
		
		public GroupSizeConstraint groupSizeConstraint()
		{
			return this.groupSizeConstraint;
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (this == o)
				return true;
			if (!(o instanceof SDUProject))
				return false;
			SDUProject that = (SDUProject) o;
			return projectId.equals(that.projectId);
		}
		
		@Override
		public int hashCode()
		{
			return Objects.hash(projectId);
		}
		
		public ProjectType projectType()
		{
			return projectType;
		}
	}
	
	private record OwnedProjectSlot(SDUProject belongingToProject, ProjectSlotIndex slotIndex) implements Project.ProjectSlot
	{
		@Override
		public String id()
		{
			return "%s_slot%s".formatted(belongingToProject.name(), slotIndex.asInt());
		}

		@Override
		public int index()
		{
			return slotIndex.asInt();
		}

		public static OwnedProjectSlot from(ProjectSlotRecord projectSlotRecord, SDUProject owner)
		{
			return new OwnedProjectSlot(owner, projectSlotRecord.slotIndex);
		}
	}
	
	private record ProjectSlotRecord(ProjectId projectId, ProjectSlotIndex slotIndex, GroupsizeLowerbound gsc_lb, GroupsizeUpperbound gsc_ub, ProjectType projectType)
	{
		static ProjectSlotRecord fromColumnsInFile(String[] cols)
		{
			var projectId = ProjectId.fromString(cols[0]);
			var slotIndex = ProjectSlotIndex.fromString(cols[1]);
			var gscLw = new GroupsizeLowerbound(Integer.parseInt(cols[2]));
			var gscUb = new GroupsizeUpperbound(Integer.parseInt(cols[3]));
			var projectType = new ProjectType(cols[4]);
			
			return new ProjectSlotRecord(projectId, slotIndex, gscLw, gscUb, projectType);
		}
		
		public GroupSizeConstraint groupSizeConstraint()
		{
			return GroupSizeConstraint.manual(gsc_lb.asInt(), gsc_ub.asInt());
		}
	}
	
	private record StudentRecord(GroupId groupId, StudentId studentId, StudentType studentType)
	{
		static StudentRecord fromColumnsInFile(String[] cols)
		{
			var groupId = GroupId.fromString(cols[0]);
			var studentId = new StudentId(cols[1]);
			var studentType = new StudentType(cols[2]);
			
			return new StudentRecord(groupId, studentId, studentType);
		}
	}
	
	private record ProjectPriority(StudentId studentId, ProjectId projectId, int rank)
	{
		static ProjectPriority fromColumsInFile(String[] cols)
		{
			var studentId = new StudentId(cols[0]);
			var projectId = ProjectId.fromString(cols[1]);
			var rank = Integer.parseInt(cols[2]);
			
			return new ProjectPriority(studentId, projectId, rank);
		}
	}
	
	private record DataFile(int year, String filename)
	{
		private static final String instancesPath = "data/chiarandini";
		
		private static DataFile projects(int year) {
			return new DataFile(year, "tmp_projects.txt");
		}
		
		private static DataFile students(int year) {
			return new DataFile(year, "tmp_students.txt");
		}
		
		private static DataFile priorities(int year) {
			return new DataFile(year, "tmp_priorities.txt");
		}
		
		private static DataFile types(int year) {
			return new DataFile(year, "tmp_types.txt");
		}
		
		public File asFile()
		{
			var file = new File(String.format("%s/%s/%s", instancesPath, year, filename));
			return file;
		}
		
		public <T> List<T> readIntoObjects(Function<String[], T> mapper)
		{
			try (var lines = Files.lines(this.asFile().toPath()))
			{
				var results = new ArrayList<T>();
				lines.forEach(line -> {
					String[] cols = StringUtils.splitPreserveAllTokens(line, ';');
					var record = mapper.apply(cols);
					results.add(record);
				});
				
				return results;
			}
			catch (Exception e)
			{
				throw new RuntimeException(e);
			}
		}
	}
}
