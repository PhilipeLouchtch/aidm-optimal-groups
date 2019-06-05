package nl.tudelft.aidm.optimalgroups.metric;

import nl.tudelft.aidm.optimalgroups.algorithm.project.Matching;
import nl.tudelft.aidm.optimalgroups.model.entity.*;

public abstract class AUPCR {

    protected Matching<Group.FormedGroup, Project.ProjectSlot> matching;
    protected Projects projects;
    protected Agents students;

    protected float aupcr = -1;

    /**
     * Class to implement the Area Under Profile Curve Ratio (AUPCR) metric.
     * An AUPCR of 1 is perfect and an AUPCR of 0 is terrible.
     * Defined on page 8 of (Diebold & Bichler, 2016)
     */
    public AUPCR (Matching<Group.FormedGroup, Project.ProjectSlot> matching, Projects projects, Agents students) {
        this.matching = matching;
        this.projects = projects;
        this.students = students;
    }

    public float result() {
        if (this.aupcr == -1) {
            this.aupcr = ((float) this.aupc()) / this.totalArea();
        }
        return this.aupcr;
    }

    public abstract void printResult();

    protected abstract float totalArea();

    protected abstract int aupc();

    public static class StudentAUPCR extends AUPCR {
        public StudentAUPCR (Matching<Group.FormedGroup, Project.ProjectSlot> matching, Projects projects, Agents students) {
            super(matching, projects, students);
        }

        @Override
        public void printResult() {
            System.out.printf("Students AUPCR: %f (Area Under Profile Curve Ratio)\n", this.result());
        }

        @Override
        protected float totalArea() {
            float studentsWithPreference = 0;
            for (Agent student : this.students.asCollection()) {
                if (student.projectPreference.asArray().length > 0)
                    studentsWithPreference += 1;
            }

            float result = projects.count() * studentsWithPreference;

            // prevent division by zero
            return (result == 0) ? -1 : result;
        }

        @Override
        protected int aupc() {
            int result = 0;
            for (int r = 1; r <= this.projects.count(); r++) {
                for (Matching.Match<Group.FormedGroup, Project.ProjectSlot> match : this.matching.asList()) {
                    AssignedProjectRank assignedProjectRank = new AssignedProjectRank(match);
                    for (AssignedProjectRankStudent metric : assignedProjectRank.studentRanks()) {

                        // Student rank -1 indicates no preference, do not include this student
                        if (metric.studentsRank() <= r && metric.studentsRank() != -1) {
                            result += 1;
                        }
                    }
                }
            }

            return result;
        }
    }

    public static class GroupAUPCR extends AUPCR {
        public GroupAUPCR (Matching<Group.FormedGroup, Project.ProjectSlot> matching, Projects projects, Agents students) {
            super(matching, projects, students);
        }

        @Override
        public void printResult() {
            System.out.printf("Groups AUPCR: %f (Area Under Profile Curve Ratio)\n", this.result());
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
                for (Matching.Match<Group.FormedGroup, Project.ProjectSlot> match : matching.asList()) {
                    AssignedProjectRank assignedProjectRank = new AssignedProjectRank(match);
                    if (assignedProjectRank.groupRank() <= r) {
                        result += 1;
                    }
                }
            }

            return result;
        }
    }
}