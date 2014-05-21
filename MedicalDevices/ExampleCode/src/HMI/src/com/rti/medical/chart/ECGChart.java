package com.rti.medical.chart;

import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ECGChart extends JFrame {
	
	private XYSeries _heartRateXYSeries;
	private XYSeriesCollection _dataset;
	private JFreeChart _heartRateChart;
	private ChartPanel _chartPanel;
	

	public ECGChart(String appTitle) {
		super(appTitle);
		_heartRateXYSeries = new XYSeries("Heartrate Graph");
		_dataset = new XYSeriesCollection();
		_dataset.addSeries(_heartRateXYSeries);
		// Generate the graph
		_heartRateChart = ChartFactory.createXYLineChart(
				"XY Chart", // Title
				"x-axis", // x-axis Label
				"y-axis", // y-axis Label
				_dataset, // Dataset
				PlotOrientation.VERTICAL, // Plot Orientation
				true, // Show Legend
				true, // Use tooltips
				false // Configure chart to generate URLs?
				);
		_chartPanel = new ChartPanel(_heartRateChart);
		_chartPanel.setPreferredSize(new java.awt.Dimension(500,500));
		this.setPreferredSize(new java.awt.Dimension(500,500));
		setContentPane(_chartPanel);
	}
	
	public void drawChart() {
	}
}
