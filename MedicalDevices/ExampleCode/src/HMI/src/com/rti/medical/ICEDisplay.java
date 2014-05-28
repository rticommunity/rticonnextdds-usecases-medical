/*********************************************************************************************
(c) 2005-2014 Copyright, Real-Time Innovations, Inc.  All rights reserved.    	                             
RTI grants Licensee a license to use, modify, compile, and create derivative works 
of the Software.  Licensee has the right to distribute object form only for use with RTI 
products.  The Software is provided �as is�, with no warranty of any type, including 
any warranty for fitness for any purpose. RTI is under no obligation to maintain or 
support the Software.  RTI shall not be liable for any incidental or consequential 
damages arising out of the use or inability to use the software.
**********************************************************************************************/
package com.rti.medical;

import java.awt.BorderLayout;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.rti.medical.chart.ECGChart;

public class ICEDisplay extends JFrame {

	private JLabel _alarmLabel;
	
	// This keeps its own list of Alarms.
	private DefaultListModel<String> _listModel;
	
	private JList<String> _list;

	private static String _alarmLabelString = "Alarms";

	private JPanel _mainPanel;
	
	ICEDisplay() {
		super("ListDemo");
		
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
		_mainPanel = new JPanel(new BorderLayout());

		_listModel = new DefaultListModel<String>();
		_list = new JList<String>(_listModel);
		_list.setVisibleRowCount(5);
		
		JScrollPane listScrollPane = new JScrollPane(_list);

		_mainPanel.add(listScrollPane, BorderLayout.CENTER);		
		
		_mainPanel.setVisible(true);
		listScrollPane.setVisible(true);
		
		setContentPane(_mainPanel);
		pack();
		setVisible(true);
		
		
	}

	
	public void addAlarmString(String alarmString) {

		_listModel.insertElementAt(alarmString, _listModel.size());
	}
	
	public static void main(String[] args) {

		ECGChart chart = new ECGChart("ICE Display");
		chart.drawChart();
		chart.pack();
		chart.setVisible(true);
	}

}
