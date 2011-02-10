package org.vaadin.vkml;

import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.vaadin.vol.Area;
import org.vaadin.vol.GoogleStreetMapLayer;
import org.vaadin.vol.MapTilerLayer;
import org.vaadin.vol.OpenLayersMap;
import org.vaadin.vol.Point;
import org.vaadin.vol.VectorLayer;
import org.vaadin.vol.VectorLayer.DrawingMode;
import org.vaadin.vol.VectorLayer.VectorDrawnEvent;
import org.vaadin.vol.VectorLayer.VectorDrawnListener;
import org.xml.sax.SAXException;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

public class FeatureMap extends OpenLayersMap implements VectorDrawnListener {
	private VectorLayer vectorLayer;
	private FeatureDrawnCallback drawingListener;

	public FeatureMap() {
		addBaseLayers();
		vectorLayer = new VectorLayer();
		vectorLayer.addListener(this);
		addLayer(vectorLayer);
		setSizeFull();
		setDefaultCenterAndZoom();
	}

	protected void setDefaultCenterAndZoom() {
		setCenter(22.805, 60.447);
		setZoom(15);
	}

	protected void addBaseLayers() {
		GoogleStreetMapLayer googleStreets = new GoogleStreetMapLayer();
		addLayer(googleStreets);
		try {
			// virtuallypreinstalled don't support downloading resources form it
			// self?
			// MapTilerLayer mapTilerLayer = new MapTilerLayer(
			// "http://matti.virtuallypreinstalled.com/tiles/pirttikankare/pirttikankare/");
			MapTilerLayer mapTilerLayer = new MapTilerLayer(
					"http://dl.dropbox.com/u/4041822/pirttikankare/");
			mapTilerLayer.setDisplayName("Pirttikankare");
			mapTilerLayer.setBaseLayer(false);
			addLayer(mapTilerLayer);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void drawFeature(final FeatureDrawnCallback listener) {
		if (drawingListener != null) {
			cancelDrawing();
		}
		drawingListener = listener;

		vectorLayer.removeAllComponents();
		vectorLayer.setDrawindMode(DrawingMode.AREA);

	}

	private void cancelDrawing() {
		vectorLayer.setDrawindMode(DrawingMode.NONE);
		drawingListener = null;
	}

	public void vectorDrawn(VectorDrawnEvent event) {
		drawingListener.drawingDone(event.getVector());
		vectorLayer.setDrawindMode(DrawingMode.NONE);
	}

	public void showFeature(Feature value) {
		if (value instanceof Folder) {
			List<Feature> features = ((Folder) value).getFeature();
			for (Feature feature : features) {
				showFeature(feature);
			}
		} else if (value instanceof Document) {
			List<Feature> features = ((Document) value).getFeature();
			for (Feature feature : features) {
				showFeature(feature);
			}
		} else if (value instanceof Placemark) {
			Placemark pm = (Placemark) value;
			Geometry geometry = pm.getGeometry();
			showFeature(geometry);
		} else {
			System.err.println("Unhandled feature type.");
		}

		// TODO Auto-generated method stub

	}

	public void showFeature(Polygon p) {
		List<Coordinate> coordinates = ((Polygon) p).getOuterBoundaryIs()
				.getLinearRing().getCoordinates();
		Point[] points = new Point[coordinates.size()];
		int i = 0;
		for (Coordinate coordinate : coordinates) {
			Point point = new Point(coordinate.getLongitude(),
					coordinate.getLatitude());
			points[i++] = point;
		}

		if (points.length > 0) {
			Area area = new Area();
			area.setPoints(points);
			vectorLayer.addComponent(area);
		}
	}

	public void showFeature(Geometry p) {
		if (p instanceof Polygon) {
			showFeature((Polygon) p);
		}

	}

	public void clear() {
		vectorLayer.removeAllComponents();
		vectorLayer.requestRepaint();
	}
}
