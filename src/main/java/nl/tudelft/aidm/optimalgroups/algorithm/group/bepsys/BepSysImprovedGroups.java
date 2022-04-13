package nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys;

import nl.tudelft.aidm.optimalgroups.algorithm.group.GroupFormingAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.group.bepsys.partial.CliqueGroups;
import nl.tudelft.aidm.optimalgroups.model.*;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.agent.Agents;
import nl.tudelft.aidm.optimalgroups.model.group.FormedGroups;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.group.TentativeGroups;
import nl.tudelft.aidm.optimalgroups.model.pref.*;
import nl.tudelft.aidm.optimalgroups.model.pref.AggregatedProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import plouchtch.assertion.Assert;


import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class BepSysImprovedGroups implements GroupFormingAlgorithm
{
    private Agents students;

    private HashSet<Agent> availableStudents;
    private HashSet<Agent> unavailableStudents;

    private final GroupSizeConstraint groupSizeConstraint;

    private TentativeGroups tentativelyFormedGroups;
    private FormedGroups finalFormedGroups;

    private boolean useImprovedAlgo;

    private boolean done = false;

    // Pass the list of students to make groups from
    public BepSysImprovedGroups(Agents agents, GroupSizeConstraint groupSizeConstraint, boolean useImprovedAlgo) {
        this.students = agents;

        this.availableStudents = new HashSet<>();
        this.unavailableStudents = new HashSet<>();

        this.groupSizeConstraint = groupSizeConstraint;

//        System.out.println("Student amount: " + students.count());

        this.availableStudents.addAll(students.asCollection());

        tentativelyFormedGroups = new TentativeGroups();
        finalFormedGroups = new FormedGroups();

        this.useImprovedAlgo = useImprovedAlgo;
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

        //System.out.println(System.currentTimeMillis() + ": Start matchings ungrouped students (state 2/3)");
        bestMatchUngrouped();

        //System.out.println(System.currentTimeMillis() + ": Start merging groups (state 3/3)");
        mergeGroups();
        //System.out.println(System.currentTimeMillis() + ": Done!");


        //System.out.println(System.currentTimeMillis() + ": Amount of final groups: " + this.finalFormedGroups.count());
        //System.out.println(System.currentTimeMillis() + ": Amount of students that are grouped: " + agentsInFinalGroups.size());

        Assert.that(finalFormedGroups.countDistinctStudents() == students.count()).orThrowMessage("Not all students were matched...");

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
        var groupsFromCliques = new CliqueGroups(Agents.from(availableStudents));

        var studentsInCliqueGroups = groupsFromCliques.asAgents().asCollection();

        // update (un)available students
        this.unavailableStudents.addAll(studentsInCliqueGroups);
        this.availableStudents.removeAll(studentsInCliqueGroups);

        // add clique groups to tentatively formed
        groupsFromCliques.forEach(tentativelyFormedGroups::addAsTentative);
    }

    private void bestMatchUngrouped()
    {
//        System.out.println(System.currentTimeMillis() + ":\t\t- bestMatchUngrouped: " + this.availableStudents.size() + " students left to group");
        List<PossibleGroup> possibleGroups = new ArrayList<>();

        // todo: improve, calculate num edges in common
        BiFunction<List<Agent>, Agent, Integer> cliquenessScore = (List<Agent> friends, Agent a) -> {
            int score = 0;
            for (Agent friend : friends)
            {
                List<Agent> friendsOfFriend = getAvailableFriends(friend);
                if (friendsOfFriend.contains(a)) score += 1;
                for (Agent friend2 : friends)
                {
                    if (friend == friend2) continue;
                    if (friendsOfFriend.contains(friend2)) score += 1;
                }
            }
            return score;
        };

        // Iterate over students instead of available students to prevent ConcurrentModificationException
        // when removing from availableStudents
        for (Agent student : this.students.asCollection()) {
            if (this.unavailableStudents.contains(student))
                continue;

            List<Agent> friends = this.getAvailableFriends(student);
            int score = cliquenessScore.apply(friends, student);

            friends.add(student); // Add self to group
            possibleGroups.add(new PossibleGroup(friends, score));
        }

        this.pickBestGroups(possibleGroups);
//        System.out.println(System.currentTimeMillis() + ":\t\t- bestMatchUngrouped: done, " + this.availableStudents.size() + " students left to group");
    }

    private void pickBestGroups(List<PossibleGroup> possibleGroups)
    {
        possibleGroups.sort(Comparator.comparing(PossibleGroup::score).reversed());
        while (possibleGroups.size() > 0) {
            PossibleGroup bestGroup = findBestGroup(possibleGroups);
            if (bestGroup == null) continue;

            Group formedGroup = tentativelyFormedGroups.addAsTentative(bestGroup.toGroup());
            possibleGroups.remove(bestGroup);

            for (Agent a : formedGroup.members().asCollection()) {
                this.availableStudents.remove(a);
                this.unavailableStudents.add(a);
            }
        }
    }

    private PossibleGroup findBestGroup(List<PossibleGroup> possibleGroups)
    {
        PossibleGroup possiblyBestGroup = null;
        boolean available = false;

        while (possibleGroups.size() > 0)
        {
            possiblyBestGroup = possibleGroups.get(0);
            available = true;

            // check if any of the members are already grouped
            for (Agent member : possiblyBestGroup.members)
            {
                if (unavailableStudents.contains(member)) {
                    available = false;
                    possibleGroups.remove(0);
                    break;
                }
            }

            if (available) {
                return possiblyBestGroup;
            }
        }

        // none of the groups had a member that was available
        return null;
    }

    private void mergeGroups() {
        List<Group.TentativeGroup> unmerged = new LinkedList<>();

        // todo: split groups into those that are finalized and those that are not
        //  s.t. no need to do this kind of case distinction here...
        tentativelyFormedGroups.forEach(group -> {
            boolean groupIsOfMaximumSize = group.members().count() == this.groupSizeConstraint.maxSize();

            if (groupIsOfMaximumSize) {
                finalFormedGroups.addAsFormed(group);
            }
            else {
                unmerged.add(new Group.TentativeGroup(group));
            }
        });

//        System.out.println(System.currentTimeMillis() + ":\t\t- mergeGroups: " + finalFormedGroups.asCollection().size() + " max size (final) groups");
//        System.out.println(System.currentTimeMillis() + ":\t\t- mergeGroups: " + tentativelyFormedGroups.asCollection().size() + " groups to be merged");

        unmerged.sort(Comparator.comparingInt((Group group) -> group.members().count()));

        int amountOfStudentsToGroup = tentativelyFormedGroups.countDistinctStudents() - finalFormedGroups.countDistinctStudents();
        var groupConstraints = new SetOfGroupSizesThatCanStillBeCreated(amountOfStudentsToGroup, groupSizeConstraint);

        /*
            The following is a greedy merge. By group merge we mean combining (partial) groups
            into groups that satisfy the
         */
        while (unmerged.size() > 0) {
            Group.TentativeGroup unmergedGroup = unmerged.get(0);
            unmerged.remove(0);

            int numMembersInUnmergedGroup = unmergedGroup.members().count();

            var possibleGroupMerges = new PriorityQueue<>(Comparator.comparing(BepSysImprovedGroups.PossibleGroupMerge::matchDisutilScore));

            for (Group.TentativeGroup otherUnmergedGroup : unmerged) {
                int numMembersInOtherUnmergedGroup = otherUnmergedGroup.members().count();
                int together = numMembersInUnmergedGroup + numMembersInOtherUnmergedGroup;

                // Only add group if it is a final form
                if(groupConstraints.mayFormGroupOfSize(together)) {
                    possibleGroupMerges.add(new BepSysImprovedGroups.PossibleGroupMerge(unmergedGroup, otherUnmergedGroup));
                }
            }

            // if no candidate group merges
            if (possibleGroupMerges.size() == 0) {
                for (Group otherUnmergedGroup : unmerged) {
                    int together = numMembersInUnmergedGroup + otherUnmergedGroup.members().count();

                    // try again with relaxed constraints on group creation (up to group size)
                    if (together <= groupSizeConstraint.maxSize()) {
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
//                        System.out.println(System.currentTimeMillis() + ":\t\t- mergeGroups: Nothing was removed from unmerged!!!");
                }

                Group.TentativeGroup tentativeGroup = bestMerge.toGroup();
                int together = tentativeGroup.members().count();

                if(groupConstraints.mayFormGroupOfSize(together)) //Does the merged group fit in group size constraints?
                {
                    groupConstraints.recordGroupFormedOfSize(together);
                    finalFormedGroups.addAsFormed(tentativeGroup);
                }
                else
                {
                    unmerged.add(tentativeGroup); // If merge can't be in final set, at least keep it
                }
            }
        }
    }


    private List<Agent> getAvailableFriends(Agent a) {
        List<Agent> friends = new ArrayList<>();

        // Some friends might be unavilable? If so, need to get group prefs and check
        for (var friend : a.groupPreference().asListOfAgents()) {
            if (availableStudents.contains(friend)) {
//                availableStudents.remove(friend);
                friends.add(friend);
            }
        }

        return friends;
    }

    private static class PossibleGroup {
        private List<Agent> members;
        private int score;

        public PossibleGroup(List<Agent> members, int score) {
            this.members = members;
            this.score = score;
        }

        public int score()
        {
            return score;
        }

        public List<Agent> members()
        {
            return members;
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
                score = computeMatchDisutilScore();
            }

            return score;
        }

        public Group.TentativeGroup toGroup()
        {
            Agents agents = g1.members().with(g2.members());
            ProjectPreference preferences = AggregatedProjectPreference.usingGloballyConfiguredMethod(agents);

            return new Group.TentativeGroup(agents, preferences);
        }

        private int computeMatchDisutilScore() {
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

            Project[] g1Prefs = g1.projectPreference().asArray();

            int score = 0;
            for (int i = 0; i < g1Prefs.length; i++) {
                var project = g1Prefs[i];
                var rankInG1 = g1.projectPreference().rankOf(project);
                var rankInG2 = g2.projectPreference().rankOf(project);

                if (rankInG2.isPresent()) {
                    score += Math.pow(rankInG1.asInt() - rankInG2.asInt(), 2);
                }
            }

            return score;
        }
    }
}
