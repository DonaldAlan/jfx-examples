package examples;

import javafx.geometry.Point3D;

public class Utilities {
	public static Point3D getPerpendicular(double x, double y, double z) {
		 if (z>=x && z>= y) {
			 return new Point3D(1,1, (x+y)/-z).normalize();
		 } else if (y>=x && y>=z) {
			 return new Point3D(1,(x+z)/-y,1).normalize();
		 } else {
			 return new Point3D((z+y)/-x,1,1).normalize();
		 }
	 }
	
	public static Point3D _perp2;
	/**
	 * Leaves a vector perpendicular to vector in the static global variable perp2;
	 * @param vector
	 * @return a vector perpendicular to vector.
	 */
	private static void check(final Point3D p1, final Point3D p2) {
		if(p1.dotProduct(p2)>=0.001) {
			System.err.println("perp violation");
			System.err.println(p1);
			System.err.println(p2);
			System.err.println(p1.dotProduct(p2));
			System.exit(0);
		}
	}
	public static Point3D getPerpendiculars(Point3D vector) {
		// We need to find vector perp that is perpendicular to diffVector in 3d.
		// It has the property   perp.dotProduct(diffVector)=0.
		//   Let perp = (x,y,z).  So, we want
		//     x*diffVector.getX() + y*diffVector.getY() + z*diffVector.getZ() = 0.
		// Assume diffVector.getX()!=0.  If diffVector.getX()==0, use Y or Z.
		//     x*diffVector.getX() = -( y*diffVector.getY() + z*diffVector.getZ());
		//     x = -( y*diffVector.getY() + z*diffVector.getZ())/diffVector.getX().
		// Let y = z = 1, and 
		Point3D perp1 =  getPerpendicular(vector.getX(),vector.getY(),vector.getZ());
		_perp2 = perp1.crossProduct(vector).normalize();
		
		check(vector,perp1);
		check(vector,_perp2);
		check(perp1,_perp2);
		return perp1;
	}
}
