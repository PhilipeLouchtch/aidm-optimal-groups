package nl.tudelft.aidm.optimalgroups.metric.group;

import nl.tudelft.aidm.optimalgroups.model.group.Group;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.util.Collection;
import java.util.stream.Collectors;

public class LeastWorstIndividualRankInGroupDistribution
{
	private final Collection<? extends Group> groups;

	public LeastWorstIndividualRankInGroupDistribution(Collection<? extends Group> groups)
	{
		this.groups = groups;
	}

	public JFreeChart asChart()
	{
		var data = groups.stream()
			.flatMapToInt(group -> new LeastWorstIndividualRankAttainableInGroup(group).asInt().stream())
			.boxed()
			.collect(Collectors.groupingBy(integer -> integer, Collectors.counting()));

		XYSeries series = new XYSeries("");
		data.forEach(series::add);

		var chart = ChartFactory.createXYBarChart(
			"Distribution of Least-Worst project ranks in groups", "Rank of Least worst project in group", false, "# Groups", new XYSeriesCollection(series));



		XYPlot plot = (XYPlot) chart.getPlot();
//        plot.getRenderer().setSeriesPaint(0, Color.RED);
//        plot.getRenderer().setSeriesFillPaint(0, Color.RED);
//        plot.getRenderer().setSeriesStroke(0, new BasicStroke());
//        plot.getRenderer().setSeries(0, new BasicStroke());

		StandardXYBarPainter painter = new StandardXYBarPainter();
		((XYBarRenderer) plot.getRenderer()).setBarPainter(painter);
//		plot.getRenderer().setSeriesOutlinePaint(0, Color.BLACK);
//		plot.getRenderer().setSeriesOutlineStroke(0, new BasicStroke(5));

//        plot.setBackgroundPaint(ChartColor.LIGHT_RED);

		NumberAxis numberAxis = new NumberAxis();
		numberAxis.setTickUnit(new NumberTickUnit(1));
		numberAxis.setAutoRangeIncludesZero(false);
		numberAxis.setLabel("Rank of Least worst project in group");

		plot.setDomainAxis(numberAxis);


		numberAxis = new NumberAxis();
		numberAxis.setTickUnit(new NumberTickUnit(1));
		numberAxis.setAutoRangeIncludesZero(false);
		numberAxis.setLabel("# Groups");
		plot.setRangeAxis(numberAxis);
//        plot.set

		return chart;
	}
}
