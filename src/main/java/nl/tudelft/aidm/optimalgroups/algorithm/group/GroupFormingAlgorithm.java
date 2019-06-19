package nl.tudelft.aidm.optimalgroups.algorithm.group;

import nl.tudelft.aidm.optimalgroups.model.entity.FormedGroups;
import nl.tudelft.aidm.optimalgroups.model.entity.Group;
import nl.tudelft.aidm.optimalgroups.model.entity.Groups;

public interface GroupFormingAlgorithm extends Groups<Group.FormedGroup>
{
    FormedGroups finalFormedGroups();
}
