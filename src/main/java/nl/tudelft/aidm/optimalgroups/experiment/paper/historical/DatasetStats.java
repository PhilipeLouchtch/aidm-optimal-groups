package nl.tudelft.aidm.optimalgroups.experiment.paper.historical;

import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEdition;
import nl.tudelft.aidm.optimalgroups.dataset.bepsys.CourseEditionFromDb;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.MinimumReqProjectAmount;

import java.util.List;

class DatasetStats
{
	record courseEdition(int ce_id)
	{
		CourseEdition courseEdition()
		{
			var ce = CourseEditionFromDb.fromLocalBepSysDbSnapshot(ce_id);
			return ce;
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
	}
	
	public static void main(String[] args)
	{
		List.of(
			new courseEdition(3),
			new courseEdition(4),
			new courseEdition(10),
			new courseEdition(11),
			new courseEdition(14),
			new courseEdition(17),
			new courseEdition(18),
			new courseEdition(23),
			new courseEdition(39),
			new courseEdition(42),
			new courseEdition(45)
		).forEach(courseEdition -> {
			System.out.printf("id %s, minReq: %s, pressure: %s\n", courseEdition.ce_id, courseEdition.minReqProjects(), courseEdition.pressure());
		});
		    
		    
		return;
	}
}