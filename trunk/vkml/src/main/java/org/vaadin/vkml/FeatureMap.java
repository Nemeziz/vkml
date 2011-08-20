package org.vaadin.vkml;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.vaadin.vol.Area;
import org.vaadin.vol.Bounds;
import org.vaadin.vol.GoogleStreetMapLayer;
import org.vaadin.vol.MapTilerLayer;
import org.vaadin.vol.OpenLayersMap;
import org.vaadin.vol.Point;
import org.vaadin.vol.Vector;
import org.vaadin.vol.VectorLayer;
import org.vaadin.vol.VectorLayer.DrawingMode;
import org.vaadin.vol.VectorLayer.VectorDrawnEvent;
import org.vaadin.vol.VectorLayer.VectorDrawnListener;
import org.vaadin.vol.VectorLayer.VectorModifiedEvent;
import org.vaadin.vol.VectorLayer.VectorModifiedListener;
import org.vaadin.vol.VectorLayer.VectorSelectedEvent;
import org.vaadin.vol.VectorLayer.VectorSelectedListener;
import org.xml.sax.SAXException;

import com.vaadin.ui.AbstractComponent;
import com.vaadin.ui.Component;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

public class FeatureMap extends OpenLayersMap implements VectorDrawnListener,
		VectorModifiedListener, VectorSelectedListener {
	static final double METERS_ABOVE_GROUND = 30;
	private VectorLayer vectorLayer;
	private FeatureDrawnCallback drawingListener;
	private DocumentView documentView;

	public FeatureMap(DocumentView documentView) {
		this.documentView = documentView;
		addBaseLayers();
		vectorLayer = new VectorLayer();
		vectorLayer.addListener((VectorDrawnListener) this);
		vectorLayer.setDrawindMode(DrawingMode.MODIFY);
		addLayer(vectorLayer);
		setSizeFull();
		setDefaultCenterAndZoom();
		vectorLayer.addListener((VectorModifiedListener) this);
		vectorLayer.addListener((VectorSelectedListener) this);
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
//			MapTilerLayer mapTilerLayer = new MapTilerLayer(
//					"http://localhost:9999/VAADIN/perus_kiint_2m/");
//			mapTilerLayer.setDisplayName("Peruskartta");
//			mapTilerLayer.setBaseLayer(false);
//			addLayer(mapTilerLayer);
			MapTilerLayer mapTilerLayer= new MapTilerLayer(
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
		vectorLayer.setDrawindMode(DrawingMode.MODIFY);
		drawingListener = null;
	}

	public void vectorDrawn(VectorDrawnEvent event) {
		drawingListener.drawingDone(event.getVector());
		vectorLayer.setDrawindMode(DrawingMode.MODIFY);
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

	}
	
	public boolean isDrawn(Polygon p) {
		return getVectorFor(p) != null;
	}
	
	private Vector getVectorFor(Geometry g) {
		Iterator<Component> componentIterator = vectorLayer.getComponentIterator();
		while(componentIterator.hasNext()) {
			AbstractComponent next = (AbstractComponent) componentIterator.next();
			if(next.getData() == g) {
				return (Vector) next;
			}
		}
		return null;
	}

	public void showFeature(Polygon p) {
		if(isDrawn(p)) {
			return;
		}
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
			area.setData(p);
			vectorLayer.addComponent(area);
		}
		vectorLayer.setDrawindMode(DrawingMode.MODIFY);
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

	public void vectorModified(VectorModifiedEvent event) {
		Vector vector = event.getVector();
			if(vector instanceof Area) {
				/*
				 * An area has been modified, update data to backing model object.
				 */
				Point[] points = ((Area)vector).getPoints();
				Polygon data = (Polygon) vector.getData();
				LinearRing linearRing = data.getOuterBoundaryIs().getLinearRing();
				List<Coordinate> coordinates2 = linearRing.getCoordinates();
				coordinates2.clear();
				for (Point p : points) {
					coordinates2.add(new Coordinate(p.getLon(), p.getLat(), METERS_ABOVE_GROUND));
				}
			}
	}

	public void vectorSelected(VectorSelectedEvent event) {
		Vector vector = event.getVector();
		documentView.selectFeatureByGeometry((Geometry) vector.getData());
	}

	public void showAllVectors() {
		Iterator<Component> componentIterator = vectorLayer.getComponentIterator();
		Bounds bounds = new Bounds();
		boolean emptyBounds = true;
		while(componentIterator.hasNext()) {
			Vector vector = (Vector) componentIterator.next();
			bounds.extend(vector.getPoints());
			if(emptyBounds) {
				if(vector.getPoints().length < 2) {
					bounds.extend(vector.getPoints());
				}
				emptyBounds = false;
			}
		}
		if(!emptyBounds) {
			zoomToExtent(bounds);
		}
		
	}

}
