package examples;

import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;
import javafx.scene.paint.Material;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;

//--------------------
public class Spiral extends MeshView {
	private final TriangleMesh mesh = new TriangleMesh();
	private final ObservableFloatArray points = mesh.getPoints();
	private final ObservableFaceArray faces = mesh.getFaces();
	private final Point3D start;
	private final Point3D end;

	public Spiral(final Point3D start, final Point3D end, final int n, final double maxRadius,
			final Material material) {
		super();
		this.start = start;
		this.end = end;
		setMesh(mesh);
		mesh.setVertexFormat(VertexFormat.POINT_TEXCOORD);
		setCullFace(CullFace.NONE);
		setDrawMode(DrawMode.FILL);
		setMaterial(material);
		build(n, maxRadius);
	}

	private void build(final int n, final double maxRadius) {
		final Point3D diffVector = end.subtract(start);
		setRotationAxis(diffVector);
		final Point3D perpendicular1 = getPerpendiculars(diffVector);
		final Point3D perpendicular2 = perp2;
		final int halfN = n / 2;
		final double fDelta = 0.1;
		final int texCoordsSize = n;
		for (int i = 0; i < texCoordsSize; i++) {
			mesh.getTexCoords().addAll((i + 0.0f) / texCoordsSize, 0);
		}
		double radius = 0.0;
		double angle = 0.0;
		int index = 0;
		for (int i = 0; i < n; i++) {
			final double ratio = (0.0 + i) / n;
			final double ratio2 = ratio / ((int) ((2.0 * Math.PI) / fDelta));
			double x = start.getX() + ratio * diffVector.getX();
			double y = start.getY() + ratio * diffVector.getY();
			double z = start.getZ() + ratio * diffVector.getZ();
			// we want to draw a circle with center (x,y,z) and parallel to
			// perpendicularVector
			Point3D p1 = perpendicular1.multiply(radius * Math.sin(angle));
			Point3D p2 = perpendicular2.multiply(radius * Math.cos(angle));
			double xx = x + p1.getX() + p2.getX();
			double yy = y + p1.getY() + p2.getY();
			double zz = z + p1.getZ() + p2.getZ();
			points.addAll((float) x, (float) y, (float) z); // index
			points.addAll((float) xx, (float) yy, (float) zz); // index+1
			if (i >= 1 && i < n - 1) {
				int t1 = (int) (texCoordsSize * ((i - 1.0) / (2 * n)));
				int t2 = (int) (texCoordsSize * ((i + 0.0) / (2 * n)));
				int t3 = (int) (texCoordsSize * ((i + 1.0) / (2 * n)));
				faces.addAll(index - 1, t1, index, t2, index + 1, t3);
			}
			index += 2;
//				Sphere s = new Sphere(0.2);
//				s.setMaterial(material);
//				s.setTranslateX(xx);
//				s.setTranslateY(yy);
//				s.setTranslateZ(zz);
//				world.getChildren(). add(s);
			x += ratio2 * diffVector.getX();
			y += ratio2 * diffVector.getY();
			z += ratio2 * diffVector.getZ();
			radius += i < halfN ? maxRadius / halfN : -(maxRadius / halfN);
			angle += 0.1;
		} // for i

	} // private void build

	private static Point3D getPerpendicular(double x, double y, double z) {
		if (z >= x && z >= y) {
			return new Point3D(1, 1, (x + y) / -z).normalize();
		} else if (y >= x && y >= z) {
			return new Point3D(1, (x + z) / -y, 1).normalize();
		} else {
			return new Point3D((z + y) / -x, 1, 1).normalize();
		}
	}

	static Point3D perp2;

	/**
	 * Leaves a vector perpendicular to vector in the static global variable perp2;
	 * 
	 * @param vector
	 * @return a vector perpendicular to vector.
	 */
	public static Point3D getPerpendiculars(Point3D vector) {
		// We need to find vector perp that is perpendicular to diffVector in 3d.
		// It has the property perp.dotProduct(diffVector)=0.
		// Let perp = (x,y,z). So, we want
		// x*diffVector.getX() + y*diffVector.getY() + z*diffVector.getZ() = 0.
		// Assume diffVector.getX()!=0. If diffVector.getX()==0, use Y or Z.
		// x*diffVector.getX() = -( y*diffVector.getY() + z*diffVector.getZ());
		// x = -( y*diffVector.getY() + z*diffVector.getZ())/diffVector.getX().
		// Let y = z = 1, and
		Point3D perp1 = getPerpendicular(vector.getX(), vector.getY(), vector.getZ());
		perp2 = perp1.crossProduct(vector).normalize();
		assert (vector.dotProduct(perp1) < 0.001);
		assert (vector.dotProduct(perp2) < 0.001);
		assert (perp1.dotProduct(perp2) < 0.001);
		return perp1;
	}
}