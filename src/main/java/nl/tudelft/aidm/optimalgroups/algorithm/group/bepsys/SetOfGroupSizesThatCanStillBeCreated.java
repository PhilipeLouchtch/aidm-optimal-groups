package nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys;

import nl.tudelft.aidm.optimalgroups.algorithm.holistic.branchnbound.group.GroupFactorization;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;

final public class SetOfGroupSizesThatCanStillBeCreated
{
    public int[] groupFactorization;
    private int nrStudents;
    private final GroupSizeConstraint groupSizeConstraint;

    /**
     * Given an amount of students and group size constraints, create a set of possible group sizes
     * This set can be used for assignment/allocation algorithms by keeping track how many of which group size can still be formed
     * To keep track of intermediate group sets, recordGroupFormedOfSize will decrement the amount of groups formable for that size, if possible
     * To check if a group with a certain size can be made, use mayFormGroupOfSize
     */
    public SetOfGroupSizesThatCanStillBeCreated(int nrStudents, GroupSizeConstraint groupSizeConstraint){
        this.nrStudents = nrStudents;
        this.groupSizeConstraint = groupSizeConstraint;
        
        // This is actually one of possible factorizations,
        // more precisely, it is a factorization wiuth least number of groups
        this.groupFactorization = GroupFactorization.cachedInstanceFor(groupSizeConstraint)
                                                    .forGivenNumberOfStudents(nrStudents)
                                                    .numGroupsOfSize();
    }


    /**
     * Check if a group with a certain size can be formed
     * @param groupSize The size of the group
     * @return True if the group can be formed, false if not
     */
    public boolean mayFormGroupOfSize(int groupSize)
    {
        if (groupSize < 0 || groupSize > groupFactorization.length-1)
            return false;
        
        return groupFactorization[groupSize] > 0;
    }

    /**
     * Try to form a group, decrementing the amount of that size if it succeeds
     * @param groupSize The size of a group
     */
    public void recordGroupFormedOfSize(int groupSize)
    {
        if (groupSize < 0 || groupSize > groupFactorization.length-1)
            throw new RuntimeException("Invalid groupSize provided - boefje");
        
        if (groupFactorization[groupSize] == 0)
            throw new RuntimeException("Could not record group formation, no groups of that size remain, did you forget to check first?");
        
        groupFactorization[groupSize]--;
    }
}
