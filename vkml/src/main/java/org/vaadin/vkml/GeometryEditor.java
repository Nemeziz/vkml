package org.vaadin.vkml;

import java.util.List;

import org.vaadin.vol.Area;
import org.vaadin.vol.Bounds;
import org.vaadin.vol.Point;
import org.vaadin.vol.Vector;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.Label;

import de.micromata.opengis.kml.v_2_2_0.Boundary;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.LinearRing;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

public class GeometryEditor extends CssLayout implements ClickListener,
        FeatureDrawnCallback {

    private Button draw;
    private Label label;
    private List<Coordinate> coordinates;
    private FeatureMap map;
    private Polygon polygon;
    private Placemark placemark;

    public GeometryEditor(Placemark placemark, DocumentView owner) {
        this.placemark = placemark;
        map = owner.getMap();
        setCaption("Geometry");
        label = new Label();
        addComponent(label);
        draw = new Button("Redraw", this);
        addComponent(draw);

        if (placemark.getGeometry() instanceof Polygon) {
            polygon = (Polygon) placemark.getGeometry();
            Boundary outerBoundary = polygon.getOuterBoundaryIs();
            LinearRing ring = outerBoundary.getLinearRing();
            coordinates = ring.getCoordinates();

            if (!ring.getCoordinates().isEmpty()) {
                updateCoordinateLabel();
                Bounds bounds = new Bounds();
                for (Coordinate c : coordinates) {
                    bounds.extend(new Point(c.getLongitude(), c.getLatitude()));
                }
                owner.getMap().zoomToExtent(bounds);

                map.showFeature(polygon, placemark.getStyleUrl());
            } else {
                // new component, draw mode by default
                drawFeature();
            }

        } else {
            addComponent(new Label("Geometry type not supported"
                    + placemark.getClass()));
        }
    }

    private void updateCoordinateLabel() {
        label.setValue(coordinates.size() + " points.");
    }

    public void buttonClick(ClickEvent event) {
        if (event.getButton() == draw) {
            drawFeature();
        }
    }

    private void drawFeature() {
        map.drawFeature(this);
        draw.setVisible(false);
        label.setValue("Drawing...");
    }

    public void drawingDone(Vector vector) {
        if (vector instanceof Area) {
            Area area = (Area) vector;
            coordinates.clear();
            Point[] points = area.getPoints();
            orderCounterClockwise(points);
            for (Point point : points) {
                /*
                 * Note that we intentionally place overlays 30 meter above
                 * ground. This way above base layers in google earth (probably
                 * bugs if there are lots of altitude differences and only few
                 * points in the area (or large area).
                 * 
                 * TODO check whats up with clamped to ground option. Last time
                 * I tried there was an issue in GE when using both a layer from
                 * maptiler and a kml areas.
                 */
                Coordinate coordinate = new Coordinate(point.getLon(),
                        point.getLat(), FeatureMap.METERS_ABOVE_GROUND);
                coordinates.add(coordinate);
            }
            // ensure first coord is the same as last as specified by kml
            if (!coordinates.get(0).equals(
                    coordinates.get(coordinates.size() - 1))) {
                coordinates.add(coordinates.get(0).clone());
            }
            updateCoordinateLabel();
            map.showFeature(polygon, placemark.getStyleUrl());
            draw.setVisible(true);
            draw.setCaption("Redraw");
        } else {
            System.err.println("Non supported vector drawn:"
                    + vector.getClass());
        }
    }

    private void orderCounterClockwise(Point[] points) {
        boolean isCCW = false;

        int highestPoint = 0;
        for (int i = 0; i < points.length; i++) {
            if (points[i].getLat() > points[highestPoint].getLat()) {
                highestPoint = i;
            }
        }

        Point next = points[(points.length + highestPoint - 1) % points.length];
        Point prev = points[(highestPoint + 1) % points.length];
        isCCW = prev.getLon() < next.getLon();
        if (!isCCW) {
            for (int i = 0; i < points.length / 2; i++) {
                Point point = points[points.length - 1 - i];
                points[points.length -1 - i] = points[i];
                points[i] = point;
            }
        }

    }

}
