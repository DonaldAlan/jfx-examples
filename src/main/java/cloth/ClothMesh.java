package cloth;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.collections.ObservableFloatArray;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.geometry.Pos;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Cloth model of 3d TriangleMesh in Javafx, with wind, gravity and spring forces between point masses.
 * The program is a JavaFX application with sliders for controlling the wind speed, the spring force, the gravity, 
 * and the simulation speed.
 * 
 * We set up an n x n grid of points (CPoint3D objects), each with a set of Springs.
 * The resting edge length of each spring is fixed to 40.   Deformations of the grid of points are resisted by the Spring objectss.
 * A variable wind blows on the cloth. See the method adjustPoint at the end of this file for the logic that determines forces.
 * In each iteration through the animation loop, the spring forces between points are updated and then the positions of the points
 * are updated, both in the CPoint3D objects and in the mesh's ObservableArray of floats representing the TriangleMesh.
 * 
 * There's a Spring between each point and its neighbor (if any) above, below, to the left and to the right.
 * In addition, to prevent ugly folding of the cloth, there are Springs between each point and neighbors 2, 3 and 5 edges to 
 * the left or right. See the method setUpSprings() for details on how that is done.
 * 
 * To prevent extreme forces, the cumulative spring force is limited to magnitude 1.0 in adjustPoint.  This is to prevent wild fluctuations in the cloth.
 * The limit value could be tuned.
 * 
 * @author Donald A. Smith, ThinkerFeeler@gmail.com
 *
 */
public class ClothMesh extends Application {
	private final static int n = 20; //38;
	private final static double edgeLength = 40; // used for springs
	private double windForceFactor = 0.56;
	private double windCycleFactor = 0.04;
	private double fabricForceFactor = 2.6; // If it's too high, the simulation gets jumpy.
	private double gravity = 0.3048;
	private double timeFactor = 57.2; //1.46;
	final static NumberFormat numberFormat = NumberFormat.getInstance();
	//............
	private final static int width = 1600;
	private final static int height = 900;
	private final Group root = new Group();
	private final XformWorld world = new XformWorld();
	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final XformCamera cameraXform = new XformCamera();
	private static double cameraInitialX = 200;
	private static double cameraInitialY = 200;
	private static double cameraInitialZ = -800;
	private static final double CAMERA_NEAR_CLIP = 0.1;
	private static final double CAMERA_FAR_CLIP = 20000.0;
	private double mousePosX, mousePosY, mouseOldX, mouseOldY, mouseDeltaX, mouseDeltaY;
	private PhongMaterial sphereMaterial1 = new PhongMaterial();
	private Sphere runStopSphere = new Sphere(10);
	private PhongMaterial redMaterial = new PhongMaterial();
	private PhongMaterial greenMaterial = new PhongMaterial();
	private boolean stopAnimation = false;
	private static final Point3D XAXIS = new Point3D(1, 0, 0);
	private static final Point3D YAXIS = new Point3D(0, 1, 0);
	private static final Point3D ZAXIS = new Point3D(0, 0, 1);
	private TriangleMesh mesh = new TriangleMesh();
	private final ObservableFloatArray points = mesh.getPoints();
	private final ObservableFaceArray faces = mesh.getFaces();
	private Image image = makeClothImageUsingN(); 
		//new Image("file:imgs/mandala1.png"); 
		//new Image("file:imgs/pattern.jpg");
	private final MeshView meshView = new MeshView();
	private final CPoint3D[][] pointsArray = new CPoint3D[n][n];
	private final CPoint3D[][] savedPointsArray = new CPoint3D[n][n];
	private final int[][] meshPointsLogicalIndexesMatrix = new int[n][n]; // this index is 1/3 the corresponding index in points Observable floats
	//private final int[][] meshPointsRealIndexesMatrix = new int[n][n]; // this index is yhe index in points Observable floats

	private double lastTime = 0.0;
	private Stage primaryStage;
	private final List<Spring> springs = new ArrayList<>(); // Springs are global and are updated between steps
	private final MutableVector3D _force = new MutableVector3D();
	private final Group gridGroup = new Group();
	// -----------------------------------------
	private static class MutableVector3D {
		double x;
		double y;
		double z;
		public void add(double x, double y, double z) {
			this.x += x;
			this.y += y;
			this.z += z;
		}
		public void add(MutableVector3D other) {
			x+= other.x;
			y+= other.y;
			z+= other.z;
		}
		public void subtract(MutableVector3D other) {
			x-= other.x;
			y-= other.y;
			z-= other.z;
		}
		public void resetToZero() {
			x = 0;
			y = 0;
			z = 0;
		}
		public double magnitude() {
			return Math.sqrt(square(x)+square(y)+square(z));
		}
		public void multiplyBy(double d) {
			x *= d;
			y *= d;
			z *= d;
		}
		@Override
		public String toString() {
			return "(" + x + ", " + y + ", " + z + ")";
		}
	}
	private static double squareRetainSign(final double d) {
		return Math.signum(d)*d*d;
	}
	/**
	 * 
	 * @author Don Smith
	 * If there's a spring between P1 and P2 then, Spring exists on both their lists
	 */
	private class Spring {
		final CPoint3D p1;
		final CPoint3D p2;
		// It always points in the direction of the difference vector between p1 and p2. We don't need sines and cosines!!!!!!!!!!!!!!!
		final MutableVector3D forceVector = new MutableVector3D();
		final double edgeLength;
		public Spring(CPoint3D p1, CPoint3D p2, double edgeLength) {
			this.p1 = p1;
			this.p2 = p2;
			this.edgeLength = edgeLength;
		}
		public void updateForce() {
			final double distance = p1.distance(p2);
			final double delta = distance-edgeLength;
			final double f = -fabricForceFactor* squareRetainSign(delta);
			forceVector.x = f*(p1.x-p2.x);
			forceVector.y = f*(p1.y-p2.y);
			forceVector.z = f*(p1.z-p2.z);
		}
		public void addForceP1(MutableVector3D force) {
			force.add(forceVector);
		}
		public void addForceP2(MutableVector3D force) {
			force.subtract(forceVector);
		}
	}
	private static String format(double d) {
		return numberFormat.format(d);
	}
	private class CPoint3D {
		final int row;
		final int col;
		double x,y,z;
		final List<Spring> springsP1 = new ArrayList<>();
		final List<Spring> springsP2 = new ArrayList<>();
		int meshPointRealIndex = -1; // set later to the index in ObservableFloatArray points of the float x three floats for x, y, z
		public CPoint3D(double x, double y, double z, int row, int col) {
			this.row = row;
			this.col = col;
			this.x = x;
			this.y = y;
			this.z = z;
		}
		public CPoint3D copy() {
			CPoint3D copy = new CPoint3D(x,y,z,row,col);
			copy.meshPointRealIndex = meshPointRealIndex;
			copy.springsP1.addAll(springsP1);
			copy.springsP2.addAll(springsP2);
			return copy;
		}
		@Override
		public String toString() {
			return "(" + format(x) + ", " + format(y) + ", " + format(z) + ")";
		}
		public void applyToObservablePoints() {
			points.set(meshPointRealIndex, (float) x);
			points.set(1+meshPointRealIndex, (float) y);
			points.set(2+meshPointRealIndex, (float) z);
		}
		public double getX() {
			return x;
		}
		public double getY() {
			return y;
		}
		public double getZ() {
			return z;
		}
		public double distance(CPoint3D other) {
			return Math.sqrt(square(x-other.x)+ square(y-other.y)+ square(z-other.z));
		}
		public void add(double xd, double yd, double zd) {
			setX(this.x + xd);
			setY(this.y + yd);
			setZ(this.z + zd);
		}
		public void setX(double x) {
			this.x = x;
			points.set(meshPointRealIndex, (float)x);
		}
		public void setY(double y) {
			this.y = y;
			points.set(1+meshPointRealIndex, (float)y);
		}
		public void setZ(double z) {
			this.z = z;
			points.set(2+meshPointRealIndex, (float)z);
		}
		public double dotProduct(CPoint3D other) {
			return x*other.x + y*other.y + z*other.z;
		}
		public double dotProduct(Point3D other) {
			return x*other.getX() + y*other.getY() + z*other.getZ();
		}
		public double cosineOfAngleWith(CPoint3D other) {
			return dotProduct(other)/(magnitude()*other.magnitude());
		}
		public double cosineOfAngleWith(Point3D other) {
			return dotProduct(other)/(magnitude()*other.magnitude());
		}
		public double magnitude() {
			return Math.sqrt(x*x+y*y+z*z);
		}
		public void addSpring(Spring spring) {
			if (this == spring.p1) {
				springsP1.add(spring);
			} else {
				springsP2.add(spring);
			}
		}
		public void addSpringForcesToArgument(final MutableVector3D force) {
			for(Spring spring: springsP1) {
				spring.addForceP1(force);
			}
			for(Spring spring: springsP2) {
				spring.addForceP2(force);
			}
		}
	
	}
	// -----------------------------------------
	static {
	}

	// Must be called in UI thread
	// From
	// https://stackoverflow.com/questions/26854301/how-to-control-the-javafx-tooltips-delay
	private void changeTooltipDefaults() {
		try {
			Tooltip obj = new Tooltip();
			Class<?> clazz = obj.getClass().getDeclaredClasses()[0];
			Constructor<?> constructor = clazz.getDeclaredConstructor(Duration.class, Duration.class, Duration.class,
					boolean.class);
			constructor.setAccessible(true);
			Object tooltipBehavior = constructor.newInstance(new Duration(500), // open
					new Duration(25000), // visible
					new Duration(1000), // close
					false);
			Field fieldBehavior = obj.getClass().getDeclaredField("BEHAVIOR");
			fieldBehavior.setAccessible(true);
			fieldBehavior.set(obj, tooltipBehavior);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// -------------------------
	private static class XformWorld extends Group {
		final Translate t = new Translate(0.0, 0.0, 0.0);
		final Rotate rx = new Rotate(0, 200, 0, 0, Rotate.X_AXIS);
		final Rotate ry = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
		final Rotate rz = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);

		public XformWorld() {
			super();
			this.getTransforms().addAll(t, rx, ry, rz);
		}
	}

	// -------------------------
	private static class XformCamera extends Group {
		final Translate t = new Translate(0.0, 0.0, 0.0);
		final Rotate rx = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
		final Rotate ry = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
		final Rotate rz = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);

		public XformCamera() {
			super();
			this.getTransforms().addAll(t, rx, ry, rz);
		}
	}

	private void buildCamera() {
		root.getChildren().add(cameraXform);
		cameraXform.getChildren().add(camera);
		camera.setNearClip(CAMERA_NEAR_CLIP);
		camera.setFarClip(CAMERA_FAR_CLIP);
		camera.setTranslateX(cameraInitialX);
		camera.setTranslateY(cameraInitialY);
		camera.setTranslateZ(cameraInitialZ);
	}


	// From http://netzwerg.ch/blog/2015/03/22/javafx-3d-line/
	public Cylinder createCylinderBetween(Point3D p1, Point3D p2, double radius, Material material) {
		Point3D diff = p2.subtract(p1);
		double height = diff.magnitude();
		Point3D mid = p2.midpoint(p1);
		Translate moveToMidpoint = new Translate(mid.getX(), mid.getY(), mid.getZ());
		Point3D axisOfRotation = diff.crossProduct(YAXIS);
		double angle = Math.acos(diff.normalize().dotProduct(YAXIS));
		Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);
		Cylinder cylinder = new Cylinder(radius, height);
		cylinder.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
		cylinder.setMaterial(material);
		return cylinder;
	}


	private static double square(double x) {
		return x * x;
	}

	private void startOrStop() {
		stopAnimation = !stopAnimation;
		if (stopAnimation) {
			runStopSphere.setMaterial(redMaterial);
		} else {
			runStopSphere.setMaterial(greenMaterial);
			lastTime = 1e-9* System.nanoTime();
		}
	}
	private void handleMouse(Scene scene) {
		scene.setOnMousePressed((MouseEvent me) -> {
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseOldX = me.getSceneX();
			mouseOldY = me.getSceneY();
			// this is done after clicking and the rotations are apparently
			// performed in coordinates that are NOT rotated with the camera.
			// (pls activate the two lines below for clicking)
			// cameraXform.rx.setAngle(-90.0);
			// cameraXform.ry.setAngle(180.0);
		});
		scene.setOnMouseDragged((MouseEvent me) -> {
			mouseOldX = mousePosX;
			mouseOldY = mousePosY;
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseDeltaX = (mousePosX - mouseOldX);
			mouseDeltaY = (mousePosY - mouseOldY);
			if (me.isPrimaryButtonDown()) {
				// this is done when the mouse is dragged and each rotation is
				// performed in coordinates, that are rotated with the camera.
				world.ry.setAngle(world.ry.getAngle() - mouseDeltaX * 0.2);
				world.rx.setAngle(world.rx.getAngle() + mouseDeltaY * 0.2);

				// world.ry.setAngle(world.ry.getAngle() + mouseDeltaX * 0.2);
				// world.rx.setAngle(world.rx.getAngle() - mouseDeltaY * 0.2);
			} else if (me.isSecondaryButtonDown()) {
				world.t.setZ(world.t.getZ() + mouseDeltaY);
				world.t.setX(world.t.getX() + mouseDeltaX);
			}
		});
	}
	private Image makeClothImageUsingN() {
		final int c=10;
		int width=n*c;
		int height=n*c;
		final WritableImage image = new WritableImage(width,height);
		final PixelWriter pixelWriter = image.getPixelWriter();
		double hue = 0.0;
		for(int row=0;row<n;row++) {
			for(int col=0;col<n;col++) {
				for(int y=row*c;y < (1+row)*c; y++) {
					for(int x=col*c; x < (col+1)*c; x++) {
						pixelWriter.setColor(y, x, Color.hsb(hue, 0.5, 0.9));
					}
				}
				hue+= 23.3;
			}
		}
		return image;
	}
	
	private void reset() {
		cameraXform.t.setZ(0);
		cameraXform.rx.setAngle(0);
		cameraXform.ry.setAngle(0);
		world.setTranslateX(0);
		world.setTranslateY(0);
		world.setTranslateZ(0);
		world.rx.setAngle(0);
		world.ry.setAngle(0);
		world.rz.setAngle(0);
		world.ry.setAngle(-40);
		world.t.setX(-200);
		world.t.setY(-7*n);
		world.t.setZ(60*n);
	}

	private void handleKeyEvents(Scene scene) {
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent ke) {
				final double f = ke.isShiftDown() ? 100 : 10;
				final double f2 = ke.isShiftDown()? 1.5: 1.1;
				switch (ke.getCode()) {
				case Q:
					System.exit(0);
					break;
				case T:
					if (ke.isShiftDown()) {
						timeFactor *= 1.1;
					} else {
						timeFactor /= 1.1;
					}
					System.out.println("timeFactor = " + timeFactor);
					break;
				case INSERT:
					windForceFactor*= f2;
					System.out.println("windForceFactor = " + windForceFactor);
					break;
				case DELETE:
					windForceFactor/= f2;
					System.out.println("windForceFactor = " + windForceFactor);
					break;
				case HOME:
					windCycleFactor *= f2;
					System.out.println("windCycleFactor = " + windCycleFactor);
					break;
				case END:
					windCycleFactor /= f2;
					System.out.println("windCycleFactor = " + windCycleFactor);
					break;
				case PAGE_UP:
					fabricForceFactor *= f2;
					System.out.println("fabricForceFactor = " + fabricForceFactor);
					break;
				case PAGE_DOWN:
					fabricForceFactor /= f2;
					System.out.println("fabricForceFactor = " + fabricForceFactor);
					break;
				case G:
					if (ke.isControlDown()) {
						gridGroup.setVisible(!gridGroup.isVisible());
					} else if (ke.isShiftDown()) {
						gravity *= 1.1;
					} else {
						gravity /= 1.1;
					}
					System.out.println("gravity = " + gravity);
					break;
				case SPACE:
					startOrStop();
					break;
				case R:
					if (ke.isShiftDown()) {
						try {
						for(int row=0;row<n;row++) {
							for(int col=0;col<n;col++) {
								pointsArray[row][col].x=savedPointsArray[row][col].x;
								pointsArray[row][col].y=savedPointsArray[row][col].y;
								pointsArray[row][col].z=savedPointsArray[row][col].z;
								pointsArray[row][col].applyToObservablePoints();
							}
						} } catch (Exception exc) {
							exc.printStackTrace();
						}
					}
					reset();
					break;
				case W:
					if (ke.isShiftDown()) {
						windForceFactor *= 1.1;
					} else {
						windForceFactor /= 1.1;
					}
					System.out.println("windForceFactor = " + format(windForceFactor));
					break;
				case LEFT:
					world.setTranslateX(world.getTranslateX() + f);
					break;
				case RIGHT:
					world.setTranslateX(world.getTranslateX() - f);
					break;
				case UP:
					if (ke.isControlDown()) {
						world.setTranslateY(world.getTranslateY() + f);
					} else {
						world.setTranslateZ(world.getTranslateZ() - f);
					}
					break;
				case DOWN:
					if (ke.isControlDown()) {
						world.setTranslateY(world.getTranslateY() - f);
					} else {
						world.setTranslateZ(world.getTranslateZ() + f);
					}
					break;
				default:
				}
			}
		});
	}

	public static void main(String[] args) {
		try {
			numberFormat.setMaximumFractionDigits(2);
			numberFormat.setMinimumFractionDigits(2);
			//test(); System.exit(0);
			launch(args);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}


	private void buildGrid() {
		PhongMaterial materialX = new PhongMaterial(Color.RED);
		PhongMaterial materialY = new PhongMaterial(Color.GREEN);
		PhongMaterial materialZ = new PhongMaterial(Color.BLUE);
		Point3D x = new Point3D(1000, 0, 0);
		Point3D y = new Point3D(0, 1000, 0);
		Point3D z = new Point3D(0, 0, 1000);
		final double radius = 1.0;
		Cylinder xAxis = createCylinderBetween(x, x.multiply(-1), radius, materialX);

		Text xText = new Text("x");
		final Font fontLocal = new Font(30);
		xText.setFont(fontLocal);
		xText.setTranslateX(x.getX());
		xText.setFill(Color.GHOSTWHITE);

		Text yText = new Text("y");
		yText.setFont(fontLocal);
		yText.setTranslateY(y.getY());
		yText.setFill(Color.GHOSTWHITE);

		Text zText = new Text("z");
		zText.setFont(fontLocal);
		zText.setTranslateZ(z.getZ());
		zText.setFill(Color.GHOSTWHITE);

		Cylinder yAxis = createCylinderBetween(y, y.multiply(-1), radius, materialY);
		Cylinder zAxis = createCylinderBetween(z, z.multiply(-1), radius, materialZ);
		gridGroup.getChildren().addAll(xAxis, yAxis, zAxis, xText, yText, zText);
	}
	
	private HBox makeSliderGroup(String name, double min, double max, final double initialValue,
			Supplier<Double> supplier, Consumer<Double> setter) {
		final HBox hbox = new HBox(10);
		hbox.setAlignment(Pos.CENTER);
		final Label nameLabel = new Label(name);
		nameLabel.setTextFill(Color.WHITE);
		final Slider slider = new Slider(min, max, initialValue);
		slider.setValue(supplier.get());
		final Label valueLabel = new Label();
		valueLabel.setText(numberFormat.format(slider.getValue()));
		valueLabel.setTextFill(Color.WHITE);
		hbox.getChildren().addAll(nameLabel, slider, valueLabel);

		String css = "-fx-tick-label-fill: white;-fx-color:white; -fx-control-inner-background: white; -fx-text-fill:white; xf-fill: white; -fx-tick-label-fill:white";
		slider.setStyle(css);
		slider.applyCss();
		slider.setPrefWidth(200);
		slider.setMaxWidth(200);
		slider.setMinWidth(200);
		slider.setShowTickLabels(true);
		slider.setShowTickMarks(true);
		slider.setMajorTickUnit((max - min) / 10);
		slider.setMinorTickCount(0);
		slider.setBlockIncrement((max - min) / 10);
		final long[] lastChangeTime = { 0 };
		slider.valueProperty().addListener(e -> {
			final long now = System.currentTimeMillis();
			valueLabel.setText(numberFormat.format(slider.getValue()));
			if (now - lastChangeTime[0] > 200 || slider.getValue() == slider.getMin()
					|| slider.getValue() == slider.getMax()) {
				setter.accept(slider.getValue());
				lastChangeTime[0] = now;
			}
		});
		slider.setOnMouseReleased(e -> {
			valueLabel.setText(numberFormat.format(slider.getValue()));
			setter.accept(slider.getValue());
		});

		return hbox;
	}
	private void buildSliders() {
		VBox vbox = new VBox(20);
		vbox.setTranslateX(320);
		vbox.setTranslateY(-20);
		HBox windSlider = makeSliderGroup("Wind",0,5,0.56, () -> windForceFactor, d -> {windForceFactor=d;});
		HBox springSlider = makeSliderGroup("Spring",0,5,2.6, () -> fabricForceFactor, d -> {fabricForceFactor=d;});
		HBox gravitySlider = makeSliderGroup("Gravity",0,1,0.3, () -> gravity, d -> {gravity=d;});
		HBox timeSlider = makeSliderGroup("Time",0,10,5.7, () -> 0.1*timeFactor, d -> {timeFactor=10*d;});
		vbox.getChildren().add(windSlider);
		vbox.getChildren().add(springSlider);
		vbox.getChildren().add(gravitySlider);
		vbox.getChildren().add(timeSlider);
		vbox.setScaleX(0.75);
		vbox.setScaleY(0.75);
		root.getChildren().add(vbox);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		changeTooltipDefaults();
		mesh.setVertexFormat(VertexFormat.POINT_TEXCOORD);
		sphereMaterial1.setDiffuseColor(new Color(1, 0, 0, 1.0));
		redMaterial.setDiffuseColor(Color.RED);
		greenMaterial.setDiffuseColor(Color.GREEN);
		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);
		Scene scene = new Scene(root, width, height, true);
		scene.setFill(Color.BLACK);
		primaryStage.setTitle("Gaussian");
		primaryStage.setScene(scene);
		handleMouse(scene);
		handleKeyEvents(scene);
		buildCamera();
		
		reset();
		buildGrid();
		buildSliders();
		//gridGroup.setVisible(false);
		world.getChildren().add(gridGroup);

		meshView.setCullFace(CullFace.NONE);
		meshView.setDrawMode(DrawMode.FILL);
		meshView.setMesh(mesh);

		world.getChildren().add(meshView);
		buildCloth();
		buildBar();
		setUpSprings();
		
		runStopSphere.setTranslateX(-167);
		runStopSphere.setTranslateY(-3);
		runStopSphere.setMaterial(greenMaterial);
		runStopSphere.setOnMouseClicked( e-> { startOrStop();});
		root.getChildren().add(runStopSphere);

		AmbientLight light = new AmbientLight(Color.ANTIQUEWHITE);
		root.getChildren().add(light);
		scene.setCamera(camera);
		primaryStage.show();
		animate();

	}
	/**
	 * 
	 * @param p
	 * @return index for use in faces array = 1/3 the "real" index in CPoint3D.meshPointRealIndex
	 */
	private int getOrSetLogicalIndex(CPoint3D p) {
		if (p.meshPointRealIndex < 0) {
			p.meshPointRealIndex = points.size();
			meshPointsLogicalIndexesMatrix[p.row][p.col] = points.size()/3;
			points.addAll((float) p.getX(), (float) p.getY(), (float) p.getZ());
		}
		return p.meshPointRealIndex/3;
	}


	private void addTriangleToMesh(CPoint3D p1, CPoint3D p2, CPoint3D p3, int t1, int t2, int t3) {
		int i1 = getOrSetLogicalIndex(p1);
		int i2 = getOrSetLogicalIndex(p2);
		int i3 = getOrSetLogicalIndex(p3);
		faces.addAll(i1, t1, i2, t2, i3, t3);
	}

	public int getMeshPointsRealIndex(int row, int col) {
		return pointsArray[row][col].meshPointRealIndex;
	}
	private static Point3D makePoint3D(CPoint3D p) {
		return new Point3D(p.x,p.y,p.z);
	}
	private void buildBar() {
		CPoint3D left = pointsArray[0][0];
		CPoint3D right = pointsArray[0][n-1];
		Point3D pLeft = makePoint3D(left);
		Point3D pRight = makePoint3D(right);
		Image image = new Image("file:imgs/wood6.jpg");
		PhongMaterial material = new PhongMaterial();
		material.setDiffuseMap(image);
		Cylinder bar = createCylinderBetween(pLeft,pRight, 3, material);
		
		Sphere s1 = new Sphere(5);
		s1.setTranslateX(left.x);
		s1.setTranslateY(left.y);
		s1.setTranslateZ(left.z);
		s1.setMaterial(material);
		Sphere s2 = new Sphere(5);
		s2.setTranslateX(right.x);
		s2.setTranslateY(right.y);
		s2.setTranslateZ(right.z);
		s2.setMaterial(material);
		world.getChildren().addAll(bar,s1,s2);
	}
	
	private void buildCloth() {
		
		for (int row = 0; row < n; row++) {
			for (int col = 0; col < n; col++) {
				double x = edgeLength * col;
				double y = edgeLength * row;
				double z = 0;
				CPoint3D p = new CPoint3D(x, y, z, row, col);
				pointsArray[row][col] = p;
			}
		}
		for (int row = 0; row < n; row++) {
			for (int col = 0; col < n; col++) {
				getOrSetLogicalIndex(pointsArray[row][col]);
			}
		}
// 0 3 6 9 12 15 18 21 24 27 30 33 36 39 42 45 48 51 54 57 
//60 63 66 69 72 75 78 81 84 87 90 93 96 99 102 105 108 111 114 117 		
		for (int row = 0; row < n - 1; row++) {
			for (int col = 0; col < n - 1; col++) {
				CPoint3D topLeft = pointsArray[col][row];
				CPoint3D bottomLeft = pointsArray[col + 1][row];
				CPoint3D bottomRight = pointsArray[col + 1][row + 1];
				CPoint3D topRight = pointsArray[col][row + 1];
				int t = col * n + row;
				addTriangleToMesh(topLeft, bottomLeft, bottomRight, t, t + n, t + n + 1);
				addTriangleToMesh(topLeft, topRight, bottomRight, t, t + 1, t + n + 1);
			}
		}
		for (int col = 0; col < n; col++) {
			double v = (0.0 + col) / n;
			for (int row = 0; row < n; row++) {
				double u = (0.0 + row) / n;
				mesh.getTexCoords().addAll((float) u, (float) v);
			}
		}
		PhongMaterial meshViewMaterial = new PhongMaterial();
		meshViewMaterial.setDiffuseMap(image);
		meshView.setMaterial(meshViewMaterial);
	}
//0 3 6 9 12 15 18 21 24 27 30 33 36 39 42 45 48 51 54 57 
//60 63 66 69 72 75 78 81 84 87 90 93 96 99 102 105 108 111 114 117 	
	private float getX(final int col, final int row) {
		//return points.get(getMeshPointsRealIndex(row,col));
		return points.get(3*(row*n+col));
	}
	private float getY(final int col, final int row) {
		//return points.get(1+getMeshPointsRealIndex(row,col));
		return points.get(1+3*(row*n+col));
	}
	private float getZ(final int col, final int row) {
		//return points.get(2+getMeshPointsRealIndex(row,col));
		return points.get(2+3*(row*n+col));
	}
	
	private void setX(final int col, final int row, final double d) {
		//points.set(getMeshPointsRealIndex(row,col), (float)d);
		points.set(3*(row*n+col), (float) d);
	}
	private void setY(final int col, final int row, final double d) {
//		points.set(1+getMeshPointsRealIndex(row,col), (float)d);
		points.set(1+3*(row*n+col), (float) d);
	}
	private void setZ(final int col, final int row, final double d) {
//		points.set(2+getMeshPointsRealIndex(row,col), (float)d);
		points.set(2+3*(row*n+col), (float) d);
	}

	private void setUpSprings() {
		// make vertical springs
		for (int row = 1; row < n; row++) {
			for (int col = 0; col < n; col++) {
				CPoint3D thisPoint = pointsArray[row][col];
				CPoint3D abovePoint = pointsArray[row - 1][col];
				Spring spring = new Spring(thisPoint, abovePoint, edgeLength);
				springs.add(spring);
				thisPoint.addSpring(spring);
				abovePoint.addSpring(spring);
			}
		}
		// make horizontal springs
		// To stop vertical folding, make springs between this point and leftLeftPoint
		// make horizontal springs
		final int deltaXs[] = {1,2,3,5};
		for(int deltaX:deltaXs)
		for (int row = 0; row < n; row++) {
			for (int col = deltaX; col < n; col++) {
				CPoint3D thisPoint = pointsArray[row][col];
				CPoint3D leftPoint = pointsArray[row][col - deltaX];
				Spring spring = new Spring(thisPoint, leftPoint, deltaX * edgeLength);
				springs.add(spring);
				thisPoint.addSpring(spring);
				leftPoint.addSpring(spring);

			}
		}
		// diagonal springs?
		
		for (int row = 0; row < n; row++) {
			for (int col = 0; col < n; col++) {
				savedPointsArray[row][col]=pointsArray[row][col].copy();
			}
		}
	}
	
	//-----------------------------
	private void animate() {
		numberFormat.setMinimumFractionDigits(3);
		numberFormat.setMaximumFractionDigits(3);
		final AnimationTimer timer = new AnimationTimer() {
			@Override
			public void handle(long nowInNanoSeconds) {
				if (stopAnimation) {
					return;
				}
				for(Spring spring:springs) {
					spring.updateForce();
				}
				final double seconds = nowInNanoSeconds*1E-9;
				if (lastTime == 0.0) {
					lastTime = seconds;
					return;
				}
				final double timeDelta = seconds-lastTime;
				primaryStage.setTitle(numberFormat.format(1.0/timeDelta) + " per second");
				lastTime = seconds;
			
				for(int row=0;row<n; row++) {
					for(int col=0;col<n;col++) {
						if (row>0 || (col!=0 && col!=n-1)) {
							adjustPoint(row, col, timeFactor*seconds, timeFactor*timeDelta);
						}						
					}
				}
			} //handle
		}; // timer
		timer.start();
	}	
	
	// https://graphics.stanford.edu/~mdfisher/cloth.html
	
	private void adjustPoint(final int row, final int col, final double time,  final double timeDelta) {
		final CPoint3D thisPoint = pointsArray[row][col];
		_force.resetToZero();
		thisPoint.addSpringForcesToArgument(_force);
		
		final double magnitude=_force.magnitude();
		if (magnitude>1) { // TODO: this is to prevent wild fluctuations from too-strong forces. It can be tuned.
			_force.multiplyBy(1.0/magnitude);
		}
		_force.y += gravity;
		
		// I could take into consideration the angle between the wind and the piece of cloth
		// (sin(x*y*t), cos(z*t), sin(cos(5*x*y*z))
		double windForceX = windForceFactor*Math.sin(0.3*windCycleFactor*time); //windForceFactor*Math.sin(windCycleFactor*thisPoint.x*thisPoint.y*time);
		double windForceY = 0; //windForceFactor*Math.cos(windCycleFactor*thisPoint.z*time);
		double windForceZ = 3.3*windForceFactor*(Math.sin(windCycleFactor*time+0.1*col));
		_force.add(windForceX,windForceY, windForceZ);
		thisPoint.add(timeDelta*_force.x, timeDelta*_force.y, timeDelta*_force.z);
	}
	
}
