package nl.tudelft.aidm.optimalgroups.experiment.agp;

import nl.tudelft.aidm.optimalgroups.algorithm.GroupProjectAlgorithm;
import nl.tudelft.aidm.optimalgroups.algorithm.holistic.chiarandini.model.PregroupingType;
import nl.tudelft.aidm.optimalgroups.dataset.generated.GeneratedDataContext;
import nl.tudelft.aidm.optimalgroups.dataset.generated.projprefs.ProjectPreferenceGenerator;
import nl.tudelft.aidm.optimalgroups.experiment.agp.report.ExperimentReportInHtml;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.ExperimentSubResult;
import nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model.MinimumReqProjectAmount;
import nl.tudelft.aidm.optimalgroups.experiment.paper.historical.comparison.setting.AnyExceptMaxCsvReport;
import nl.tudelft.aidm.optimalgroups.model.GroupSizeConstraint;
import nl.tudelft.aidm.optimalgroups.model.agent.Agent;
import nl.tudelft.aidm.optimalgroups.model.dataset.DatasetContext;
import nl.tudelft.aidm.optimalgroups.model.pref.ProjectPreference;
import nl.tudelft.aidm.optimalgroups.model.project.Projects;
import plouchtch.functional.actions.Rethrow;
import plouchtch.util.Try;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.predef.ProjPrefVariations.*;

public class PrefDistViz
{
	public static void main(String[] args)
	{
		var forExport = new ArrayList<Result>();
		
		var pregroupingType = PregroupingType.anyCliqueHardGrouped();

		List<GroupProjectAlgorithm> algorithms = List.of(
			new GroupProjectAlgorithm.BepSys(pregroupingType),
			new GroupProjectAlgorithm.BEPSys_RSD(pregroupingType)
			/*new ILPPP_TGAlgorithm()*/); // will not succeed on CE10

		var groupSize = GroupSizeConstraint.manual(4, 5);
		
		
		for (var pref_type : List.of(singleton(), linearPerturbedSlightly(), linearPerturbedMore(), random(), realistic()))
		{
			for (Integer num_students : List.of(50, 200, 600))
			{
				int numProjects = new MinimumReqProjectAmount(groupSize, num_students).asInt();
				var projects = Projects.generated(numProjects, 1);

				var pref_gen = pref_type.makeGeneratorFor(projects);
				var generatedDataset = new GeneratedDataContext(num_students, projects, groupSize, pref_gen);
				var sample = new Result(generatedDataset, num_students, pref_type.shortName());
				forExport.add(sample);
			}
		}

		// export to csv - all in single csv, include pref_type_name and num_students

		write(forExport, new File("results/gen_prefs_export.csv"));

		return;
	}

	record Result(DatasetContext datasetContext, Integer num_students, String pref_gen_name)
	{
		List<ProjectPreference> prefs()
		{
			return datasetContext.allAgents()
					.asCollection().stream()
					.map(Agent::projectPreference).toList();
		}
	}

	public static void write(List<Result> results, File file)
	{
		var headers = List.of("num_students", "pref_gen_name", "project", "rank");


		try (var writer = new FileWriter(file))
		{
			var headersAsLine = headers.stream().collect(Collectors.joining(",", "","\n"));
			writer.write(headersAsLine);

			for (var result : results)
			{
				result.prefs().forEach(pref -> {
					pref.forEach((project, rank, iterControl) -> {

						var NA = Integer.valueOf(0);

						writeUsing(writer,

							result.num_students().toString(),
							result.pref_gen_name(),

							project.name(),
							(rank.isPresent() ? rank.asInt() : NA).toString()

						);

					});
				});

			}
		}
		catch (IOException ex)
		{
			throw new RuntimeException(ex);
		}

	}

	public static void writeUsing(Writer writer, String... fields)
	{
		try
		{
			for (int i = 0; i < fields.length; i++)
			{
				var field = fields[i];
				writer.write(field);

				if (i < fields.length-1)
					writer.write(",");
				else
					writer.write("\n");
			}
		}
		catch (IOException exception)
		{
			throw new RuntimeException(exception);
		}
	}


}
