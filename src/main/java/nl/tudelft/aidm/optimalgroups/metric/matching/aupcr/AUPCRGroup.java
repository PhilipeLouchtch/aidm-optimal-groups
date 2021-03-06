package nl.tudelft.aidm.optimalgroups.metric.matching.aupcr;

import nl.tudelft.aidm.optimalgroups.metric.rank.AssignedRank;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;

public class AUPCRGroup extends AUPCR {

    private final Matching<? extends Group, Project> matching;
    private final Projects projects;
    private final Agents students;

    public AUPCRGroup(Matching<? extends Group, Project> matching) {
        this(matching, matching.datasetContext().allProjects(), matching.datasetContext().allAgents());
    }


    public AUPCRGroup(Matching<? extends Group, Project> matching, Projects projects, Agents students) {
        this.matching = matching;
        this.projects = projects;
        this.students = students;
    }

    @Override
    public void printResult() {
        System.out.printf("Groups AUPCR: %f\n", this.asDouble());
    }

    @Override
    protected float totalArea() {
        int totalProjectCapacity = 0;
        for (Project p : this.projects.asCollection()) {
            totalProjectCapacity += p.slots().size();
        }

        float result = (projects.count() * Math.min(this.matching.asList().size(), totalProjectCapacity));

        // prevent division by zero
        return (result == 0) ? -1 : result;
    }

    @Override
    protected int aupc() {
        int result = 0;
        for (int r = 1; r <= projects.count(); r++) {
            for (Match<? extends Group, Project> match : matching.asList()) {
                AssignedRank.ProjectToGroup assignedProjectRank = new AssignedRank.ProjectToGroup(match);
                if (r >=assignedProjectRank.asInt().orElseThrow()) {
                    result += 1;
                }
            }
        }

        return result;
    }
}
