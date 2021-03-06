package org.apache.hadoop.mapred;

import java.util.List;

public interface VotingSystem {
	/**
	 * Number of successful tasks
	 * @return
	 */
	int size();
	
	/**
	 * Get list of replicas
	 * @param taskId TaskId without replica
	 * @return
	 */
	List<Integer> getTask(String taskId);
	
	/**
	 * Add Taskid without replica
	 * @param key
	 */
	void addKey(String key);
	
	/**
	 * 
	 * @param key Taskid without replica
	 * @param value Replica number
	 */
	void addValue(String key, Integer value);
	
	/**
	 * Saves digest
	 * @param tid
	 * @param values
	 */
	void addHash(TaskID tid, boolean map, String[] values);
	
	/**
	 * Removes digest
	 * @param tid
	 * @return
	 */
	boolean removeHash(TaskID tid);
	/**
	 * Get the hash for the tid
	 * @param tid
	 * @return
	 */
	String[] getHash(TaskID tid);
	/**
	 * Get the limit to consider a majority
	 * @return (n/2)+1
	 */
	int getThreshold();
	
	/**
	 * Check if a reduce tasks have a majority of digests
	 * 
	 * @param tid ID 
	 *
	 * @return MAJORITY_VOTING, or NO_MAJORITY, or NOT_ENOUGH_ELEMENTS
	 */
	int hasMajorityOfDigests(TaskID tid);
	
	/**
	 * Get a task id of a reduce task that hasn't got a majority of equal digests
	 * @return
	 */
	int getTaskWithoutMajority();
	
	
	/**
	 * All digests are equal to digest param
	 * @param tid
	 * @param digest
	 * @return
	 */
	boolean allEqual(TaskID tid, String[] digest);
	
	/**
	 * Add task completion event
	 * @param tid
	 * @param event
	 */
	void addTaskCompletionEvent(TaskID tid, TaskCompletionEvent event);
	
	/**
     * Add the first replica of each map task that ended.
     * E.g. M_000000_2 and M_000001_0, or M_000000_3 and M_000001_0
     * @param event
     */
	void addFirst(TaskCompletionEvent event);
	
	
	/**
	 * Compare digests
	 * @param digest1
	 * @param digest2
	 * @return
	 */
	boolean digestsEquals(String[] digest1, String[] digest2);
	
	/**
	 * Add the first tid who got the digests
	 * @param tid
	 * @param values
	 */
	void addFirstHash(TaskID tid, String[] values);
	
	/**
	 * Return an array of digests related to the first event
	 * @param tid
	 * @return
	 */
	String[] getFirstHash(TaskID tid);
	/**
	 * Is empty 
	 * @param tid
	 * @return
	 */
	boolean isEmpty(TaskID tid);
	
	/**
	 * Get Task Completion Events
	 * @param tid
	 * @return
	 */
	List<TaskCompletionEvent> getTaskCompletionEvent(TaskID tid);
	
	/**
	 * Get a list of tasks	
	 * @param tid
	 * @return
	 */
	List<TaskID> getTask(TaskID tid);
}
