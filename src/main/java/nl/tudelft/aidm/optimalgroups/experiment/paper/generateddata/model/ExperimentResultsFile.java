package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

import nl.tudelft.aidm.optimalgroups.model.Profile;
import nl.tudelft.aidm.optimalgroups.model.matching.AgentToProjectMatching;
import plouchtch.lang.Lazy;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ExperimentResultsFile implements ExperimentResultsCollector
{
	private static final List<String> columnHeaders = List.of(
			"num_students", "num_projects", "num_slots",
			"proj_pref_type", "pregroup_type", "mechanism", "trial",
			"duration_ms",
			"profile_all", "profile_singles", "profile_pregrouped", "profile_unsatpregroup"
		);
	
	// A row of placeholders for printf of proper length
	private static final Lazy<String> rowFormat = new Lazy<>(() -> columnHeaders.stream().map(__ -> "%s")
				.collect(Collectors.joining(",", "", "\n")));
	
	
	private final File file;
	
	private final ArrayList<ExperimentSubResult> resultsBuffer;
	
	private boolean printColumnHeaders = true;
	
	
	public ExperimentResultsFile(String path)
	{
		this.file = new File(path);
		this.resultsBuffer = new ArrayList<>(100);
	}
	
	/**
	 * Indicates if the result collection must be done for the (partial)experiment that is linked
	 * to this ExperimentResultsFile
	 */
	@Override
	public boolean resultsCollectionCanBeSkipped()
	{
		// Very simple and naive implementation:
		//         If the file aleady exists, then nothing needs to be done
		// Future: would have to read and parse the file and check if all subresults are present
		//         either by simply counting and comparing with expected or checking if all
		//         defined experiments have results present in the file
		return file.exists();
	}
	
	/**
	 * Adds the given subresult to the results. Does not immediately write the results
	 * to file, but waits until a batch is ready.
	 * @param subResult
	 */
	@Override
	public void add(ExperimentSubResult subResult)
	{
		resultsBuffer.add(subResult);
		
		if (resultsBuffer.size() >= 100) {
			writeResults(resultsBuffer);
			resultsBuffer.clear();
		}
	}
	
	private void writeColumnHeaders(PrintWriter writer)
	{
		writer.printf(rowFormat.get(), (Object[]) columnHeaders.toArray(new String[0]));
	}
	
	protected void writeResults(Collection<ExperimentSubResult> results)
	{
		try (var writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true))))
		{
			if (printColumnHeaders) {
				writeColumnHeaders(writer);
				printColumnHeaders = false;
			}
			
			for (ExperimentSubResult result : results)
			{
				// metrics: export only rank profile, generate metrics in R
				// runtime: do record runtime duration
				// TODO: Need 4 variants,
				//  - all, individuals, grouped, grouped-unsatisfied
				// but for now, the size experiment only has one variant: all
				
				var profileOfAllStudentRanks = Profile.of(
						AgentToProjectMatching.from(result.matching())
				);
				
				// todo: move some parts to the subresult
				writer.printf(rowFormat.get(),
						result.params().numStudents(),
						result.params().numProjects(),
						result.params().numSlotsPerProj(),
						
						result.params().prefGenerator().shortName(),
						result.params().pregroupingGenerator().shortName(),
						result.mechanism().name(),
						result.trialRunNum(),
						
						result.runtime().toMillis(),
						
						serializeProfile(result.profileAllStudents()),
						serializeProfile(result.profileSingles()),
						serializeProfile(result.profilePregrouped()),
						serializeProfile(result.profileUnsatpregroup())
				);
			}
			
			writer.flush();
		}
		catch (IOException ex)
		{
			// Can't do anything if we can't open the file
			throw new RuntimeException(ex);
		}
	}
	
	protected String serializeProfile(Profile profile)
	{
		var profileAsArray = new Integer[profile.maxRank()+1];
		
		profile.forEach((rank, count) -> {
			profileAsArray[rank] = count;
		});
		
		return Arrays.stream(profileAsArray)
		             .map(i -> i == null ? 0 : i)
		             .map(Object::toString)
		             .collect(Collectors.joining("|"));
	}
	
}
