package nl.tudelft.aidm.optimalgroups.metric.profile;

import nl.tudelft.aidm.optimalgroups.metric.rank.AssignedRank;
import nl.tudelft.aidm.optimalgroups.model.group.Group;
import nl.tudelft.aidm.optimalgroups.model.matching.Match;
import nl.tudelft.aidm.optimalgroups.model.matching.Matching;
import nl.tudelft.aidm.optimalgroups.model.project.Project;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class GroupRankProfile extends AbstractRankProfile
{

    private final Matching<Group.FormedGroup, Project> matching;

    public GroupRankProfile(Matching<Group.FormedGroup, Project> matching) {
        this.matching = matching;
    }

    @Override
    protected void calculate()
    {
        if (this.profile == null)
        {
            this.profile = new HashMap<>();
            for (Match<Group.FormedGroup, Project> match : this.matching.asList())
            {
                AssignedRank.ProjectToGroup assignedProjectRank = new AssignedRank.ProjectToGroup(match);

                if (assignedProjectRank.isOfIndifferentAgent())
                    continue;

                var rank = assignedProjectRank.asInt().orElseThrow();
                this.worstRank = Math.max(this.worstRank, rank);
                this.profile.merge(rank, 1, Integer::sum);
            }
        }
    }

    @Override
    public void printResult(PrintStream printStream) {
        printStream.println("Group project profile results:");
        for (Map.Entry<Integer, Integer> entry : this.asMap().entrySet()) {
            printStream.printf("\t- Rank %d: %d group(s)\n", entry.getKey(), entry.getValue());
        }
        printStream.printf("\t- Cumulative rank of groups: %d\n\n", this.sumOfRanks());
    }

    public JFreeChart asChart()
    {
        this.calculate();

        XYSeries series = new XYSeries("");
        this.profile.forEach(series::add);

        var chart = ChartFactory.createXYStepAreaChart("Distribution assigned project ranks in group aggregate preferences", "Rank", "# Groups", new XYSeriesCollection(series));
        return chart;
    }

	public void displayChart()
	{
        var chart = this.asChart();

        /* Output stuff */
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new java.awt.Dimension(1000, 800));

        ApplicationFrame chartFrame = new ApplicationFrame(chart.getTitle().getText());
        chartFrame.setContentPane(chartPanel);
        chartFrame.pack();
        chartFrame.setVisible(true);
	}
}
