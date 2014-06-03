/*******************************************************************************
 (c) 2005-2014 Copyright, Real-Time Innovations, Inc.  All rights reserved.
 RTI grants Licensee a license to use, modify, compile, and create derivative
 works of the Software.  Licensee has the right to distribute object form only
 for use with RTI products.  The Software is provided "as is", with no warranty
 of any type, including any warranty for fitness for any purpose. RTI is under
 no obligation to maintain or support the Software.  RTI shall not be liable for
 any incidental or consequential damages arising out of the use or inability to
 use the software.
 ******************************************************************************/

package com.rti.medical;

import java.awt.BorderLayout;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class ICEDisplay extends JFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// This keeps its own list of Alarms.
	private DefaultTableModel _tableModel;
	
	private JTable _table;

	private JPanel _mainPanel;
	
	ICEDisplay() {
		super("Patient Alarms");
		
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
		_mainPanel = new JPanel(new BorderLayout());

		_tableModel = new DefaultTableModel();
		_tableModel.addColumn("Patient ID");
		_tableModel.addColumn("Alarm Kind");
		_tableModel.addColumn("Device IDs");
		_table = new JTable(_tableModel);
		_table.setVisible(true);
		
		JScrollPane listScrollPane = new JScrollPane(_table);

		_mainPanel.add(listScrollPane, BorderLayout.CENTER);		
		
		_mainPanel.setVisible(true);
		listScrollPane.setVisible(true);
		
		setContentPane(_mainPanel);
		pack();
		setVisible(true);
		
		
	}

	
	public void addOrUpdateAlarmData(Vector<String> alarmDetails) {

		boolean isUpdate = false;
		for (int i = 0; i < _tableModel.getRowCount(); i++) {
			if (_tableModel.getValueAt(i, 0).equals(alarmDetails.get(0))) {
				for (int j = 0; j < alarmDetails.size(); j++) {
					_tableModel.setValueAt(alarmDetails.get(j), i, j);
					_tableModel.fireTableRowsUpdated(i, i);
					isUpdate = true;
				}
				
				break;
			}	
		}
		
		if (!isUpdate) {
			int rowNum = _tableModel.getRowCount();
			_tableModel.insertRow(rowNum, alarmDetails);
			_tableModel.fireTableRowsInserted(rowNum, rowNum);
		}
		
	}
	

}
