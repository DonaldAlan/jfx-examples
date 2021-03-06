package examples;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

/**
 * Demonstrates Tube.java, with a File Chooser to select the image to map to the Tube. (Try a png with transparency!)
 * The Flatten/Curl button lets you flatten/curl the tube via an animation.
 * 
 * @author Don Smith
 *
 */
public class TestTube extends Application {
	private Group root = new Group();
	private final XformCamera cameraXform = new XformCamera();
	private static double cameraInitialX = 140;
	private static double cameraInitialY = 10;
	private static double cameraInitialZ = -180;
	private static final double CAMERA_NEAR_CLIP = 0.1;
	private static final double CAMERA_FAR_CLIP = 16000.0;
	private static final Point3D X_AXIS = new Point3D(1,0,0);
	private static final Point3D Y_AXIS = new Point3D(0,1,0);
	private static final Point3D Z_AXIS = new Point3D(0,0,1);
	private double mousePosX, mousePosY, mouseOldX, mouseOldY, mouseDeltaX, mouseDeltaY;
	private final PerspectiveCamera camera = new PerspectiveCamera(true);
	private final XformWorld world = new XformWorld();
	private boolean animate = false;
	private final List<MeshView> meshViews = new ArrayList<>();
	private Cylinder c;
	private final Rotate rx = new Rotate(0,X_AXIS); 
	private final Rotate ry = new Rotate(0,Y_AXIS); 
	private final Rotate rz = new Rotate(0,Z_AXIS); 
	//private final WritableImage tubeImage = new WritableImage(1000,1);
	private final Image tubeImage = new Image("file:imgs/abc.png"); 
	 //new Image("file:imgs/earth/diffuse-map.jpg");
	private static final Random random = new Random();
	private Tube tube;
	private boolean flattening = true;
	private final Button flattenUnFlattenButton = new Button("Stopped");
	// -------------------------
	private static class XformWorld extends Group {
		final Translate t = new Translate(0.0, 0.0, 0.0);
		final Rotate rx = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
		final Rotate ry = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
		final Rotate rz = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);
		  
		public XformWorld() {
			super();
			this.getTransforms().addAll(t, rx, ry, rz);
		}
	}
		private static class XformCamera extends Group {
			final Translate t = new Translate(cameraInitialX, cameraInitialY, cameraInitialZ);
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
			camera.setFieldOfView(80);
		}
	@Override
	public void init() {
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
			  PickResult pr = me.getPickResult();
			  if (pr.getIntersectedNode() instanceof Sphere) {
			  }
			  if (pr.getIntersectedNode() instanceof Cylinder) {
			  }
		});
		scene.setOnMouseReleased((MouseEvent me) -> {
		});
		scene.setOnMouseDragExited((MouseEvent me) -> {
		});
		scene.setOnMouseDragged((MouseEvent me) -> {
			mouseOldX = mousePosX;
			mouseOldY = mousePosY;
			mousePosX = me.getSceneX();
			mousePosY = me.getSceneY();
			mouseDeltaX = (mousePosX - mouseOldX);
			mouseDeltaY = (mousePosY - mouseOldY);
			if (me.isPrimaryButtonDown()) {
				if (me.isControlDown()) {
					world.rz.setAngle(mouseDeltaX+world.rz.getAngle());
				} else {
					world.ry.setAngle(world.ry.getAngle() + mouseDeltaX * 0.2);
					world.rx.setAngle(world.rx.getAngle() - mouseDeltaY * 0.2);
				}
			} else if (me.isSecondaryButtonDown()) {
				world.t.setZ(world.t.getZ() - mouseDeltaY);
				world.t.setX(world.t.getX() - mouseDeltaX);
			}
			//System.out.println("world.rx = " + world.rx.getAngle()+ ", world.ry = " + world.ry.getAngle() + ", world.rz = " + world.rz.getAngle());
		});
	}
	private void reset() {
		world.rx.setAngle(0);
		world.ry.setAngle(0);
		world.t.setX(0);
		world.t.setZ(0);
		rx.setAngle(0);
		ry.setAngle(0);
		rz.setAngle(0);
	}
	private void handleKeyEvents(Scene scene) {
		scene.setOnKeyPressed(new EventHandler<KeyEvent>() {
			public void handle(KeyEvent ke) {
				final double delta = (ke.isShiftDown() ? 100 : 10);
				switch (ke.getCode()) {
				case Q:
					System.exit(0);
					break;
				case SPACE:
					animate = !animate;
					break;
				case LEFT:
					world.t.setX(world.t.getX() + delta);
					break;
				case RIGHT:
					world.t.setX(world.t.getX() - delta);
					break;
				case UP:
					if (ke.isControlDown()) {
						world.t.setY(world.t.getY() + delta);
					} else {
						world.t.setZ(world.t.getZ()-delta);
					}
					break;
				case DOWN:
					if (ke.isControlDown()) {
						world.t.setY(world.t.getY() - delta);
					} else {
						world.t.setZ(world.t.getZ()+delta);
					}
					break;
				case R:
					reset();
					c.setRotate(0);
					break;
				case INSERT:
					rx.setAngle(1+rx.getAngle());
					break;
				case DELETE:
					rx.setAngle(-1+rx.getAngle());
					break;
				case HOME:
					ry.setAngle(1+ry.getAngle());
					break;
				case END:
					ry.setAngle(-1+ry.getAngle());
					break;
				case PAGE_UP:
					rz.setAngle(1+rz.getAngle());
					break;
				case PAGE_DOWN:
					rz.setAngle(-1+rz.getAngle());
					break;
				default:break;
				}
			}
		});
	}
	// From http://netzwerg.ch/blog/2015/03/22/javafx-3d-line/
		public Cylinder createCylinderBetween(Point3D p1, Point3D p2, double radius, Material material) {
			Cylinder cylinder = new Cylinder();
			updateCylinderBetween(cylinder, p1, p2, radius, material);
			return cylinder;
		}
		// From http://netzwerg.ch/blog/2015/03/22/javafx-3d-line/
		public Cylinder updateCylinderBetween(Cylinder cylinder, Point3D p1, Point3D p2, double radius, Material material) {
			cylinder.getTransforms().clear();
			cylinder.setRadius(radius);
			Point3D diff = p2.subtract(p1);
			double height = diff.magnitude();
			cylinder.setHeight(height);
			Point3D mid = p2.midpoint(p1);
			Translate moveToMidpoint = new Translate(mid.getX(), mid.getY(), mid.getZ());
			Point3D axisOfRotation = diff.crossProduct(Y_AXIS);
			double angle = Math.acos(diff.normalize().dotProduct(Y_AXIS));
			Rotate rotateAroundCenter = new Rotate(-Math.toDegrees(angle), axisOfRotation);
			cylinder.getTransforms().addAll(moveToMidpoint, rotateAroundCenter);
			cylinder.setMaterial(material);
			return cylinder;
		}

	@Override
	public void start(Stage primaryStage) throws Exception {
		try {
			startAux(primaryStage);
		} catch (Exception exc) {
			exc.printStackTrace();
			System.exit(1);
		}
	}

	
	public static void main(String[] args) {
		try {
			Application.launch(args);
		} catch (Exception exc) {
			exc.printStackTrace();
			System.exit(1);
		}
		}
	private static double square(final double d) {
		return d*d;
	}
	private static Point3D getRandomVector(final double length) {
		return new Point3D(random.nextDouble()-0.5,random.nextDouble()-0.5,random.nextDouble()-0.5).normalize().multiply(length);
	}
	double ratioFlat = 0.0;
	private void animate() {
		final AnimationTimer timer= new AnimationTimer() {
			@Override
			public void handle(long nowInNanoSeconds) {
				tube.setRotate(0.3+tube.getRotate());
				if (!animate) {
					return;
				}
				if (flattening) {
					if (ratioFlat<1) {
						ratioFlat = Math.min(1.0, ratioFlat+ 0.001);
						if (ratioFlat == 1.0) {
							flattening = false;
							stopAnimation();
						}
					}
				} else {
					if (ratioFlat>0) {
						ratioFlat = Math.max(0.0, ratioFlat- 0.001);
						if (ratioFlat == 0.0) {
							flattening = true;
							stopAnimation();
						}
					}
				}
				tube.flatten(ratioFlat);
			} // handle
           };
           timer.start();
	}
	private static int addAlpha(int rgb) {
		rgb ^= (127 << 24);
		return rgb;
	}
	private void showTube(File file) throws IOException {
		if (file==null) {
			System.exit(0);
		}
		final PhongMaterial material = new PhongMaterial();
		BufferedImage bufferedImage = ImageIO.read(file);

		final WritableImage image = new WritableImage(bufferedImage.getWidth(),bufferedImage.getHeight());
		SwingFXUtils.toFXImage(bufferedImage,image);
		// TODO: figure out why the alpha applies to only half the image
		if (false) {
			final PixelReader pixelReader = image.getPixelReader(); 
			PixelWriter pixelWriter = image.getPixelWriter();
			for(int row=0;row<image.getHeight();row++) {
				for(int col=0;col<image.getWidth();col++) {
					pixelWriter.setArgb(col, row, addAlpha(pixelReader.getArgb(col,row)));
				}
			}
		}
		material.setDiffuseMap(image);
		Point3D p1 = new Point3D(-40,-140,0);
		Point3D p2 = new Point3D(60,-40,100);
		Point3D delta = p2.subtract(p1).multiply(0.1);
		tube = new Tube(p1,p2,20, 50,material);
		tube.setRotationAxis(p1.subtract(p2));
		world.getChildren().add(tube);
		p1 = p1.add(delta);
		p2 = p2.add(delta);
	}
	private void stopAnimation() {
		animate = false;
		flattenUnFlattenButton.setTextFill(Color.RED);
		flattenUnFlattenButton.setText("Click to " + (flattening? "flatten":"curl  "));
	}
	public void startAux(final Stage primaryStage) throws Exception {
		Scene scene = new Scene(root, 1600, 1000, true);
		scene.setFill(Color.DARKGREY.darker().darker());
		primaryStage.setTitle("Test Tube");
		root.getChildren().add(world);
		handleKeyEvents(scene);
		handleMouse(scene);
		buildCamera();
		world.ry.setAngle(45.0);
		
		ExtensionFilter filter = new ExtensionFilter("Images", "*.jpg","*.png","*.jpeg");
		new ChooseFile(primaryStage,"Choose image",filter,new Consumer<File>() {
			@Override
			public void accept(File file) {
				try {
					showTube(file);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}});
		
		AmbientLight light = new AmbientLight(Color.WHITE);
		root.getChildren().add(light);


		flattenUnFlattenButton.setTranslateX(600);
		flattenUnFlattenButton.setTranslateY(-250);
		flattenUnFlattenButton.setTextFill(Color.RED);
		flattenUnFlattenButton.setOnAction(e -> {
			animate = !animate;
			if (animate) {
				flattenUnFlattenButton.setTextFill(Color.GREEN);
				flattenUnFlattenButton.setText(flattening? "Flattening": "Curling  ");
			} else {
				stopAnimation();
			}
			});
		stopAnimation();
		root.getChildren().add(flattenUnFlattenButton);
		
		world.rx.setAngle(-69);
		world.ry.setAngle(-214);
		world.rz.setAngle(253);
		scene.setCamera(camera);
		primaryStage.setScene(scene);
		primaryStage.show();
		animate();
	}
}
