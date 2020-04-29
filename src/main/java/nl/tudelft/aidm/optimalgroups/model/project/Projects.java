package nl.tudelft.aidm.optimalgroups.model.project;

import java.util.*;
import java.util.function.Consumer;

public interface Projects
{
	int count();
	int countAllSlots();

	List<Project.ProjectSlot> slotsForProject(int projectId);

	void forEach(Consumer<Project> fn);
	Projects without(Project project);

	Collection<Project> asCollection();

	default Optional<Project> findWithId(int projectId)
	{
		return asCollection().stream().filter(project -> project.id() == projectId).findAny();
	}

	default Projects without(Projects other)
	{
		// Note: this is eerly similar to filteredprojects, but use-case is different...?
		Collection<Project> toFilter = new ArrayList<>(asCollection());
		toFilter.removeAll(other.asCollection());

		return Projects.from(toFilter);
	}

	/**
	 * Copy of the given Projects
	 * @param projects Projects to copy
	 * @return A copy
	 */
	static Projects from(Collection<Project> projects)
	{
		return new ListBasedProjects.ListBasedProjectsImpl(new ArrayList<>(projects));
	}


}
