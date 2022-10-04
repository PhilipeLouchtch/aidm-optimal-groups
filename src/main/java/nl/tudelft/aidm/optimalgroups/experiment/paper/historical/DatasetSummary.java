package nl.tudelft.aidm.optimalgroups.experiment.paper.historical;

import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.dataset.chiarandini.SDUDatasetContext;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.MinimumReqProjectAmount;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.joining;

class DatasetSummary
{
	interface Summary
	{
		String dataset_identifier();
		Integer num_students();
		Integer num_projects();
		Double pressure();
		String pregroup_dist();
		String gscAsString();
	}

	record SDUStats(SDUDatasetContext datasetContext) implements Summary
	{
//		public CEStats(int ce_id)
//		{
//			this(CourseEditionFromDb.fromLocalBepSysDbSnapshot(ce_id));
//		}

		public String dataset_identifier()
		{
			return "SDU" + datasetContext.year;
		}

		public Integer num_students()
		{
			return datasetContext.allAgents().count();
		}

		public Integer num_projects()
		{
			return datasetContext.allProjects().count();
		}

		public Integer minReqProjects()
		{
			var gsc = datasetContext.groupSizeConstraint();
			var numberOfStudents = datasetContext.allAgents().count();
			return new MinimumReqProjectAmount(gsc, numberOfStudents).asInt();
		}

		public Double pressure()
		{
			// TODO
//			var totalCapacity = 1d * datasetContext.allProjects().countAllSlots();
			
			// For SDU: count the capacity in students instead of in projects as with TUDelft
			// because we cannot compute the minReqProjects as easily. The method below is not as precise,
			// nor would an approach with "correct" minReqProjects, but it should be pretty close.
			var totalCapacity = datasetContext().allProjects().asCollection().stream()
					.mapToInt(project -> {
						var numSlots = project.slots().size();
						var projectGsc = datasetContext.groupSizeBoundsOf(project);
						return projectGsc.maxSize() * numSlots;
					})
					.sum();
			
			return 1d * num_students() / totalCapacity;
		}

		public String pregroup_dist()
		{
			var pregroupings = CliqueGroups.from(datasetContext.allAgents());
			var pregroupProportion = 1d * pregroupings.asAgents().count() / datasetContext.allAgents().count();

			// TODO: workaround for the SDU datasets because they have a per-project group size bounds
			var maxAllowedGroupSize = maxGroupUpperbound();

			// Calc distribution of pregrouping sizes
			var pregroupDist = IntStream.rangeClosed(2, maxAllowedGroupSize)
									 .mapToObj(i -> Integer.toString(pregroupings.ofSize(i).count() * i))
									 .collect(joining("|"));
			return pregroupDist;
		}

		int maxGroupUpperbound()
		{
			return datasetContext.allProjects().asCollection().stream()
						.map(datasetContext::groupSizeBoundsOf)
						.mapToInt(GroupSizeConstraint::maxSize)
						.max().orElseThrow();
		}

		public String gscAsString()
		{
//			var min = new AtomicInteger(Integer.MAX_VALUE);
//			var max  = new AtomicInteger(0);
//
			var bounds = datasetContext.allProjects().asCollection().stream()
					.map(datasetContext::groupSizeBoundsOf)
					.collect(groupingBy(Function.identity(), counting()));

//					.sorted(Comparator.comparingInt(GroupSizeConstraint::minSize))
			var boundsDist = bounds.entrySet().stream()
					.sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
					.map(entry -> String.format("%s: %s", entry.getValue(), entry.getKey().toString().replaceAll("GSC", "")))
					.collect(joining(" | "));
			
			return boundsDist;
//			return "[%s-%s]".formatted(min.getPlain(), max.getPlain()); // or dist
		}

	}

	record TUStats(CourseEdition datasetContext) implements Summary
	{
//		public CEStats(int ce_id)
//		{
//			this(CourseEditionFromDb.fromLocalBepSysDbSnapshot(ce_id));
//		}

		public String dataset_identifier()
		{
			return "CE" + datasetContext.bepSysId();
		}

		public Integer num_students()
		{
			return datasetContext.allAgents().count();
		}

		public Integer num_projects()
		{
			return datasetContext.allProjects().count();
		}
		
		public Integer minReqProjects()
		{
			var gsc = datasetContext.groupSizeConstraint();
			var numberOfStudents = datasetContext.allAgents().count();
			return new MinimumReqProjectAmount(gsc, numberOfStudents).asInt();
		}
		
		public Double pressure()
		{
			var totalCapacity = 1d * datasetContext.allProjects().countAllSlots();
			return minReqProjects() / totalCapacity;
		}
		
		public String pregroup_dist()
		{
			var pregroupings = CliqueGroups.from(datasetContext.allAgents());
			var pregroupProportion = 1d * pregroupings.asAgents().count() / datasetContext.allAgents().count();
			
			// TODO: workaround for the SDU datasets because they have a per-project group size bounds
			var maxAllowedGroupSize = maxGroupUpperbound();
			
			// Calc distribution of pregrouping sizes
			var pregroupDist = IntStream.rangeClosed(2, maxAllowedGroupSize)
									 .mapToObj(i -> Integer.toString(pregroupings.ofSize(i).count() * i))
									 .collect(joining("|"));
			return pregroupDist;
		}

		int maxGroupUpperbound()
		{
			return datasetContext.groupSizeConstraint().maxSize();
		}

		public String gscAsString()
		{
			var gsc = datasetContext.groupSizeConstraint();
			return "[%s-%s]".formatted(gsc.minSize(), gsc.maxSize());
		}
	}

	public static void println(String... strings)
	{
		var string = String.join(",", strings);
		System.out.println(string);
	}

	public static void printTable(List<? extends Summary> stats)
	{
//		println(name);
		println("==================");
		println("");

		// headers
		println("id, gsc, numStudents, numProjects, pressure, pregroup dist");

		// body
		stats.forEach(stat ->
				println(
					stat.dataset_identifier(),
					stat.gscAsString(),
					stat.num_students().toString(),
					stat.num_projects().toString(),
					stat.pressure().toString(),
					stat.pregroup_dist()
				)
		);

		println("");
	}
	
	public static void main(String[] args)
	{

		// todo make nice format string
		var tuDelft = List.of(3, 4, 10, 11, 14,17, 18, 23, 39, 42, 45)
				.stream().map(CourseEditionFromDb::fromLocalBepSysDbSnapshot).map(TUStats::new).toList();

		var sdu = List.of(2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016)
				.stream().map(SDUDatasetContext::instanceOfYear).map(SDUStats::new).toList();

		Stream.of(tuDelft, sdu)
				// The table is printed to stdout
				.forEach(DatasetSummary::printTable);

// System.out.printf("id %s, gsc: %s, minReq: %s, pressure: %s, pregroup dist: %s\n", courseEdition.id(), courseEdition.gscAsString(), courseEdition.minReqProjects(), courseEdition.pressure(), courseEdition.pregroup_dist());

		    
		    
		return;
	}
}