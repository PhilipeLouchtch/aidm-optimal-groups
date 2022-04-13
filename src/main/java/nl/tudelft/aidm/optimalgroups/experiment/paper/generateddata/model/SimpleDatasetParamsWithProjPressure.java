package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

public record SimpleDatasetParamsWithProjPressure(Integer numStudents, Integer numProjects, Integer numSlotsPerProj, GroupSizeConstraint gsc, NamedPrefGenerator prefGenerator, NamedPregroupingGenerator pregroupingGenerator) implements DatasetParams
{
	@Override
	public DatasetContext intoNewlyGeneratedDataset()
	{
		var projects = Projects.generated(numProjects, numSlotsPerProj);
		var prefGenerator = this.prefGenerator.makeGeneratorFor(projects);
		
		return new GeneratedDataContext(numStudents, projects, gsc, prefGenerator, pregroupingGenerator.generator());
	}
	
	@Override
	public String toString()
	{
		return ("DatasetParams[ st#%s, pr#%s[%s], pp[%s], gp[%s] ]").formatted(
				numStudents,
				numProjects,
				numSlotsPerProj,
				prefGenerator.shortName(),
				pregroupingGenerator().shortName()
		);
	}
}
