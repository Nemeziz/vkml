package org.vaadin.vkml;

import java.util.Iterator;
import java.util.List;

import org.vaadin.vol.Area;
import org.vaadin.vol.Bounds;
import org.vaadin.vol.GoogleStreetMapLayer;
import org.vaadin.vol.MapTilerLayer;
import org.vaadin.vol.OpenLayersMap;
import org.vaadin.vol.Point;
import org.vaadin.vol.StyleMap;
import org.vaadin.vol.Vector;
import org.vaadin.vol.VectorLayer;
import org.vaadin.vol.VectorLayer.DrawingMode;
import org.vaadin.vol.VectorLayer.SelectionMode;
import org.vaadin.vol.VectorLayer.VectorDrawnEvent;
import org.vaadin.vol.VectorLayer.VectorDrawnListener;
import org.vaadin.vol.VectorLayer.VectorModifiedEvent;
import org.vaadin.vol.VectorLayer.VectorModifiedListener;
import org.vaadin.vol.VectorLayer.VectorSelectedEvent;
import org.vaadin.vol.VectorLayer.VectorSelectedListener;

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
	private boolean modifiable;

    public FeatureMap(DocumentView documentView) {
    	setJsMapOptions("{projection: new OpenLayers.Projection(\"EPSG:900913\")," +
    			"maxResolution: 156543.0339,units: \"m\"," +
    			"maxExtent: new OpenLayers.Bounds(-20037508, -20037508, 20037508, 20037508.34)" +
    			"}");
        this.documentView = documentView;
        addBaseLayers();
        vectorLayer = new VectorLayer();
        vectorLayer.addListener((VectorDrawnListener) this);
        addLayer(vectorLayer);
        setSizeFull();
        setDefaultCenterAndZoom();
        vectorLayer.addListener((VectorModifiedListener) this);
        vectorLayer.addListener((VectorSelectedListener) this);
        vectorLayer.setSelectionMode(SelectionMode.SIMPLE);
    }

    protected void setDefaultCenterAndZoom() {
        setCenter(22.805, 60.447);
        setZoom(15);
    }

    protected void addBaseLayers() {
        GoogleStreetMapLayer googleStreets = new GoogleStreetMapLayer();
        addLayer(googleStreets);
        
        List<MapTilerLayer> extraLayers = BackgroundLayerEditor.getExtraLayers();
        synchronized (extraLayers) {
			for (MapTilerLayer layer : extraLayers) {
				MapTilerLayer mapTilerLayer = new MapTilerLayer(layer.getBounds(), layer.getMaxZoom(), layer.getMinZoom());
				mapTilerLayer.setBaseLayer(false);
				mapTilerLayer.setUri(layer.getUri());
				mapTilerLayer.setCaption(layer.getCaption());
				mapTilerLayer.setDisplayName(layer.getDisplayName());
				addLayer(mapTilerLayer);
			}
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
        setModifiable(modifiable);
        drawingListener = null;
    }

    public void vectorDrawn(VectorDrawnEvent event) {
        drawingListener.drawingDone(event.getVector());
        setModifiable(modifiable);
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
            showFeature(geometry, pm.getStyleUrl());
        } else {
            System.err.println("Unhandled feature type.");
        }

    }

    public boolean isDrawn(Polygon p) {
        return getVectorFor(p) != null;
    }

    public Vector getVectorFor(Geometry g) {
        Iterator<Component> componentIterator = vectorLayer
                .getComponentIterator();
        while (componentIterator.hasNext()) {
            AbstractComponent next = (AbstractComponent) componentIterator
                    .next();
            if (next.getData() == g) {
                return (Vector) next;
            }
        }
        return null;
    }

    public void showFeature(Polygon p, String styleUrl) {
        if (isDrawn(p)) {
            return;
        }
        List<Coordinate> coordinates = (p).getOuterBoundaryIs().getLinearRing()
                .getCoordinates();
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
            if(styleUrl.startsWith("#")) {
            	styleUrl = styleUrl.substring(1);
            }
            area.setRenderIntent(styleUrl);
            vectorLayer.addComponent(area);
        }
    }

    public void showFeature(Geometry p, String styleUrl) {
        if (p instanceof Polygon) {
            showFeature((Polygon) p, styleUrl);
        }

    }

    public void clear() {
        vectorLayer.removeAllComponents();
        vectorLayer.requestRepaint();
    }

    public void vectorModified(VectorModifiedEvent event) {
        Vector vector = event.getVector();
        if (vector instanceof Area) {
            /*
             * An area has been modified, update data to backing model object.
             */
            Point[] points = ((Area) vector).getPoints();
            Polygon data = (Polygon) vector.getData();
            LinearRing linearRing = data.getOuterBoundaryIs().getLinearRing();
            List<Coordinate> coordinates2 = linearRing.getCoordinates();
            coordinates2.clear();
            for (Point p : points) {
                coordinates2.add(new Coordinate(p.getLon(), p.getLat(),
                        METERS_ABOVE_GROUND));
            }
        }
    }

    public void vectorSelected(VectorSelectedEvent event) {
        Vector vector = event.getVector();
        documentView.selectFeatureByGeometry((Geometry) vector.getData());
    }

    public void showAllVectors() {
        Iterator<Component> componentIterator = vectorLayer
                .getComponentIterator();
        Bounds bounds = new Bounds();
        boolean emptyBounds = true;
        while (componentIterator.hasNext()) {
            Vector vector = (Vector) componentIterator.next();
            bounds.extend(vector.getPoints());
            if (emptyBounds) {
                if (vector.getPoints().length < 2) {
                    bounds.extend(vector.getPoints());
                }
                emptyBounds = false;
            }
        }
        if (!emptyBounds) {
            zoomToExtent(bounds);
        }

    }

    public void setStyleMap(StyleMap styleMap) {
        vectorLayer.setStyleMap(styleMap);
    }

	public void setModifiable(boolean modifiable) {
		if(modifiable) {
			vectorLayer.setDrawindMode(DrawingMode.MODIFY);
		} else {
			vectorLayer.setDrawindMode(DrawingMode.NONE);
		}
		this.modifiable = modifiable;
	}

}
