package nl.tudelft.aidm.optimalgroups.algorithm.project.da;

import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.project.Project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ProposableProject implements Project
{
	private final int capacity;
	public final Project project;

	private List<TentativelyAcceptedProposal> tentativelyAccepted;
	private Consumer<ProposingAgent> rejectedAgentConsumer;
//	private Function<ProposingAgent, Integer> utilityAfterReject;

	/**
	 * @param project The underlying project
	 * @param rejectedAgentConsumer Handles rejection of the given agent
	 */
	public ProposableProject(Project project, Consumer<ProposingAgent> rejectedAgentConsumer/*, Function<ProposingAgent, Integer> utilityAfterReject*/)
	{
		this.capacity = project.slots().size();
		this.tentativelyAccepted = new ArrayList<>(capacity);

		this.project = project;
		this.rejectedAgentConsumer = rejectedAgentConsumer;
//		this.utilityAfterReject = utilityAfterReject;
	}

	public ProposalAnswer receiveProposal(Proposal proposal)
	{
		boolean notAtCapacity = tentativelyAccepted.size() < capacity;

		// Have room - accept
		if (notAtCapacity) {
			tentativelyAccepted.add(new TentativelyAcceptedProposal(proposal));
			return ProposalAnswer.ACCEPT;
		}

		// No capacity left - decline proposal, or decline this proposing agent?
// TODO: Use this to do a search - it's a "what if reject, where will the agent end up (heuristic?)"
//		int utilityAfterRejectingThisProposingAgent = utilityAfterReject.apply(agent);

		// Find all agents that are better off being rejected than the currently proposing agent
		var rejectableProposals = tentativelyAccepted.stream()
			.filter(tentativelyAcceptedProposal ->
				tentativelyAcceptedProposal.proposal.utilityIfRejected() > proposal.utilityIfRejected())
			.collect(Collectors.toList());

		if (rejectableProposals.isEmpty()) {
			// Nobody is better off being rejected than the currently proposing agent
			return ProposalAnswer.REJECT;
		}


		// Fairness IDEA: keep track of num demotions and demote the agent with least amount of demotions

		// For now: kick top (propably the oldest)
		var toReject = rejectableProposals.remove(0);
		rejectedAgentConsumer.accept(toReject.proposal.proposingAgent);

		tentativelyAccepted.remove(toReject);
		tentativelyAccepted.add(new TentativelyAcceptedProposal(proposal));

		return ProposalAnswer.ACCEPT;
	}

	public Collection<Agent> acceptedAgents()
	{
		return tentativelyAccepted.stream().map(tentativelyAcceptedProposal -> tentativelyAcceptedProposal.proposal.proposingAgent.agent)
			.collect(Collectors.toUnmodifiableList());
	}

	@Override
	public String name()
	{
		return project.name();
	}

	@Override
	public int id()
	{
		return project.id();
	}

	@Override
	public List<ProjectSlot> slots()
	{
		return project.slots();
	}


	public enum ProposalAnswer
	{
		ACCEPT,
		REJECT
	}

	class TentativelyAcceptedProposal
	{
		public final Proposal proposal;

		public TentativelyAcceptedProposal(Proposal proposal)
		{
			this.proposal = proposal;
		}
	}

}
