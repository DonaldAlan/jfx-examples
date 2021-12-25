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

/**
 * 
 * @author Donald A. Smith Smith (ThinkerFeeler@gmail.com)
 * 
 * A Tube Mesh that displays an image on the surface of a hollow cylinder.
 *
 */
public class Tube extends MeshView {
	private final TriangleMesh mesh = new TriangleMesh();
	private final ObservableFloatArray points = mesh.getPoints();
	private final ObservableFaceArray faces = mesh.getFaces();
	private final Point3D start;
	private final Point3D end;
	private final Point3D diffVector;
	private final Point3D perp1;
	private final Point3D perp2;
	private final double radius;
	private final int divisions;

	public Tube(final Point3D start, final Point3D end, final int divisions, final double radius, final Material material) {
		super();
		this.start = start;
		this.end = end;
		this.radius = radius;
		this.divisions = divisions;
		diffVector = end.subtract(start);
		perp1 = Utilities.getPerpendiculars(diffVector);
		perp2 = Utilities._perp2;
		System.out.println(perp1);
		System.out.println(perp2);
		setMesh(mesh);
		mesh.setVertexFormat(VertexFormat.POINT_TEXCOORD);
		setCullFace(CullFace.NONE);
		setDrawMode(DrawMode.FILL);
		setMaterial(material);
		build(material);
		// System.out.println("Tube " + start + " -- " + end);
	}

	private void build(final Material material) {
		for(int row=0;row<divisions;row++) {
			final float ty = (row+0.0f)/(divisions-1);// (n-row-1.0f)/n; 
			mesh.getTexCoords().addAll(ty,1.0f);
			mesh.getTexCoords().addAll(ty,0.0f);
		}
		System.out.println("n = " + divisions // + ", imageWidth = " + imageWidth + ", imageHeight = " + imageHeight 
				+ ", mesh.getTexCoords().size = " + mesh.getTexCoords().size());
/*
n = 20, imageWidth = 2500, imageHeight = 1250, mesh.getTexCoords().size = 5000
faces.size() = 240	
 */
		final double angleDelta = 2.0 * Math.PI / (divisions-1);
		final int nn = divisions + divisions;
		double angle = 0.0;
		for (int row = 0; row < divisions; row++) {
			Point3D p1 = perp1.multiply(radius * Math.sin(angle));
			Point3D p2 = perp2.multiply(radius * Math.cos(angle));
			double x1 = start.getX() + p1.getX() + p2.getX();
			double y1 = start.getY() + p1.getY() + p2.getY();
			double z1 = start.getZ() + p1.getZ() + p2.getZ();
			double x2 = x1 + diffVector.getX();
			double y2 = y1 + diffVector.getY();
			double z2 = z1 + diffVector.getZ();
			points.addAll((float) x1, (float) y1, (float) z1); // left
			points.addAll((float) x2, (float) y2, (float) z2); // right
			angle += angleDelta;
		} // for i
		for (int row = 0; row < divisions-1; row ++) {
			final int row2=(row+row)%nn;
			final int tt = row2;
			faces.addAll(row2,     tt,             row2 + 1,     (tt + 1) % nn, (row2 + 3)%nn, (tt + 3) % nn); // t1 t2 t1
			faces.addAll(row2,     tt,             row2 + 3,     (tt + 3) % nn, (row2 + 2)%nn, (tt + 2) % nn); // t1 t2 t1
			//faces.addAll(row2 + 1, (tt + 1) % nn, (row2 + 2)%nn, (tt + n) % nn, (row2 + 3)%nn, (tt + n + 1) % nn);
		}
	} // private void build
	
	public void flatten(final double ratioFlat) {
		final double oneMinusRatioFlat = 1.0-ratioFlat;
		final double angleDelta = 2.0 * Math.PI / (divisions-1);
		final double oneMinusRatioFlatTimesRadius = oneMinusRatioFlat * radius;
		final double ratioFlatTimesRadius = 0.4*ratioFlat * radius; 
		double angle = 0.0;
		int index=0;
		for(int row=0;row<divisions;row++) {
			final double flatFactor = row*ratioFlatTimesRadius;
			Point3D p1 = perp1.multiply(oneMinusRatioFlatTimesRadius * Math.sin(angle)).add(perp2.multiply(flatFactor));
			Point3D p2 = perp2.multiply(oneMinusRatioFlatTimesRadius * Math.cos(angle)).subtract(perp1.multiply(flatFactor));
			double x1 = start.getX() + p1.getX() + p2.getX();
			double y1 = start.getY() + p1.getY() + p2.getY();
			double z1 = start.getZ() + p1.getZ() + p2.getZ();
			double x2 = x1 + diffVector.getX();
			double y2 = y1 + diffVector.getY();
			double z2 = z1 + diffVector.getZ();
			points.set(index++, (float) x1);
			points.set(index++, (float) y1);
			points.set(index++, (float) z1);
			points.set(index++, (float) x2);
			points.set(index++, (float) y2);
			points.set(index++, (float) z2);
			angle += angleDelta;
		}
	}
} // Tube
