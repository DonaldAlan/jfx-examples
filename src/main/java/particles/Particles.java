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
 * @author Donald A. Smith, ThinkerFeeler@gmail.com
 *
 */
public class Particles extends Application {
	private double groupLengthLongevity = 200;
	private static int numberOfParticleGroups = 120;
	private int numberOfParticlesInGroup = 6000;
	private final Point3D direction = new Point3D(1.0,-1,0).normalize();
	private final Point3D nozzleLocation = new Point3D(-50,140,0);  
	private double particleSize = 0.25;
	private double rotationSpeed = 3.6*random.nextDouble();
	private double nozzleSize = 20;
	private double directionSpread1=0.1; 
	private double spread2ParticleGroupSpread= 2.0;
	private double motionSpeed = 1.9;
	
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
	private Slider _slider;
	//-------------------
	private class ParticleGroup extends Group {
		private final TriangleMesh mesh = new TriangleMesh();
		private final ObservableFloatArray points = mesh.getPoints();
		private final ObservableFaceArray faces = mesh.getFaces();
		// new Image("file:imgs/mandala1.png");
		// new Image("file:imgs/pattern.jpg");
		private final MeshView meshView = new MeshView();
		private final Point3D direction;
		private final Point3D nozzleLocation;
		private double startX;
		private double startY;
		private double startZ;
		//........
	
		public ParticleGroup(final Point3D inDirection, final Point3D nozzleLocation) {
			super();
			this.nozzleLocation = nozzleLocation;
			this.direction = inDirection.add(directionSpread1*(random.nextDouble()-0.5), 
					directionSpread1*(random.nextDouble()-0.5),directionSpread1*(random.nextDouble()-0.5)).normalize();
		
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
			initialize(true);
			
		}
		private void initialize(boolean first) {
			//final double f = first?  groupLength *random.nextDouble() : 0;
			startX = nozzleLocation.getX();//+ f * direction.getX();
			startY = nozzleLocation.getY(); //+ f * direction.getY();
			startZ = nozzleLocation.getZ(); //+ f * direction.getZ();
			setTranslateX(startX);
			setTranslateY(startY);
			setTranslateZ(startZ);
		}
		public void move() {
			setRotate(rotationSpeed+getRotate());
			double length = Math.sqrt(square(getTranslateX()-nozzleLocation.getX())+square(getTranslateY()-nozzleLocation.getY())+ square(getTranslateZ()-nozzleLocation.getZ()));
			if (length> groupLengthLongevity) { 
				initialize(false);
			} else {
				setTranslateX(motionSpeed*direction.getX() + getTranslateX());
				setTranslateY(motionSpeed*direction.getY() + getTranslateY());
				setTranslateZ(motionSpeed*direction.getZ() + getTranslateZ());
			}
		}
		public Group buildParticles() {
			final Group group = new Group();
			for (int i = 0; i < numberOfParticlesInGroup; i++) {
				final double len = groupLengthLongevity *random.nextDouble();
				double x = len*direction.getX();
				double y = len*direction.getY();
				double z = len*direction.getZ();
				double nozzleSpread=random.nextDouble();
				final double lenRatio = len/groupLengthLongevity;
//				double x= nozzleX + len*nozzleSpread*nozzleSize*perpendicular.getX();
//				double y= nozzleY + len*nozzleSpread*nozzleSize*perpendicular.getY();
//				double z= nozzleZ + len*nozzleSpread*nozzleSize*perpendicular.getZ();
				Point3D perpendicular = getRandomPerpendicular(direction);
				double perpF = nozzleSize *nozzleSpread * (1.0+ lenRatio*spread2ParticleGroupSpread);
				x += perpF * perpendicular.getX();
				y += perpF * perpendicular.getY();
				z += perpF * perpendicular.getZ();
				final double offsetX = i % 2 == 0 ? 0 : particleSize;
				final double offsetZ = particleSize - offsetX;
				CPoint3D p1 = new CPoint3D(x,y,z);
				CPoint3D p2 = new CPoint3D(p1.x + offsetX, p1.y, p1.z + offsetZ);
				CPoint3D p3 = new CPoint3D(p1.x, p1.y - 1, p1.z);
				addTriangleToMesh(p1, p2, p3, 0, 0, 0);
			}
			mesh.getTexCoords().addAll(0, 0);
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
		} // CPoint3D
		
	

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
	
	   static Point3D perp2;
       /**
        * Leaves a vector perpendicular to vector in the static global variable perp2;
        * @param vector
        * @return a vector perpendicular to vector.
        */
       public static Point3D getRandomPerpendicular(Point3D vector) {
               // We need to find vector perp that is perpendicular to diffVector in 3d.
               // It has the property   perp.dotProduct(diffVector)=0.
               //   Let perp = (x,y,z).  So, we want
               //     x*vector.getX() + y*vector.getY() + z*vector.getZ() = 0.
               // Assume vector.getX()!=0.  If vector.getX()==0, use Y or Z.
               //     x*vector.getX() = -( y*vector.getY() + z*vector.getZ());
               //     x = -(y*vector.getY() + z*vector.getZ())/vector.getX().
               // Let y and z be random doubles
    	   if (Math.abs(vector.getX()) > 0.001) {
    		   double y = random.nextDouble()-0.5;
    		   double z = random.nextDouble()-0.5;
    		   double x = - (y*vector.getY() + z*vector.getZ())/vector.getX();
    		   return new Point3D(x,y,z).normalize();
    	   } else if (Math.abs(vector.getY()) > 0.001) {
    		   double x = random.nextDouble()-0.5;
    		   double z = random.nextDouble()-0.5;
    		   double y = - (x*vector.getX() + z*vector.getZ())/vector.getY();
    		   return new Point3D(x,y,z).normalize();
    	   } else {
    		   double x = random.nextDouble()-0.5;
    		   double y = random.nextDouble()-0.5;
    		   double z = - (x*vector.getX() + y*vector.getY())/vector.getZ();
    		   return new Point3D(x,y,z).normalize();
    	   }
       }

	
	private static String format(double d) {
		return numberFormat.format(d);
	}
	private static double square(final double d) {
		return d*d;
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
					nozzleSize *= f2;
					System.out.println("spread = " + nozzleSize);
					break;
				case PAGE_DOWN:
					nozzleSize /= f2;
					System.out.println("spread = " + nozzleSize);
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
			numberFormat.setMaximumFractionDigits(2);
			numberFormat.setMinimumFractionDigits(2);
			// test(); System.exit(0);
			launch(args);
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Leaves slider in _slider
	 * @param name
	 * @param min
	 * @param max
	 * @param initialValue
	 * @param supplier
	 * @param setter
	 * @return HBox holding slider
	 */
	private HBox makeSliderGroup(String name, double min, double max, final double initialValue,
			Supplier<Double> supplier, Consumer<Double> setter) {
		final HBox hbox = new HBox(10);
		hbox.setAlignment(Pos.CENTER);
		final Label nameLabel = new Label(name);
		Font font = new Font(20);
		nameLabel.setFont(font);
		nameLabel.setTextFill(Color.WHITE);
		final Slider slider = new Slider(min, max, initialValue);
		_slider = slider;
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
		HBox motionSpeedSliderHBox = makeSliderGroup("Speed", 0, 10, motionSpeed, () -> motionSpeed, d -> {
			motionSpeed = d;
		});
		HBox nozzleSizeSliderHBox = makeSliderGroup("Nozzle size", 0, 50, nozzleSize, () -> nozzleSize, d -> {
			nozzleSize = d;
			world.getChildren().clear();
			particleGroups.clear();
		});
//		HBox directionSpreadSlider = makeSliderGroup("Spread1", 0, 1, directionSpread1, () -> directionSpread1, d -> {
//			directionSpread1 = d;
//			world.getChildren().clear();
//			particleGroups.clear();
//		});
		HBox particleGroupSpreadSliderHBox = makeSliderGroup("Spread", 0, 4, spread2ParticleGroupSpread, () -> spread2ParticleGroupSpread, d -> {
			spread2ParticleGroupSpread = d;
			world.getChildren().clear();
			particleGroups.clear();
		});
		HBox particlesSliderHBox = makeSliderGroup("# particles", 0, 10000, numberOfParticlesInGroup, () -> 0.0+numberOfParticlesInGroup, d -> {
			{
				numberOfParticlesInGroup = d.intValue();
				world.getChildren().clear();
				particleGroups.clear();
			}
		});
		final Slider longevitySlider[] = {null};
		final HBox numberOfGroupsSliderHBox = makeSliderGroup("# groups", 0, 500, numberOfParticleGroups, () -> 0.0+numberOfParticleGroups, d -> {
			numberOfParticleGroups = d.intValue();
			while (particleGroups.size()>numberOfParticleGroups) {
				ParticleGroup g = particleGroups.remove(particleGroups.size()-1);
				world.getChildren().remove(g);
			}
			if (numberOfParticleGroups < 0.6*groupLengthLongevity) {
				longevitySlider[0].setValue(numberOfParticleGroups/0.6);
			}
		});
		
		final Slider numberOfParticleGroupsSlider = _slider;
		final HBox longevitySliderHBox = makeSliderGroup("lifespan", 0, 400, groupLengthLongevity, () -> groupLengthLongevity, d -> {
			groupLengthLongevity = d;
			if (numberOfParticleGroups < 0.6*groupLengthLongevity) {
				numberOfParticleGroups = (int)(0.6 *groupLengthLongevity);
				numberOfParticleGroupsSlider.setValue(numberOfParticleGroups);
			}
			world.getChildren().clear();
			particleGroups.clear();
		});
		longevitySlider[0] = _slider;
		vbox.getChildren().add(motionSpeedSliderHBox);
		vbox.getChildren().add(nozzleSizeSliderHBox);
		//vbox.getChildren().add(directionSpreadSlider);
		vbox.getChildren().add(particleGroupSpreadSliderHBox);
		vbox.getChildren().add(particlesSliderHBox);
		vbox.getChildren().add(numberOfGroupsSliderHBox);
		vbox.getChildren().add(longevitySliderHBox);
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
