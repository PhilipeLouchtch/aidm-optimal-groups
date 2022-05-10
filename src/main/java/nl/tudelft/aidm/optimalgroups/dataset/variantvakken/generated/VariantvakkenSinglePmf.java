package nl.tudelft.aidm.optimalgroups.dataset.variantvakken.generated;

import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.ProjectPreferencesFromPmfGenerator;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.agent.SimpleAgent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.pref.GroupPreference;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import org.apache.commons.math3.util.Pair;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class VariantvakkenSinglePmf implements DatasetContext
{
	private final Projects projects;
	private final Agents agents;
	private final GroupSizeConstraint groupSizeConstraint;

	private final double pop220;
	private final double pop110;
	private final double pop88;

	public VariantvakkenSinglePmf(double pop220, double pop110, double pop88)
	{
		this.pop220 = pop220;
		this.pop110 = pop110;
		this.pop88 = pop88;
		var proj220 = new Project.ProjectWithStaticSlotAmount(220, 220);
		var proj110 = new Project.ProjectWithStaticSlotAmount(110, 110);
		var proj88 = new Project.ProjectWithStaticSlotAmount(88, 88);

		this.projects = Projects.from(List.of(proj220, proj88, proj110));

		var pmf = List.<Pair<Project, Double>>of(Pair.create(proj220, pop220), Pair.create(proj110, pop110), Pair.create(proj88, pop88));

		var prefGen = new ProjectPreferencesFromPmfGenerator(projects, pmf);
		
		this.agents = Agents.from(IntStream.rangeClosed(1, 400)
				.mapToObj(seqNum -> {
					return new SimpleAgent.AgentInDatacontext(seqNum, prefGen.generateNew(), GroupPreference.none(), this);
				})
				.collect(Collectors.toList())
		);

		groupSizeConstraint = new GroupSizeConstraint.Manual(0, 1);
	}

	@Override
	public String identifier()
	{
		var popTotal = pop220+pop110+pop88;
		double normPop220 = pop220 / popTotal * 100;
		double normPop110 = pop110 / popTotal * 100;
		double normPop88 = pop88 / popTotal * 100;

		return String.format("Variantvakken [ p220-(pop %.2f%%), p110-(pop %.2f%%), p88-(pop %.2f%%) ]", normPop220, normPop110, normPop88);
	}

	@Override
	public Projects allProjects()
	{
		return projects;
	}

	@Override
	public Agents allAgents()
	{
		return agents;
	}

	@Override
	public GroupSizeConstraint groupSizeConstraint()
	{
		return groupSizeConstraint;
	}
}
