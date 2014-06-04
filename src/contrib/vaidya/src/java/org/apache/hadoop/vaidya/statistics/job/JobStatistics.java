/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.vaidya.statistics.job;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.JobHistory;
import org.apache.hadoop.mapred.JobHistory.JobInfo;
import org.apache.hadoop.mapred.JobHistory.Keys;

/**
 *
 */
public class JobStatistics implements JobStatisticsInterface {


	/*
	 * Pattern for parsing the COUNTERS
	 */
	private static final Pattern _pattern = Pattern.compile("[[^,]?]+");  //"[[^,]?]+"

	/*
	 * Job configuration
	 */
	private JobConf _jobConf;

	/**
	 * @param jobConf the jobConf to set
	 */
	void setJobConf(JobConf jobConf) {
		this._jobConf = jobConf;
		// TODO: Add job conf to _job array 
	}

	/*
	 * Aggregated Job level counters 
	 */
	private JobHistory.JobInfo _jobInfo;

	/*
	 * Job stats 
	 */
	private java.util.Hashtable<Enum, String> _job;

	/**
	 * @param jobConf the jobConf to set
	 */
	public JobConf getJobConf() {
		return this._jobConf;
	}

	/*
	 * Get Job Counters of type long
	 */
	public long getLongValue(Enum key) {
		return Long.parseLong(this._job.get(key));
	}

	/*
	 * Get job Counters of type Double
	 */
	public double getDoubleValue(Enum key) {
		return Double.parseDouble(this._job.get(key));
	}

	/* 
	 * Get Job Counters of type String
	 */
	public String getStringValue(Enum key) {
		return this._job.get(key);
	}

	/*
	 * Set key value of type long
	 */
	public void setValue(Enum key, long value) {
		this._job.put(key, Long.toString(value));
	}

	/*
	 * Set key value of type double
	 */
	public void setValue(Enum key, double value) {
		this._job.put(key, Double.toString(value));
	}

	/*
	 * Set key value of type String
	 */
	public void setValue(Enum key, String value) {
		this._job.put(key, value);
	}

	/*
	 * Map Task List (Sorted by task id)
	 */
	private ArrayList<MapTaskStatistics> _mapTaskList = new ArrayList<MapTaskStatistics>();

	/*
	 * Reduce Task List (Sorted by task id)
	 */
	private ArrayList<ReduceTaskStatistics> _reduceTaskList = new ArrayList<ReduceTaskStatistics>();


	/* 
	 * Ctor:
	 */
	public JobStatistics (JobConf jobConf, JobInfo jobInfo) throws ParseException {
		this._jobConf = jobConf;
		this._jobInfo = jobInfo;
		this._job = new Hashtable<Enum, String>();
		populate_Job(this._job, this._jobInfo.getValues());
		populate_MapReduceTaskLists(this._mapTaskList, this._reduceTaskList, this._jobInfo.getAllTasks());
	}

	/*
	 * 
	 */
	private void populate_MapReduceTaskLists (ArrayList<MapTaskStatistics> mapTaskList, 
			ArrayList<ReduceTaskStatistics> reduceTaskList, 
			java.util.Map<String, JobHistory.Task> taskMap) throws ParseException {
		/*
		 * 
		 */
		int num_tasks = taskMap.entrySet().size();
		java.util.Iterator<Map.Entry<String, JobHistory.Task>> ti = taskMap.entrySet().iterator();
		for (int i = 0; i < num_tasks; i++)
		{
			Map.Entry<String, JobHistory.Task> entry = (Map.Entry<String, JobHistory.Task>) ti.next();
			JobHistory.Task task = entry.getValue();
			if (task.get(Keys.TASK_TYPE).equals("MAP")) {
				MapTaskStatistics mapT = new MapTaskStatistics();
				java.util.Map<JobHistory.Keys, String> mapTask = task.getValues();
				java.util.Map<JobHistory.Keys, String> successTaskAttemptMap  =  getLastSuccessfulTaskAttempt(task);
				// NOTE: Following would lead to less number of actual tasks collected in the tasklist array
				if (successTaskAttemptMap != null) {
					mapTask.putAll(successTaskAttemptMap);
				} else {
					System.out.println("Task:<"+task.get(Keys.TASKID)+"> is not successful - SKIPPING");
				}
				int size = mapTask.size();
				java.util.Iterator<Map.Entry<JobHistory.Keys, String>> kv = mapTask.entrySet().iterator();
				for (int j = 0; j < size; j++)
				{
					Map.Entry<JobHistory.Keys, String> mtc = kv.next();
					JobHistory.Keys key = mtc.getKey();
					String value = mtc.getValue();
					switch (key) {
					case TASKID: mapT.setValue(MapTaskKeys.TASK_ID, value); break;
					case TASK_ATTEMPT_ID: mapT.setValue(MapTaskKeys.ATTEMPT_ID, value); break;
					case HOSTNAME: mapT.setValue(MapTaskKeys.HOSTNAME, value); break;
					case TASK_TYPE: mapT.setValue(MapTaskKeys.TASK_TYPE, value); break;
					case TASK_STATUS: mapT.setValue(MapTaskKeys.STATUS, value); break;
					case START_TIME: mapT.setValue(MapTaskKeys.START_TIME, value); break;
					case FINISH_TIME: mapT.setValue(MapTaskKeys.FINISH_TIME, value); break;
					case SPLITS: mapT.setValue(MapTaskKeys.SPLITS, value); break;
					case COUNTERS:
						value.concat(",");
						parseAndAddMapTaskCounters(mapT, value);
						mapTaskList.add(mapT);
						break;
					default: System.out.println("JobHistory.Keys."+key+" : NOT INCLUDED IN PERFORMANCE ADVISOR MAP COUNTERS");
					break;
					}
				}

				// Add number of task attempts
				mapT.setValue(MapTaskKeys.NUM_ATTEMPTS, (new Integer(task.getTaskAttempts().size())).toString());

			}else if (task.get(Keys.TASK_TYPE).equals("REDUCE")) {
				ReduceTaskStatistics reduceT = new ReduceTaskStatistics();
				java.util.Map<JobHistory.Keys, String> reduceTask = task.getValues();
				java.util.Map<JobHistory.Keys, String> successTaskAttemptMap  =  getLastSuccessfulTaskAttempt(task);
				// NOTE: Following would lead to less number of actual tasks collected in the tasklist array
				if (successTaskAttemptMap != null) {
					reduceTask.putAll(successTaskAttemptMap);
				} else {
					System.out.println("Task:<"+task.get(Keys.TASKID)+"> is not successful - SKIPPING");
				}
				int size = reduceTask.size();
				java.util.Iterator<Map.Entry<JobHistory.Keys, String>> kv = reduceTask.entrySet().iterator();
				for (int j = 0; j < size; j++)
				{
					Map.Entry<JobHistory.Keys, String> rtc = kv.next();
					JobHistory.Keys key = rtc.getKey();
					String value = rtc.getValue();
					switch (key) {
					case TASKID: reduceT.setValue(ReduceTaskKeys.TASK_ID, value); break;
					case TASK_ATTEMPT_ID: reduceT.setValue(ReduceTaskKeys.ATTEMPT_ID, value); break;
					case HOSTNAME: reduceT.setValue(ReduceTaskKeys.HOSTNAME, value); break;
					case TASK_TYPE: reduceT.setValue(ReduceTaskKeys.TASK_TYPE, value); break;
					case TASK_STATUS: reduceT.setValue(ReduceTaskKeys.STATUS, value); break;
					case START_TIME: reduceT.setValue(ReduceTaskKeys.START_TIME, value); break;
					case FINISH_TIME: reduceT.setValue(ReduceTaskKeys.FINISH_TIME, value); break;
					case SHUFFLE_FINISHED: reduceT.setValue(ReduceTaskKeys.SHUFFLE_FINISH_TIME, value); break;
					case SORT_FINISHED: reduceT.setValue(ReduceTaskKeys.SORT_FINISH_TIME, value); break;
					case COUNTERS:
						value.concat(",");
						parseAndAddReduceTaskCounters(reduceT, value);
						reduceTaskList.add(reduceT);
						break;
					default: System.out.println("JobHistory.Keys."+key+" : NOT INCLUDED IN PERFORMANCE ADVISOR REDUCE COUNTERS");
					break;
					}

					// Add number of task attempts
					reduceT.setValue(ReduceTaskKeys.NUM_ATTEMPTS, (new Integer(task.getTaskAttempts().size())).toString());
				}
			} else if (task.get(Keys.TASK_TYPE).equals("CLEANUP")) {
				//System.out.println("INFO: IGNORING TASK TYPE : "+task.get(Keys.TASK_TYPE));
			} else {
				System.out.println("UNKNOWN TASK TYPE : "+task.get(Keys.TASK_TYPE));
			}
		}
	}

	/*
	 * Get last successful task attempt to be added in the stats
	 */
	private java.util.Map<JobHistory.Keys, String> getLastSuccessfulTaskAttempt(JobHistory.Task task) {

		Map<String, JobHistory.TaskAttempt> taskAttempts = task.getTaskAttempts();
		int size = taskAttempts.size();
		java.util.Iterator<Map.Entry<String, JobHistory.TaskAttempt>> kv = taskAttempts.entrySet().iterator();
		for (int i=0; i<size; i++) {
			// CHECK_IT: Only one SUCCESSFUL TASK ATTEMPT
			Map.Entry<String, JobHistory.TaskAttempt> tae = kv.next();
			JobHistory.TaskAttempt attempt = tae.getValue();
			if (attempt.getValues().get(JobHistory.Keys.TASK_STATUS).equals("SUCCESS")) {
				return attempt.getValues();
			}
		}

		return null;
	}

	/*
	 * Popuate the job stats 
	 */
	private void populate_Job (Hashtable<Enum, String> job, java.util.Map<JobHistory.Keys, String> jobC) throws ParseException {
		int size = jobC.size(); 
		java.util.Iterator<Map.Entry<JobHistory.Keys, String>> kv = jobC.entrySet().iterator();
		for (int i = 0; i < size; i++)
		{
			Map.Entry<JobHistory.Keys, String> entry = (Map.Entry<JobHistory.Keys, String>) kv.next();
			JobHistory.Keys key = entry.getKey();
			String value = entry.getValue();
			switch (key) {
			case JOBTRACKERID: job.put(JobKeys.JOBTRACKERID, value); break;
			//case START_TIME: job.put(JobKeys., value); break;
			case FINISH_TIME: job.put(JobKeys.FINISH_TIME, value); break;
			case JOBID: job.put(JobKeys.JOBID, value); break;
			case JOBNAME: job.put(JobKeys.JOBNAME, value); break;
			case USER: job.put(JobKeys.USER, value); break;
			case JOBCONF: job.put(JobKeys.JOBCONF, value); break;
			case SUBMIT_TIME: job.put(JobKeys.SUBMIT_TIME, value); break;
			case LAUNCH_TIME: job.put(JobKeys.LAUNCH_TIME, value); break;
			case TOTAL_MAPS: job.put(JobKeys.TOTAL_MAPS, value); break;
			case TOTAL_REDUCES: job.put(JobKeys.TOTAL_REDUCES, value); break;
			case FAILED_MAPS: job.put(JobKeys.FAILED_MAPS, value); break;
			case FAILED_REDUCES: job.put(JobKeys.FAILED_REDUCES, value); break;
			case FINISHED_MAPS: job.put(JobKeys.FINISHED_MAPS, value); break;
			case FINISHED_REDUCES: job.put(JobKeys.FINISHED_REDUCES, value); break;
			case JOB_STATUS: job.put(JobKeys.STATUS, value); break;
			case COUNTERS:
				value.concat(",");
				parseAndAddJobCounters(job, value);
				break;
			default:   System.out.println("JobHistory.Keys."+key+" : NOT INCLUDED IN PERFORMANCE ADVISOR COUNTERS");
			break;
			}
		}
	}

	/*
	 * Parse and add the job counters
	 */
	private void parseAndAddJobCounters(Hashtable<Enum, String> job, String counters) throws ParseException {
		Matcher m = _pattern.matcher(counters);
		while(m.find()){
			String ctuple = m.group(0);
			//String ctuple = c1tuple.substring(0, c1tuple.length()-1);
			String []parts = ctuple.split(":");
			if (parts[0].equals("File Systems.Local bytes read")) {
				job.put(JobKeys.LOCAL_BYTES_READ, parts[1]);
			} else if (parts[0].equals("File Systems.Local bytes written")) {
				job.put(JobKeys.LOCAL_BYTES_WRITTEN, parts[1]);
			} else if (parts[0].equals("File Systems.HDFS bytes read")) {
				job.put(JobKeys.HDFS_BYTES_READ, parts[1]);
			} else if (parts[0].equals("File Systems.HDFS bytes written")) {
				job.put(JobKeys.HDFS_BYTES_WRITTEN, parts[1]);
			} else if (parts[0].equals("Job Counters .Launched map tasks")) {
				job.put(JobKeys.LAUNCHED_MAPS, parts[1]);
			} else if (parts[0].equals("Job Counters .Launched reduce tasks")) {
				job.put(JobKeys.LAUNCHED_REDUCES, parts[1]);
			} else if (parts[0].equals("Job Counters .Data-local map tasks")) {
				job.put(JobKeys.DATALOCAL_MAPS, parts[1]);
			} else if (parts[0].equals("Job Counters .Rack-local map tasks")) {
				job.put(JobKeys.RACKLOCAL_MAPS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Map input records")) {
				job.put(JobKeys.MAP_INPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Map output records")) {
				job.put(JobKeys.MAP_OUTPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Map input bytes")) {
				job.put(JobKeys.MAP_INPUT_BYTES, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Map output bytes")) {
				job.put(JobKeys.MAP_OUTPUT_BYTES, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Combine input records")) {
				job.put(JobKeys.COMBINE_INPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Combine output records")) {
				job.put(JobKeys.COMBINE_OUTPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Reduce input groups")) {
				job.put(JobKeys.REDUCE_INPUT_GROUPS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Reduce input records")) {
				job.put(JobKeys.REDUCE_INPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Reduce output records")) {
				job.put(JobKeys.REDUCE_OUTPUT_RECORDS, parts[1]);
			} else {
				System.out.println("Pattern:<"+ctuple+"> ==> NOT INCLUDED IN PERFORMANCE ADVISOR");
			}
		}  
	}

	/*
	 * Parse and add the Map task counters
	 */
	private void parseAndAddMapTaskCounters(MapTaskStatistics mapTask, String counters) {
		Matcher m = _pattern.matcher(counters);
		while(m.find()){
			String ctuple = m.group(0);
			//String ctuple = c1tuple.substring(0, c1tuple.length()-1);
			String []parts = ctuple.split(":");
			if (parts[0].equals("File Systems.Local bytes read")) {
				mapTask.setValue(MapTaskKeys.LOCAL_BYTES_READ, parts[1]);
			} else if (parts[0].equals("File Systems.Local bytes written")) {
				mapTask.setValue(MapTaskKeys.LOCAL_BYTES_WRITTEN, parts[1]);
			} else if (parts[0].equals("File Systems.HDFS bytes read")) {
				mapTask.setValue(MapTaskKeys.HDFS_BYTES_READ, parts[1]);
			} else if (parts[0].equals("File Systems.HDFS bytes written")) {
				mapTask.setValue(MapTaskKeys.HDFS_BYTES_WRITTEN, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Map input records")) {
				mapTask.setValue(MapTaskKeys.INPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Map output records")) {
				mapTask.setValue(MapTaskKeys.OUTPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Map input bytes")) {
				mapTask.setValue(MapTaskKeys.INPUT_BYTES, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Map output bytes")) {
				mapTask.setValue(MapTaskKeys.OUTPUT_BYTES, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Combine input records")) {
				mapTask.setValue(MapTaskKeys.COMBINE_INPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Combine output records")) {
				mapTask.setValue(MapTaskKeys.COMBINE_OUTPUT_RECORDS, parts[1]);
			} else {
				System.out.println("Pattern:<"+ctuple+"> ==> NOT INCLUDED IN PERFORMANCE ADVISOR MAP TASK");
			}
		}    
	}

	/*
	 * Parse and add the reduce task counters
	 */
	private void parseAndAddReduceTaskCounters(ReduceTaskStatistics reduceTask, String counters) {
		Matcher m = _pattern.matcher(counters);
		while(m.find()){
			String ctuple = m.group(0);
			//String ctuple = c1tuple.substring(0, c1tuple.length()-1);
			String []parts = ctuple.split(":");
			if (parts[0].equals("File Systems.Local bytes read")) {
				reduceTask.setValue(ReduceTaskKeys.LOCAL_BYTES_READ, parts[1]);
			} else if (parts[0].equals("File Systems.Local bytes written")) {
				reduceTask.setValue(ReduceTaskKeys.LOCAL_BYTES_WRITTEN, parts[1]);
			} else if (parts[0].equals("File Systems.HDFS bytes read")) {
				reduceTask.setValue(ReduceTaskKeys.HDFS_BYTES_READ, parts[1]);
			} else if (parts[0].equals("File Systems.HDFS bytes written")) {
				reduceTask.setValue(ReduceTaskKeys.HDFS_BYTES_WRITTEN, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Reduce input records")) {
				reduceTask.setValue(ReduceTaskKeys.INPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Reduce output records")) {
				reduceTask.setValue(ReduceTaskKeys.OUTPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Combine input records")) {
				reduceTask.setValue(ReduceTaskKeys.COMBINE_INPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Combine output records")) {
				reduceTask.setValue(ReduceTaskKeys.COMBINE_OUTPUT_RECORDS, parts[1]);
			} else if (parts[0].equals("Map-Reduce Framework.Reduce input groups")) {
				reduceTask.setValue(ReduceTaskKeys.INPUT_GROUPS, parts[1]);
			} else {
				System.out.println("Pattern:<"+ctuple+"> ==> NOT INCLUDED IN PERFORMANCE ADVISOR MAP TASK");
			}
		}    
	}

	/*
	 * Print the Job Execution Statistics
	 * TODO: split to pring job, map/reduce task list and individual map/reduce task stats
	 */
	public void printJobExecutionStatistics() {
		/*
		 * Print Job Counters
		 */
		System.out.println("JOB COUNTERS *********************************************");
		int size = this._job.size();
		java.util.Iterator<Map.Entry<Enum, String>> kv = this._job.entrySet().iterator();
		for (int i = 0; i < size; i++)
		{
			Map.Entry<Enum, String> entry = (Map.Entry<Enum, String>) kv.next();
			Enum key = entry.getKey();
			String value = entry.getValue();
			System.out.println("Key:<" + key.name() + ">, value:<"+ value +">"); 
		}
		/*
		 * 
		 */
		System.out.println("MAP COUNTERS *********************************************");
		int size1 = this._mapTaskList.size();
		for (int i = 0; i < size1; i++)
		{
			System.out.println("MAP TASK *********************************************");
			this._mapTaskList.get(i).printKeys();
		}
		/*
		 * 
		 */
		System.out.println("REDUCE COUNTERS *********************************************");
		int size2 = this._mapTaskList.size();
		for (int i = 0; i < size2; i++)
		{
			System.out.println("REDUCE TASK *********************************************");
			this._reduceTaskList.get(i).printKeys();
		}
	}

	/*
	 * Hash table keeping sorted lists of map tasks based on the specific map task key
	 */
	private Hashtable <Enum, ArrayList<MapTaskStatistics>> _sortedMapTaskListsByKey = new Hashtable<Enum, ArrayList<MapTaskStatistics>>();

	/*
	 * @return mapTaskList : ArrayList of MapTaskStatistics
	 * @param mapTaskSortKey : Specific counter key used for sorting the task list
	 * @param datatype : indicates the data type of the counter key used for sorting
	 * If sort key is null then by default map tasks are sorted using map task ids.
	 */
	public synchronized ArrayList<MapTaskStatistics> 
	getMapTaskList(Enum mapTaskSortKey, KeyDataType dataType) {

		/* 
		 * If mapTaskSortKey is null then use the task id as a key.
		 */
		if (mapTaskSortKey == null) {
			mapTaskSortKey = MapTaskKeys.TASK_ID;
		}

		if (this._sortedMapTaskListsByKey.get(mapTaskSortKey) == null) {
			ArrayList<MapTaskStatistics> newList = (ArrayList<MapTaskStatistics>)this._mapTaskList.clone();
			this._sortedMapTaskListsByKey.put(mapTaskSortKey, this.sortMapTasksByKey(newList, mapTaskSortKey, dataType));
		} 
		return this._sortedMapTaskListsByKey.get(mapTaskSortKey);
	}

	private ArrayList<MapTaskStatistics> sortMapTasksByKey (ArrayList<MapTaskStatistics> mapTasks, 
			Enum key, Enum dataType) {
		MapCounterComparator mcc = new MapCounterComparator(key, dataType);
		Collections.sort (mapTasks, mcc);
		return mapTasks;
	}

	private class MapCounterComparator implements Comparator<MapTaskStatistics> {

		public Enum _sortKey;
		public Enum _dataType;

		public MapCounterComparator(Enum key, Enum dataType) {
			this._sortKey = key;
			this._dataType = dataType;
		}

		// Comparator interface requires defining compare method.
		public int compare(MapTaskStatistics a, MapTaskStatistics b) {
			if (this._dataType == KeyDataType.LONG) {
				long aa = a.getLongValue(this._sortKey);
				long bb = b.getLongValue(this._sortKey);
				if (aa<bb) return -1; if (aa==bb) return 0; if (aa>bb) return 1;
			} else {
				return a.getStringValue(this._sortKey).compareToIgnoreCase(b.getStringValue(this._sortKey));
			}

			return 0;
		}
	}

	/*
	 * Reduce Array List sorting
	 */
	private Hashtable <Enum, ArrayList<ReduceTaskStatistics>> _sortedReduceTaskListsByKey = new Hashtable<Enum,ArrayList<ReduceTaskStatistics>>();

	/*
	 * @return reduceTaskList : ArrayList of ReduceTaskStatistics
	 * @param reduceTaskSortKey : Specific counter key used for sorting the task list
	 * @param dataType : indicates the data type of the counter key used for sorting
	 * If sort key is null then, by default reduce tasks are sorted using task ids.
	 */
	public synchronized ArrayList<ReduceTaskStatistics> 
	getReduceTaskList (Enum reduceTaskSortKey, KeyDataType dataType) {

		/* 
		 * If reduceTaskSortKey is null then use the task id as a key.
		 */
		if (reduceTaskSortKey == null) {
			reduceTaskSortKey = ReduceTaskKeys.TASK_ID;
		}

		if (this._sortedReduceTaskListsByKey.get(reduceTaskSortKey) == null) {
			ArrayList<ReduceTaskStatistics> newList = (ArrayList<ReduceTaskStatistics>)this._reduceTaskList.clone();
			this._sortedReduceTaskListsByKey.put(reduceTaskSortKey, this.sortReduceTasksByKey(newList, reduceTaskSortKey, dataType));
		} 

		return this._sortedReduceTaskListsByKey.get(reduceTaskSortKey);  
	}

	private ArrayList<ReduceTaskStatistics> sortReduceTasksByKey (ArrayList<ReduceTaskStatistics> reduceTasks, 
			Enum key, Enum dataType) {
		ReduceCounterComparator rcc = new ReduceCounterComparator(key, dataType);
		Collections.sort (reduceTasks, rcc);
		return reduceTasks;
	}

	private class ReduceCounterComparator implements Comparator<ReduceTaskStatistics> {

		public Enum _sortKey;
		public Enum _dataType;  //either long or string

		public ReduceCounterComparator(Enum key, Enum dataType) {
			this._sortKey = key;
			this._dataType = dataType;
		}

		// Comparator interface requires defining compare method.
		public int compare(ReduceTaskStatistics a, ReduceTaskStatistics b) {
			if (this._dataType == KeyDataType.LONG) {
				long aa = a.getLongValue(this._sortKey);
				long bb = b.getLongValue(this._sortKey);
				if (aa<bb) return -1; if (aa==bb) return 0; if (aa>bb) return 1;
			} else {
				return a.getStringValue(this._sortKey).compareToIgnoreCase(b.getStringValue(this._sortKey));
			}

			return 0;
		}
	}
}