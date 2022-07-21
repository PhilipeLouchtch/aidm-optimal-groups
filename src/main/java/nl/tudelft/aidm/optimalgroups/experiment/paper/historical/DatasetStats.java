package nl.tudelft.aidm.optimalgroups.experiment.paper.historical;

import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.MinimumReqProjectAmount;

import java.util.List;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

class DatasetStats
{
	record CEStats(CourseEdition courseEdition)
	{
		CEStats(int ce_id)
		{
			this(CourseEditionFromDb.fromLocalBepSysDbSnapshot(ce_id));
		}
		
		int id()
		{
			return courseEdition.bepSysId();
		}
		
		int minReqProjects()
		{
			var courseEdition = courseEdition();
			return new MinimumReqProjectAmount(courseEdition.groupSizeConstraint(), courseEdition.allAgents().count())
					       .asInt();
		}
		
		double pressure()
		{
			var courseEdition = courseEdition();
			var totalCapacity = 1d * courseEdition.allProjects().countAllSlots();
			return totalCapacity / minReqProjects();
		}
		
		String pregroup_dist()
		{
			var ce = courseEdition();
			
			var pregroupings = CliqueGroups.from(ce.allAgents());
			var pregroupProportion = 1d * pregroupings.asAgents().count() / ce.allAgents().count();
			
			// workaround for the SDU datasets because they have a per-project group size bounds
			var maxAllowedGroupSize = ce.groupSizeConstraint().maxSize();
			
	         // Calc distribution of pregrouping sizes
	         var pregroupDist = IntStream.rangeClosed(2, maxAllowedGroupSize)
	                                     .mapToObj(i -> Integer.toString(pregroupings.ofSize(i).count() * i))
	                                     .collect(joining("|"));
			 return pregroupDist;
		}
		
		String gsc()
		{
			var gsc = courseEdition().groupSizeConstraint();
			return "[%s-%s]".formatted(gsc.minSize(), gsc.maxSize());
		}
		
		
	}
	
	public static void main(String[] args)
	{
		List.of(
			new CEStats(3),
			new CEStats(4),
			new CEStats(10),
			new CEStats(11),
			new CEStats(14),
			new CEStats(17),
			new CEStats(18),
			new CEStats(23),
			new CEStats(39),
			new CEStats(42),
			new CEStats(45)
		).forEach(courseEdition -> {
			System.out.printf("id %s, gsc: %s, minReq: %s, pressure: %s, pregroup dist: %s\n",
			                  courseEdition.id(), courseEdition.gsc(), courseEdition.minReqProjects(), courseEdition.pressure(), courseEdition.pregroup_dist());
		});
		    
		    
		return;
	}
}