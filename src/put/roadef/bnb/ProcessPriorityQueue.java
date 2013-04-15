package put.roadef.bnb;

import java.util.Comparator;

/**
 * Implemented as a heap storing best processes in the root
 * 
 * @author marcin
 * 
 */
class ProcessPriorityQueue {
	private int size;
	private int[] processes;
	private Comparator<Integer> comparator;

	public ProcessPriorityQueue(int maxSize, Comparator<Integer> comparator) {
		this.size = 0;
		this.processes = new int[maxSize];
		this.comparator = comparator;
	}

	public void add(int processId) {
		processes[++size] = processId;
		upHeapify(size);
	}

	public int getBest() {
		return processes[1];
	}

	public void removeBest() {
		swap(1, size--);
		downHeapify(1);
	}

	private void upHeapify(int index) {
		while (index > 1 && isBetter(index, index / 2)) {
			swap(index / 2, index);
			index /= 2;
		}
	}

	private void downHeapify(int index) {
		while (2 * index <= size) {
			int smallerChild = 2 * index;
			if (smallerChild < size && isBetter(smallerChild + 1, smallerChild)) {
				smallerChild++;
			}

			if (isBetter(index, smallerChild)) {
				break;
			}

			swap(index, smallerChild);
			index = smallerChild;
		}
	}

	private boolean isBetter(int index1, int index2) {
		return (comparator.compare(processes[index1], processes[index2]) < 0);
	}

	private void swap(int index1, int index2) {
		int temp = processes[index1];
		processes[index1] = processes[index2];
		processes[index2] = temp;
	}
}