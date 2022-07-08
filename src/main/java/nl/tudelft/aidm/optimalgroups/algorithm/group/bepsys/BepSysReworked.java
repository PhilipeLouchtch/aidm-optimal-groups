package nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys;

import nl.tudelft.aidm.optimalgroups.algorithm.group.GroupFormingAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.group.FormedGroups;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.TentativeGroups;
import nl.tudelft.aidm.optimalgroups.model.pref.AggregatedProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This is a refactored, streamlined, cleaned up version of "BepSys Improved".
 * In experiments, it behaves almost identical to it, and can successfully handle more cases
 * (some corner case bugs were elimited) but also ever so slightly worse...
 */
@SuppressWarnings("DuplicatedCode")
public class BepSysReworked implements GroupFormingAlgorithm
{
    private PregroupingType pregroupingType;
    private Agents students;

    private HashSet<Agent> availableStudents;
    private HashSet<Agent> unavailableStudents;

    private final GroupSizeConstraint groupSizeConstraint;

    private TentativeGroups tentativelyFormedGroups;
    private FormedGroups finalFormedGroups;

    private boolean done = false;
    
    /**
     * Default BEPSys pregrouping - only max-size pregroupings are allowed in the first pregrouping round
     * @param agents
     * @param groupSizeConstraint
     */
    public BepSysReworked(Agents agents, GroupSizeConstraint groupSizeConstraint)
    {
        this(PregroupingType.sizedCliqueHardGrouped(groupSizeConstraint.maxSize()), agents, groupSizeConstraint);
    }
    
    /**
     *
     * A BEPSys mechanism with configurable pregrouping-determination algorithm
     * @param pregroupingType
     * @param agents
     * @param groupSizeConstraint
     */
    public BepSysReworked(PregroupingType pregroupingType, Agents agents, GroupSizeConstraint groupSizeConstraint)
    {
        this.pregroupingType = pregroupingType;
        this.students = agents;

        this.availableStudents = new HashSet<>();
        this.unavailableStudents = new HashSet<>();

        this.groupSizeConstraint = groupSizeConstraint;

        this.availableStudents.addAll(students.asCollection());

        tentativelyFormedGroups = new TentativeGroups();
        finalFormedGroups = new FormedGroups();
    }

    private void constructGroups() {
        /*
            _, nogroup, group_list = clique_friends
            best_match = best_match_ungrouped nogroup
            best_match.each do |group|
                group_list.append group
            end

            group_list = merge_groups group_list

            db_list = []
            group_list.each do |group|
                participant = group[:leader]
                friends = group[:members]
                db_list.append create_group_without_project participant, friends
            end
            db_list
        */

        //System.out.println(System.currentTimeMillis() + ": Start constructing cliques (state 1/3)");
        constructGroupsFromCliques();

        // System.out.println(System.currentTimeMillis() + ": Start matchings ungrouped students (state 2/3)");
        
        // If the pregrouping type is configured to none, skip grouping agents by checking their friend lists
        // because if we do that, we're going to do a sort of anyClique pregrouping, which is probably not what
        // the user who set the Pregrouping.none() type expected.
        // Also: brittle implementation with the name comparison... so take care with refactorings
        if (!pregroupingType.simpleName().equals("no_grouping"))
            bestMatchUngrouped();

        //System.out.println(System.currentTimeMillis() + ": Start merging groups (state 3/3)");
        mergeGroups();
        //System.out.println(System.currentTimeMillis() + ": Done!");


//        Assert.that(finalFormedGroups.countDistinctStudents() == students.count()).orThrowMessage("Not all students were matched...");
        //System.out.println(System.currentTimeMillis() + ": Amount of final groups: " + this.finalFormedGroups.count());
        //System.out.println(System.currentTimeMillis() + ": Amount of students that are grouped: " + agentsInFinalGroups.size());

        this.done = true;
    }

    @Override
    public FormedGroups asFormedGroups()
    {
        if (!done) {
            constructGroups();
        }

        return this.finalFormedGroups;
    }

    @Override
    public Collection<Group.FormedGroup> asCollection()
    {
        return asFormedGroups().asCollection();
    }

    @Override
    public void forEach(Consumer<Group.FormedGroup> fn)
    {
        asFormedGroups().forEach(fn);
    }

    @Override
    public int count()
    {
        return asFormedGroups().count();
    }


    private void constructGroupsFromCliques()
    {
        var groupsFromCliques = pregroupingType.instantiateFor(students).groups();

        var studentsInCliqueGroups = groupsFromCliques.asAgents().asCollection();

        // update (un)available students
        this.unavailableStudents.addAll(studentsInCliqueGroups);
        this.availableStudents.removeAll(studentsInCliqueGroups);

        // add clique groups to tentatively formed
        groupsFromCliques.forEach(tentativelyFormedGroups::addAsTentative);
    }

    private List<Agent> availableFriendsOf(Agent agent)
    {
        return agent.groupPreference().asListOfAgents().stream()
            .filter(availableStudents::contains)
            .collect(Collectors.toList());
    }

    private void bestMatchUngrouped()
    {
//        System.out.println(System.currentTimeMillis() + ":\t\t- bestMatchUngrouped: " + this.availableStudents.size() + " students left to group");
        var possibleGroups = new PriorityQueue<PossibleGroup>(Comparator.comparing(possibleGroup -> -possibleGroup.score));

        // todo: improve, calculate num edges in common - also keep in mind invidiual preferences over groups
        // that is, a group where all n-1 members want to include some other person, n, but n wants to be with none
        // of them but a group of n with its own prefered peers will have a lower score -> strategic behavior
        BiFunction<List<Agent>, Agent, Integer> cliquenessScore = (List<Agent> friends, Agent a) -> {
            int score = 0;
            for (Agent friend : friends)
            {
                List<Agent> availableFriendsOfFriend = availableFriendsOf(friend);

                if (availableFriendsOfFriend.contains(a)) score += 1;
                for (Agent friend2 : friends)
                {
                    if (friend == friend2) continue;
                    if (availableFriendsOfFriend.contains(friend2)) score += 1;
                }
            }
            return score;
        };

        // Iterate over students instead of available students to prevent ConcurrentModificationException
        // when removing from availableStudents
        for (Agent student : this.availableStudents) {

            List<Agent> friends = availableFriendsOf(student);
            int score = cliquenessScore.apply(friends, student);

            friends.add(student); // Add self to group
            possibleGroups.add(new PossibleGroup(friends, score));
        }

        // PICK BEST GROUPS
        while (possibleGroups.size() > 0) {
            PossibleGroup bestGroup = possibleGroups.poll();

            if (bestGroup == null) throw new RuntimeException("Possible groups not empty but pop result was none...");

            Agents agents = Agents.from(bestGroup.members);
            Group formedGroup = tentativelyFormedGroups.addAsTentative(bestGroup.toGroup());

            for (Agent a : formedGroup.members().asCollection()) {
                this.availableStudents.remove(a);
                this.unavailableStudents.add(a);
                possibleGroups.removeIf(possibleGroup -> possibleGroup.members.contains(a));
            }

        }
//        System.out.println(System.currentTimeMillis() + ":\t\tR- bestMatchUngrouped: done, " + this.availableStudents.size() + " students left to group");
    }


    private void mergeGroups() {
        List<Group> unmerged = new LinkedList<>();
        
        // include all available students (not in tentative or final)
        availableStudents.forEach(availableStudent -> {
            var asGroup = new Group.TentativeGroup(Agents.from(availableStudent), availableStudent.projectPreference());
            unmerged.add(asGroup);
        });

        tentativelyFormedGroups.forEach(group -> {
            boolean groupIsOfMaximumSize = group.members().count() == this.groupSizeConstraint.maxSize();

            if (groupIsOfMaximumSize) {
                finalFormedGroups.addAsFormed(group);
            }
            else {
                unmerged.add(group);
            }
        });

        unmerged.sort(Comparator.comparingInt((Group group) -> group.members().count()));

        int amountOfStudentsToGroup = unmerged.stream().mapToInt(g -> g.members().count()).sum();
        
        var groupFactorization = new SetOfGroupSizesThatCanStillBeCreated(amountOfStudentsToGroup, groupSizeConstraint);

        /*
            The following is a greedy merge. By group merge we mean combining (partial) groups
            into groups that satisfy the
         */
        while (unmerged.size() > 0) {
            var unmergedGroup = unmerged.remove(0);

            int numMembersInUnmergedGroup = unmergedGroup.members().count();

            var possibleGroupMerges = new PriorityQueue<>(Comparator.comparing(BepSysReworked.PossibleGroupMerge::matchDisutilScore));

            // Create potential group merges
            for (var otherUnmergedGroup : unmerged) {
                int numMembersInOtherUnmergedGroup = otherUnmergedGroup.members().count();
                int together = numMembersInUnmergedGroup + numMembersInOtherUnmergedGroup;

                // Only add group if it is a final form
                if(groupFactorization.mayFormGroupOfSize(together)) {
                    possibleGroupMerges.add(new BepSysReworked.PossibleGroupMerge(unmergedGroup, otherUnmergedGroup));
                }
            }

            // If there were no potential merges found, try doing a partial merge (merged group that cannot be 'formed' yet)
            if (possibleGroupMerges.size() == 0) {
                for (Group otherUnmergedGroup : unmerged) {
                    int together = numMembersInUnmergedGroup + otherUnmergedGroup.members().count();

                    // Determine the group size upperbound for the potential merges:
                    int togetherMaxBound = groupSizeConstraint.maxSize();
                    for (int i = groupSizeConstraint.maxSize(); i > 0; i--) {
                        if (groupFactorization.mayFormGroupOfSize(i))
                            togetherMaxBound = i;
                    }
                    
                    // original bepsys: if together <= groupSizeConstraint.maxSize()
                    // but we must have a tigher bound to prevent infinite loops
                    // (eg group of 4 and group of 6 (with that order in the unmergedGroups collection) with gsc(5,8)
                    if (together <= togetherMaxBound) {
                        possibleGroupMerges.add(new PossibleGroupMerge(unmergedGroup, otherUnmergedGroup));
                    }
                }
            }

            // if still no candidate group merges
            if (possibleGroupMerges.size() == 0) {
                for (Agent a : unmergedGroup.members().asCollection()) {
                    Agents singleAgent = Agents.from(a);
                    unmerged.add(new Group.TentativeGroup(singleAgent, a.projectPreference()));
                }
            }

            if (possibleGroupMerges.size() > 0)
            {
                // take best scoring group (it's a priority queue)
                PossibleGroupMerge bestMerge = possibleGroupMerges.peek();

                // remove the "other" group from unmerged
                boolean removedSomething = unmerged.remove(bestMerge.g2); // todo: proper check for no candidates & exception
                if (!removedSomething) {
//                        System.out.println(System.currentTimeMillis() + ":\t\t- mergeGroups: Nothing was removed from unmerged but was expected!!!");
                }

                Group.TentativeGroup tentativeGroup = bestMerge.asTentativeGroup();
                int together = tentativeGroup.members().count();

                if(groupFactorization.mayFormGroupOfSize(together)) //Does the merged group fit in group size constraints?
                {
                    groupFactorization.recordGroupFormedOfSize(together);
                    finalFormedGroups.addAsFormed(tentativeGroup);
                }
                else
                {
                    unmerged.add(tentativeGroup); // If merge can't be in final set, at least keep it
                }
            }
        }
        return;
    }


    private static class PossibleGroup {
        public List<Agent> members;
        public int score;

        public PossibleGroup(List<Agent> members, int score) {
            this.members = members;
            this.score = score;
        }

        public Group.TentativeGroup toGroup()
        {
            Agents agents = Agents.from(members);
            return new Group.TentativeGroup(agents, AggregatedProjectPreference.usingGloballyConfiguredMethod(agents));
        }
    }


    private static class PossibleGroupMerge
    {
        Group g1;
        Group g2;

        public PossibleGroupMerge(Group g1, Group g2)
        {
            this.g1 = g1;
            this.g2 = g2;
        }

        private int score = -1;
        int matchDisutilScore()
        {
            if (score == -1) {
                score = computeMatchDisutil();
            }

            return score;
        }

        public Group.TentativeGroup asTentativeGroup()
        {
            Agents agents = g1.members().with(g2.members());
            ProjectPreference preferences = AggregatedProjectPreference.usingGloballyConfiguredMethod(agents);

            return new Group.TentativeGroup(agents, preferences);
        }

        private int computeMatchDisutil() {
            /* RUBY CODE:
                score = 0
                g1users = g1[:members].each { |m| m } + [g1[:leader]]
                g2users = g2[:members].each { |m| m } + [g2[:leader]]

                g1prefs = comp_average_pref g1users
                g2prefs = comp_average_pref g2users

                g1sorted = g1prefs.sort_by { |_k, v| v }.map { |v| v[0] }
                g2sorted = g2prefs.sort_by { |_k, v| v }.map { |v| v[0] }

                cnt = 0
                g1sorted.each do |p|
                    cnt2 = g2sorted.index(p)
                    score += (cnt - cnt2) * (cnt - cnt2) unless cnt2.nil?
                    cnt += 1
                end

                score
            */

            var score = new AtomicInteger(0);
            g1.projectPreference().forEach((project, rankInG1, iterControl) -> {
                var rankInG2 = g2.projectPreference().rankOf(project);
                
                if (rankInG2.isPresent()) {
                    var delta = (int) Math.pow(rankInG1.asInt() - rankInG2.asInt(), 2);
                    score.addAndGet(delta);
                }
            });

            return score.get();
        }
    }

    static class GroupProposal implements Group
    {
        private final Agent agent;
        private final Collection<Agent> exclude;
        private AggregatedProjectPreference projectPrefs;
        private Agents members;

        public GroupProposal(Agent agent)
        {
            this(agent, Collections.emptyList());
        }

        public GroupProposal(Agent agent, Collection<Agent> exclude)
        {
            this.agent = agent;
            this.exclude = exclude;
        }

        @Override
        public Agents members()
        {
            if (members == null) {
                var fromPrefs = new ArrayList<>(agent.groupPreference().asListOfAgents());
                fromPrefs.add(this.agent);

                this.members = Agents.from(fromPrefs).without(exclude);
            }

            return members;
        }

        @Override
        public ProjectPreference projectPreference()
        {
            if (projectPrefs == null) {
                this.projectPrefs = AggregatedProjectPreference.usingGloballyConfiguredMethod(members());
            }

            return this.projectPrefs;
        }
    }
}
