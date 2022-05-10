package nl.tudelft.aidm.optimalgroups.experiment.paper.generateddata.model;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ExperimentResultsFile implements ExperimentResultsCollector
{
	private final File file;
	
	// same as number of trials so that only a full set of trials is recorded
	// (brittle implementation though)
	private final static int bufferSize = 3;
	
	private final ArrayList<ExperimentSubResult> resultsBuffer;
	
	private boolean printColumnHeaders = true;
	
	
	public ExperimentResultsFile(String path)
	{
		this.file = new File(path);
		this.resultsBuffer = new ArrayList<>(bufferSize);
		
		// If the application is closed, or somehow terminated, try flushing the buffer with results
//		Runtime.getRuntime().addShutdownHook(new Thread(this::writeBufferToFile));
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
		
		if (resultsBuffer.size() >= bufferSize) {
			writeBufferToFile();
		}
	}
	
	private void writeBufferToFile()
	{
		writeResults(resultsBuffer);
		resultsBuffer.clear();
	}
	
	private void writeRow(PrintWriter writer, List<Object> rowData)
	{
		var rowAsCsv = rowData.stream()
		                      .map(Object::toString)
		                      .collect(Collectors.joining(","));
		writer.println(rowAsCsv);
	}
	
	protected void writeResults(Collection<ExperimentSubResult> results)
	{
		try (var writer = new PrintWriter(new BufferedWriter(new FileWriter(file, true))))
		{
			for (ExperimentSubResult result : results)
			{
				if (printColumnHeaders) {
					writeRow(writer, result.columnHeaders());
					printColumnHeaders = false;
				}
				
				writeRow(writer, result.columnValues());
			}
			
			writer.flush();
		}
		catch (IOException ex)
		{
			// Can't do anything if we can't open the file
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	public void close() throws Exception
	{
		writeBufferToFile();
	}
}
