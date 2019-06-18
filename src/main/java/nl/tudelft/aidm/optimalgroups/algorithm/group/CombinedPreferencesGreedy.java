package nl.tudelft.aidm.optimalgroups.algorithm.group;

import nl.tudelft.aidm.optimalgroups.model.entity.Agent;
import nl.tudelft.aidm.optimalgroups.model.entity.Agents;
import nl.tudelft.aidm.optimalgroups.model.entity.FormedGroups;
import nl.tudelft.aidm.optimalgroups.model.entity.Group;
import nl.tudelft.aidm.optimalgroups.model.pref.AverageProjectPreferenceOfAgents;
import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Map.Entry.*;

public class CombinedPreferencesGreedy implements GroupFormingAlgorithm {

    private Agents students;
    private Map<String, Agent> availableStudents;
    private Map<String, Agent> unavailableStudents;

    private final int maxGroupSize;
    private final int minGroupSize;

    private FormedGroups formedGroups;

    private boolean done = false;

    public CombinedPreferencesGreedy(Agents students, int minGroupSize, int maxGroupSize) {
        this.students = students;

        this.minGroupSize = minGroupSize;
        this.maxGroupSize = maxGroupSize;
        this.initializeObjects(students);
    }

    private void initializeObjects(Agents students) {
        this.formedGroups = new FormedGroups();
        this.availableStudents = new HashMap<>(students.count());
        this.unavailableStudents = new HashMap<>(students.count());

        this.students.forEach(student -> {
            this.availableStudents.put(student.name, student);
        });
    }

    private void constructGroups() {
        if (this.formedGroups.count() > 0) {
            initializeObjects(this.students);
        }

        this.students.useCombinedPreferences();

        // Construct a list of agent sorted (descending) by the size of their group preference (as an attempt for a nice heuristic)
        List<Agent> sortedList = new ArrayList<>(this.students.asCollection());
        sortedList.sort(Comparator.comparing(Agent::groupPreferenceLength).reversed());

        SetOfConstrainedGroupSizes groupSizes = new SetOfConstrainedGroupSizes(this.students.count(), this.minGroupSize, this.maxGroupSize, SetOfConstrainedGroupSizes.SetCreationAlgorithm.MAX_FOCUS);

        // Start iterating and forming groups greedily
        for (Agent student : sortedList) {
            if (this.unavailableStudents.containsKey(student.name)) {
                continue;
            }

            Map<String, Integer> differences = new HashMap<>(this.availableStudents.size());

            for (Agent other : this.students.asCollection()) {
                if (student.name == other.name || this.unavailableStudents.containsKey(other.name)) {
                    continue;
                }

                differences.put(other.name, student.getProjectPreference().differenceTo(other.getProjectPreference()));
            }

            // Sort differences in ascending order (least difference first)
            List<Map.Entry<String, Integer>> sortedDifferences = new ArrayList<>(differences.entrySet());
            sortedDifferences.sort(comparingByValue());


            List<Agent> agents = new ArrayList<>(this.maxGroupSize);

            // Put the student in the group
            this.availableStudents.remove(student.name);
            this.unavailableStudents.put(student.name, student);
            agents.add(student);

            // Determine the group size
            int groupSize = this.maxGroupSize;
            while (groupSizes.mayFormGroupOfSize(groupSize) == false && groupSize >= this.minGroupSize) {
                groupSize--;
            }

            // Start inserting the remaining group members (students of which the combined preference has the least difference)
            for (Map.Entry<String, Integer> entry : sortedDifferences) {
                if (agents.size() >= groupSize) {
                    break;
                }

                Agent removed = this.availableStudents.remove(entry.getKey());
                this.unavailableStudents.put(entry.getKey(), removed);

                agents.add(removed);
            }

            // Transform the new agents into a formed group
            Agents newGroupAgents = Agents.from(agents);
            ProjectPreference aggregatedPreference = new AverageProjectPreferenceOfAgents(newGroupAgents);
            Group.TentativeGroup newTentativeGroup = new Group.TentativeGroup(newGroupAgents, aggregatedPreference);
            this.formedGroups.addAsFormed(newTentativeGroup);

            // Inform the group sizes object that a group of this size has been formed
            groupSizes.recordGroupFormedOfSize(groupSize);
        }

        this.students.useDatabasePreferences();
    }

    @Override
    public FormedGroups finalFormedGroups() {
        if (this.done == false) {
            this.constructGroups();
        }

        return this.formedGroups;
    }

    @Override
    public Collection<Group.FormedGroup> asCollection() {
        return this.finalFormedGroups().asCollection();
    }

    @Override
    public void forEach(Consumer<Group.FormedGroup> fn) { this.formedGroups.forEach(fn); }

    @Override
    public int count() { return this.formedGroups.count(); }
}
