/**
 * Stores targeting meta-data for an allied grid square.
 */
public class Node {
    	
	public Direction targetDirection = Direction.STILL;
	public double targetValue = -1;
	public int targetNeed = 1000000000;
	
	public String toString() {
		return "Node value: " + targetValue + ", need: " + targetNeed + ", direction: " + targetDirection.toString();
	}
	
	public void clearTarget() {
		targetDirection = Direction.STILL;
		targetValue = -1;
		targetNeed = 1000000000;
	}

}