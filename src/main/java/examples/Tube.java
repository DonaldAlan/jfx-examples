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

	public Tube(final Point3D start, final Point3D end, final int n, final double radius, final Material material) {
		super();
		this.start = start;
		this.end = end;
		setMesh(mesh);
		mesh.setVertexFormat(VertexFormat.POINT_TEXCOORD);
		setCullFace(CullFace.NONE);
		setDrawMode(DrawMode.FILL);
		setMaterial(material);
		build(n, radius, material);
		// System.out.println("Tube " + start + " -- " + end);
	}

	private void build(final int n, final double radius, final Material material) {
		final Point3D diffVector = end.subtract(start);
		final Point3D perp1 = Utilities.getPerpendiculars(diffVector);
		final Point3D perp2 = Utilities._perp2;
//		int imageWidth = 1;
//		int imageHeight = 1;
//		if (material instanceof PhongMaterial) {
//			PhongMaterial phongMaterial = (PhongMaterial) material;
//			Image image = null;
//			image = phongMaterial.getSelfIlluminationMap();
//			if (image == null) {
//				image = phongMaterial.getDiffuseMap();
//			}
//			if (image != null) {
//				imageWidth = (int) image.getWidth();
//				imageHeight = (int) image.getHeight();
//			}
//		}
//		for (int row = 0; row <= imageHeight; row++) {
//			final float ty = (row + 0.0f) / imageWidth;
//			for (int col = 0; col <= imageWidth; col += imageWidth) { // only two
//				final float tx = (col + 0.0f) / imageWidth;
//				mesh.getTexCoords().addAll(tx, ty);
//			}
//		}
		for(int row=0;row<n;row++) {
			final float ty = (row+0.0f)/(n-1);// (n-row-1.0f)/n; 
			mesh.getTexCoords().addAll(ty,1.0f);
			mesh.getTexCoords().addAll(ty,0.0f);
		}
		System.out.println("n = " + n // + ", imageWidth = " + imageWidth + ", imageHeight = " + imageHeight 
				+ ", mesh.getTexCoords().size = " + mesh.getTexCoords().size());
/*
n = 20, imageWidth = 2500, imageHeight = 1250, mesh.getTexCoords().size = 5000
faces.size() = 240	
 */
		final double angleDelta = 2.0 * Math.PI / (n-1);
		final int nn = n + n;
		double angle = 0.0;
		for (int row = 0; row <= n; row++) {
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
		for (int row = 0; row < n; row ++) {
			final int row2=(row+row)%nn;
			final int tt = row2;
			faces.addAll(row2,     tt,             row2 + 1,     (tt + 1) % nn, (row2 + 3)%nn, (tt + 3) % nn); // t1 t2 t1
			faces.addAll(row2,     tt,             row2 + 3,     (tt + 3) % nn, (row2 + 2)%nn, (tt + 2) % nn); // t1 t2 t1
			//faces.addAll(row2 + 1, (tt + 1) % nn, (row2 + 2)%nn, (tt + n) % nn, (row2 + 3)%nn, (tt + n + 1) % nn);
		}
		System.out.println("faces.size() = " + faces.size());
	} // private void build
} // Tube
