package particles;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import java.util.Random;

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
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.text.Font;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

/**
 * Simulation of smoke, with sliders to control the shape, size and longevity of the smoke.
 * 
 * It creates a configurable set of ParticleGroup objects. Each ParticleGroup has a direction and a set of smoke particles.
 * The smoke particles are stored as individual tiny triangles of a TriangleMesh. The triangles are discrete (spread
 * out in different areas).    The ParticleGroups move as a Group (cheaper than moving each individual particle) and
 * when they reach their longevity, they ParticleGroups are restarted in a randomized initial location. ParticleGroups
 * also rotate.
 * 
 * Member variables spread1OfParticles, spread2GroupSpread, and spread3ParticleGroupSpread control the spreading of 
 * groups and particles.   The member variable motionSpeed controls how fat the particle Group moves. The member variable 
 * longevity controls how long the particles live.  The member variable rotationSpeed controls how fast the ParticleGroups 
 * rotate.  Not every member variable has a slider. 
 * 
 * @author Donald A. Smith, ThinkerFeeler@gmail.com
 *
 */
public class Particles extends Application {
	private static int numberOfParticleGroups = 500;
	private int numberOfParticlesInGroup = 1000;
	private final Point3D direction = new Point3D(1.0,-1,0).normalize();
	private final Point3D nozzleLocation = new Point3D(-100,140,0);  
	private double particleSize = 0.25;
	private double rotationSpeed = 3.6*random.nextDouble();
	private final double nozzleSize = 4;
	private double spread1OfParticles= 17.0;
	private double spread2GroupSpread=0.4; 
	private double spread3ParticleGroupSpread= 2.5;
	private double motionSpeed = 1.9;
	private double minY = -200;
	
	///.......
	private double windCycleFactor = 0.04;
	private double timeFactor = 57.2; // 1.46;
	final static NumberFormat numberFormat = NumberFormat.getInstance();
	final private static Random random = new Random();
	// ............
	private final static int width = 1600;
	private final static int height = 900;
	private final Group root = new Group();
	private final XformWorld world = new XformWorld();
	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final XformCamera cameraXform = new XformCamera();
	private static double cameraInitialX = 0;
	private static double cameraInitialY = 0;
	private static double cameraInitialZ = -900;
	private static final double CAMERA_NEAR_CLIP = 0.1;
	private static final double CAMERA_FAR_CLIP = 20000.0;
	private double mousePosX, mousePosY, mouseOldX, mouseOldY, mouseDeltaX, mouseDeltaY;
	private boolean stopAnimation = false;

	private final Group gridGroup = new Group();
	private final List<ParticleGroup> particleGroups = new ArrayList<>();

	private class ParticleGroup extends Group {
		private final TriangleMesh mesh = new TriangleMesh();
		private final ObservableFloatArray points = mesh.getPoints();
		private final ObservableFaceArray faces = mesh.getFaces();
		// new Image("file:imgs/mandala1.png");
		// new Image("file:imgs/pattern.jpg");
		private final MeshView meshView = new MeshView();
		private final Point3D direction;
		private final double nozzleX;
		private final double nozzleY;
		private final double nozzleZ;
		
		//........
	
		public ParticleGroup(Point3D inDirection, Point3D nozzleLocation) {
			super();
			this.direction = inDirection.add(spread2GroupSpread*random.nextDouble(), spread2GroupSpread*random.nextDouble(),spread2GroupSpread*random.nextDouble()).normalize();
			this.nozzleX = nozzleLocation.getX();
			this.nozzleY = nozzleLocation.getY();
			this.nozzleZ = nozzleLocation.getZ();
			initialize();
			mesh.setVertexFormat(VertexFormat.POINT_TEXCOORD);
			meshView.setCullFace(CullFace.NONE);
			meshView.setDrawMode(DrawMode.FILL);
			PhongMaterial meshMaterial = new PhongMaterial();
			meshMaterial.setDiffuseColor(Color.SILVER);
			meshView.setMaterial(meshMaterial);
			meshView.setMesh(mesh);
			getChildren().add(meshView);
			buildParticles();
			setRotationAxis(inDirection);
		}
		private void initialize() {
			setRotate(0);
			double f = spread3ParticleGroupSpread*random.nextDouble();
			setTranslateX(nozzleX+f*direction.getX());
			setTranslateY(nozzleY+f*direction.getY());
			setTranslateZ(nozzleZ+f*direction.getZ());
		}
		public void move() {
			setRotate(rotationSpeed+getRotate());
			if (getTranslateY() < minY) { // TODO make maxLength
				initialize();
			} else {
				setTranslateX(motionSpeed*direction.getX()+ getTranslateX());
				setTranslateY(motionSpeed*direction.getY() + getTranslateY());
				setTranslateZ(motionSpeed*direction.getZ() + getTranslateZ());
			}
		}
		public Group buildParticles() {
			final Group group = new Group();
			for (int i = 0; i < numberOfParticlesInGroup; i++) {
				final double offsetX = i % 2 == 0 ? 0 : particleSize;
				final double offsetZ = particleSize - offsetX;
				CPoint3D p1 = new CPoint3D(spread1OfParticles*nozzleSize * (random.nextDouble() - 0.5),
						spread1OfParticles*2.0*(random.nextDouble() - 0.5), spread1OfParticles*nozzleSize * (random.nextDouble() - 0.5));
				CPoint3D p2 = new CPoint3D(p1.x + offsetX, p1.y, p1.z + offsetZ);
				CPoint3D p3 = new CPoint3D(p1.x, p1.y - 1, p1.z);
				addTriangleToMesh(p1, p2, p3, 0, 0, 0);
			}
			mesh.getTexCoords().addAll(0, 0);
			//System.out.println("n = " + n + ", #faces = " + faces.size() / 3 + ", #points = " + points.size() / 3);
			return group;
		}

	
		private class CPoint3D {
			double x, y, z;
			int meshPointRealIndex = -1; // set later to the index in ObservableFloatArray points of the float x three
											// floats for x, y, z
			public CPoint3D(double x, double y, double z) {
				this.x = x;
				this.y = y;
				this.z = z;
			}
			@Override
			public String toString() {
				return "(" + format(x) + ", " + format(y) + ", " + format(z) + ")";
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
		}


		private int getOrSetLogicalIndex(CPoint3D p) {
			if (p.meshPointRealIndex < 0) {
				p.meshPointRealIndex = points.size();
				points.addAll((float) p.getX(), (float) p.getY(), (float) p.getZ());
			}
			return p.meshPointRealIndex / 3;
		}

		private void addTriangleToMesh(CPoint3D p1, CPoint3D p2, CPoint3D p3, int t1, int t2, int t3) {
			int i1 = getOrSetLogicalIndex(p1);
			int i2 = getOrSetLogicalIndex(p2);
			int i3 = getOrSetLogicalIndex(p3);
			faces.addAll(i1, t1, i2, t2, i3, t3);
		}

	
	} // ParticleGroup
	private static String format(double d) {
		return numberFormat.format(d);
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


	private void startOrStop() {
		stopAnimation = !stopAnimation;
		if (stopAnimation) {
		} else {
		}
	}

	private void handleMouse(Scene scene) {
		scene.setOnMousePressed((MouseEvent me) -> {
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseOldX = me.getSceneX();
			mouseOldY = me.getSceneY();
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
		world.ry.setAngle(0);
		world.t.setX(0);
		world.t.setY(0);
		world.t.setZ(0);
	}

	private void handleKeyEvents(Scene scene) {
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent ke) {
				final double f = ke.isShiftDown() ? 100 : 10;
				final double f2 = ke.isShiftDown() ? 1.5 : 1.1;
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
					motionSpeed *= f2;
					System.out.println("motionSpeed = " + motionSpeed);
					break;
				case DELETE:
					motionSpeed /= f2;
					System.out.println("motionSpeed = " + motionSpeed);
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
					spread1OfParticles *= f2;
					System.out.println("spread = " + spread1OfParticles);
					break;
				case PAGE_DOWN:
					spread1OfParticles /= f2;
					System.out.println("spread = " + spread1OfParticles);
					break;
				case G:
					if (ke.isControlDown()) {
						gridGroup.setVisible(!gridGroup.isVisible());
					} else if (ke.isShiftDown()) {
						numberOfParticlesInGroup *= 1.1;
					} else {
						numberOfParticlesInGroup /= 1.1;
					}
					System.out.println("numberOfParticlesInGroup = " + numberOfParticlesInGroup);
					break;
				case SPACE:
					startOrStop();
					break;
				case R:
					if (ke.isShiftDown()) {
					}
					reset();
					break;
				case W:
					if (ke.isShiftDown()) {
						motionSpeed *= 1.1;
					} else {
						motionSpeed /= 1.1;
					}
					System.out.println("windForceFactor = " + format(motionSpeed));
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
			numberFormat.setMaximumFractionDigits(0);
			numberFormat.setMinimumFractionDigits(0);
			// test(); System.exit(0);
			launch(args);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	private HBox makeSliderGroup(String name, double min, double max, final double initialValue,
			Supplier<Double> supplier, Consumer<Double> setter) {
		final HBox hbox = new HBox(10);
		hbox.setAlignment(Pos.CENTER);
		final Label nameLabel = new Label(name);
		Font font = new Font(20);
		nameLabel.setFont(font);
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
		VBox vbox = new VBox(10);
		vbox.setTranslateX(-400);
		vbox.setTranslateY(-220);
		vbox.setTranslateZ(-20);
		HBox motionSpeedSlider = makeSliderGroup("Speed", 0, 10, motionSpeed, () -> motionSpeed, d -> {
			motionSpeed = d;
		});
		HBox spreadSlider = makeSliderGroup("Spread1", 0, 30, spread1OfParticles, () -> spread1OfParticles, d -> {
			spread1OfParticles = d;
			world.getChildren().clear();
			particleGroups.clear();
		});
		HBox directionSpreadSlider = makeSliderGroup("Spread2", 0, 1, spread2GroupSpread, () -> spread2GroupSpread, d -> {
			spread2GroupSpread = d;
			world.getChildren().clear();
			particleGroups.clear();
		});
		HBox particlesSlider = makeSliderGroup("# particles", 0, 5000, numberOfParticlesInGroup, () -> 0.0+numberOfParticlesInGroup, d -> {
			{
				numberOfParticlesInGroup = d.intValue();
				world.getChildren().clear();
				particleGroups.clear();
			}
		});
		HBox numberOfGroupsSlider = makeSliderGroup("# groups", 0, 2000, numberOfParticleGroups, () -> 0.0+numberOfParticleGroups, d -> {
			numberOfParticleGroups = d.intValue();
			while (particleGroups.size()>numberOfParticlesInGroup) {
				ParticleGroup g = particleGroups.get(particleGroups.size()-1);
				particleGroups.remove(g);
				world.getChildren().remove(g);
			}
		});
		
		HBox longevitySlider = makeSliderGroup("lifespan", 0, 400, minY, () -> -minY+150, d -> {
			minY = -(d-150);
		});
		vbox.getChildren().add(motionSpeedSlider);
		vbox.getChildren().add(spreadSlider);
		vbox.getChildren().add(directionSpreadSlider);
		vbox.getChildren().add(particlesSlider);
		vbox.getChildren().add(numberOfGroupsSlider);
		vbox.getChildren().add(longevitySlider);
//		vbox.setScaleX(1.25);
//		vbox.setScaleY(1.25);
		root.getChildren().add(vbox);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		root.getChildren().add(world);
		root.setDepthTest(DepthTest.ENABLE);
		Scene scene = new Scene(root, width, height, true);
		scene.setFill(Color.BLACK);
		primaryStage.setTitle("Particles");
		primaryStage.setScene(scene);
		handleMouse(scene);
		handleKeyEvents(scene);
		buildCamera();
		buildSliders();
		reset();

		AmbientLight light = new AmbientLight(Color.ANTIQUEWHITE);
		root.getChildren().add(light);
		scene.setCamera(camera);
		primaryStage.show();
		animate();
		System.out.println(direction);
	}
	// -----------------------------
	private void animate() {
		final AnimationTimer timer = new AnimationTimer() {
			@Override
			public void handle(long nowInNanoSeconds) {
				if (stopAnimation) {
					return;
				}
				if (particleGroups.size()<numberOfParticleGroups) {
					ParticleGroup group = new ParticleGroup(direction, nozzleLocation);
					particleGroups.add(group);
					world.getChildren().add(group);
				}
				for (ParticleGroup g : particleGroups) {
					g.move();
				}
			} // handle
		}; // timer
		timer.start();
	}

}
