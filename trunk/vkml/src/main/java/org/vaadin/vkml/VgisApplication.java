package org.vaadin.vkml;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.vaadin.Application;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.Upload.StartedEvent;
import com.vaadin.ui.Upload.StartedListener;
import com.vaadin.ui.Window;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.LabelStyle;
import de.micromata.opengis.kml.v_2_2_0.LineStyle;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.PolyStyle;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.Style;

public class VgisApplication extends Application {
	private static final boolean debug = false;

	@Override
	public void init() {

		if (debug) {
			try {
				testKmlCreation();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			showStartScreen();
		}

	}

	private void showStartScreen() {
		final Window window = new Window();
		setMainWindow(window);
		
		try {
			CustomLayout customLayout = new CustomLayout(getClass().getResourceAsStream("README.html"));
			customLayout.setWidth("100%");
			window.addComponent(customLayout);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		

		Upload upload = new Upload();
		upload.setCaption("Choose Kml file as a starting point (guarranteed not to support (yet) all types of kml files). Simple areas and folders should work.:");
		upload.setImmediate(true);
		upload.setReceiver(new Receiver() {
			public OutputStream receiveUpload(String filename, String mimeType) {
				if (filename.endsWith(".kml")) {
					PipedOutputStream pipedOutputStream = new PipedOutputStream();
					final PipedInputStream pipedInputStream = new PipedInputStream();
					try {
						pipedInputStream.connect(pipedOutputStream);
						Thread thread = new Thread() {
							public void run() {
								Kml unmarshal = Kml.unmarshal(pipedInputStream);
								DocumentView documentWindow = new DocumentView(
										unmarshal);
								window.setContent(documentWindow);
							};
						};
						thread.start();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return pipedOutputStream;
				}
				return null;
			}
		});

		final ProgressIndicator progressIndicator = new ProgressIndicator();
		upload.addListener(new StartedListener() {
			public void uploadStarted(StartedEvent event) {
				window.addComponent(progressIndicator);
				progressIndicator.setValue(0);
				progressIndicator.setPollingInterval(1000);
			}
		});
		upload.addListener(new Upload.ProgressListener() {
			public void updateProgress(long readBytes, long contentLength) {
				progressIndicator.setValue(readBytes / contentLength);
			}
		});

		window.addComponent(upload);

		window.addComponent(new Label("...or use my test file:"));

		Button button = new Button("Start with test file");
		button.addListener(new ClickListener() {

			public void buttonClick(ClickEvent event) {
				window.setContent(new DocumentView(Kml.unmarshal(getClass()
						.getResourceAsStream("test.kml"))));
			}
		});
		window.addComponent(button);

	}

	private void testKmlCreation() throws FileNotFoundException {
		final Kml kml = new Kml();
		final Document document = new Document();
		kml.setFeature(document);
		document.setName("Paimion metsästysseuran paikkatietokanta");
		document.setOpen(false);

		Folder folder = new Folder();
		folder.setOpen(true);
		folder.setName("Pienriistan metsästysalueet");
		document.getFeature().add(folder);

		Folder folder2 = new Folder();
		folder2.setName("Veikkarin metsät");
		folder2.setOpen(true);
		folder.getFeature().add(folder2);
		folder = folder2;

		final Placemark placemark = new Placemark();

		placemark.setStyleUrl("PMS_A");
		placemark.setDescription("This is area foo, owner: dfs");
		folder.getFeature().add(placemark);
		placemark.setName("Esimerkki alue");

		final Polygon polygon = new Polygon();
		polygon.setTessellate(true);
		polygon.setAltitudeMode(AltitudeMode.RELATIVE_TO_GROUND);
		placemark.setGeometry(polygon);

		final Boundary outerboundary = new Boundary();
		polygon.setOuterBoundaryIs(outerboundary);

		final LinearRing outerlinearring = new LinearRing();
		outerboundary.setLinearRing(outerlinearring);

		List<Coordinate> outercoord = new ArrayList<Coordinate>();
		outerlinearring.setCoordinates(outercoord);

		/*
		 * Note that we intentionally place overlays 10 meter above ground. This
		 * way above base layers in google earth.
		 */
		outercoord.add(new Coordinate(22.68980733194, 60.42408832510198, 10));
		outercoord
				.add(new Coordinate(22.69102555615145, 60.42231935375133, 10));
		outercoord
				.add(new Coordinate(22.69474461065597, 60.42312095042898, 10));
		outercoord
				.add(new Coordinate(22.69325612486804, 60.42468962874019, 10));
		outercoord.add(new Coordinate(22.68980733194, 60.42408832510198, 10));

		addStyles(document);

		setMainWindow(new Window("Test window", new DocumentView(kml)));

		// kml.marshal(FILE);
		// kml.marshal();
	}

	private void addStyles(final Document document) {
		// <!-- Begin Style Definitions -->
		final Style style = new Style();
		style.setId("PMS_A");
		document.getStyleSelector().add(style);

		final LabelStyle labelstyle = new LabelStyle();
		labelstyle.setColor("7fffaaff");
		labelstyle.setScale(1.5);
		style.setLabelStyle(labelstyle);

		final LineStyle linestyle = new LineStyle();
		linestyle.setColor("6300ffb0");
		linestyle.setWidth(15.0);
		style.setLineStyle(linestyle);

		final PolyStyle polystyle = new PolyStyle();
		polystyle.setColor("6300ffb0");
		polystyle.setFill(true);
		style.setPolyStyle(polystyle);
	}

}
